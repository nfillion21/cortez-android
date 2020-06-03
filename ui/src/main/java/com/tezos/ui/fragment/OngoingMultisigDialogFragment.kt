package com.tezos.ui.fragment

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BulletSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import com.tezos.core.models.Contract
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.DataExtractor
import com.tezos.core.utils.MultisigBinaries
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.database.MultisigOperation
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.dialog_ongoing_multisig.*
import kotlinx.android.synthetic.main.multisig_ongoing_proposal_signatories.*
import kotlinx.android.synthetic.main.multisig_ongoing_signatories.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

class OngoingMultisigDialogFragment : AppCompatDialogFragment()
{
    private var listener: OnOngoinMultisigDialogInteractionListener? = null

    private var mClickCalculate:Boolean = false

    private var mPayload:String? = null
    private var mFees:Long = -1L

    private var mStorageInfoLoading:Boolean = false

    private var mSignaturesLoading:Boolean = false
    private var mAcceptLoading:Boolean = false

    private var mRemoveOperationLoading:Boolean = false

    private var mInitTransferLoading:Boolean = false
    private var mFinalizeTransferLoading:Boolean = false

    private var mServerOperation:MultisigOperation? = null

    private var mContract: Contract? = null

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var operationDatabase: DatabaseReference

    private var mSignatoriesList:ArrayList<String> = ArrayList(SIGNATORIES_CAPACITY)

    interface OnOngoinMultisigDialogInteractionListener
    {
        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean
        fun saveFingerprintAllowed(useInFuture: Boolean)

        fun onMultisigOperationConfirmed()
        fun onMultisigOngoingOperationRemoved()
    }

    companion object
    {
        const val TAG = "ongoing_multisig_dialog_fragment"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private const val PAYLOAD_KEY = "payload_key"
        private const val OPERATION_FEES_KEY = "operation_fees_key"

        private const val LOAD_SIGNATURES_TAG = "load_signatures"
        private const val ACCEPT_OPERATION_TAG = "accept_operation"

        private const val REMOVE_OPERATION_TAG = "remove_operation"

        private const val LOAD_STORAGE_TAG = "load_storage"

        private const val TRANSFER_INITIALIZE_TAG = "transfer_initialize"
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
            onSendClick(VALIDATION_OPERATION_TYPE.ACCEPT_OPERATION)
        }

        accept_button_layout.setOnClickListener {
            onSendClick(VALIDATION_OPERATION_TYPE.ACCEPT_OPERATION)
        }

        confirm_operation_multisig_button_layout.setOnClickListener {
            onSendClick(VALIDATION_OPERATION_TYPE.CONFIRM_OPERATION)
        }

        close_button.setOnClickListener {
            dismiss()
        }

        delete_button.setOnClickListener {

            val dialogClickListener = { dialog: DialogInterface, which:Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE ->
                    {
                        dialog.dismiss()
                        startInitRemoveOngoingOperationDatabase(isOperationConfirmed = false)
                    }

                    DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                }
            }

            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle(R.string.alert_remove_ongoing_operation_title)
                    .setMessage(getString(R.string.alert_remove_ongoing_operation))

                    .setNegativeButton(android.R.string.cancel, dialogClickListener)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setCancelable(false)
                    .show()
        }

        swipe_refresh_multisig_dialog_layout.setOnRefreshListener {

            arguments?.let {
                startInitContractInfoLoading()
            }
        }

        if (savedInstanceState != null)
        {
            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mPayload = savedInstanceState.getString(PAYLOAD_KEY, null)
            mFees = savedInstanceState.getLong(OPERATION_FEES_KEY, -1L)

            mStorageInfoLoading = savedInstanceState.getBoolean(LOAD_STORAGE_TAG)

            mSignaturesLoading = savedInstanceState.getBoolean(LOAD_SIGNATURES_TAG)

            mAcceptLoading = savedInstanceState.getBoolean(ACCEPT_OPERATION_TAG)

            mRemoveOperationLoading = savedInstanceState.getBoolean(REMOVE_OPERATION_TAG)

            mInitTransferLoading = savedInstanceState.getBoolean(TRANSFER_INITIALIZE_TAG)
            mFinalizeTransferLoading = savedInstanceState.getBoolean(TRANSFER_FINALIZE_TAG)

            val serverOperationBundle = savedInstanceState.getBundle(SERVER_OPERATION_KEY)
            if (serverOperationBundle != null)
            {
                mServerOperation = MultisigOperation.fromBundle(serverOperationBundle)
            }

            val contractBundle = savedInstanceState.getBundle(CONTRACT_DATA_KEY)
            if (contractBundle != null)
            {
                mContract = Contract.fromBundle(contractBundle)
            }

            mSignatoriesList = savedInstanceState.getStringArrayList(SIGNATORIES_LIST_KEY)

            if (mStorageInfoLoading)
            {
                startInitContractInfoLoading()
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
                    onSignaturesInfoComplete(databaseError = null)

                    if (mAcceptLoading)
                    {
                        startInitAcceptOperationLoading()
                    }
                    else
                    {
                        onAcceptInfoComplete(error = null)

                        if (mInitTransferLoading)
                        {
                            startInitTransferLoading()
                        }
                        else
                        {
                            onInitTransferLoadComplete(error = null)

                            if (mFinalizeTransferLoading)
                            {
                                startFinalizeTransferLoading()
                            }
                            else
                            {
                                onFinalizeTransferLoadComplete(error = null)

                                if (mRemoveOperationLoading)
                                {
                                    startInitRemoveOngoingOperationDatabase(isOperationConfirmed = false)
                                }
                                else
                                {
                                    onRemoveOngoingOperationDatabaseComplete(error = null)
                                }
                            }
                        }
                    }
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

        arguments?.let {

            delete_button.visibility =
                    if (it.getBoolean(FROM_NOTARY))
                        View.VISIBLE
                    else
                        View.INVISIBLE
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
        validateConfirmEditionButton(isInputDataValid() && isFeeValid())
    }

    private fun validateConfirmEditionButton(validate: Boolean)
    {
        if (activity != null)
        {
//val themeBundle = arguments!!.getBundle(CustomTheme.TAG)
//val theme = CustomTheme.fromBundle(themeBundle)
            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

            if (validate)
            {
                confirm_operation_multisig_button.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                confirm_operation_multisig_button_layout.isEnabled = true
                confirm_operation_multisig_button_layout.background = makeSelector(theme)

                val drawables = confirm_operation_multisig_button.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                }
            }
            else
            {
                confirm_operation_multisig_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                confirm_operation_multisig_button_layout.isEnabled = false

                val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
                confirm_operation_multisig_button_layout.background = makeSelector(greyTheme)

                val drawables = confirm_operation_multisig_button.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }
            }

        }
    }

    private fun pk():String
    {
        return Storage(activity!!).getMnemonics().pk
    }

    enum class VALIDATION_OPERATION_TYPE
    {
        ACCEPT_OPERATION, CONFIRM_OPERATION
    }

    private fun onSendClick(validate:VALIDATION_OPERATION_TYPE)
    {
        val dialog = AuthenticationDialog()
        if (listener?.isFingerprintAllowed()!! && listener?.hasEnrolledFingerprints()!!)
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices().prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = {
                validateKeyAuthentication(it, validate)
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

            when (validate)
            {
                VALIDATION_OPERATION_TYPE.ACCEPT_OPERATION ->
                {
                    startInitAcceptOperationLoading()
                }

                VALIDATION_OPERATION_TYPE.CONFIRM_OPERATION ->
                {
                    startFinalizeTransferLoading()
                }
            }
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
        validateConfirmEditionButton(validate = false)

        startGetRequestLoadContractInfo()
    }

    private fun startInitSignaturesInfoLoading()
    {
        transferLoading(true)

        validateAcceptDeclineButtons(validate = false)
        validateConfirmEditionButton(validate = false)

        startGetRequestLoadSignatures()
    }

    private fun startInitRemoveOngoingOperationDatabase(isOperationConfirmed: Boolean)
    {
        transferLoading(true)
        mRemoveOperationLoading = true

        startPostRequestRemoveOngoingOperationDatabase(isOperationConfirmed)
    }

    private fun startPostRequestRemoveOngoingOperationDatabase(isOperationConfirmed:Boolean)
    {
        val childUpdates = HashMap<String, Any?>()

        val opBundle = arguments!!.getBundle(ONGOING_OPERATION_KEY)
        val op = MultisigOperation.fromBundle(opBundle)
        val binaryReader = MultisigBinaries(op.binary)

        childUpdates["/multisig_operations/${binaryReader.getContractAddress()}"] = null
        childUpdates["/notary_multisig_operations/${mServerOperation?.notary}/${binaryReader.getContractAddress()}"] = null


        val signatoriesList = getSignatoriesList()
        for (s in signatoriesList)
        {
            childUpdates["/signatory_multisig_operations/$s/${binaryReader.getContractAddress()}"] = null
        }

        mDatabaseReference = FirebaseDatabase.getInstance().reference
        mDatabaseReference.updateChildren(childUpdates)
                .addOnSuccessListener {

                    if (swipe_refresh_multisig_dialog_layout != null)
                    {
                        onRemoveOngoingOperationDatabaseComplete(error = null)
                        dismiss()
                        if (isOperationConfirmed)
                        {
                            listener?.onMultisigOperationConfirmed()
                        }
                        else
                        {
                            listener?.onMultisigOngoingOperationRemoved()
                        }
                    }
                }
                .addOnFailureListener {

                    if (swipe_refresh_multisig_dialog_layout != null)
                    {
                        onRemoveOngoingOperationDatabaseComplete(error = it)
                    }
                }
    }

    private fun onRemoveOngoingOperationDatabaseComplete(error:Exception?)
    {
        mRemoveOperationLoading = false
        transferLoading(false)
        cancelRequests(resetBooleans = true)

        swipe_refresh_multisig_dialog_layout?.isEnabled = true
        swipe_refresh_multisig_dialog_layout?.isRefreshing = false

        if (error != null || mClickCalculate)
        {
            mPayload = null
            mFees = -1L

            showSnackBar(error.toString(), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }

        refreshTextsAndLayouts()
    }


    private fun startGetRequestLoadSignatures()
    {
        cancelRequests(resetBooleans = true)

        mSignaturesLoading = true

        arguments?.let {
            val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(opBundle)

            val binaryReader = MultisigBinaries(op.binary)

            val postListener = object : ValueEventListener
            {
                override fun onDataChange(dataSnapshot: DataSnapshot)
                {
                    if (swipe_refresh_multisig_dialog_layout != null)
                    {
                        addMultisigOngoingOperationsFromJSON(dataSnapshot)
                        onSignaturesInfoComplete(databaseError = null)

                        if (hasEnoughSignatures() && it.getBoolean(FROM_NOTARY))
                        {
                            startInitTransferLoading()
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError)
                {
                    if (swipe_refresh_multisig_dialog_layout != null)
                    {
                        onSignaturesInfoComplete(databaseError = databaseError)
                    }
                }
            }
            //notaryOperationsDatabase.addValueEventListener(postListener)

            // Initialize Database

            val seed = Storage(activity!!).getMnemonics()

            operationDatabase =
                    if (it.getBoolean(FROM_NOTARY))
                    {
                        FirebaseDatabase.getInstance().reference
                                .child("notary_multisig_operations").child(seed.pkh).child(binaryReader.getContractAddress()!!)
                    }
                    else
                    {
                        FirebaseDatabase.getInstance().reference
                                .child("signatory_multisig_operations").child(seed.pk).child(binaryReader.getContractAddress()!!)
                    }

            operationDatabase.addListenerForSingleValueEvent(postListener)
        }
    }

    private fun startInitAcceptOperationLoading()
    {
        transferLoading(true)

        validateAcceptDeclineButtons(validate = false)
        validateConfirmEditionButton(validate = false)

        startAcceptOperationLoading()
    }

    private fun startInitTransferLoading()
    {
        transferLoading(true)

        validateConfirmEditionButton(validate = false)
        startPostRequestLoadInitTransfer()
    }



    private fun hasEnoughSignatures():Boolean
    {
        if (mServerOperation != null)
        {
            val signatures = mServerOperation?.signatures
            val list = getSignatoriesList()

            val keys = ArrayList(signatures?.keys)

            if (list.containsAll(keys) && keys.containsAll(list))
            {
                if (!list.isNullOrEmpty())
                {
                    val length = list.size

                    var confirmedSignatures = 0

                    for (i in 0 until length)
                    {
                        val pk = list[i]
                        if (signatures!![pk]!!.isNotEmpty())
                        {
                            confirmedSignatures++
                        }
                    }

                    val threshold = getThreshold()!!.toLong()
                    val necessarySignatures = threshold - confirmedSignatures

                    if (necessarySignatures <= 0)
                    {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun startAcceptOperationLoading()
    {
        cancelRequests(resetBooleans = true)

        mAcceptLoading = true

        arguments?.let {

            val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(opBundle)

            val mnemonicsData = Storage(activity!!).getMnemonics()
            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val sk = CryptoUtils.generateSk(mnemonics, "")

            val signature = KeyPair.sign(sk, op.binary.hexToByteArray())

            mServerOperation!!.signatures[mnemonicsData.pk] = CryptoUtils.generateEDSig(signature)

            val binaryReader = MultisigBinaries(op.binary)

            val childUpdates = HashMap<String, Any>()
            childUpdates["/multisig_operations/${binaryReader.getContractAddress()}"] = mServerOperation!!.toMap()

            childUpdates["/notary_multisig_operations/${mServerOperation!!.notary}/${binaryReader.getContractAddress()}"] = mServerOperation!!.toMap()


            val signatures = mServerOperation?.signatures
            val keys = ArrayList(signatures?.keys)

            for (pk in keys)
            {
                childUpdates["/signatory_multisig_operations/$pk/${binaryReader.getContractAddress()}"] = mServerOperation!!.toMap()
            }

            mDatabaseReference = FirebaseDatabase.getInstance().reference
            mDatabaseReference.updateChildren(childUpdates)
                    .addOnSuccessListener {

                        if (swipe_refresh_multisig_dialog_layout != null)
                        {
                            onAcceptInfoComplete(error = null)

                            showSnackBar(getString(R.string.multisig_operation_accepted), ContextCompat.getColor(context!!, android.R.color.holo_green_light), ContextCompat.getColor(context!!, R.color.tz_light))
                            startInitSignaturesInfoLoading()
                        }
                    }
                    .addOnFailureListener { exception ->

                        if (swipe_refresh_multisig_dialog_layout != null)
                        {
                            onAcceptInfoComplete(error = exception)
                        }
                    }
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
        if (hasEnoughSignatures() && mPayload != null)
        {
            var postParams = JSONObject()
            var dstObjects = JSONArray()
            var dstObject = JSONObject()

            val mnemonicsData = Storage(activity!!).getMnemonics()

            postParams.put("src", mnemonicsData.pkh)
            postParams.put("src_pk", mnemonicsData.pk)

            val opBundle = arguments!!.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(opBundle)
            val binaryReader = MultisigBinaries(op.binary)
            dstObject.put("dst", binaryReader.getContractAddress())

            dstObject.put("amount", "0")

            dstObjects.put(dstObject)
            postParams.put("dsts", dstObjects)

            if (/*isTransferPayloadValid(mPayload!!, postParams)*/true)
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mPayload!!.hexToByteArray()

                val xLen = zeroThree.size
                val yLen = byteArrayThree.size
                val result = ByteArray(xLen + yLen)

                System.arraycopy(zeroThree, 0, result, 0, xLen)
                System.arraycopy(byteArrayThree, 0, result, xLen, yLen)

                var compressedSignature: ByteArray
                val mnemonics = EncryptionServices().decrypt(seed.mnemonics)
                val sk = CryptoUtils.generateSk(mnemonics, "")
                compressedSignature = KeyPair.sign(sk, result)

                val pLen = byteArrayThree.size
                val sLen = compressedSignature.size
                val newResult = ByteArray(pLen + sLen)

                System.arraycopy(byteArrayThree, 0, newResult, 0, pLen)
                System.arraycopy(compressedSignature, 0, newResult, pLen, sLen)

                var payloadsign = newResult.toNoPrefixHexString()

                val stringRequest = object : StringRequest(Method.POST, url,
                        Response.Listener {

                            if (swipe_refresh_multisig_dialog_layout != null)
                            {
                                onFinalizeTransferLoadComplete(null)

                                startInitRemoveOngoingOperationDatabase(isOperationConfirmed = true)

                            }
                        },
                        Response.ErrorListener
                        {
                            if (swipe_refresh_multisig_dialog_layout != null)
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

    // volley
    private fun startGetRequestLoadContractInfo()
    {
        cancelRequests(resetBooleans = true)

        mStorageInfoLoading = true

        arguments?.let { bundle ->

            val operationBundle = bundle.getBundle(ONGOING_OPERATION_KEY)
            val op = MultisigOperation.fromBundle(operationBundle)

            val binaryReader = MultisigBinaries(op.binary)
            val url = String.format(getString(R.string.contract_info2_url), binaryReader.getContractAddress())

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener
            {o ->

                //prevents from async crashes
                if (dialogRootView != null)
                {
                    addContractInfoFromJSON(o)
                    onStorageInfoComplete(error = null)

                    startInitSignaturesInfoLoading()
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
            mContract = Contract.fromJSONArray(answer)
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
        cancelRequests(resetBooleans = true)

        swipe_refresh_multisig_dialog_layout?.isEnabled = true
        swipe_refresh_multisig_dialog_layout?.isRefreshing = false

        if (error != null || mClickCalculate)
        {

            mPayload = null
            mFees = -1L

            if(error != null)
            {
                showSnackBar(getString(R.string.generic_error), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
            }
        }

        refreshTextsAndLayouts()
    }

    private fun onSignaturesInfoComplete(databaseError: DatabaseError?)
    {
        mSignaturesLoading = false
        transferLoading(false)
        cancelRequests(resetBooleans = true)

        swipe_refresh_multisig_dialog_layout?.isEnabled = true
        swipe_refresh_multisig_dialog_layout?.isRefreshing = false

        if (databaseError != null)
        {
            mPayload = null
            mFees = -1L

            when
            {
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
        }

        refreshTextsAndLayouts()
    }

    private fun onAcceptInfoComplete(error:Exception?)
    {
        mAcceptLoading = false
        transferLoading(false)
        cancelRequests(resetBooleans = true)

        swipe_refresh_multisig_dialog_layout?.isEnabled = true
        swipe_refresh_multisig_dialog_layout?.isRefreshing = false

        if (error != null || mClickCalculate)
        {
            mPayload = null
            mFees = -1L

            showSnackBar(error.toString(), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }
        else
        {
        }

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

                    contract_address_edittext.setText(binaryReader.getContractAddress())
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

                            validateAcceptDeclineButtons(areButtonsValid())
                        }
                    }
                }

                MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.SET_DELEGATE ->
                {
                    contract_address_edittext.setText(binaryReader.getContractAddress())
                    operation_type_item.text = binaryReader.getOperationTypeString()

                    if (mContract != null)
                    {
                        refreshSignatories()
                        storage_proposal_layout.visibility = View.GONE
                        update_signatories_layout.visibility = View.VISIBLE

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
                    contract_address_edittext.setText(binaryReader.getContractAddress())
                    operation_type_item.text = binaryReader.getOperationTypeString()
                    if (mContract != null)
                    {
                        refreshSignatories()
                        update_signatories_layout.visibility = View.VISIBLE
                        storage_proposal_layout.visibility = View.GONE

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


            if (mServerOperation != null)
            {
                notary_tz1_edittext.setText(mServerOperation?.notary)

                if (it.getBoolean(FROM_NOTARY))
                {
                    confirm_operation_button_relative_layout.visibility = View.VISIBLE
                }
                else
                {
                    confirm_operation_button_relative_layout.visibility = View.GONE
                }

                val signatures = mServerOperation?.signatures
                val list = getSignatoriesList()

                val keys = ArrayList(signatures?.keys)

                if (list.containsAll(keys) && keys.containsAll(list))
                {
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

                    if (!list.isNullOrEmpty())
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
                                "It needs $necessarySignatures signature before it can be confirmed by the contract notary."
                            }
                            else
                            {
                                "It needs $necessarySignatures more signature before it can be confirmed by the contract notary."
                            }
                        }
                        else if (necessarySignatures >1L)
                        {
                            if (threshold == necessarySignatures)
                            {
                                "It needs $necessarySignatures signatures before it can be confirmed by the contract notary."
                            }
                            else
                            {
                                "It needs $necessarySignatures more signatures before it can be confirmed by the contract notary."
                            }
                        }
                        else
                        {
                            "It has enough signatures to be confirmed by the contract notary."
                        }

                        var text3 = ""

                        if (!arguments!!.getBoolean(FROM_NOTARY))
                        {
                            text3 += if (signatures!![pk()]!!.isNullOrEmpty())
                            {
                                "\n\nYou ( ${pk().substring(IntRange(0,8))} ... ) have not signed yet."
                            }
                            else
                            {
                                "\n\nYou ( ${pk().substring(IntRange(0,8))} ... ) have signed already."
                            }
                        }

                        //val text4 =
                        var text4 = ""
                        if (necessarySignatures <= 0)
                        {
                            text4 =
                                    if (arguments!!.getBoolean(FROM_NOTARY))
                                    {
                                        "\n\nAs a notary, you can confirm this operation."
                                    }
                                    else
                                    {
                                        "\n\nYou can contact the contract notary to confirm this operation."
                                    }
                        }

                        can_confirm_textview.text = text + text2 + text3 + text4

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
                    //TODO cancel the operation
                }

                validateConfirmEditionButton(hasEnoughSignatures() && isFeeValid() && mPayload != null)
            }
        }
    }

    private fun startPostRequestLoadInitTransfer()
    {
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
        dstObject.put("dst", binaryReader.getContractAddress())

        dstObject.put("amount", "0")


        val file = when (binaryReader.getType())
        {
            MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UPDATE_SIGNATORIES ->
            {
                "multisig_update_storage.json"
            }

            MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.SET_DELEGATE ->
            {
                "multisig_set_delegate.json"
            }

            MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UNDELEGATE ->
            {
                "multisig_withdraw_delegate.json"
            }
            else -> {""}
        }

        val contract = context!!.assets.open(file).bufferedReader()
                .use {
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

        when (binaryReader.getType())
        {
            MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UPDATE_SIGNATORIES ->
            {

                val args = ((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray
                val argsb = (((((args[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

                val signatoriesCount = argsb[0] as JSONObject
                signatoriesCount.put("int", binaryReader.getThreshold().toString())

                val signatories = argsb[1] as JSONArray
                signatories.remove(0)

                for (it in binaryReader.getSignatories()!!)
                {
                    var decodedValue = Base58.decode(it)
                    var bytes = decodedValue.slice(4 until (decodedValue.size - 4)).toByteArray()
                    bytes = byteArrayOf(0x00) + bytes

                    val signatory = JSONObject()
                    signatory.put("bytes", bytes.toNoPrefixHexString())

                    signatories.put(signatory)
                }
            }

            MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.SET_DELEGATE ->
            {
                val argBaker = (((((((((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject

                var decodedValue = Base58.decode(binaryReader.getBaker())
                var bakerBytes = decodedValue.slice(3 until (decodedValue.size - 4)).toByteArray()
                bakerBytes = byteArrayOf(0x00) + bakerBytes
                argBaker.put("bytes", bakerBytes.toNoPrefixHexString())
            }

            MultisigBinaries.Companion.MULTISIG_BINARY_TYPE.UNDELEGATE -> {}
            else -> {}
        }

        dstObject.put("parameters", value)

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)


        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            if (swipe_refresh_multisig_dialog_layout != null)
            {
                mPayload = answer.getString("result")
                mFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mPayload != null && isFeeValid() && activity != null)
                {
                    onInitTransferLoadComplete(error = null)

                    val feeInTez = mFees.toDouble()/1000000.0
                    //fee_edittext?.setText(feeInTez.toString())

                    validateConfirmEditionButton(hasEnoughSignatures() && isInputDataValid() && isFeeValid() && mPayload != null)
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
            if (swipe_refresh_multisig_dialog_layout != null)
            {
                onInitTransferLoadComplete(it)
                mClickCalculate = true
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

        cancelRequests(resetBooleans = true)

        jsObjRequest.tag = LOAD_STORAGE_TAG
        mInitTransferLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
    }

    private fun isFeeValid():Boolean
    {
        return (mFees != -1L && mFees >= 0.000001 && mPayload != null)
        //val isFeeValid = true

        /*
        val isFeeValid = false

        if (fee_limit_edittext.text != null && !TextUtils.isEmpty(fee_limit_edittext.text))
        {
            try
            {
//val amount = java.lang.Double.parseDouble()
                val fee = fee_limit_edittext.text.toString().toDouble()

                if (fee >= 0.000001f)
                {
                    val longTransferFee = fee*1000000
                    mMultisigFees = longTransferFee.roundToLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mMultisigFees = -1
                return false
            }
        }

        return isFeeValid
        */
    }

    fun isInputDataValid(): Boolean
    {
        //return isDelegateAmountValid() && isDailySpendingLimitValid()
        //TODO check if the baker on its specific edittext is the same as specified in the BinaryReader.
        //TODO do the same with the different operations.
        return true
    }

    private fun onInitTransferLoadComplete(error: VolleyError?)
    {
        mStorageInfoLoading = false
        transferLoading(loading = false)
        cancelRequests(resetBooleans = true)

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred

            mPayload = null
            mFees = -1L

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

            showSnackBar(getString(R.string.generic_error), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }
    }

    private fun onFinalizeTransferLoadComplete(error: VolleyError?)
    {
        // everything is over, there's no call to make
        cancelRequests(true)

        if (error != null)
        {
            transferLoading(false)

            var err: String = error.toString()

            showSnackBar(err, ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }
        else
        {
            // the finish call is made already
        }
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

            if (mContract != null && mServerOperation != null)
            {
                /*
                val opBundle = it.getBundle(ONGOING_OPERATION_KEY)
                val op = MultisigOperation.fromBundle(opBundle)
                val binaryReader = MultisigBinaries(op.binary)
                */

                val signatures = mServerOperation?.signatures
                val mnemonicsData = Storage(activity!!).getMnemonics()

                if (
                        !signatures.isNullOrEmpty()
                        && signatures.containsKey(mnemonicsData.pk)
                        && signatures[mnemonicsData.pk]!!.isEmpty()
                )
                {
                    accept_decline_buttons_layout.visibility = View.VISIBLE
                    return true
                }

                /*
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
                */
            }
        }

        accept_decline_buttons_layout.visibility = View.GONE
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

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)
        outState.putString(PAYLOAD_KEY, mPayload)

        outState.putLong(OPERATION_FEES_KEY, mFees)

        outState.putBoolean(LOAD_STORAGE_TAG, mStorageInfoLoading)

        outState.putBoolean(LOAD_SIGNATURES_TAG, mSignaturesLoading)

        outState.putBoolean(ACCEPT_OPERATION_TAG, mAcceptLoading)

        outState.putBoolean(REMOVE_OPERATION_TAG, mRemoveOperationLoading)

        outState.putBoolean(TRANSFER_FINALIZE_TAG, mFinalizeTransferLoading)

        outState.putBoolean(TRANSFER_INITIALIZE_TAG, mInitTransferLoading)

        outState.putBundle(SERVER_OPERATION_KEY, mServerOperation?.toBundle())

        outState.putBundle(CONTRACT_DATA_KEY, mContract?.toBundle())


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

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject, validate:VALIDATION_OPERATION_TYPE)
    {
        if (EncryptionServices().validateFingerprintAuthentication(cryptoObject))
        {
            when (validate)
            {
                VALIDATION_OPERATION_TYPE.ACCEPT_OPERATION ->
                {
                    startInitAcceptOperationLoading()
                }

                VALIDATION_OPERATION_TYPE.CONFIRM_OPERATION ->
                {
                    startFinalizeTransferLoading()
                }
            }
        }
        else
        {
            onSendClick(validate)
        }
    }

    private fun cancelRequests(resetBooleans:Boolean)
    {
        if (activity != null)
        {
            val requestQueue = VolleySingleton.getInstance(activity!!.applicationContext).requestQueue
            requestQueue?.cancelAll(LOAD_STORAGE_TAG)
            requestQueue?.cancelAll(TRANSFER_INITIALIZE_TAG)
            requestQueue?.cancelAll(TRANSFER_FINALIZE_TAG)

            if (resetBooleans)
            {
                mStorageInfoLoading = false
                mInitTransferLoading = false
                mFinalizeTransferLoading = false
                mSignaturesLoading = false
                mAcceptLoading = false
                mRemoveOperationLoading = false
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
