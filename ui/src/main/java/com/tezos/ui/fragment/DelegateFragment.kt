/*
(*****************************************************************************)
(*                                                                           *)
(* Open Source License                                                       *)
(* Copyright (c) 2018 Nomadic Development, Inc. <contact@tezcore.com>        *)
(*                                                                           *)
(* Permission is hereby granted, free of charge, to any person obtaining a   *)
(* copy of this software and associated documentation files (the "Software"),*)
(* to deal in the Software without restriction, including without limitation *)
(* the rights to use, copy, modify, merge, publish, distribute, sublicense,  *)
(* and/or sell copies of the Software, and to permit persons to whom the     *)
(* Software is furnished to do so, subject to the following conditions:      *)
(*                                                                           *)
(* The above copyright notice and this permission notice shall be included   *)
(* in all copies or substantial portions of the Software.                    *)
(*                                                                           *)
(* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR*)
(* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  *)
(* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL   *)
(* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER*)
(* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING   *)
(* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER       *)
(* DEALINGS IN THE SOFTWARE.                                                 *)
(*                                                                           *)
(*****************************************************************************)
*/

package com.tezos.ui.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.tezos.core.crypto.Base58
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.*
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.database.MultisigOperation
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.fragment_delegate.*
import kotlinx.android.synthetic.main.fragment_delegate.loading_textview
import kotlinx.android.synthetic.main.fragment_delegate.nav_progress
import kotlinx.android.synthetic.main.fragment_delegate.storage_info_textview
import kotlinx.android.synthetic.main.fragment_delegate.update_storage_button
import kotlinx.android.synthetic.main.fragment_delegate.update_storage_button_layout
import kotlinx.android.synthetic.main.fragment_delegate.update_storage_form_card
import kotlinx.android.synthetic.main.fragment_script.*
import kotlinx.android.synthetic.main.redelegate_form_card_info.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Instant
import kotlin.math.roundToLong

class DelegateFragment : Fragment()
{
    private var mCallback: OnAddedDelegationListener? = null

    private var mInitDelegateLoading:Boolean = false
    private var mFinalizeDelegateLoading:Boolean = false

    private var mInitRemoveDelegateLoading:Boolean = false
    private var mFinalizeRemoveDelegateLoading:Boolean = false

    private var mContractInfoLoading:Boolean = false

    private var mStorageInfoLoading:Boolean = false

    private var mDelegatePayload:String? = null
    private var mDelegateFees:Long = -1

    private var mDelegateTezosAddress:String? = null

    private var mClickCalculate:Boolean = false

    private var mContract:Contract? = null

    private var mWalletEnabled:Boolean = false

    private var mStorage:String? = null

    private var mSig:String? = null

    private var mContractManager:String? = null

    private var mContractManagerLoading:Boolean = false

    private var mClickReloadNotary:Boolean = false

    private lateinit var mDatabaseReference: DatabaseReference

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

    fun toBundle(contract: Contract?): Bundle?
    {
        if (contract != null)
        {
            val serializer = ContractSerialization(contract)
            return serializer.getSerializedBundle()
        }
        return null
    }

    fun fromBundle(bundle: Bundle): Contract
    {
        val mapper = ContractMapper(bundle)
        return mapper.mappedObjectFromBundle()
    }

    companion object
    {
        private const val CONTRACT_KEY = "CONTRACT_KEY"

        private const val DELEGATE_INIT_TAG = "delegate_init"
        private const val DELEGATE_FINALIZE_TAG = "delegate_finalize"

        private const val REMOVE_DELEGATE_INIT_TAG = "remove_delegate_init"
        private const val REMOVE_DELEGATE_FINALIZE_TAG = "remove_delegate_finalize"

        private const val CONTRACT_INFO_TAG = "contract_info"
        private const val STORAGE_INFO_TAG = "storage_info"

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val DELEGATE_TEZOS_ADDRESS_KEY = "delegate_tezos_address_key"
        private const val DELEGATE_FEE_KEY = "delegate_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private const val STORAGE_DATA_KEY = "storage_data_key"

        private const val WALLET_AVAILABLE_KEY = "wallet_available_key"

        private const val CONTRACT_DATA_KEY = "contract_data_key"

        private const val CONTRACT_SIG_KEY = "contract_sig_key"

        private const val CONTRACT_MANAGER_KEY = "contract_manager_key"

        private const val LOAD_CONTRACT_MANAGER_TAG = "load_contract_manager_delegate"

        private const val RELOAD_NOTARY_KEY = "reload_notary_key"

        private const val SIGNATORIES_CAPACITY = 10

        enum class MULTISIG_UPDATE_STORAGE_ENUM
        {
            CONFIRM_UPDATE, REQUEST_TO_SIGNATORIES, NOTIFY_NOTARY, NEITHER_NOTARY_NOR_SIGNATORY, NO_NOTARY_YET
        }

        @JvmStatic
        fun newInstance(theme: CustomTheme, contract: String?) =
                DelegateFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(CONTRACT_KEY, contract)
                    }
                }
    }

    interface OnAddedDelegationListener
    {
        fun showSnackBar(res:String, color:Int, textColor:Int)
        fun finish(res:Int)

        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean
        fun saveFingerprintAllowed(useInFuture: Boolean)
    }

    override fun onAttach(context: Context?)
    {
        super.onAttach(context)

        try
        {
            mCallback = context as OnAddedDelegationListener?
        }
        catch (e: ClassCastException)
        {
            throw ClassCastException(context!!.toString() + " must implement OnAddedDelegationListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_delegate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        validateAddButton(isInputDataValid() && isDelegateFeeValid())
        validateRemoveDelegateButton(isDelegateFeeValid())

        update_storage_button_layout.setOnClickListener {
            onDelegateClick()
        }

        request_delegate_button_layout.setOnClickListener {
            onDelegateClick()
        }

        remove_delegate_button_layout.setOnClickListener {
            onRemoveDelegateClick()
        }

        swipe_refresh_layout.setOnRefreshListener {
            startContractInfoLoading()
        }

        redelegate_address_edittext.addTextChangedListener(GenericTextWatcher(redelegate_address_edittext))

        redelegate_address_edittext.onFocusChangeListener = focusChangeListener()

        mDatabaseReference = FirebaseDatabase.getInstance().reference

        if (savedInstanceState != null)
        {
            mDelegatePayload = savedInstanceState.getString(DELEGATE_PAYLOAD_KEY, null)

            mInitDelegateLoading = savedInstanceState.getBoolean(DELEGATE_INIT_TAG)
            mFinalizeDelegateLoading = savedInstanceState.getBoolean(DELEGATE_FINALIZE_TAG)

            mInitRemoveDelegateLoading = savedInstanceState.getBoolean(REMOVE_DELEGATE_INIT_TAG)
            mFinalizeRemoveDelegateLoading = savedInstanceState.getBoolean(REMOVE_DELEGATE_FINALIZE_TAG)

            mContractInfoLoading = savedInstanceState.getBoolean(CONTRACT_INFO_TAG)

            mDelegateTezosAddress = savedInstanceState.getString(DELEGATE_TEZOS_ADDRESS_KEY, null)

            mDelegateFees = savedInstanceState.getLong(DELEGATE_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mClickReloadNotary = savedInstanceState.getBoolean(RELOAD_NOTARY_KEY, false)

            mWalletEnabled = savedInstanceState.getBoolean(WALLET_AVAILABLE_KEY, false)

            mStorage = savedInstanceState.getString(STORAGE_DATA_KEY, null)

            mStorageInfoLoading = savedInstanceState.getBoolean(STORAGE_INFO_TAG)

            mSig = savedInstanceState.getString(CONTRACT_SIG_KEY, null)

            mContractManagerLoading = savedInstanceState.getBoolean(LOAD_CONTRACT_MANAGER_TAG)

            mContractManager = savedInstanceState.getString(CONTRACT_MANAGER_KEY, null)

            val contractBundle = savedInstanceState.getBundle(CONTRACT_DATA_KEY)
            if (contractBundle != null)
            {
                mContract = this.fromBundle(contractBundle)
            }

            if (mContractInfoLoading)
            {
                refreshTextUnderDelegation()
                mWalletEnabled = true
                startContractInfoLoading()
            }
            else
            {
                onContractInfoComplete()

                if (mStorageInfoLoading)
                {
                    startGetRequestLoadContractStorage()
                }
                else
                {
                    onStorageInfoComplete()

                    if (mContractManagerLoading)
                    {
                        startNotaryLoading()
                    }
                    else
                    {
                        onContractManagerLoadComplete(error = null)

                        if (mInitRemoveDelegateLoading)
                        {
                            startInitRemoveDelegateLoading()
                        }
                        else
                        {
                            onInitRemoveDelegateLoadComplete(null)

                            if (mFinalizeRemoveDelegateLoading)
                            {
                                startFinalizeRemoveDelegateLoading()
                            }
                            else
                            {
                                onFinalizeDelegationLoadComplete(null)

                                //TODO we got to keep in mind there's an id already.
                                if (mInitDelegateLoading)
                                {
                                    startInitDelegationLoading()
                                }
                                else
                                {
                                    onInitDelegateLoadComplete(null)

                                    if (mFinalizeDelegateLoading)
                                    {
                                        startFinalizeAddDelegateLoading()
                                    }
                                    else
                                    {
                                        onFinalizeDelegationLoadComplete(null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else
        {
            mWalletEnabled = true
            startContractInfoLoading()
        }
    }

    override fun onResume()
    {
        super.onResume()

        putEverythingInRed()

        if (isDelegateFeeValid())
        {
            validateRemoveDelegateButton(true)

            if (isInputDataValid())
            {
                validateAddButton(true)
            }
        }

        if (!mWalletEnabled)
        {
            mWalletEnabled = true

            // put the good layers
            //mBalanceLayout?.visibility = View.VISIBLE
            //mCreateWalletLayout?.visibility = View.GONE

            startContractInfoLoading()
        }
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id
            if (i == R.id.redelegate_address_edittext)
            {
                putTzAddressInRed(!hasFocus)
            }
            else
            {
                throw UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }

    fun pkh():String?
    {
        var pkh:String? = null

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            pkh = arguments!!.getString(CONTRACT_KEY)
            if (pkh == null)
            {
                //should not happen
                //val seed = Storage(activity!!).getMnemonics()
                //pkh = seed.pkh
            }
        }

        return pkh
    }

    private fun startContractInfoLoading()
    {
        transferLoading(true)

        //putFeesToNegative()
        //putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validateAddButton(false)

        swipe_refresh_layout?.isEnabled = false

        startGetRequestLoadContractInfo()
    }

    private fun startInitDelegationLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()
        putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validateAddButton(false)


        when (askingForMultisigButton())
        {
            MULTISIG_UPDATE_STORAGE_ENUM.CONFIRM_UPDATE ->
            {
                startPostRequestLoadInitAddDelegate()
            }

            MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES ->
            {
                //startPostRequestLoadInitRequestUpdateStorage()

                validateAddButton(isInputDataValid() && isDelegateFeeValid())
                transferLoading(loading = false)
            }

            MULTISIG_UPDATE_STORAGE_ENUM.NOTIFY_NOTARY ->
            {
                startPostRequestLoadInitNotifyUpdateStorage()
            }

            MULTISIG_UPDATE_STORAGE_ENUM.NEITHER_NOTARY_NOR_SIGNATORY ->
            {
                // not related to the contract, please contact the notary
                showSnackBar(null, getString(R.string.alert_neither_notary_nor_signatory))
            }
            MULTISIG_UPDATE_STORAGE_ENUM.NO_NOTARY_YET ->
            {
                // fail message, retry loading notary
                showSnackBar(null, getString(R.string.alert_reload_the_notary))
            }
        }
    }

    private fun startInitRemoveDelegateLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()
        putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validateRemoveDelegateButton(false)


        if (!getThreshold().isNullOrEmpty())
        {
            when (askingForMultisigButton())
            {
                MULTISIG_UPDATE_STORAGE_ENUM.CONFIRM_UPDATE ->
                {
                    startPostRequestLoadInitRemoveDelegate()
                }

                MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES ->
                {
                    startPostRequestLoadInitRequestUpdateStorage()
                }

                MULTISIG_UPDATE_STORAGE_ENUM.NOTIFY_NOTARY ->
                {
                    startPostRequestLoadInitNotifyUpdateStorage()
                }

                MULTISIG_UPDATE_STORAGE_ENUM.NEITHER_NOTARY_NOR_SIGNATORY ->
                {
                    // not related to the contract, please contact the notary
                    showSnackBar(null, getString(R.string.alert_neither_notary_nor_signatory))
                }
                MULTISIG_UPDATE_STORAGE_ENUM.NO_NOTARY_YET ->
                {
                    // fail message, retry loading notary
                    showSnackBar(null, getString(R.string.alert_reload_the_notary))
                }
            }
        }
        else
        {
            startPostRequestLoadInitRemoveDelegate()
        }
    }

    private fun startFinalizeOngoingMultisigAddDelegateLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        val nowInEpoch =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    Instant.now().epochSecond
                }
                else
                {
                    System.currentTimeMillis()/1000
                }

        //val signatoriesHashMap = HashMap<String, Signatory>()
        //signatoriesHashMap["hello"] = Signatory("hello", "world")

        val dataVisitable = Primitive(
                Primitive.Name.Pair,
                arrayOf(
                        Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.chainID(getString(R.string.chain_ID)),
                                        Visitable.address(pkh()!!)
                                )
                        ),

                        Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.integer(getMultisigCounter()!!.toLong()),
                                        Primitive(Primitive.Name.Right,
                                                arrayOf(
                                                        Primitive (Primitive.Name.Left,
                                                                arrayOf(
                                                                        Primitive(Primitive.Name.Some,
                                                                                arrayOf(
                                                                                        Visitable.keyHash(mDelegateTezosAddress!!)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        )

        val o = ByteArrayOutputStream()
        o.write(0x05)

        val dataPacker = Packer(o)
        dataVisitable.accept(dataPacker)

        val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()


        val signatures = HashMap<String, String>()
        val signatoriesList = getSignatoriesList()

        val mnemonicsData = Storage(activity!!).getMnemonics()

        for (s in signatoriesList)
        {
            if (s == mnemonicsData.pk)
            {
                val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                val sk = CryptoUtils.generateSk(mnemonics, "")
                val signature = KeyPair.sign(sk, dataPack)

                signatures[s] = CryptoUtils.generateEDSig(signature)
            }
            else
            {
                signatures[s] = ""
            }
        }

        val ongoingOperation = MultisigOperation(binary = dataPack.toNoPrefixHexString(), timestamp = nowInEpoch, notary = pkhtz1()!!, signatures = signatures)

        val childUpdates = HashMap<String, Any>()
        childUpdates["/multisig_operations/${pkh()}"] = ongoingOperation.toMap()

        for (s in signatoriesList)
        {
            childUpdates["/signatory-operations/$s/${pkh()}"] = ongoingOperation.toMap()
        }

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

    private fun startFinalizeAddDelegateLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        val mnemonicsData = Storage(activity!!).getMnemonics()
        startPostRequestLoadFinalizeAddDelegate(mnemonicsData)
    }

    private fun startFinalizeRemoveDelegateLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        val mnemonicsData = Storage(activity!!).getMnemonics()
        startPostRequestLoadFinalizeRemoveDelegate(mnemonicsData)
    }

    // volley
    private fun startGetRequestLoadContractInfo()
    {

        loading_textview.setText(R.string.loading_contract_info)

        nav_progress.visibility = View.VISIBLE

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.contract_info_url), pkh)

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            {

                //prevents from async crashes
                if (swipe_refresh_layout != null)
                {
                    addContractInfoFromJSON(it)

                    onContractInfoComplete()
                    validateAddButton(isInputDataValid() && isDelegateFeeValid())

                    startGetRequestLoadContractStorage()
                }
            },
                    Response.ErrorListener {

                        if (swipe_refresh_layout != null)
                        {
                            onContractInfoComplete()
                            showSnackBar(it, null)
                        }
                    })

            cancelRequests(true)
            mContractInfoLoading = true

            jsonArrayRequest.tag = CONTRACT_INFO_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    // volley
    private fun startGetRequestLoadContractStorage()
    {

        loading_textview.setText(R.string.loading_contract_info)
        nav_progress.visibility = View.VISIBLE

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.contract_storage_url), pkh)

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<JSONObject>
            {

                //prevents from async crashes
                if (swipe_refresh_layout != null)
                {
                    addStorageInfoFromJSON(it)
                    onStorageInfoComplete()

                    //TODO verification ici

                    if (getThreshold() != null)
                    {
                        // here I need to check the user is notary or signatory only.
                        startNotaryLoading()
                    }
                    else
                    {
                        val hasMnemonics = Storage(activity!!).hasMnemonics()
                        if (hasMnemonics)
                        {
                            val seed = Storage(activity!!).getMnemonics()

                            if (seed.mnemonics.isNotEmpty())
                            {
                                //TODO need to check here if we need to get salt
                                //TODO a l'arrivee de get salt, on charge removeDelegateBlabla.

                                if (mContract?.delegate != null)
                                {
                                    startInitRemoveDelegateLoading()
                                }
                                else
                                {
                                    validateAddButton(isInputDataValid() && isDelegateFeeValid())
                                }
                            }
                        }
                    }
                }
            },
                    Response.ErrorListener {

                        if (swipe_refresh_layout != null)
                        {
                            showSnackBar(it, null)
                            onStorageInfoComplete()
                        }
                    })

            cancelRequests(true)
            mStorageInfoLoading = true

            jsonArrayRequest.tag = STORAGE_INFO_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    private fun startNotaryLoading()
    {
        transferLoading(true)

        putNotaryToNegative()

        startGetRequestManagerKey()
    }

    // volley
    private fun startGetRequestManagerKey()
    {
        cancelRequests(resetBooleans = true)

        mContractManagerLoading = true

        transferLoading(loading = true)
        //val randomNumber = Math.random()
        val url = String.format(getString(R.string.manager_key_url), pkh())

        // Request a string response from the provided URL.
        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
        {
            if (swipe_refresh_layout != null)
            {
                mContractManager = it.getJSONObject(0)["manager"] as String

                onContractManagerLoadComplete(error = null)

                //TODO control better this

                val hasMnemonics = Storage(activity!!).hasMnemonics()
                if (hasMnemonics)
                {
                    val seed = Storage(activity!!).getMnemonics()

                    if (seed.mnemonics.isNotEmpty())
                    {
                        if (mContract?.delegate != null)
                        {
                            startInitRemoveDelegateLoading()
                        }
                        else
                        {
                            validateAddButton(isInputDataValid() && isDelegateFeeValid())
                        }
                    }
                }
            }
        },
                Response.ErrorListener {

                    if (swipe_refresh_layout != null)
                    {
                        onContractManagerLoadComplete(it)

                        mClickReloadNotary = true
                    }
                })

        jsonArrayRequest.tag = LOAD_CONTRACT_MANAGER_TAG
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
    }


    private fun onContractManagerLoadComplete(error: VolleyError?)
    {
        mContractManagerLoading = false

        if (error != null || mClickReloadNotary)
        {
// stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            mContractManager = null

            notary_tz1_edittext.isEnabled = true
            notary_tz1_edittext.isFocusable = false
            notary_tz1_edittext.isClickable = false
            notary_tz1_edittext.isLongClickable = false
            notary_tz1_edittext.hint = getString(R.string.click_to_reload)

            notary_tz1_edittext.setOnClickListener {
                startNotaryLoading()
            }

            if (error != null)
            {
                showSnackBar(error, null)
            }
        }
        else
        {
            refreshTextUnderDelegation()
            transferLoading(loading = false)
            cancelRequests(true)
        }
    }


    private fun updateMultisigInfos()
    {
        notary_layout.visibility = View.VISIBLE

        val numberAndSpotPair = getNumberAndSpot(pk()!!)
        if (numberAndSpotPair.first != -1)
        {
            signatory_textview.text = getString(R.string.warning_signatory_info)
            signatory_info.visibility = View.VISIBLE

            // about ths signatories
            threshold_info.visibility = View.VISIBLE

            var threshold = getThreshold()
            if (threshold!!.toInt() == 1)
            {
                threshold_textview.text = getString(R.string.warning_no_need_signatories_info)
            }
            else
            {
                threshold_textview.text = String.format(getString(R.string.warning_need_signatories_signatory_info), threshold.toInt()-1)
            }

            if (!mContractManager.isNullOrEmpty())
            {
                notary_info.visibility = View.VISIBLE

                notary_tz1_edittext.setText(mContractManager)
                notary_tz1_edittext.isEnabled = true
                notary_tz1_edittext.isFocusable = false
                notary_tz1_edittext.isClickable = false
                notary_tz1_edittext.isLongClickable = false

                if (mContractManager == pkhtz1())
                {
                    if (threshold!!.toInt() == 1)
                    {
                        notary_textview.text = getString(R.string.warning_notary_threshold_signatory_info)
                    }
                    else
                    {
                        notary_textview.text = String.format(getString(R.string.warning_notary_signatory_not_threshold_info), threshold.toInt()-1)
                    }
                }
                else
                {
                    notary_textview.text = getString(R.string.warning_not_notary_signatory_info)
                }

            }
            else
            {
                notary_info.visibility = View.GONE
            }
        }
        else
        {
            signatory_textview.text = getString(R.string.warning_not_signatory_info)
            signatory_info.visibility = View.VISIBLE

            threshold_info.visibility = View.VISIBLE

            var threshold = getThreshold()
            threshold_textview.text = String.format(getString(R.string.warning_need_signatories_not_signatory_info), threshold)

            if (!mContractManager.isNullOrEmpty())
            {
                notary_info.visibility = View.VISIBLE

                notary_tz1_edittext.setText(mContractManager)
                notary_tz1_edittext.isEnabled = true

                notary_tz1_edittext.isFocusable = false
                notary_tz1_edittext.isClickable = false
                notary_tz1_edittext.isLongClickable = false
                //notary_tz1_edittext.hint = getString(R.string.click_to_reload)

                if (mContractManager == pkhtz1())
                {
                    notary_textview.text = String.format(getString(R.string.warning_notary_not_signatory_info), threshold)
                }
                else
                {
                    notary_textview.text = getString(R.string.warning_not_notary_not_signatory_info)
                }
            }
            else
            {
                notary_info.visibility = View.GONE
            }
        }
    }

    private fun putNotaryToNegative()
    {
        notary_tz1_edittext?.setText("")

        mClickReloadNotary = false
        notary_tz1_edittext?.isEnabled = false
        notary_tz1_edittext?.hint = getString(R.string.neutral)

        mContractManager = null
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

            val storage = DataExtractor.getJSONObjectFromField(script, "storage")

            //val resScript = JSONObject(getString(R.string.default_contract))
            mContract = Contract(blk as String, spendable as Boolean, delegatable as Boolean, delegate, script.toString(), storage.toString())
        }
    }

    private fun addStorageInfoFromJSON(answer: JSONObject)
    {
        if (answer.length() > 0)
        {
            mStorage = answer.toString()
        }
    }

    private fun onStorageInfoComplete()
    {
        mStorageInfoLoading = false
        nav_progress?.visibility = View.GONE

        //TODO handle the swipe refresh
        swipe_refresh_script_layout?.isEnabled = true
        swipe_refresh_script_layout?.isRefreshing = false

        refreshTextUnderDelegation()
    }

    private fun onContractInfoComplete()
    {
        mContractInfoLoading = false
        nav_progress?.visibility = View.GONE

        swipe_refresh_layout?.isEnabled = true
        swipe_refresh_layout?.isRefreshing = false

        refreshTextUnderDelegation()
    }

    private fun askingForMultisigButton():MULTISIG_UPDATE_STORAGE_ENUM
    {
        // 1. on recupere le storage
        // --> on sait si on est signataire, et si on a assez de signature ou non.

        // confirm_update --> signatory && notary && threshold 1
        // request_to_signatories --> signatory && signatory < threshold && notary || notary && not signatory
        // notify_notary --> signatory && not notary
        // nothgin --> not signatory, not notary.

        val numberAndSpotPair = getNumberAndSpot(pk()!!)
        if (numberAndSpotPair.first != -1)
        {
            var threshold = getThreshold()

            if (!mContractManager.isNullOrEmpty())
            {
                return if (mContractManager == pkhtz1())
                {
                    if (threshold!!.toInt() == 1)
                    {
                        MULTISIG_UPDATE_STORAGE_ENUM.CONFIRM_UPDATE
                    }
                    else
                    {
                        MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES
                    }
                }
                else
                {
                    MULTISIG_UPDATE_STORAGE_ENUM.NOTIFY_NOTARY
                }
            }
            else
            {
                // we don't know yet if we are a notary, then we just wait.
            }
        }
        else
        {
            if (!mContractManager.isNullOrEmpty())
            {
                return if (mContractManager == pkhtz1())
                {
                    MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES
                }
                else
                {
                    MULTISIG_UPDATE_STORAGE_ENUM.NEITHER_NOTARY_NOR_SIGNATORY
                }
            }
            else
            {
                // we don't know yet if we are a notary, then we just wait.
            }
        }

        return MULTISIG_UPDATE_STORAGE_ENUM.NO_NOTARY_YET
    }

    private fun refreshTextUnderDelegation()
    {
        //TODO refreshing text depending on contract type : default, multisig or spending limit.

        if (mContract != null)
        {
            if (!mStorage.isNullOrEmpty())
            {
                // si le storage n'envoie rien, on n'affiche rien


                val defaultContract = JSONObject().put("string", pkhtz1())
                val isDefaultContract = mStorage.toString() == defaultContract.toString()

                if (isDefaultContract)
                {

                }
                else if (!getStorageSecureKeyHash().isNullOrEmpty())
                {

                }
                else if (!getThreshold().isNullOrEmpty())
                {
                    updateMultisigInfos()
                }


                /*
                if (!getThreshold().isNullOrEmpty() && mContractManager.isNullOrEmpty())
                {
                    return
                }
                */

                // affichage de l'update storage form card

                if (mContract?.delegate != null)
                {
                    limits_info_textview?.visibility = View.GONE
                    update_storage_form_card?.visibility = View.VISIBLE

                    redelegate_address_layout?.visibility = View.GONE


                    //TODO this is about having the right buttons
                    if (!getThreshold().isNullOrEmpty())
                    {

                        when (askingForMultisigButton())
                        {
                            MULTISIG_UPDATE_STORAGE_ENUM.CONFIRM_UPDATE ->
                            {
                                update_storage_button_layout?.visibility = View.GONE
                                remove_delegate_button_layout?.visibility = View.VISIBLE

                                notify_delegate_button_layout?.visibility = View.GONE
                                request_delegate_button_layout?.visibility = View.GONE
                            }

                            MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES ->
                            {
                                update_storage_button_layout?.visibility = View.GONE
                                remove_delegate_button_layout?.visibility = View.GONE
                                notify_delegate_button_layout?.visibility = View.GONE
                                request_delegate_button_layout?.visibility = View.VISIBLE
                                request_delegate_button.text = getString(R.string.request_remove_delegate)
                            }

                            MULTISIG_UPDATE_STORAGE_ENUM.NOTIFY_NOTARY ->
                            {
                                update_storage_button_layout?.visibility = View.GONE
                                remove_delegate_button_layout?.visibility = View.GONE

                                notify_delegate_button_layout?.visibility = View.VISIBLE
                                notify_delegate_button.text = getString(R.string.notify_remove_delegate)

                                request_delegate_button_layout?.visibility = View.GONE
                            }

                            else ->
                            {
                                update_storage_button_layout?.visibility = View.GONE
                                remove_delegate_button_layout?.visibility = View.GONE
                                notify_delegate_button_layout?.visibility = View.GONE
                                request_delegate_button_layout?.visibility = View.GONE
                            }
                        }
                    }
                    else
                    {
                        update_storage_button_layout?.visibility = View.GONE
                        remove_delegate_button_layout?.visibility = View.VISIBLE
                    }

                    storage_info_textview?.visibility = View.VISIBLE

                    storage_info_address_textview?.visibility = View.VISIBLE
                    storage_info_address_textview?.text = String.format(getString(R.string.baker_address, mContract?.delegate))

                    val hasMnemonics = Storage(activity!!).hasMnemonics()
                    if (hasMnemonics)
                    {
                        val seed = Storage(activity!!).getMnemonics()

                        if (seed.mnemonics.isEmpty())
                        {
                            //remove_delegate_button_layout?.visibility = View.GONE
                            update_storage_form_card?.visibility = View.GONE

                            no_mnemonics?.visibility = View.VISIBLE
                        }
                    }
                }
                else
                {
                    limits_info_textview?.visibility = View.VISIBLE
                    limits_info_textview?.text = getString(R.string.redelegate_address_info)

                    update_storage_form_card?.visibility = View.VISIBLE

                    redelegate_address_layout?.visibility = View.VISIBLE


                    storage_info_textview?.visibility = View.GONE
                    storage_info_address_textview?.visibility = View.GONE


                    //TODO this is about having the right buttons
                    if (!getThreshold().isNullOrEmpty())
                    {

                        when (askingForMultisigButton())
                        {
                            MULTISIG_UPDATE_STORAGE_ENUM.CONFIRM_UPDATE ->
                            {
                                update_storage_button_layout?.visibility = View.VISIBLE
                                remove_delegate_button_layout?.visibility = View.GONE

                                notify_delegate_button_layout?.visibility = View.GONE
                                request_delegate_button_layout?.visibility = View.GONE
                            }

                            MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES ->
                            {
                                update_storage_button_layout?.visibility = View.GONE
                                remove_delegate_button_layout?.visibility = View.GONE
                                notify_delegate_button_layout?.visibility = View.GONE

                                request_delegate_button_layout?.visibility = View.VISIBLE
                                request_delegate_button.text = getString(R.string.request_add_delegation)
                            }

                            MULTISIG_UPDATE_STORAGE_ENUM.NOTIFY_NOTARY ->
                            {
                                update_storage_button_layout?.visibility = View.GONE
                                remove_delegate_button_layout?.visibility = View.GONE

                                notify_delegate_button_layout?.visibility = View.VISIBLE
                                notify_delegate_button.text = getString(R.string.notify_add_delegation)

                                request_delegate_button_layout?.visibility = View.GONE
                            }

                            else ->
                            {
                                update_storage_button_layout?.visibility = View.GONE
                                remove_delegate_button_layout?.visibility = View.GONE
                                notify_delegate_button_layout?.visibility = View.GONE
                                request_delegate_button_layout?.visibility = View.GONE
                            }
                        }
                    }
                    else
                    {
                        update_storage_button_layout?.visibility = View.VISIBLE
                        remove_delegate_button_layout?.visibility = View.GONE
                    }



                    val hasMnemonics = Storage(activity!!).hasMnemonics()
                    if (hasMnemonics)
                    {
                        val seed = Storage(activity!!).getMnemonics()

                        if (seed.mnemonics.isEmpty())
                        {
                            limits_info_textview?.text = getString(R.string.no_baker_at_the_moment)
                            update_storage_form_card?.visibility = View.GONE

                            no_mnemonics?.visibility = View.VISIBLE
                        }
                    }
                }

                loading_textview?.visibility = View.GONE
                loading_textview?.text = null
                }
        }
        else
        {
            // mContract is null then just show "-"
            //loading_textview will be hidden behind other textview

            loading_textview?.visibility = View.VISIBLE
            loading_textview?.text = "-"
        }
    }

    private fun getSalt():Int?
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")
            if (args != null)
            {
                val argsMasterKey = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray
                val masterKeySaltJSONObject = argsMasterKey[1] as JSONObject

                val saltLeft = (masterKeySaltJSONObject["args"] as JSONArray)[0] as JSONObject

                val saltRight = (masterKeySaltJSONObject["args"] as JSONArray)[0] as JSONObject

                return DataExtractor.getStringFromField(saltLeft, "int").toInt()
            }
        }

        return null
    }

    private fun getStorageSecureKeyHash(): String?
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

            // get securekey hash
            if (args != null)
            {
                val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args")
                if (argsSecureKey != null)
                {
                    val secureKeyJSONObject = argsSecureKey[0] as JSONObject
                    return DataExtractor.getStringFromField(secureKeyJSONObject, "string")
                }
            }
        }

        return null
    }

    private fun getThreshold(): String?
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)
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

    private fun getNumberAndSpot(publicKey:String): Pair<Int, Int>
    {
        val signatories = getSignatoriesList()
        if (!signatories.isNullOrEmpty())
        {
            return Pair(signatories.indexOf(publicKey), signatories.size)
        }

        return Pair(-1, -1)
    }

    private fun getSignatoriesList(): ArrayList<String>
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args") as JSONArray

            val counter = DataExtractor.getStringFromField(args[0] as JSONObject, "int")
            if (counter != null)
            {
                val argsPk = (DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray)[1] as JSONArray

                val list = ArrayList<String> ()

                for (it in 0 until argsPk.length())
                {
                    val item = argsPk.getJSONObject(it)

                    val pk = DataExtractor.getStringFromField(item, "string")
                    list.add(pk)
                }

                return list
            }
        }

        return ArrayList(SIGNATORIES_CAPACITY)
    }

    private fun getMultisigCounter(): String?
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args") as JSONArray

            return DataExtractor.getStringFromField(args[0] as JSONObject, "int")
        }

        return null
    }

    // volley
    private fun startPostRequestLoadFinalizeRemoveDelegate(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isRemoveButtonValid() && mDelegatePayload != null && mDelegateFees != -1L)
        {

            var postParams = JSONObject()
            postParams.put("src", mnemonicsData.pkh)
            postParams.put("src_pk", mnemonicsData.pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            //dstObject.put("dst", mDstAccount)

            dstObject.put("dst", pkh())


            if (!getStorageSecureKeyHash().isNullOrEmpty())
            {
                dstObject.put("contract_type", "remove_delegate_slc")
                dstObject.put("edsig", mSig)
            }
            else if (!getThreshold().isNullOrEmpty())
            {

            }
            else
            {
                dstObject.put("contract_type", "remove_delegate")
            }

            dstObject.put("amount", (0).toLong())

            dstObject.put("fee", mDelegateFees)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

            if (/*isRemoveDelegatePayloadValid(mDelegatePayload!!, postParams)*/true)
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mDelegatePayload!!.hexToByteArray()

                val xLen = zeroThree.size
                val yLen = byteArrayThree.size
                val result = ByteArray(xLen + yLen)

                System.arraycopy(zeroThree, 0, result, 0, xLen)
                System.arraycopy(byteArrayThree, 0, result, xLen, yLen)

                val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                val sk = CryptoUtils.generateSk(mnemonics, "")
                val signature = KeyPair.sign(sk, result)

                //TODO verify signature
                //val signVerified = KeyPair.verifySign(signature, pk, payload_hash)

                val pLen = byteArrayThree.size
                val sLen = signature.size
                val newResult = ByteArray(pLen + sLen)

                System.arraycopy(byteArrayThree, 0, newResult, 0, pLen)
                System.arraycopy(signature, 0, newResult, pLen, sLen)

                var payloadsign = newResult.toNoPrefixHexString()

                val stringRequest = object : StringRequest(Method.POST, url,
                        Response.Listener<String> {
                            if (swipe_refresh_layout != null)
                            {
                                //there's no need to do anything because we call finish()
                                onFinalizeDelegationLoadComplete(null)

                                mCallback?.finish(R.id.remove_delegate_succeed)
                            }
                        },
                        Response.ErrorListener
                        {
                            if (swipe_refresh_layout != null)
                            {
                                onFinalizeDelegationLoadComplete(it)
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

                stringRequest.tag = REMOVE_DELEGATE_FINALIZE_TAG

                mFinalizeDelegateLoading = true
                VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(stringRequest)
            }
            else
            {
                val volleyError = VolleyError(getString(R.string.generic_error))
                onFinalizeDelegationLoadComplete(volleyError)
            }
        }
        else
        {
            val volleyError = VolleyError(getString(R.string.generic_error))
            onFinalizeDelegationLoadComplete(volleyError)
        }
    }

    // volley
    private fun startPostRequestLoadFinalizeAddDelegate(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isAddButtonValid() && mDelegatePayload != null && mDelegateTezosAddress != null && mDelegateFees != -1L)
        {
            var postParams = JSONObject()
            postParams.put("src", mnemonicsData.pkh)
            postParams.put("src_pk", mnemonicsData.pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            //dstObject.put("dst", mDstAccount)

            dstObject.put("dst", pkh())

            when {
                getStorageSecureKeyHash() != null ->
                {
                    dstObject.put("contract_type", "add_delegate_slc")
                    dstObject.put("edsig", mSig)
                }
                getThreshold() != null ->
                {
                    //TODO necessary to verify the binary
                }
                else ->
                {
                    dstObject.put("contract_type", "add_delegate")
                }
            }

            dstObject.put("dst_account", mDelegateTezosAddress)

            dstObject.put("amount", (0).toLong())

            dstObject.put("fee", mDelegateFees)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

            if (/*isAddDelegatePayloadValid(mDelegatePayload!!, postParams)*/true)
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mDelegatePayload!!.hexToByteArray()

                val xLen = zeroThree.size
                val yLen = byteArrayThree.size
                val result = ByteArray(xLen + yLen)

                System.arraycopy(zeroThree, 0, result, 0, xLen)
                System.arraycopy(byteArrayThree, 0, result, xLen, yLen)

                val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                val sk = CryptoUtils.generateSk(mnemonics, "")
                val signature = KeyPair.sign(sk, result)

                //TODO verify signature
                //val signVerified = KeyPair.verifySign(signature, pk, payload_hash)

                val pLen = byteArrayThree.size
                val sLen = signature.size
                val newResult = ByteArray(pLen + sLen)

                System.arraycopy(byteArrayThree, 0, newResult, 0, pLen)
                System.arraycopy(signature, 0, newResult, pLen, sLen)

                var payloadsign = newResult.toNoPrefixHexString()

                val stringRequest = object : StringRequest(Request.Method.POST, url,
                        Response.Listener<String> {
                            if (swipe_refresh_layout != null)
                            {
                                //there's no need to do anything because we call finish()
                                onFinalizeDelegationLoadComplete(null)

                                mCallback?.finish(R.id.add_delegate_succeed)
                            }
                        },
                        Response.ErrorListener
                        {
                            if (swipe_refresh_layout != null)
                            {
                                onFinalizeDelegationLoadComplete(it)
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

                stringRequest.tag = DELEGATE_FINALIZE_TAG

                mFinalizeDelegateLoading = true
                VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(stringRequest)
            }
            else
            {
                val volleyError = VolleyError(getString(R.string.generic_error))
                onFinalizeDelegationLoadComplete(volleyError)
            }
        }
        else
        {
            val volleyError = VolleyError(getString(R.string.generic_error))
            onFinalizeDelegationLoadComplete(volleyError)
        }
    }

    private fun onFinalizeDelegationLoadComplete(error: VolleyError?)
    {
        // everything is over, there's no call to make
        cancelRequests(false)

        if (error != null)
        {
            transferLoading(false)

            showSnackBar(error, null)
        }
        else
        {
            // there is no finish
            transferLoading(false)
        }
    }

    private fun onInitDelegateLoadComplete(error:VolleyError?)
    {
        mInitDelegateLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            mDelegatePayload = null

            fee_edittext.isEnabled = true
            fee_edittext.isFocusable = false
            fee_edittext.isClickable = false
            fee_edittext.isLongClickable = false
            fee_edittext.hint = getString(R.string.click_for_fees)

            fee_edittext.setOnClickListener {
                startInitDelegationLoading()
            }

            if(error != null)
            {
                //TODO handle the show snackbar
                showSnackBar(error, null)
            }
        }
        else
        {
            transferLoading(false)
            cancelRequests(false)
            // it's signed, looks like it worked.
            //transferLoading(true)
        }
    }

    private fun onInitRemoveDelegateLoadComplete(error:VolleyError?)
    {
        mInitRemoveDelegateLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            mDelegatePayload = null

            fee_edittext?.isEnabled = true
            fee_edittext?.isFocusable = false
            fee_edittext?.isClickable = false
            fee_edittext?.isLongClickable = false
            fee_edittext?.hint = getString(R.string.click_for_fees)

            fee_edittext?.setOnClickListener {
                startInitRemoveDelegateLoading()
            }

            if(error != null)
            {
                //TODO handle the show snackbar
                showSnackBar(error, null)
            }
        }
        else
        {
            transferLoading(false)
            cancelRequests(false)
            // it's signed, looks like it worked.
            //transferLoading(true)
        }
    }

    private fun pk():String?
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()
        return mnemonicsData.pk
    }

    private fun pkhtz1():String?
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()
        return mnemonicsData.pkh
    }

    // volley
    private fun startPostRequestLoadInitAddDelegate()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val pk = if (mnemonicsData.pk.isNullOrEmpty())
        {
            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
        }
        else
        {
            mnemonicsData.pk
        }

        var postParams = JSONObject()
        postParams.put("src", mnemonicsData.pkh)
        postParams.put("src_pk", pk)

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        //dstObject.put("dst", mDstAccount)

        dstObject.put("dst", pkh())

        dstObject.put("amount", "0")

        if (!getStorageSecureKeyHash().isNullOrEmpty())
        {

            dstObject.put("entrypoint", "appel_clef_maitresse")

            val dataVisitable = Primitive(
                    Primitive.Name.Right,
                    arrayOf(
                            Primitive(Primitive.Name.Pair,
                                    arrayOf(
                                            Visitable.sequenceOf(
                                                    Primitive(Primitive.Name.DROP),
                                                    Primitive(
                                                            Primitive.Name.NIL, arrayOf(Primitive(Primitive.Name.operation))
                                                    ),
                                                    Primitive(
                                                            Primitive.Name.PUSH,
                                                            arrayOf(
                                                                    Primitive(Primitive.Name.key_hash),
                                                                    Visitable.keyHash(mDelegateTezosAddress!!)
                                                            )
                                                    ),
                                                    Primitive(Primitive.Name.SOME),
                                                    Primitive(Primitive.Name.SET_DELEGATE),
                                                    Primitive(Primitive.Name.CONS)

                                            ),
                                            Visitable.keyHash(mnemonicsData.pkh)
                                    )
                            )
                    )
            )

            val o = ByteArrayOutputStream()
            o.write(0x05)

            val dataPacker = Packer(o)
            dataVisitable.accept(dataPacker)

            val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

            val addressAndChainVisitable = Primitive(Primitive.Name.Pair,
                    arrayOf(
                            Visitable.address(pkh()!!),
                            Visitable.chainID(getString(R.string.chain_ID))
                    )
            )

            val output = ByteArrayOutputStream()
            output.write(0x05)

            val p = Packer(output)
            addressAndChainVisitable.accept(p)

            val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()


            val salt = getSalt()
            val saltVisitable = Visitable.integer(salt!!.toLong())

            val outputStream = ByteArrayOutputStream()
            outputStream.write(0x05)

            val packer = Packer(outputStream)
            saltVisitable.accept(packer)

            val saltPack = (packer.output as ByteArrayOutputStream).toByteArray()



            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val sk = CryptoUtils.generateSk(mnemonics, "")

            //val signedData = KeyPair.b2b("0x".hexToByteArray()+dataPack + addressAndChainPack + saltPack)
            //val signature = KeyPair.sign(sk, signedData)

            val signature = KeyPair.sign(sk,dataPack + addressAndChainPack + saltPack)
            val edsig = CryptoUtils.generateEDSig(signature)


            val spendingLimitFile = "spending_limit_delegate.json"
            val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                    .use {
                        it.readText()
                    }

            val value = JSONObject(contract)
            val args = ((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

            val pkValue = args[0] as JSONObject
            pkValue.put("string", pk)

            val sig = args[1] as JSONObject
            sig.put("string", edsig)

            mSig = edsig

            val argsRight = ((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

            val argsDelegate = ((argsRight[0] as JSONArray)[2] as JSONObject)["args"] as JSONArray
            val delegate = argsDelegate[1] as JSONObject
            delegate.put("string", mDelegateTezosAddress)

            val masterKey = argsRight[1] as JSONObject
            masterKey.put("string", mnemonicsData.pkh)

            dstObject.put("parameters", value)
        }
        else if (!getThreshold().isNullOrEmpty())
        {
            dstObject.put("entrypoint", "default")

            val dataVisitable = Primitive(
                    Primitive.Name.Pair,
                    arrayOf(
                            Primitive(Primitive.Name.Pair,
                                    arrayOf(
                                            Visitable.chainID(getString(R.string.chain_ID)),
                                            Visitable.address(pkh()!!)
                                    )
                            ),
                            Primitive(Primitive.Name.Pair,
                                    arrayOf(
                                            Visitable.integer(getMultisigCounter()!!.toLong()),
                                            Primitive(Primitive.Name.Right,
                                                    arrayOf(
                                                            Primitive (Primitive.Name.Left,
                                                                    arrayOf(
                                                                            Primitive(Primitive.Name.Some,
                                                                                    arrayOf(
                                                                                            Visitable.keyHash(mDelegateTezosAddress!!)
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                                    )
                                            )
                                    )
                            )
                    )
            )

            val o = ByteArrayOutputStream()
            o.write(0x05)

            val dataPacker = Packer(o)
            dataVisitable.accept(dataPacker)

            val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val sk = CryptoUtils.generateSk(mnemonics, "")

            val signature = KeyPair.sign(sk, dataPack)
            val edsig = CryptoUtils.generateEDSig(signature)

            val spendingLimitFile = "multisig_set_delegate.json"
            val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                    .use {
                        it.readText()
                    }

            val value = JSONObject(contract)

            /*
            val vanillaArgSig = (value["args"] as JSONArray)[1] as JSONArray
            val argSig = ((((value["args"] as JSONArray)[1] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject
            //TODO handle verification here
            argSig.put("string", edsig)
            mSig = edsig
            */

            val sigs = (value["args"] as JSONArray)[1] as JSONArray
            sigs.remove(0)

            val numberAndSpot = getNumberAndSpot(pk)

            for (i in 0 until numberAndSpot.second)
            {
                val sigParam = JSONObject()

                if (i == numberAndSpot.first)
                {
                    sigParam.put("prim", "Some")

                    val argSig = JSONArray()
                    val argSigStr = JSONObject()

                    argSigStr.put("string", edsig)
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

            var decodedValue = Base58.decode(mDelegateTezosAddress)
            var bakerBytes = decodedValue.slice(3 until (decodedValue.size - 4)).toByteArray()
            bakerBytes = byteArrayOf(0x00) + bakerBytes
            argBaker.put("bytes", bakerBytes.toNoPrefixHexString())

            dstObject.put("parameters", value)
        }
        else
        {
            dstObject.put("entrypoint", "do")

            val json = JSONArray(String.format(getString(R.string.set_delegate_contract), mDelegateTezosAddress))
            dstObject.put("parameters", json)
        }

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (swipe_refresh_layout != null)
            {
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
            }

        }, Response.ErrorListener
        {
            if (swipe_refresh_layout != null)
            {
                onInitDelegateLoadComplete(it)

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

        jsObjRequest.tag = DELEGATE_INIT_TAG
        mInitDelegateLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
    }




    // volley
    private fun startPostRequestLoadInitRequestUpdateStorage()
    {
        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.manager_key_url), pkh)

// Request a string response from the provided URL.
            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            {
                if (swipe_refresh_script_layout != null)
                {
                    //addContractAddressesFromJSON(it, pkh)

                    //reloadList()
                    //onDelegatedAddressesComplete(true)

                    //startGetRequestLoadMultisigAsSignatoryContracts()
                }
            },
                    Response.ErrorListener {

                        if (swipe_refresh_script_layout != null)
                        {
                            //onDelegatedAddressesComplete(false)
                            //showSnackbarError(it)
                        }
                    })

//jsonArrayRequest.tag = ContractsFragment.LOAD_DELEGATED_ADDRESSES_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    // volley
    private fun startPostRequestLoadInitNotifyUpdateStorage()
    {
        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.manager_key_url), pkh)

            //TODO here we will need to check mContract.delegate to know if we remove or add a delegate.

// Request a string response from the provided URL.
            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            {
                if (swipe_refresh_script_layout != null)
                {
                    //addContractAddressesFromJSON(it, pkh)

                    //reloadList()
                    //onDelegatedAddressesComplete(true)

                    //startGetRequestLoadMultisigAsSignatoryContracts()
                }
            },
                    Response.ErrorListener {

                        if (swipe_refresh_script_layout != null)
                        {
                            //onDelegatedAddressesComplete(false)
                            //showSnackbarError(it)
                        }
                    })

//jsonArrayRequest.tag = ContractsFragment.LOAD_DELEGATED_ADDRESSES_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }


    private fun updateMnemonicsData(data: Storage.MnemonicsData, pk:String):String
    {
        with(Storage(activity!!)) {
            saveSeed(Storage.MnemonicsData(data.pkh, pk, data.mnemonics))
        }
        return pk
    }

    // volley
    private fun startPostRequestLoadInitRemoveDelegate()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val pk = if (mnemonicsData.pk.isNullOrEmpty())
        {
            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
        }
        else
        {
            mnemonicsData.pk
        }

        var postParams = JSONObject()
        postParams.put("src", mnemonicsData.pkh)
        postParams.put("src_pk", pk)

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        //dstObject.put("dst", mDstAccount)

        dstObject.put("dst", pkh())

        //dstObject.put("amount", (mTransferAmount*1000000).toLong().toString())
        dstObject.put("amount", "0")


        if (!getStorageSecureKeyHash().isNullOrEmpty())
        {
            dstObject.put("entrypoint", "appel_clef_maitresse")

            val dataVisitable = Primitive(
                    Primitive.Name.Right,
                    arrayOf(
                            Primitive(Primitive.Name.Pair,
                                    arrayOf(
                                            Visitable.sequenceOf(
                                                    Primitive(Primitive.Name.DROP),
                                                    Primitive(
                                                            Primitive.Name.NIL, arrayOf(Primitive(Primitive.Name.operation))
                                                    ),
                                                    Primitive(
                                                            Primitive.Name.NONE,
                                                            arrayOf(
                                                                    Primitive(Primitive.Name.key_hash)
                                                            )
                                                    ),
                                                    Primitive(Primitive.Name.SET_DELEGATE),
                                                    Primitive(Primitive.Name.CONS)

                                            ),
                                            Visitable.keyHash(mnemonicsData.pkh)
                                    )
                            )
                    )
            )

            val o = ByteArrayOutputStream()
            o.write(0x05)

            val dataPacker = Packer(o)
            dataVisitable.accept(dataPacker)

            val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

            val addressAndChainVisitable = Primitive(Primitive.Name.Pair,
                    arrayOf(
                            Visitable.address(pkh()!!),
                            Visitable.chainID(getString(R.string.chain_ID))
                    )
            )

            val output = ByteArrayOutputStream()
            output.write(0x05)

            val p = Packer(output)
            addressAndChainVisitable.accept(p)

            val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()

            val salt = getSalt()
            val saltVisitable = Visitable.integer(salt!!.toLong())

            val outputStream = ByteArrayOutputStream()
            outputStream.write(0x05)

            val packer = Packer(outputStream)
            saltVisitable.accept(packer)

            val saltPack = (packer.output as ByteArrayOutputStream).toByteArray()



            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val sk = CryptoUtils.generateSk(mnemonics, "")

            //val signedData = KeyPair.b2b("0x".hexToByteArray()+dataPack + addressAndChainPack + saltPack)
            //val signature = KeyPair.sign(sk, signedData)

            val signature = KeyPair.sign(sk,"0x".hexToByteArray() + dataPack + addressAndChainPack + saltPack)
            val edsig = CryptoUtils.generateEDSig(signature)


            val spendingLimitFile = "spending_limit_remove_delegate.json"
            val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                    .use {
                        it.readText()
                    }

            val value = JSONObject(contract)
            val args = ((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

            val pkValue = args[0] as JSONObject
            pkValue.put("string", pk)

            val sig = args[1] as JSONObject
            sig.put("string", edsig)

            mSig = edsig

            val argsRight = ((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

            val masterKey = argsRight[1] as JSONObject
            masterKey.put("string", mnemonicsData.pkh)

            dstObject.put("parameters", value)
        }
        else if (!getThreshold().isNullOrEmpty())
        {
            dstObject.put("entrypoint", "default")

            val dataVisitable = Primitive(
                    Primitive.Name.Pair,
                    arrayOf(
                            Primitive(Primitive.Name.Pair,
                                    arrayOf(
                                            Visitable.chainID(getString(R.string.chain_ID)),
                                            Visitable.address(pkh()!!)
                                    )
                            ),

                            Primitive(Primitive.Name.Pair,
                                    arrayOf(
                                            Visitable.integer(getMultisigCounter()!!.toLong()),
                                            Primitive(Primitive.Name.Right,
                                                    arrayOf(
                                                            Primitive (Primitive.Name.Left,
                                                                    arrayOf(
                                                                            Primitive(Primitive.Name.None)
                                                                    )
                                                            )
                                                    )
                                            )
                                    )
                            )
                    )
            )

            val o = ByteArrayOutputStream()
            o.write(0x05)

            val dataPacker = Packer(o)
            dataVisitable.accept(dataPacker)

            val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val sk = CryptoUtils.generateSk(mnemonics, "")

            val signature = KeyPair.sign(sk, dataPack)
            val edsig = CryptoUtils.generateEDSig(signature)

            val spendingLimitFile = "multisig_withdraw_delegate.json"
            val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                    .use {
                        it.readText()
                    }

            val value = JSONObject(contract)

            val sigs = (value["args"] as JSONArray)[1] as JSONArray
            sigs.remove(0)

            val numberAndSpot = getNumberAndSpot(pk)

            for (i in 0 until numberAndSpot.second)
            {
                val sigParam = JSONObject()

                if (i == numberAndSpot.first)
                {
                    sigParam.put("prim", "Some")

                    val argSig = JSONArray()
                    val argSigStr = JSONObject()

                    argSigStr.put("string", edsig)
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

            dstObject.put("parameters", value)
        }
        else
        {
            dstObject.put("entrypoint", "do")

            val json = JSONArray(getString(R.string.remove_delegate_contract))
            dstObject.put("parameters", json)
        }

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            if (swipe_refresh_layout != null)
            {
                mDelegatePayload = answer.getString("result")
                mDelegateFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mDelegatePayload != null && mDelegateFees != -1L)
                {
                    onInitRemoveDelegateLoadComplete(null)

                    val feeInTez = mDelegateFees.toDouble()/1000000.0
                    fee_edittext?.setText(feeInTez.toString())

                    validateRemoveDelegateButton(isDelegateFeeValid())
                }
                else
                {
                    val volleyError = VolleyError(getString(R.string.generic_error))
                    onInitRemoveDelegateLoadComplete(volleyError)
                    mClickCalculate = true

                    //the call failed
                }
            }
        }, Response.ErrorListener
        {
            if (swipe_refresh_layout != null)
            {
                onInitRemoveDelegateLoadComplete(it)

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

        jsObjRequest.tag = REMOVE_DELEGATE_INIT_TAG
        mInitRemoveDelegateLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
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

    private fun putFeesToNegative()
    {
        fee_edittext?.setText("")

        mClickCalculate = false
        fee_edittext?.isEnabled = false
        fee_edittext?.hint = getString(R.string.neutral)

        mDelegateFees = -1

        mDelegatePayload = null
    }

    private fun putPayButtonToNull()
    {
        update_storage_button?.text = getString(R.string.delegate_format, "")
    }

    private fun showSnackBar(error:VolleyError?, message:String?)
    {
        if (error != null)
        {
            mCallback?.showSnackBar(error.toString(), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))

            loading_textview?.text = getString(R.string.generic_error)
        }
        else if (message != null)
        {
            mCallback?.showSnackBar(message, ContextCompat.getColor(context!!, android.R.color.holo_green_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }
    }

    private fun validateAddButton(validate: Boolean)
    {
        if (activity != null)
        {
            //val themeBundle = arguments!!.getBundle(CustomTheme.TAG)
            //val theme = CustomTheme.fromBundle(themeBundle)

            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

            if (validate)
            {
                update_storage_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                update_storage_button_layout?.isEnabled = true
                update_storage_button_layout?.background = makeSelector(theme)

                val drawables = update_storage_button?.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                }


                notify_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                notify_delegate_button_layout?.isEnabled = true
                notify_delegate_button_layout?.background = makeSelector(theme)

                val drawablesNotify = notify_delegate_button?.compoundDrawables
                if (drawablesNotify != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawablesNotify[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                }


                request_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                request_delegate_button_layout?.isEnabled = true
                request_delegate_button_layout?.background = makeSelector(theme)

                val drawablesRequest = request_delegate_button?.compoundDrawables
                if (drawablesRequest != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawablesRequest[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                }


            }
            else
            {
                update_storage_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                update_storage_button_layout?.isEnabled = false

                val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
                update_storage_button_layout?.background = makeSelector(greyTheme)

                val drawables = update_storage_button?.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }



                request_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                request_delegate_button_layout?.isEnabled = false

                val greyThemeRequest = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
                request_delegate_button_layout?.background = makeSelector(greyThemeRequest)

                val drawablesRequest = request_delegate_button?.compoundDrawables
                if (drawablesRequest != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawablesRequest[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }

                notify_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                notify_delegate_button_layout?.isEnabled = false

                val greyThemeNotify = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
                notify_delegate_button_layout?.background = makeSelector(greyThemeNotify)

                val drawablesNotify = notify_delegate_button?.compoundDrawables
                if (drawablesNotify != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawablesNotify[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }

            }
        }
    }

    private fun validateRemoveDelegateButton(validate: Boolean)
    {
        if (validate)
        {
            //val theme = CustomTheme(R.color.tz_error, R.color.tz_accent, R.color.tz_light)
            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

            remove_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            remove_delegate_button_layout?.isEnabled = true
            remove_delegate_button_layout?.background = makeSelector(theme)

            val drawables = remove_delegate_button?.compoundDrawables
            if (activity != null && drawables != null)
            {
                val wrapDrawable = DrawableCompat.wrap(drawables[0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            }

            notify_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            notify_delegate_button_layout?.isEnabled = true
            notify_delegate_button_layout?.background = makeSelector(theme)

            val drawablesNotify = notify_delegate_button?.compoundDrawables
            if (drawablesNotify != null)
            {
                val wrapDrawable = DrawableCompat.wrap(drawablesNotify[0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            }


            request_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            request_delegate_button_layout?.isEnabled = true
            request_delegate_button_layout?.background = makeSelector(theme)

            val drawablesRequest = request_delegate_button?.compoundDrawables
            if (drawablesRequest != null)
            {
                val wrapDrawable = DrawableCompat.wrap(drawablesRequest[0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            }

        }
        else
        {
            remove_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            remove_delegate_button_layout?.isEnabled = false
            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            remove_delegate_button_layout?.background = makeSelector(greyTheme)

            val drawables = remove_delegate_button?.compoundDrawables
            if (activity != null && drawables != null)
            {
                val wrapDrawable = DrawableCompat.wrap(drawables[0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
            }


            request_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            request_delegate_button_layout?.isEnabled = false

            val greyThemeRequest = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            request_delegate_button_layout?.background = makeSelector(greyThemeRequest)

            val drawablesRequest = request_delegate_button?.compoundDrawables
            if (drawablesRequest != null)
            {
                val wrapDrawable = DrawableCompat.wrap(drawablesRequest[0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
            }

            notify_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            notify_delegate_button_layout?.isEnabled = false

            val greyThemeNotify = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            notify_delegate_button_layout?.background = makeSelector(greyThemeNotify)

            val drawablesNotify = notify_delegate_button?.compoundDrawables
            if (drawablesNotify != null)
            {
                val wrapDrawable = DrawableCompat.wrap(drawablesNotify[0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
            }

        }
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    private inner class GenericTextWatcher internal constructor(private val v: View) : TextWatcher
    {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable)
        {
            val i = v.id

            if (i == R.id.redelegate_address_edittext && !isDelegateTezosAddressEquals(editable))
            {
                putTzAddressInRed(false)

                //TODO text changed
                //TODO load again but only if we don't have any same forged data.

                if (isInputDataValid())
                {
                    startInitDelegationLoading()
                }
                else
                {
                    validateAddButton(false)

                    cancelRequests(false)
                    transferLoading(false)

                    putFeesToNegative()
                    putPayButtonToNull()
                }
            }
            else if (i != R.id.amount_edittext && i != R.id.redelegate_address_edittext)
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }

    private fun isDelegateTezosAddressEquals(editable: Editable):Boolean
    {
        val isTezosAddressEquals = true

        if (!TextUtils.isEmpty(editable))
        {
            val tezosAddress = editable.toString()
            return tezosAddress == mDelegateTezosAddress
        }
        return isTezosAddressEquals
    }

    fun isInputDataValid(): Boolean
    {
        return isTzAddressValid()
    }

    private fun isTzAddressValid(): Boolean
    {
        var isTzAddressValid = false

        if (!TextUtils.isEmpty(redelegate_address_edittext.text))
        {
            if (Utils.isTzAddressValid(redelegate_address_edittext.text!!.toString()))
            {
                mDelegateTezosAddress = redelegate_address_edittext.text.toString()
                isTzAddressValid = true
            }
        }

        return isTzAddressValid
    }

    private fun isDelegateFeeValid():Boolean
    {
        if (askingForMultisigButton() == MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES)
        {
            // there's no need for fees
            return true
        }
        else
        {
            if (fee_edittext?.text != null && !TextUtils.isEmpty(fee_edittext?.text))
            {
                try
                {
                    //val amount = java.lang.Double.parseDouble()
                    val fee = fee_edittext.text.toString().toDouble()

                    if (fee >= 0.000001f)
                    {
                        val longTransferFee = fee*1000000
                        mDelegateFees = longTransferFee.roundToLong()
                        return true
                    }
                }
                catch (e: NumberFormatException)
                {
                    mDelegateFees = -1
                    return false
                }
            }
        }

        return false
    }

    private fun putEverythingInRed()
    {
        this.putTzAddressInRed(true)
    }

    private fun isAddButtonValid(): Boolean
    {
        return mDelegatePayload != null
                && isDelegateFeeValid()
                && isInputDataValid()
    }

    private fun isRemoveButtonValid(): Boolean
    {
        return mDelegatePayload != null
                && isDelegateFeeValid()
    }

    private fun putTzAddressInRed(red: Boolean)
    {
        val color: Int

        val tzAddressValid = isTzAddressValid()

        if (red && !tzAddressValid)
        {
            color = R.color.tz_error
        }
        else
        {
            color = R.color.tz_accent
        }

        redelegate_address_edittext.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun onDelegateClick()
    {
        val dialog = AuthenticationDialog()
        if (isFingerprintAllowed() && hasEnrolledFingerprints())
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

            when (askingForMultisigButton())
            {
                MULTISIG_UPDATE_STORAGE_ENUM.CONFIRM_UPDATE ->
                {
                    startFinalizeAddDelegateLoading()
                }

                MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES ->
                {
                    startFinalizeOngoingMultisigAddDelegateLoading()
                }

                MULTISIG_UPDATE_STORAGE_ENUM.NOTIFY_NOTARY -> {}
                MULTISIG_UPDATE_STORAGE_ENUM.NEITHER_NOTARY_NOR_SIGNATORY -> {}
                MULTISIG_UPDATE_STORAGE_ENUM.NO_NOTARY_YET -> {}
            }
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity!!.supportFragmentManager, "Authentication")
    }

    private fun onRemoveDelegateClick()
    {
        val dialog = AuthenticationDialog()
        if (isFingerprintAllowed() && hasEnrolledFingerprints())
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices().prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = {
                validateKeyAuthenticationRemoveDelegate(it)
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
            startFinalizeRemoveDelegateLoading()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity!!.supportFragmentManager, "Authentication")
    }

    private fun isFingerprintAllowed():Boolean
    {
        return mCallback!!.isFingerprintAllowed()
    }

    private fun hasEnrolledFingerprints():Boolean
    {
        return mCallback!!.hasEnrolledFingerprints()
    }

    private fun saveFingerprintAllowed(useInFuture:Boolean)
    {
        mCallback?.saveFingerprintAllowed(useInFuture)
    }

    /**
     * Fingerprint was invalidated, decide what to do in this case.
     */
    private fun onFingerprintInvalidation(useInFuture: Boolean)
    {
        saveFingerprintAllowed(useInFuture)
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
            when (askingForMultisigButton())
            {
                MULTISIG_UPDATE_STORAGE_ENUM.CONFIRM_UPDATE ->
                {
                    startFinalizeAddDelegateLoading()
                }

                MULTISIG_UPDATE_STORAGE_ENUM.REQUEST_TO_SIGNATORIES ->
                {
                    startFinalizeOngoingMultisigAddDelegateLoading()
                }

                MULTISIG_UPDATE_STORAGE_ENUM.NOTIFY_NOTARY -> {}
                MULTISIG_UPDATE_STORAGE_ENUM.NEITHER_NOTARY_NOR_SIGNATORY -> {}
                MULTISIG_UPDATE_STORAGE_ENUM.NO_NOTARY_YET -> {}
            }

        }
        else
        {
            onDelegateClick()
        }
    }

    private fun validateKeyAuthenticationRemoveDelegate(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices().validateFingerprintAuthentication(cryptoObject))
        {
            startFinalizeRemoveDelegateLoading()
        }
        else
        {
            onRemoveDelegateClick()
        }
    }

    private fun cancelRequests(resetBooleans:Boolean)
    {
        if (activity != null)
        {
            val requestQueue = VolleySingleton.getInstance(activity!!.applicationContext).requestQueue
            requestQueue?.cancelAll(DELEGATE_INIT_TAG)
            requestQueue?.cancelAll(DELEGATE_FINALIZE_TAG)
            requestQueue?.cancelAll(REMOVE_DELEGATE_INIT_TAG)
            requestQueue?.cancelAll(REMOVE_DELEGATE_FINALIZE_TAG)
            requestQueue?.cancelAll(CONTRACT_INFO_TAG)
            requestQueue?.cancelAll(LOAD_CONTRACT_MANAGER_TAG)
            requestQueue?.cancelAll(STORAGE_INFO_TAG)

            if (resetBooleans)
            {
                mInitDelegateLoading = false
                mFinalizeDelegateLoading = false
                mInitRemoveDelegateLoading = false
                mFinalizeRemoveDelegateLoading = false
                mContractInfoLoading = false
                mStorageInfoLoading = false
                mContractManagerLoading = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(DELEGATE_INIT_TAG, mInitDelegateLoading)
        outState.putBoolean(DELEGATE_FINALIZE_TAG, mFinalizeDelegateLoading)

        outState.putBoolean(REMOVE_DELEGATE_INIT_TAG, mInitRemoveDelegateLoading)
        outState.putBoolean(REMOVE_DELEGATE_FINALIZE_TAG, mFinalizeRemoveDelegateLoading)

        outState.putBoolean(CONTRACT_INFO_TAG, mContractInfoLoading)

        outState.putString(DELEGATE_PAYLOAD_KEY, mDelegatePayload)

        outState.putString(DELEGATE_TEZOS_ADDRESS_KEY, mDelegateTezosAddress)

        outState.putLong(DELEGATE_FEE_KEY, mDelegateFees)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)

        outState.putBoolean(WALLET_AVAILABLE_KEY, mWalletEnabled)

        outState.putBundle(CONTRACT_DATA_KEY, this.toBundle(mContract))

        outState.putString(STORAGE_DATA_KEY, mStorage)

        outState.putBoolean(STORAGE_INFO_TAG, mStorageInfoLoading)

        outState.putString(CONTRACT_SIG_KEY, mSig)

        outState.putString(CONTRACT_MANAGER_KEY, mContractManager)

        outState.putBoolean(LOAD_CONTRACT_MANAGER_TAG, mContractManagerLoading)

        outState.putBoolean(RELOAD_NOTARY_KEY, mClickReloadNotary)
    }

    override fun onDetach()
    {
        super.onDetach()
        mCallback = null
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests(false)
    }
}
