package com.tezos.ui.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BulletSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.tezos.core.crypto.Base58
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.DataExtractor
import com.tezos.core.utils.MultisigBinaries
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.database.MultisigOperation
import com.tezos.ui.database.Signatory
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.fragment.ScriptFragment.Companion.CONTRACT_PUBLIC_KEY
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.dialog_ongoing_multisig.*
import kotlinx.android.synthetic.main.multisig_ongoing_proposal_signatories.*
import kotlinx.android.synthetic.main.multisig_ongoing_signatories.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.interfaces.ECPublicKey

class OngoingMultisigDialogFragment : AppCompatDialogFragment()
{
    private var listener: OnOngoinMultisigDialogInteractionListener? = null

    private var mClickCalculate:Boolean = false

    private var mTransferPayload:String? = null
    private var mStorageInfoLoading:Boolean = false

    private var mSignaturesLoading:Boolean = false

    private var mFinalizeTransferLoading:Boolean = false

    private var mServerOperation:MultisigOperation? = null

    private var mContract:Contract? = null

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var operationDatabase: DatabaseReference

    data class Contract
    (
            val blk: String,
            val spendable: Boolean,
            val delegatable: Boolean,
            val delegate: String?,
            val script: String,
            val storage: String
    )

    internal class ContractSerialization internal constructor(private val contract: Contract)
    {
        internal fun getSerializedBundle():Bundle
        {
            val contractBundle = Bundle()

            contractBundle.putString("blk", contract.blk)
            contractBundle.putBoolean("spendable", contract.spendable)
            contractBundle.putBoolean("delegatable", contract.delegatable)
            contractBundle.putString("delegate", contract.delegate)
            contractBundle.putString("script", contract.script)
            contractBundle.putString("storage", contract.storage)

            return contractBundle
        }
    }

    internal class ContractMapper internal constructor(private val bundle: Bundle)
    {
        internal fun mappedObjectFromBundle(): Contract
        {
            val blk = this.bundle.getString("blk", null)
            val spendable = this.bundle.getBoolean("spendable", false)
            val delegatable = this.bundle.getBoolean("delegatable", false)
            val delegate = this.bundle.getString("delegate", null)
            val script = this.bundle.getString("script", null)
            val storage = this.bundle.getString("storage", null)

            return Contract(blk, spendable, delegatable, delegate, script, storage)
        }
    }

    private fun toContractBundle(contract: Contract?): Bundle?
    {
        if (contract != null)
        {
            val serializer = ContractSerialization(contract)
            return serializer.getSerializedBundle()
        }
        return null
    }

    private fun fromContractBundle(bundle: Bundle): Contract
    {
        val mapper = ContractMapper(bundle)
        return mapper.mappedObjectFromBundle()
    }

    private var mSignatoriesList:ArrayList<String> = ArrayList(SIGNATORIES_CAPACITY)

    interface OnOngoinMultisigDialogInteractionListener
    {
        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean
        fun saveFingerprintAllowed(useInFuture: Boolean)

        fun onSigSentSucceed()
    }

    companion object
    {
        const val TAG = "ongoing_multisig_dialog_fragment"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"
        private const val TRANSFER_PAYLOAD_KEY = "transfer_payload_key"

        private const val LOAD_SIGNATURES_TAG = "load_signatures"

        private const val LOAD_STORAGE_TAG = "load_storage"
        private const val TRANSFER_FINALIZE_TAG = "transfer_finalize"

        private const val SERVER_OPERATION_KEY = "server_operation_key"

        private const val CONTRACT_DATA_KEY = "contract_info_key"

        private const val SIGNATORIES_CAPACITY = 10

        private const val SIGNATORIES_LIST_KEY = "signatories_list"

        private const val ONGOING_OPERATION_KEY = "ongoing_operation"

        private const val FROM_NOTARY = "is_from_notary"

        @JvmStatic
        fun newInstance(operation: MultisigOperation, isFromNotary: Boolean) =
                OngoingMultisigDialogFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(ONGOING_OPERATION_KEY, operation.toBundle())
                        putBoolean(FROM_NOTARY, isFromNotary)
                    }
                }
    }



    /*
    private fun postComment()
    {
        //val uid = uid
        FirebaseDatabase.getInstance().reference.child("users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // Get user information
                        val user = dataSnapshot.getValue(User::class.java)
                        if (user == null) {
                            return
                        }

                        val authorName = user.username

                        // Create new comment object
                        val commentText = fieldCommentText.text.toString()
                        val comment = Comment(uid, authorName, commentText)

                        // Push the comment, it will appear in the list
                        commentsReference.push().setValue(comment)

                        // Clear the field
                        fieldCommentText.text = null
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                })
    }
    */

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme)
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnOngoinMultisigDialogInteractionListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException("$context must implement OnGoingMultisigInteractionListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        decline_button_layout.setOnClickListener {
            onSendClick()
        }

        decline_button_layout.setOnClickListener {
            onSendClick()
        }

        confirm_operation_multisig_button_layout.setOnClickListener {

            val opBundle = arguments!!.getBundle(ONGOING_OPERATION_KEY)

            val mnemonicsData = Storage(activity!!).getMnemonics()

            val url = getString(R.string.transfer_forge)

            var postParams = JSONObject()
            postParams.put("src", mnemonicsData.pkh)
            postParams.put("src_pk", mnemonicsData.pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()

            val op = MultisigOperation.fromBundle(opBundle)
            val binaryReader = MultisigBinaries(op.binary)
            binaryReader.getType()
            dstObject.put("dst", binaryReader.getContractAddress())

            dstObject.put("amount", "0")

            val spendingLimitFile = "multisig_set_delegate.json"
            val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                    .use { it ->
                        it.readText()
                    }

            val value = JSONObject(contract)

            val sigs = (value["args"] as JSONArray)[1] as JSONArray
            sigs.remove(0)

            val list = getSignatoriesList()

            val signatures = mServerOperation?.signatures
            val orderedSignatures = ArrayList<String>(signatures!!.count())
            for (pk in list)
            {
                orderedSignatures.add(signatures[pk]!!)
            }

            for (sig in orderedSignatures)
            {
                val sigParam = JSONObject()

                if (!sig.isNullOrEmpty())
                {
                    sigParam.put("prim", "Some")

                    val argSig = JSONArray()
                    val argSigStr = JSONObject()

                    argSigStr.put("string", sig)
                    argSig.put(argSigStr)
                    sigParam.put("args", argSig)
                }
                else
                {
                    sigParam.put("prim", "None")
                }

                sigs.put(sigParam)
            }

            val argCounter = (((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject
            argCounter.put("int", getMultisigCounter())

            val argBaker = (((((((((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject
            //argBaker.put("bytes", getMultisigCounter())

            var decodedValue = Base58.decode(binaryReader.getBaker())
            var bakerBytes = decodedValue.slice(3 until (decodedValue.size - 4)).toByteArray()
            bakerBytes = byteArrayOf(0x00) + bakerBytes
            argBaker.put("bytes", bakerBytes.toNoPrefixHexString())

            dstObject.put("parameters", value)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

            val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
            { answer ->

                if (swipe_refresh_multisig_dialog_layout != null)
                {
                    val k = ""
                    val k2 = ""


                    /*
                    mDelegatePayload = answer.getString("result")
                    mDelegateFees = answer.getLong("total_fee")

                    // we use this call to ask for payload and fees
                    if (mDelegatePayload != null && mDelegateFees != -1L && activity != null)
                    {
                        onInitDelegateLoadComplete(null)

                        val feeInTez = mDelegateFees.toDouble()/1000000.0
                        fee_edittext?.setText(feeInTez.toString())

                        validateAddButton(isInputDataValid() && isDelegateFeeValid())

                    }
                    else
                    {
                        val volleyError = VolleyError(getString(R.string.generic_error))
                        onInitDelegateLoadComplete(volleyError)
                        mClickCalculate = true

                        //the call failed
                    }
                    */
                }

            }, Response.ErrorListener
            {
                if (swipe_refresh_multisig_dialog_layout != null)
                {
                    //onInitDelegateLoadComplete(it)
                    val k = ""
                    val k2 = ""
                    //mClickCalculate = true
                }
            })
            {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String>
                {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }

            cancelRequests(true)

            jsObjRequest.tag = "hello"
            //mInitDelegateLoading = true
            VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)

        }

        accept_button_layout.setOnClickListener {

            arguments?.let {
                val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
                val op = MultisigOperation.fromBundle(opBundle)

                val mnemonicsData = Storage(activity!!).getMnemonics()
                val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                val sk = CryptoUtils.generateSk(mnemonics, "")

                val signature = KeyPair.sign(sk, op.binary.hexToByteArray())

                mServerOperation!!.signatures[mnemonicsData.pk] = CryptoUtils.generateEDSig(signature)

                val binaryReader = MultisigBinaries(op.binary)
                binaryReader.getType()

                val childUpdates = HashMap<String, Any>()
                childUpdates["/multisig_operations/${binaryReader.getContractAddress()}"] = mServerOperation!!.toMap()

                val signatures = mServerOperation?.signatures
                val keys = ArrayList(signatures?.keys)

                for (pk in keys)
                {
                    childUpdates["/signatory-operations/$pk/${binaryReader.getContractAddress()}"] = mServerOperation!!.toMap()
                }

                mDatabaseReference = FirebaseDatabase.getInstance().reference
                mDatabaseReference.updateChildren(childUpdates)
                        .addOnSuccessListener {

                            val v = "hello world"
                            val v2 = "hello world"
                            //writeNewSignatory()

                        }
                        .addOnFailureListener {

                            val v = "hello world"
                            val v2 = "hello world"
                        }
            }
        }

        close_button.setOnClickListener {
            dismiss()
        }

        swipe_refresh_multisig_dialog_layout.setOnRefreshListener {

            arguments?.let {
                startInitContractInfoLoading()
            }
        }

        if (savedInstanceState != null)
        {
            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mTransferPayload = savedInstanceState.getString(TRANSFER_PAYLOAD_KEY, null)

            mStorageInfoLoading = savedInstanceState.getBoolean(LOAD_STORAGE_TAG)

            mSignaturesLoading = savedInstanceState.getBoolean(LOAD_SIGNATURES_TAG)

            mFinalizeTransferLoading = savedInstanceState.getBoolean(TRANSFER_FINALIZE_TAG)

            val serverOperationBundle = savedInstanceState.getBundle(SERVER_OPERATION_KEY)
            if (serverOperationBundle != null)
            {
                mServerOperation = MultisigOperation.fromBundle(serverOperationBundle)
            }

            val contractBundle = savedInstanceState.getBundle(CONTRACT_DATA_KEY)
            if (contractBundle != null)
            {
                mContract = this.fromContractBundle(contractBundle)
            }

            mSignatoriesList = savedInstanceState.getStringArrayList(SIGNATORIES_LIST_KEY)

            if (mStorageInfoLoading)
            {
                arguments?.let {
                    startInitContractInfoLoading()
                }
            }
            else
            {
                onStorageInfoComplete(error = null)

                if (mSignaturesLoading)
                {
                    startInitSignaturesInfoLoading()
                }
                else
                {
                    onSignaturesInfoComplete(error = null, databaseError = null)
                }
            }
        }
        else
        {
            refreshTextsAndLayouts()

            arguments?.let {
                startInitContractInfoLoading()
            }
        }

        val ss = SpannableString(" ")
        ss.setSpan(BulletSpan(8, ContextCompat.getColor(activity!!, R.color.colorAccent)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        bullet_textview_01.text = ss
        bullet_textview_02.text = ss
        bullet_textview_03.text = ss
        bullet_textview_04.text = ss
        bullet_textview_05.text = ss
        bullet_textview_06.text = ss
        bullet_textview_07.text = ss
        bullet_textview_08.text = ss
        bullet_textview_09.text = ss
        bullet_textview_10.text = ss

        bullet_proposal_textview_01.text = ss
        bullet_proposal_textview_02.text = ss
        bullet_proposal_textview_03.text = ss
        bullet_proposal_textview_04.text = ss
        bullet_proposal_textview_05.text = ss
        bullet_proposal_textview_06.text = ss
        bullet_proposal_textview_07.text = ss
        bullet_proposal_textview_08.text = ss
        bullet_proposal_textview_09.text = ss
        bullet_proposal_textview_10.text = ss
    }

    override fun onResume()
    {
        super.onResume()
        validateAcceptDeclineButtons(areButtonsValid())
    }

    /*
    enum class CONTRACT_INFO_TYPE
    {
        CONTRACT_STORAGE, CONTRACT_INFO
    }
    */

    private fun onSendClick()
    {
        val dialog = AuthenticationDialog()
        if (listener?.isFingerprintAllowed()!! && listener?.hasEnrolledFingerprints()!!)
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices().prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = {
                validateKeyAuthentication(it)
            }
            if (dialog.cryptoObjectToAuthenticateWith == null)
            {
                dialog.stage = AuthenticationDialog.Stage.NEW_FINGERPRINT_ENROLLED
            }
            else
            {
                dialog.stage = AuthenticationDialog.Stage.FINGERPRINT
            }
        }
        else
        {
            dialog.stage = AuthenticationDialog.Stage.PASSWORD
        }
        dialog.authenticationSuccessListener = {
            startFinalizeTransferLoading()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity?.supportFragmentManager, "Authentication")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.dialog_ongoing_multisig, container, false)
    }

    private fun startInitContractInfoLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        //TODO need to handle transfers from KT1 or from tz1

        validateAcceptDeclineButtons(validate = false)

        arguments?.let {
            val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(opBundle)

            val binaryReader = MultisigBinaries(op.binary)
            startGetRequestLoadContractInfo(binaryReader.getType())
        }
    }

    private fun startInitSignaturesInfoLoading()
    {
        transferLoading(true)

        validateAcceptDeclineButtons(validate = false)

        startGetRequestLoadSignatures()
    }

    private fun startGetRequestLoadSignatures()
    {
        cancelRequests(resetBooleans = true)

        mSignaturesLoading = true

        arguments?.let {
            val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(opBundle)

            val binaryReader = MultisigBinaries(op.binary)
            binaryReader.getType()

            val postListener = object : ValueEventListener
            {
                override fun onDataChange(dataSnapshot: DataSnapshot)
                {
                    addMultisigOngoingOperationsFromJSON(dataSnapshot)
                    onSignaturesInfoComplete(error = null, databaseError = null)
                }

                override fun onCancelled(databaseError: DatabaseError)
                {
                    onSignaturesInfoComplete(error = null, databaseError = databaseError)
                }
            }
            //notaryOperationsDatabase.addValueEventListener(postListener)

            // Initialize Database

            val seed = Storage(activity!!).getMnemonics()

            operationDatabase = FirebaseDatabase.getInstance().reference
                    .child("signatory-operations").child(seed.pk).child(binaryReader.getContractAddress()!!)
            operationDatabase.addListenerForSingleValueEvent(postListener)














            /*
            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener
            {o ->

                //prevents from async crashes
                if (dialogRootView != null)
                {
                    addContractStorageBisFromJSON(o)
                    onSignaturesInfoComplete(error = null)
                }
            },
                    Response.ErrorListener {

                        if (dialogRootView != null)
                        {
                            onSignaturesInfoComplete(error = it)
                        }
                    })

            jsonArrayRequest.tag = LOAD_SIGNATURES_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
            */
        }
    }

    private fun startFinalizeTransferLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        startPostRequestLoadFinalizeTransfer()
    }

    // REQUESTS

    // volley
    private fun startPostRequestLoadFinalizeTransfer()
    {
        val url = getString(R.string.transfer_injection_operation)

        val seed = Storage(activity!!).getMnemonics()

        //TODO we got to verify at this very moment.
        if (areButtonsValid() && mTransferPayload != null)
        {
            var postParams = JSONObject()
            var dstObjects = JSONArray()
            var dstObject = JSONObject()

            if (true)
            {
                val ecKeys = retrieveECKeys()
                val p2pk = CryptoUtils.generateP2Pk(ecKeys)

                postParams.put("src_pk", p2pk)

                val tz3 = CryptoUtils.generatePkhTz3(ecKeys)
                postParams.put("src", tz3)

                val kt1 = arguments!!.getString(CONTRACT_PUBLIC_KEY)
                dstObject.put("dst", kt1)

                dstObject.put("dst_account", tz3)

                dstObject.put("amount", 0.toLong())
                dstObject.put("contract_type", "slc_enclave_transfer")

                //0.1 tez == 100 000 mutez
                dstObject.put("transfer_amount", "100000".toLong())
            }
            else
            {
                val mnemonicsData = Storage(activity!!).getMnemonics()

                postParams.put("src", mnemonicsData.pkh)
                postParams.put("src_pk", mnemonicsData.pk)

                dstObject.put("dst", retrieveTz3())

                dstObject.put("amount", "100000".toLong())

            }

            dstObjects.put(dstObject)
            postParams.put("dsts", dstObjects)

            if (isTransferPayloadValid(mTransferPayload!!, postParams))
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mTransferPayload!!.hexToByteArray()

                val xLen = zeroThree.size
                val yLen = byteArrayThree.size
                val result = ByteArray(xLen + yLen)

                System.arraycopy(zeroThree, 0, result, 0, xLen)
                System.arraycopy(byteArrayThree, 0, result, xLen, yLen)

                var compressedSignature: ByteArray
                if (true)
                {
                    val bytes = KeyPair.b2b(result)
                    var signature = EncryptionServices().sign(bytes)
                    compressedSignature = compressFormat(signature)
                }
                else
                {
                    val mnemonics = EncryptionServices().decrypt(seed.mnemonics)
                    val sk = CryptoUtils.generateSk(mnemonics, "")
                    compressedSignature = KeyPair.sign(sk, result)
                }

                val pLen = byteArrayThree.size
                val sLen = compressedSignature.size
                val newResult = ByteArray(pLen + sLen)

                System.arraycopy(byteArrayThree, 0, newResult, 0, pLen)
                System.arraycopy(compressedSignature, 0, newResult, pLen, sLen)

                var payloadsign = newResult.toNoPrefixHexString()

                val stringRequest = object : StringRequest(Method.POST, url,
                        Response.Listener<String> {

                            if (dialogRootView != null)
                            {
                                onFinalizeTransferLoadComplete(null)
                                dismiss()
                                listener?.onSigSentSucceed()
                            }
                        },
                        Response.ErrorListener
                        {
                            if (dialogRootView != null)
                            {
                                onFinalizeTransferLoadComplete(it)
                            }
                        }
                )
                {
                    @Throws(AuthFailureError::class)
                    override fun getBody(): ByteArray
                    {
                        val pay = "\""+payloadsign+"\""
                        return pay.toByteArray()
                    }

                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String>
                    {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = "application/json"
                        return headers
                    }
                }

                cancelRequests(true)

                stringRequest.tag = TRANSFER_FINALIZE_TAG

                mFinalizeTransferLoading = true
                VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(stringRequest)
            }
            else
            {
                val volleyError = VolleyError(getString(R.string.generic_error))
                onFinalizeTransferLoadComplete(volleyError)
            }
        }
        else
        {
            val volleyError = VolleyError(getString(R.string.generic_error))
            onFinalizeTransferLoadComplete(volleyError)
        }
    }

    enum class CONTRACT_INFO_TYPE
    {
        CONTRACT_STORAGE, CONTRACT_INFO
    }

    // volley
    private fun startGetRequestLoadContractInfo(operationType:MultisigBinaries.Companion.MULTISIG_BINARY_TYPE?)
    {
        cancelRequests(resetBooleans = true)

        mStorageInfoLoading = true

        arguments?.let { bundle ->

            val operationBundle = bundle.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(operationBundle)

            val binaryReader = MultisigBinaries(op.binary)
            binaryReader.getType()
            val url = String.format(getString(R.string.contract_info2_url), binaryReader.getContractAddress())

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener
            {o ->

                //prevents from async crashes
                if (dialogRootView != null)
                {
                    addContractInfoFromJSON(o)
                    onStorageInfoComplete(error = null)

                    arguments?.let {

                        if (it.getBoolean(FROM_NOTARY))
                        {
                            startInitSignaturesInfoLoading()
                        }
                    }
                }
            },
                    Response.ErrorListener {

                        if (dialogRootView != null)
                        {
                            onStorageInfoComplete(error = it)
                            mClickCalculate = true
                        }
                    })

            jsonArrayRequest.tag = LOAD_STORAGE_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    private fun addMultisigOngoingOperationsFromJSON(answer:DataSnapshot)
    {
        if (answer.exists() && answer.hasChildren())
        {
            mServerOperation = MultisigOperation.fromMap(answer.value as HashMap<String, Any>)
        }
    }

    private fun addContractInfoFromJSON(answer: JSONArray)
    {
        if (answer.length() > 0)
        {
            val contractJSON = DataExtractor.getJSONObjectFromField(answer,0)

            val blk = DataExtractor.getStringFromField(contractJSON, "blk")
            val spendable = DataExtractor.getBooleanFromField(contractJSON, "spendable")
            val delegatable = DataExtractor.getBooleanFromField(contractJSON, "delegatable")
            val delegate = DataExtractor.getStringFromField(contractJSON, "delegate")
            val script = DataExtractor.getJSONObjectFromField(contractJSON, "script")

            val storage = DataExtractor.getJSONObjectFromField(contractJSON, "storage")

            mContract = Contract(blk as String, spendable as Boolean, delegatable as Boolean, delegate, script.toString(), storage.toString())
        }
    }

    private fun getThreshold(): String?
    {
        if (mContract != null && !mContract?.storage.isNullOrEmpty())
        {
            val storageJSONObject = JSONObject(mContract!!.storage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")
            if (args != null)
            {
                val counter = DataExtractor.getStringFromField(args[0] as JSONObject, "int")
                if (counter != null)
                {
                    val argsPk = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray
                    return DataExtractor.getStringFromField(argsPk[0] as JSONObject, "int")
                }
            }
        }

        return null
    }

    private fun getMultisigCounter(): String?
    {
        if (mContract != null && !mContract?.storage.isNullOrEmpty())
        {
            val storageJSONObject = JSONObject(mContract!!.storage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args") as JSONArray

            return DataExtractor.getStringFromField(args[0] as JSONObject, "int")
        }

        return null
    }

    private fun refreshSignatories()
    {
        mSignatoriesList = getSignatoriesList()

        val signatoryLayouts = listOf<LinearLayout>(
                signatory_layout_01,
                signatory_layout_02,
                signatory_layout_03,
                signatory_layout_04,
                signatory_layout_05,
                signatory_layout_06,
                signatory_layout_07,
                signatory_layout_08,
                signatory_layout_09,
                signatory_layout_10
        )

        val bulletEditTexts = listOf<TextView>(
                bullet_address_edittext_01,
                bullet_address_edittext_02,
                bullet_address_edittext_03,
                bullet_address_edittext_04,
                bullet_address_edittext_05,
                bullet_address_edittext_06,
                bullet_address_edittext_07,
                bullet_address_edittext_08,
                bullet_address_edittext_09,
                bullet_address_edittext_10
        )

        if (!mSignatoriesList.isNullOrEmpty())
        {
            val length = mSignatoriesList.size

            for (i in 0 until length)
            {
                bulletEditTexts[i].text = mSignatoriesList[i]
                signatoryLayouts[i].visibility = View.VISIBLE
            }

            for (i in length until SIGNATORIES_CAPACITY)
            {
                bulletEditTexts[i].text = getString(R.string.neutral)
                signatoryLayouts[i].visibility = View.GONE
            }
        }
        else
        {
            for (it in signatoryLayouts)
            {
                it.visibility = View.GONE
            }
        }
    }

    private fun refreshProposalSignatories(sigList:ArrayList<String>?)
    {
        if (!sigList.isNullOrEmpty())
        {
            val signatoryLayouts = listOf<LinearLayout>(
                    signatory_proposal_layout_01,
                    signatory_proposal_layout_02,
                    signatory_proposal_layout_03,
                    signatory_proposal_layout_04,
                    signatory_proposal_layout_05,
                    signatory_proposal_layout_06,
                    signatory_proposal_layout_07,
                    signatory_proposal_layout_08,
                    signatory_proposal_layout_09,
                    signatory_proposal_layout_10
            )

            val bulletEditTexts = listOf<TextView>(
                    bullet_address_proposal_edittext_01,
                    bullet_address_proposal_edittext_02,
                    bullet_address_proposal_edittext_03,
                    bullet_address_proposal_edittext_04,
                    bullet_address_proposal_edittext_05,
                    bullet_address_proposal_edittext_06,
                    bullet_address_proposal_edittext_07,
                    bullet_address_proposal_edittext_08,
                    bullet_address_proposal_edittext_09,
                    bullet_address_proposal_edittext_10
            )

            if (!sigList.isNullOrEmpty())
            {
                val length = sigList.size

                for (i in 0 until length)
                {
                    bulletEditTexts[i].text = sigList[i]
                    signatoryLayouts[i].visibility = View.VISIBLE
                }

                for (i in length until SIGNATORIES_CAPACITY)
                {
                    bulletEditTexts[i].text = getString(R.string.neutral)
                    signatoryLayouts[i].visibility = View.GONE
                }
            }
            else
            {
                for (it in signatoryLayouts)
                {
                    it.visibility = View.GONE
                }
            }
        }
    }

    private fun getSignatoriesList(): ArrayList<String>
    {
        if (mContract != null)
        {
            val storageJSONObject = JSONObject(mContract?.storage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args") as JSONArray

            val counter = DataExtractor.getStringFromField(args[0] as JSONObject, "int")
            if (counter != null)
            {
                val argsPk = (DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray)[1] as JSONArray

                val list = ArrayList<String> ()

                for (it in 0 until argsPk.length())
                {
                    val item = argsPk.getJSONObject(it)

                    val hexPk = DataExtractor.getStringFromField(item, "bytes")
                    if (!hexPk.isNullOrEmpty())
                    {
                        val bytes = hexPk.hexToByteArray()
                        val hashPublicKey = bytes.slice(1 until bytes.size).toByteArray()
                        val el = CryptoUtils.genericHashToPk(hashPublicKey)

                        list.add(el)
                    }
                    else
                    {
                        val pk = DataExtractor.getStringFromField(item, "string")
                        list.add(pk)
                    }
                }

                return list
            }
        }

        return ArrayList(SIGNATORIES_CAPACITY)
    }

    private fun getNumberAndSpot(publicKey:String): Pair<Int, Int>
    {
        val signatories = getSignatoriesList()
        if (!signatories.isNullOrEmpty())
        {
            return Pair(signatories.indexOf(publicKey), signatories.size)
        }

        return Pair(-1, -1)
    }


    private fun onStorageInfoComplete(error:VolleyError?)
    {
        mStorageInfoLoading = false
        transferLoading(false)

        swipe_refresh_multisig_dialog_layout?.isEnabled = true
        swipe_refresh_multisig_dialog_layout?.isRefreshing = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            cancelRequests(resetBooleans = true)

            mTransferPayload = null

            /*
            fee_edittext?.isEnabled = true
            fee_edittext?.isFocusable = false
            fee_edittext?.isClickable = false
            fee_edittext?.isLongClickable = false
            fee_edittext?.hint = getString(R.string.click_for_fees)

            fee_edittext?.setOnClickListener {
                startInitContractInfoLoading()
            }
            */

            if(error != null)
            {
                showSnackBar(getString(R.string.generic_error), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
            }
        }
        else
        {
            cancelRequests(true)
        }

        //TODO check if necessary
        refreshTextsAndLayouts()
    }

    private fun onSignaturesInfoComplete(error:VolleyError?, databaseError: DatabaseError?)
    {
        mSignaturesLoading = false
        transferLoading(false)

        swipe_refresh_multisig_dialog_layout?.isEnabled = true
        swipe_refresh_multisig_dialog_layout?.isRefreshing = false

        if (error != null || databaseError != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            cancelRequests(resetBooleans = true)

            //mTransferPayload = null

            /*
            fee_edittext?.isEnabled = true
            fee_edittext?.isFocusable = false
            fee_edittext?.isClickable = false
            fee_edittext?.isLongClickable = false
            fee_edittext?.hint = getString(R.string.click_for_fees)

            fee_edittext?.setOnClickListener {
                startInitContractInfoLoading()
            }
            */

            when
            {
                error != null ->
                {
                    showSnackBar(error.toString(), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
                }

                databaseError != null ->
                {
                    showSnackBar(databaseError.toString(), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
                }

                else ->
                {
                    showSnackBar(getString(R.string.generic_error), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
                }
            }
        }
        else
        {
            cancelRequests(true)
        }

        arguments?.let {

            val fromNotary = it.getBoolean(FROM_NOTARY)

            val hasSignedTextview = listOf(
                    has_signature_textview_01,
                    has_signature_textview_02,
                    has_signature_textview_03,
                    has_signature_textview_04,
                    has_signature_textview_05,
                    has_signature_textview_06,
                    has_signature_textview_07,
                    has_signature_textview_08,
                    has_signature_textview_09,
                    has_signature_textview_10
            )

            if (mServerOperation != null)
            {
                val signatures = mServerOperation?.signatures
                val list = getSignatoriesList()

                val keys = ArrayList(signatures?.keys)

                if (list.containsAll(keys) && keys.containsAll(list))
                {
                    if (!list.isNullOrEmpty() && fromNotary)
                    {
                        val length = list.size

                        var confirmedSignatures = 0

                        for (i in 0 until length)
                        {
                            val pk = list[i]
                            hasSignedTextview[i].visibility = View.VISIBLE

                            if (signatures!![pk]!!.isNotEmpty())
                            {
                                hasSignedTextview[i].text = getString(R.string.has_signed_already)
                                confirmedSignatures++
                            }
                            else
                            {
                                hasSignedTextview[i].text = getString(R.string.has_not_signed_yet)
                            }
                        }

                        for (i in length until SIGNATORIES_CAPACITY) {
                            hasSignedTextview[i].visibility = View.GONE
                        }

                        can_confirm_textview.visibility = View.VISIBLE

                        val threshold = getThreshold()!!.toLong()
                        val necessarySignatures = threshold - confirmedSignatures

                        var text = if (threshold == necessarySignatures)
                        {
                            "This ongoing operation has no signature yet.\n"
                        }
                        else
                        {
                            if (confirmedSignatures == 1)
                            {
                                "This ongoing operation already has $confirmedSignatures signature.\n"
                            }
                            else
                            {
                                "This ongoing operation already has $confirmedSignatures signatures.\n"
                            }
                        }

                        val text2 = if (necessarySignatures == 1L)
                        {
                            if (threshold == necessarySignatures)
                            {
                                "It needs $necessarySignatures signature before it can be confirmed by the notary."
                            }
                            else
                            {
                                "It needs $necessarySignatures more signature before it can be confirmed by the notary."
                            }
                        }
                        else if (necessarySignatures >1L)
                        {
                            if (threshold == necessarySignatures)
                            {
                                "It needs $necessarySignatures signatures before it can be confirmed by the notary."
                            }
                            else
                            {
                                "It needs $necessarySignatures more signatures before it can be confirmed by the notary."
                            }
                        }
                        else
                        {
                            "It has enough signatures to be confirmed by the notary."
                        }

                        can_confirm_textview.text = text + text2

                    }
                    else
                    {
                        for (it in hasSignedTextview)
                        {
                            it.visibility = View.GONE
                        }
                    }
                }
                else
                {
                    // the operation is different than storage. cancel the operation.
                    val k = ""
                    val k2 = ""
                    //TODO cancel the operation
                }
            }
        }

        validateAcceptDeclineButtons(areButtonsValid())

        refreshTextsAndLayouts()
    }

    private fun refreshTextsAndLayouts()
    {
        arguments?.let {

            val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(opBundle)

            submission_item_date.text = op.timestamp.toString()

            val binaryReader = MultisigBinaries(op.binary)

            when (binaryReader.getType())
            {
                MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UPDATE_SIGNATORIES ->
                {
                    threshold_proposal_edittext.setText(binaryReader.getThreshold().toString())
                    refreshProposalSignatories(binaryReader.getSignatories())

                    contract_address_item.text = binaryReader.getContractAddress()
                    operation_type_item.text = binaryReader.getOperationTypeString()

                    if (mContract != null)
                    {
                        if (getThreshold() != null)
                        {
                            refreshSignatories()

                            update_signatories_layout.visibility = View.VISIBLE
                            storage_proposal_layout.visibility = View.VISIBLE

                            no_baker_textview.visibility = View.GONE

                            threshold_edittext.setText(getThreshold())

                            //TODO notary
                            //notary_tz1_edittext.setText(mContract.delegate)

                            validateAcceptDeclineButtons(areButtonsValid())
                        }
                    }
                }

                MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.SET_DELEGATE ->
                {
                    contract_address_item.text = binaryReader.getContractAddress()
                    operation_type_item.text = binaryReader.getOperationTypeString()

                    if (mContract != null)
                    {
                        val fromNotary = it.getBoolean(FROM_NOTARY)
                        if (fromNotary)
                        {
                            refreshSignatories()
                            storage_proposal_layout.visibility = View.GONE
                            update_signatories_layout.visibility = View.VISIBLE
                        }

                        threshold_edittext.setText(getThreshold())

                        set_baker_layout.visibility = View.VISIBLE

                        remove_baker_textview.visibility = View.GONE
                        current_baker_textview.visibility = View.GONE

                        set_baker_edittext.setText(binaryReader.getBaker())

                        validateAcceptDeclineButtons(areButtonsValid())
                    }
                }

                MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UNDELEGATE ->
                {
                    contract_address_item.text = binaryReader.getContractAddress()
                    operation_type_item.text = binaryReader.getOperationTypeString()

                    if (mContract != null)
                    {
                        val fromNotary = it.getBoolean(FROM_NOTARY)
                        if (fromNotary)
                        {
                            refreshSignatories()
                            update_signatories_layout.visibility = View.VISIBLE
                            storage_proposal_layout.visibility = View.GONE
                        }

                        threshold_edittext.setText(getThreshold())

                        set_baker_layout.visibility = View.VISIBLE

                        set_baker_textview.visibility = View.GONE

                        no_baker_textview.visibility = View.GONE

                        current_baker_textview.setText(mContract?.delegate)

                        set_baker_edittext.visibility = View.GONE

                        validateAcceptDeclineButtons(areButtonsValid())
                    }
                }
            }
        }
    }

        /*
        private fun startPostRequestLoadInitTransfer(fromContract: Boolean)
        {
            val mnemonicsData = Storage(activity!!).getMnemonics()



            val url = String.format(getString(R.string.contract_storage_url), "KT1Gen5CXA9Uh5TQSGKtGYAptsZEbpCz7kKX")
            //val url = getString(R.string.transfer_forge)

            val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
            { answer ->

                if (rootView != null)
                {
                    mTransferPayload = answer.getString("result")
                    mTransferFees = answer.getLong("total_fee")

                    // we use this call to ask for payload and fees
                    if (mTransferPayload != null && mTransferFees != -1L)
                    {
                        onInitTransferLoadComplete(null)

                        val feeInTez = mTransferFees.toDouble()/1000000.0
                        fee_edittext?.setText(feeInTez.toString())

                        validateSendCentsButton(isTransferFeeValid())
                        setTextPayButton()
                    }
                    else
                    {
                        val volleyError = VolleyError(getString(R.string.generic_error))
                        onInitTransferLoadComplete(volleyError)
                        mClickCalculate = true

                        //the call failed
                    }
                }
            }, Response.ErrorListener
            {
                if (rootView != null)
                {
                    onInitTransferLoadComplete(it)

                    mClickCalculate = true
                    //Log.i("mTransferId", ""+mTransferId)
                    //Log.i("mDelegatePayload", ""+mDelegatePayload)
                }
            })
            {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String>
                {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }

            cancelRequests(true)

            jsObjRequest.tag = LOAD_STORAGE_TAG
            mStorageInfoLoading = true
            VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
        }
        */

    private fun updateMnemonicsData(data: Storage.MnemonicsData, pk:String):String
    {
        with(Storage(activity!!)) {
            saveSeed(Storage.MnemonicsData(data.pkh, pk, data.mnemonics))
        }
        return pk
    }

    /*
    private fun onInitTransferLoadComplete(error: VolleyError?)
    {
        mStorageInfoLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            mTransferPayload = null

            fee_edittext?.isEnabled = true
            fee_edittext?.isFocusable = false
            fee_edittext?.isClickable = false
            fee_edittext?.isLongClickable = false
            fee_edittext?.hint = getString(R.string.click_for_fees)

            fee_edittext?.setOnClickListener {
                startInitContractInfoLoading()
            }

            if(error != null)
            {
                //TODO handle the show snackbar
                showSnackBar(getString(R.string.generic_error), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
            }
        }
        else
        {
            transferLoading(false)
            cancelRequests(true)
            // it's signed, looks like it worked.
            //transferLoading(true)
        }
    }
    */

    private fun onFinalizeTransferLoadComplete(error: VolleyError?)
    {
        // everything is over, there's no call to make
        cancelRequests(true)

        if (error != null)
        {
            transferLoading(false)

            var err: String = error.toString()

            //showSnackBar(error, ContextCompat.getColor(this,
            //android.R.color.holo_red_light), null)
            //listener?.onTransferFailed(error)
            showSnackBar(err, ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }
        else
        {
            // the finish call is made already
        }
    }

    private fun retrieveECKeys():ByteArray
    {
        var keyPair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        val ecKey = keyPair!!.public as ECPublicKey
        return ecKeyFormat(ecKey)
    }

    fun showSnackBar(res:String, color:Int, textColor:Int)
    {
        val snackbar = Snackbar.make(dialogRootView, res, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(color)
        snackbar.setActionTextColor(textColor)
        snackbar.show()
    }

    private fun transferLoading(loading:Boolean)
    {
        // handle the visibility of bottom buttons

        if (loading)
        {
            nav_progress?.visibility = View.VISIBLE
        }
        else
        {
            nav_progress?.visibility = View.GONE
        }
    }

    private fun areButtonsValid():Boolean
    {
        arguments?.let {

            val fromNotary = it.getBoolean(FROM_NOTARY)
            if (fromNotary)
            {
                return (mContract != null && mServerOperation != null)
            }
            else
            {
                val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
                val op = MultisigOperation.fromBundle(opBundle)
                val binaryReader = MultisigBinaries(op.binary)

                when (binaryReader.getType())
                {
                    MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UPDATE_SIGNATORIES ->
                    {
                        if (!mContract?.storage.isNullOrEmpty())
                        {
                            return true
                        }
                    }

                    MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.SET_DELEGATE ->
                    {
                        if (mContract != null)
                        {
                            return true
                        }
                    }

                    MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UNDELEGATE ->
                    {
                        if (mContract != null)
                        {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun validateAcceptDeclineButtons(validate: Boolean)
    {
        //val bundleTheme = arguments!!.getBundle(CustomTheme.TAG)
        //val theme = CustomTheme.fromBundle(bundleTheme)

        //val theme = CustomTheme(R.color.colorPrimaryDark, R.color.colorPrimaryVeryDark, R.color.colorTitleText)

        if (validate)
        {
            val redTheme = CustomTheme(R.color.colorPrimaryDark, R.color.colorPrimaryVeryDark, R.color.colorTitleText)
            decline_button.setTextColor(ContextCompat.getColor(activity!!, redTheme.textColorPrimaryId))
            decline_button_layout.isEnabled = true
            decline_button_layout.background = makeSelector(redTheme)

            val declineDrawables = decline_button.compoundDrawables
            val declineWrapDrawable = DrawableCompat.wrap(declineDrawables[0])
            DrawableCompat.setTint(declineWrapDrawable, ContextCompat.getColor(activity!!, redTheme.textColorPrimaryId))


            val yellowTheme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)
            accept_button.setTextColor(ContextCompat.getColor(activity!!, yellowTheme.textColorPrimaryId))
            accept_button_layout.isEnabled = true
            accept_button_layout.background = makeSelector(yellowTheme)

            val acceptDrawables = decline_button.compoundDrawables
            val acceptWrapDrawable = DrawableCompat.wrap(acceptDrawables[0])
            DrawableCompat.setTint(acceptWrapDrawable, ContextCompat.getColor(activity!!, yellowTheme.textColorPrimaryId))
        }
        else
        {
            decline_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            decline_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            decline_button_layout.background = makeSelector(greyTheme)

            val declineDrawables = decline_button.compoundDrawables
            val declineWrapDrawable = DrawableCompat.wrap(declineDrawables[0])
            DrawableCompat.setTint(declineWrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))


            accept_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            accept_button_layout.isEnabled = false

            accept_button_layout.background = makeSelector(greyTheme)

            val drawables = decline_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
        }
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    private fun retrieveTz3():String?
    {
        val keyPair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        if (keyPair != null)
        {
            val ecKey = keyPair.public as ECPublicKey
            return CryptoUtils.generatePkhTz3(ecKeyFormat(ecKey))
        }

        return null
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)
        outState.putString(TRANSFER_PAYLOAD_KEY, mTransferPayload)

        outState.putBoolean(LOAD_STORAGE_TAG, mStorageInfoLoading)

        outState.putBoolean(LOAD_SIGNATURES_TAG, mSignaturesLoading)

        outState.putBoolean(TRANSFER_FINALIZE_TAG, mFinalizeTransferLoading)

        outState.putBundle(SERVER_OPERATION_KEY, mServerOperation?.toBundle())

        outState.putBundle(CONTRACT_DATA_KEY, toContractBundle(mContract))

        outState.putStringArrayList(SIGNATORIES_LIST_KEY, mSignatoriesList)
    }

    /**
     * Fingerprint was invalidated, decide what to do in this case.
     */
    private fun onFingerprintInvalidation(useInFuture: Boolean)
    {
        listener?.saveFingerprintAllowed(useInFuture)
        if (useInFuture)
        {
            EncryptionServices().createFingerprintKey()
        }
    }

    /**
     * Validate password inputted from Authentication Dialog.
     */
    private fun validatePassword(inputtedPassword: String): Boolean
    {
        val storage = Storage(activity!!)
        return EncryptionServices().decrypt(storage.getPassword()) == inputtedPassword
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices().validateFingerprintAuthentication(cryptoObject))
        {
            startFinalizeTransferLoading()
        }
        else
        {
            onSendClick()
        }
    }

    private fun cancelRequests(resetBooleans:Boolean)
    {
        if (activity != null)
        {
            val requestQueue = VolleySingleton.getInstance(activity!!.applicationContext).requestQueue
            requestQueue?.cancelAll(LOAD_STORAGE_TAG)
            requestQueue?.cancelAll(TRANSFER_FINALIZE_TAG)

            if (resetBooleans)
            {
                mStorageInfoLoading = false
                mFinalizeTransferLoading = false
                mSignaturesLoading = false
            }
        }
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests(false)
    }
}
