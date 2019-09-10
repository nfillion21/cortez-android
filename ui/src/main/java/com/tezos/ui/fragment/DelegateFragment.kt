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
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
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
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.DataExtractor
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.fragment_delegate.*
import kotlinx.android.synthetic.main.redelegate_form_card_info.*
import org.json.JSONArray
import org.json.JSONObject

class DelegateFragment : Fragment()
{
    private var mCallback: OnAddedDelegationListener? = null

    private var mInitDelegateLoading:Boolean = false
    private var mFinalizeDelegateLoading:Boolean = false

    private var mInitRemoveDelegateLoading:Boolean = false
    private var mFinalizeRemoveDelegateLoading:Boolean = false

    private var mContractInfoLoading:Boolean = false

    private var mDelegatePayload:String? = null
    private var mDelegateFees:Long = -1

    private var mDelegateTezosAddress:String? = null

    private var mClickCalculate:Boolean = false

    private var mContract:Contract? = null

    private var mWalletEnabled:Boolean = false

    data class Contract
    (
            val blk: String,
            val spendable: Boolean,
            val delegatable: Boolean,
            val delegate: String?,
            val script: String
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

            return Contract(blk, spendable, delegatable, delegate)
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

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val DELEGATE_TEZOS_ADDRESS_KEY = "delegate_tezos_address_key"
        private const val DELEGATE_FEE_KEY = "delegate_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private const val WALLET_AVAILABLE_KEY = "wallet_available_key"

        private const val CONTRACT_DATA_KEY = "contract_data_key"

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

        remove_delegate_button_layout.setOnClickListener {
            onRemoveDelegateClick()
        }

        swipe_refresh_layout.setOnRefreshListener {
            startContractInfoLoading()
        }

        redelegate_address_edittext.addTextChangedListener(GenericTextWatcher(redelegate_address_edittext))

        redelegate_address_edittext.onFocusChangeListener = focusChangeListener()

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

            mWalletEnabled = savedInstanceState.getBoolean(WALLET_AVAILABLE_KEY, false)

            val contractBundle = savedInstanceState.getBundle(CONTRACT_DATA_KEY)
            if (contractBundle != null)
            {
                mContract = this.fromBundle(contractBundle)
            }

            if (mContractInfoLoading)
            {
                refreshTextUnderDelegation(false)
                mWalletEnabled = true
                startContractInfoLoading()
            }
            else
            {
                onContractInfoComplete(false)

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

        startPostRequestLoadInitAddDelegate()
    }

    private fun startInitRemoveDelegateLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()
        putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validateRemoveDelegateButton(false)

        startPostRequestLoadInitRemoveDelegate()
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
        cancelRequests(true)

        mContractInfoLoading = true

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
                if (R.id.content != null)
                {
                    addContractInfoFromJSON(it)
                    onContractInfoComplete(true)

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

                        onContractInfoComplete(false)

                        showSnackBar(it, null)
                    })

            jsonArrayRequest.tag = CONTRACT_INFO_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    private fun addContractInfoFromJSON(answer: JSONArray)
    {
        if (answer != null && answer.length() > 0)
        {
            val contractJSON = DataExtractor.getJSONObjectFromField(answer,0)

            val blk = DataExtractor.getStringFromField(contractJSON, "blk")
            val spendable = DataExtractor.getBooleanFromField(contractJSON, "spendable")
            val delegatable = DataExtractor.getBooleanFromField(contractJSON, "delegatable")
            val delegate = DataExtractor.getStringFromField(contractJSON, "delegate")

            mContract = Contract(blk as String, spendable as Boolean, delegatable as Boolean, delegate)
        }
    }

    private fun onContractInfoComplete(animating:Boolean)
    {
        mContractInfoLoading = false
        nav_progress?.visibility = View.GONE

        //TODO handle the swipe refresh
        swipe_refresh_layout?.isEnabled = true
        swipe_refresh_layout?.isRefreshing = false

        refreshTextUnderDelegation(animating)
    }

    private fun refreshTextUnderDelegation(animating:Boolean)
    {
        //this method handles the data and loading texts

        //TODO refreshing text with mnemonics or not.

        if (mContract != null)
        {
            if (mContract!!.delegate != null)
            {
                limits_info_textview?.visibility = View.GONE
                update_storage_form_card?.visibility = View.VISIBLE

                redelegate_address_layout?.visibility = View.GONE

                update_storage_button_layout?.visibility = View.GONE

                remove_delegate_button_layout?.visibility = View.VISIBLE

                storage_info_textview?.visibility = View.VISIBLE

                storage_info_address_textview?.visibility = View.VISIBLE
                storage_info_address_textview?.text = String.format(getString(R.string.remove_delegate_info_address, mContract?.delegate))


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

                update_storage_button_layout?.visibility = View.VISIBLE

                storage_info_textview?.visibility = View.GONE
                storage_info_address_textview?.visibility = View.GONE
                remove_delegate_button_layout?.visibility = View.GONE

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

            if (!animating)
            {
                //no_delegates_text_layout.text = mBalanceItem.toString()

                //reloadList()
            }

            loading_textview?.visibility = View.GONE
            loading_textview?.text = null
        }
        else
        {
            // mContract is null then just show "-"
            //loading_textview will be hidden behind other textview

            loading_textview?.visibility = View.VISIBLE
            loading_textview?.text = "-"
        }
    }

    // volley
    private fun startPostRequestLoadFinalizeRemoveDelegate(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isRemoveButtonValid() && mDelegatePayload != null && mDelegateFees != null)
        {
            //val pkhSrc = mnemonicsData.pkh
            //val pkhDst = mDstAccount?.pubKeyHash

            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val pk = CryptoUtils.generatePk(mnemonics, "")

            var postParams = JSONObject()
            postParams.put("src", pkh())
            postParams.put("src_pk", pk)

            postParams.put("fee", mDelegateFees)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()

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
                        Response.Listener<String> { response ->
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

        if (isAddButtonValid() && mDelegatePayload != null && mDelegateTezosAddress != null && mDelegateFees != null)
        {
            //val pkhSrc = mnemonicsData.pkh
            //val pkhDst = mDstAccount?.pubKeyHash

            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val pk = CryptoUtils.generatePk(mnemonics, "")

            var postParams = JSONObject()
            postParams.put("src", pkh())
            postParams.put("src_pk", pk)
            postParams.put("delegate", mDelegateTezosAddress)
            postParams.put("fee", mDelegateFees)

            if (/*isChangeDelegatePayloadValid(mDelegatePayload!!, postParams)*/true)
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mDelegatePayload!!.hexToByteArray()

                val xLen = zeroThree.size
                val yLen = byteArrayThree.size
                val result = ByteArray(xLen + yLen)

                System.arraycopy(zeroThree, 0, result, 0, xLen)
                System.arraycopy(byteArrayThree, 0, result, xLen, yLen)

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
                        Response.Listener<String> { response ->
                            if (activity != null)
                            {
                                //there's no need to do anything because we call finish()
                                onFinalizeDelegationLoadComplete(null)

                                mCallback?.finish(R.id.add_delegate_succeed)
                            }
                        },
                        Response.ErrorListener
                        {
                            if (activity != null)
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
        cancelRequests(true)

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
            cancelRequests(true)
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
            cancelRequests(true)
            // it's signed, looks like it worked.
            //transferLoading(true)
        }
    }

    // volley
    private fun startPostRequestLoadInitAddDelegate()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
        val pk = CryptoUtils.generatePk(mnemonics, "")

        var postParams = JSONObject()
        postParams.put("src", mnemonicsData.pkh)
        postParams.put("src_pk", pk)

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        //dstObject.put("dst", mDstAccount)

        dstObject.put("dst", pkh())

        //dstObject.put("amount", (mTransferAmount*1000000).toLong().toString())
        dstObject.put("amount", (0).toLong().toString())

        dstObject.put("entrypoint", "do")

        val json = JSONArray(String.format(getString(R.string.set_delegate_contract), mDelegateTezosAddress))
        dstObject.put("parameters", json)

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Request.Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (activity != null)
            {
                mDelegatePayload = answer.getString("result")
                mDelegateFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mDelegatePayload != null && mDelegateFees != null && activity != null)
                {
                    onInitDelegateLoadComplete(null)

                    val feeInTez = mDelegateFees?.toDouble()/1000000.0
                    fee_edittext?.setText(feeInTez.toString())

                    validateAddButton(isInputDataValid() && isDelegateFeeValid())

                    if (isInputDataValid() && isDelegateFeeValid())
                    {
                        validateAddButton(true)

                        //this.setTextPayButton()
                    }
                    else
                    {
                        // should no happen
                        validateAddButton(false)
                    }
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
            if (activity != null)
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
    private fun startPostRequestLoadInitRemoveDelegate()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
        val pk = CryptoUtils.generatePk(mnemonics, "")

        var postParams = JSONObject()
        postParams.put("src", mnemonicsData.pkh)
        postParams.put("src_pk", pk)

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        //dstObject.put("dst", mDstAccount)

        dstObject.put("dst", pkh())

        //dstObject.put("amount", (mTransferAmount*1000000).toLong().toString())
        dstObject.put("amount", (0).toLong().toString())

        dstObject.put("entrypoint", "do")

        val json = JSONArray(getString(R.string.remove_delegate_contract))
        dstObject.put("parameters", json)

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            if (activity != null)
            {
                mDelegatePayload = answer.getString("result")
                mDelegateFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mDelegatePayload != null && mDelegateFees != null)
                {
                    onInitRemoveDelegateLoadComplete(null)

                    val feeInTez = mDelegateFees?.toDouble()/1000000.0
                    fee_edittext?.setText(feeInTez?.toString())

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
            if (activity != null)
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
            val themeBundle = arguments!!.getBundle(CustomTheme.TAG)
            val theme = CustomTheme.fromBundle(themeBundle)

            if (validate)
            {
                update_storage_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                update_storage_button_layout?.isEnabled = true
                update_storage_button_layout?.background = makeSelector(theme)

                val drawables = update_storage_button?.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables!![0])
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
                    val wrapDrawable = DrawableCompat.wrap(drawables!![0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }
            }

        }
    }

    private fun validateRemoveDelegateButton(validate: Boolean)
    {
        if (validate)
        {
            val theme = CustomTheme(R.color.tz_error, R.color.tz_accent, R.color.tz_light)

            remove_delegate_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            remove_delegate_button_layout?.isEnabled = true
            remove_delegate_button_layout?.background = makeSelector(theme)

            val drawables = remove_delegate_button?.compoundDrawables
            if (activity != null && drawables != null)
            {
                val wrapDrawable = DrawableCompat.wrap(drawables!![0])
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
                val wrapDrawable = DrawableCompat.wrap(drawables!![0])
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

        if (editable != null && !TextUtils.isEmpty(editable))
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
        val isFeeValid = false

        if (fee_edittext?.text != null && !TextUtils.isEmpty(fee_edittext?.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val fee = fee_edittext.text.toString().toDouble()

                if (fee >= 0.000001f)
                {
                    val longTransferFee = fee*1000000
                    mDelegateFees = longTransferFee.toLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mDelegateFees = -1
                return false
            }
        }

        return isFeeValid
    }

    private fun putEverythingInRed()
    {
        this.putTzAddressInRed(true)
    }

    fun isAddButtonValid(): Boolean
    {
        return mDelegatePayload != null
                && isDelegateFeeValid()
                && isInputDataValid()
    }

    fun isRemoveButtonValid(): Boolean
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
        if (isFingerprintAllowed()!! && hasEnrolledFingerprints()!!)
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
            startFinalizeAddDelegateLoading()
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
        if (isFingerprintAllowed()!! && hasEnrolledFingerprints()!!)
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
            startFinalizeAddDelegateLoading()
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

            if (resetBooleans)
            {
                mInitDelegateLoading = false
                mFinalizeDelegateLoading = false
                mInitRemoveDelegateLoading = false
                mFinalizeRemoveDelegateLoading = false
                mContractInfoLoading = false
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
