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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.fragment_script.*
import kotlinx.android.synthetic.main.update_storage_form_card.*
import org.json.JSONArray
import org.json.JSONObject

class ScriptFragment : Fragment()
{
    private var mCallback: OnUpdateScriptListener? = null

    private var mInitUpdateStorageLoading:Boolean = false
    private var mFinalizeDelegateLoading:Boolean = false

    private var mStorageInfoLoading:Boolean = false

    private var mUpdateStoragePayload:String? = null
    private var mUpdateStorageFees:Long = -1L

    private var mUpdateStorageAddress:String? = null

    private var mClickCalculate:Boolean = false

    private var mStorage:String? = null
    private var mWalletEnabled:Boolean = false

    private var mEditMode:Boolean = false

    private var mSpendingLimitAmount:Long = -1L

    companion object
    {
        private const val CONTRACT_PUBLIC_KEY = "CONTRACT_PUBLIC_KEY"

        private const val UPDATE_STORAGE_INIT_TAG = "update_storage_init"
        private const val DELEGATE_FINALIZE_TAG = "delegate_finalize"

        private const val CONTRACT_SCRIPT_INFO_TAG = "contract_script_info"

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val DELEGATE_TEZOS_ADDRESS_KEY = "delegate_tezos_address_key"
        private const val DELEGATE_FEE_KEY = "delegate_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private const val WALLET_AVAILABLE_KEY = "wallet_available_key"

        private const val STORAGE_DATA_KEY = "storage_data_key"

        private const val SPENDING_AMOUNT_KEY = "spending_amount_key"

        private const val EDIT_MODE_KEY = "edit_mode_key"

        @JvmStatic
        fun newInstance(theme: CustomTheme, contract: String?) =
                ScriptFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(CONTRACT_PUBLIC_KEY, contract)
                    }
                }
    }

    interface OnUpdateScriptListener
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
            mCallback = context as OnUpdateScriptListener?
        }
        catch (e: ClassCastException)
        {
            throw ClassCastException(context!!.toString() + " must implement OnUpdateScriptListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_script, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        validateConfirmEditionButton(isInputDataValid() && isDelegateFeeValid())

        update_storage_button_layout.setOnClickListener {
            onDelegateClick()
        }

        fab_edit_storage.setOnClickListener {
            animateFabEditMode(true)
        }

        fab_undo_storage.setOnClickListener {
            animateFabEditMode(false)
        }

        swipe_refresh_script_layout.setOnRefreshListener {
            startStorageInfoLoading()
        }

        daily_spending_limit_edittext.addTextChangedListener(GenericTextWatcher(daily_spending_limit_edittext))
        //daily_spending_limit_edittext.onFocusChangeListener = focusChangeListener()

        if (savedInstanceState != null)
        {
            mUpdateStoragePayload = savedInstanceState.getString(DELEGATE_PAYLOAD_KEY, null)

            mInitUpdateStorageLoading = savedInstanceState.getBoolean(UPDATE_STORAGE_INIT_TAG)
            mFinalizeDelegateLoading = savedInstanceState.getBoolean(DELEGATE_FINALIZE_TAG)

            mStorageInfoLoading = savedInstanceState.getBoolean(CONTRACT_SCRIPT_INFO_TAG)

            mUpdateStorageAddress = savedInstanceState.getString(DELEGATE_TEZOS_ADDRESS_KEY, null)

            mUpdateStorageFees = savedInstanceState.getLong(DELEGATE_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mWalletEnabled = savedInstanceState.getBoolean(WALLET_AVAILABLE_KEY, false)

            mStorage = savedInstanceState.getString(STORAGE_DATA_KEY, null)

            mSpendingLimitAmount = savedInstanceState.getLong(SPENDING_AMOUNT_KEY, -1L)

            mEditMode = savedInstanceState.getBoolean(EDIT_MODE_KEY, false)

            if (mStorageInfoLoading)
            {
                refreshTextUnderDelegation(false)
                mWalletEnabled = true
                startStorageInfoLoading()
            }
            else
            {
                onStorageInfoComplete(false)

                //TODO we got to keep in mind there's an id already.
                if (mInitUpdateStorageLoading)
                {
                    startInitUpdateStorageLoading()
                }
                else
                {
                    onInitDelegateLoadComplete(null)

                    if (mFinalizeDelegateLoading)
                    {
                        startFinalizeUpdateStorageLoading()
                    }
                    else
                    {
                        onFinalizeDelegationLoadComplete(null)
                    }
                }
            }
        }
        else
        {
            mWalletEnabled = true
            startStorageInfoLoading()
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (mEditMode)
        {
            putEverythingInRed()

            if (isDelegateFeeValid())
            {
                if (isInputDataValid())
                {
                    validateConfirmEditionButton(true)
                }
            }
        }


        if (!mWalletEnabled)
        {
            mWalletEnabled = true

            startStorageInfoLoading()
        }
    }

    /*
    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id
            when (i) {
                //R.id.public_address_edittext -> putTzAddressInRed(!hasFocus)
                R.id.daily_spending_limit_edittext -> putSpendingLimitInRed(!hasFocus)
                else -> throw UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }
    */

    private fun pkh():String?
    {
        var pkh:String? = null

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            pkh = arguments!!.getString(CONTRACT_PUBLIC_KEY)
            if (pkh == null)
            {
                //should not happen
                //val seed = Storage(activity!!).getMnemonics()
                //pkh = seed.pkh
            }
        }

        return pkh
    }

    private fun switchToEditMode(editMode:Boolean)
    {
        if (editMode)
        {
            update_storage_button_relative_layout.visibility = View.VISIBLE

            gas_textview.visibility = View.VISIBLE
            gas_layout.visibility = View.VISIBLE

            daily_spending_limit_edittext.isEnabled = true
            daily_spending_limit_edittext.setText("")
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(daily_spending_limit_edittext, InputMethodManager.SHOW_IMPLICIT)

            fab_edit_storage.hide()
            fab_undo_storage.show()
        }
        else
        {
            validateConfirmEditionButton(false)
            cancelRequests(true)
            transferLoading(false)
            putFeesToNegative()

            update_storage_button_relative_layout.visibility = View.GONE

            gas_textview.visibility = View.GONE
            gas_layout.visibility = View.GONE

            daily_spending_limit_edittext.isEnabled = false

            if (mStorage != JSONObject(getString(R.string.default_storage)).toString())
            {
                val storageJSONObject = JSONObject(mStorage)
                val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

                val args2 = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args")
                val pk = DataExtractor.getStringFromField(args2[1] as JSONObject, "string")
                public_address_edittext.setText(pk)

                val args3 = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args")
                val args4 = DataExtractor.getJSONArrayFromField(args3[0] as JSONObject, "args")

                val dailySpendingLimit = DataExtractor.getStringFromField(args4[0] as JSONObject, "int")
                daily_spending_limit_edittext.setText(mutezToTez(dailySpendingLimit))
            }

            fab_undo_storage.hide()
            fab_edit_storage.show()
        }
    }

    private fun animateFabEditMode(editMode:Boolean)
    {
        mEditMode = editMode

        if (editMode)
        {
            val textViewAnimator = ObjectAnimator.ofFloat(fab_edit_storage, View.SCALE_X, 0f)
            val textViewAnimator2 = ObjectAnimator.ofFloat(fab_edit_storage, View.SCALE_Y, 0f)
            val textViewAnimator3 = ObjectAnimator.ofFloat(fab_edit_storage, View.ALPHA, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.interpolator = FastOutSlowInInterpolator()
            animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
            animatorSet.addListener(object : AnimatorListenerAdapter()
            {
                override fun onAnimationEnd(animation: Animator)
                {
                    super.onAnimationEnd(animation)
                    switchToEditMode(editMode)
                }
            })
            animatorSet.start()
        }
        else
        {
            val textViewAnimator = ObjectAnimator.ofFloat(fab_undo_storage, View.SCALE_X, 0f)
            val textViewAnimator2 = ObjectAnimator.ofFloat(fab_undo_storage, View.SCALE_Y, 0f)
            val textViewAnimator3 = ObjectAnimator.ofFloat(fab_undo_storage, View.ALPHA, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.interpolator = FastOutSlowInInterpolator()
            animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
            animatorSet.addListener(object : AnimatorListenerAdapter()
            {
                override fun onAnimationEnd(animation: Animator)
                {
                    super.onAnimationEnd(animation)
                    switchToEditMode(editMode)
                }
            })
            animatorSet.start()
        }
    }

    private fun startStorageInfoLoading()
    {
        transferLoading(true)

        // validatePay cannot be valid if there is no fees
        validateConfirmEditionButton(false)

        //swipe_refresh_script_layout?.isEnabled = false

        startGetRequestLoadContractInfo()
    }

    private fun startInitUpdateStorageLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()

        // validatePay cannot be valid if there is no fees
        validateConfirmEditionButton(false)

        startPostRequestLoadInitUpdateStorage()
    }

    private fun startFinalizeUpdateStorageLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        val mnemonicsData = Storage(activity!!).getMnemonics()
        startPostRequestLoadFinalizeAddDelegate(mnemonicsData)
    }

    // volley
    private fun startGetRequestLoadContractInfo()
    {
        cancelRequests(true)

        mStorageInfoLoading = true

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
                if (activity != null)
                {
                    addContractInfoFromJSON(it)
                    onStorageInfoComplete(true)

                    //if (mContract?.delegate != null)
                    if (mStorage != JSONObject(getString(R.string.default_storage)).toString())
                    {
                        validateConfirmEditionButton(isInputDataValid() && isDelegateFeeValid())
                    }
                    else
                    {
                        //TODO I don't need to forge a transfer for now
                        //TODO hide the whole thing

                        //I need the right data inputs before.
                        //startInitRemoveDelegateLoading()
                    }
                }
            },
                    Response.ErrorListener {

                        onStorageInfoComplete(false)

                        showSnackBar(it, null)
                    })

            jsonArrayRequest.tag = CONTRACT_SCRIPT_INFO_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    private fun addContractInfoFromJSON(answer: JSONObject)
    {
        if (answer != null && answer.length() > 0)
        {
            mStorage = answer.toString()
        }
    }

    private fun onStorageInfoComplete(animating:Boolean)
    {
        mStorageInfoLoading = false
        nav_progress?.visibility = View.GONE

        //TODO handle the swipe refresh
        swipe_refresh_script_layout?.isEnabled = true
        swipe_refresh_script_layout?.isRefreshing = false

        refreshTextUnderDelegation(animating)
    }

    private fun refreshTextUnderDelegation(animating:Boolean)
    {
        //this method handles the data and loading texts

        if (mStorage != null)
        {
            //if (mContract!!.delegate != null)
            if (mStorage != JSONObject(getString(R.string.default_storage)).toString())
            {
                //TODO at this point, just show that there is no script.

                val storageJSONObject = JSONObject(mStorage)
                val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

                val args2 = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args")
                val pk = DataExtractor.getStringFromField(args2[1] as JSONObject, "string")

                val args3 = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args")
                val args4 = DataExtractor.getJSONArrayFromField(args3[0] as JSONObject, "args")

                val dailySpendingLimit = DataExtractor.getStringFromField(args4[0] as JSONObject, "int")

                val dailySpendingLimitInTez = mutezToTez(dailySpendingLimit)
                daily_spending_limit_edittext?.setText(dailySpendingLimitInTez)

                update_storage_form_card?.visibility = View.VISIBLE

                public_address_layout?.visibility = View.VISIBLE

                public_address_edittext?.setText(pk)

                update_storage_button_layout?.visibility = View.VISIBLE

                storage_info_textview?.visibility = View.VISIBLE
                storage_info_textview?.text = getString(R.string.contract_storage_info)

                if (mEditMode)
                {
                    switchToEditMode(true)
                }
                else
                {
                    switchToEditMode(false)
                }
            }
            else
            {
                update_storage_form_card?.visibility = View.GONE

                public_address_layout?.visibility = View.VISIBLE

                update_storage_button_layout?.visibility = View.GONE

                storage_info_textview?.visibility = View.VISIBLE
                storage_info_textview?.text = getString(R.string.no_script_info)

                //TODO show everything related to the removing
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
    private fun startPostRequestLoadFinalizeAddDelegate(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isUpdateButtonValid() && mUpdateStoragePayload != null && mUpdateStorageAddress != null && mUpdateStorageFees != -1L)
        {
            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            val pk = CryptoUtils.generatePk(mnemonics, "")

            var postParams = JSONObject()
            postParams.put("src", pkh())
            postParams.put("src_pk", pk)
            postParams.put("delegate", mUpdateStorageAddress)
            postParams.put("fee", mUpdateStorageFees)

            if (!isChangeDelegatePayloadValid(mUpdateStoragePayload!!, postParams))
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mUpdateStoragePayload!!.hexToByteArray()

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
        mInitUpdateStorageLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            mUpdateStoragePayload = null

            storage_fee_edittext.isEnabled = true
            storage_fee_edittext.isFocusable = false
            storage_fee_edittext.isClickable = false
            storage_fee_edittext.isLongClickable = false
            storage_fee_edittext.hint = getString(R.string.click_for_fees)

            storage_fee_edittext.setOnClickListener {
                startInitUpdateStorageLoading()
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
    private fun startPostRequestLoadInitUpdateStorage()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
        val pk = CryptoUtils.generatePk(mnemonics, "")

        var postParams = JSONObject()
        postParams.put("src", pkh())
        postParams.put("src_pk", pk)

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        dstObject.put("dst", pkh())
        dstObject.put("amount", "0")

        val resScript = JSONObject(getString(R.string.update_parameters_storage))

        //TODO check about generating a new P256 key
        mUpdateStorageAddress = pk
        val spendingLimitContract = String.format(resScript.toString(), mUpdateStorageAddress, (mSpendingLimitAmount*1000000L).toString())

        val json = JSONObject(spendingLimitContract)
        dstObject.put("parameters", json)

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Request.Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (activity != null)
            {
                mUpdateStoragePayload = answer.getString("result")
                mUpdateStorageFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mUpdateStoragePayload != null && mUpdateStorageFees != -1L && activity != null)
                {
                    onInitDelegateLoadComplete(null)

                    val feeInTez = mUpdateStorageFees?.toDouble()/1000000.0
                    storage_fee_edittext?.setText(feeInTez.toString())

                    validateConfirmEditionButton(isInputDataValid() && isDelegateFeeValid())

                    if (isInputDataValid() && isDelegateFeeValid())
                    {
                        validateConfirmEditionButton(true)
                    }
                    else
                    {
                        // should no happen
                        validateConfirmEditionButton(false)
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
                //Log.i("mUpdateStoragePayload", ""+mUpdateStoragePayload)
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

        jsObjRequest.tag = UPDATE_STORAGE_INIT_TAG
        mInitUpdateStorageLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
    }

    private fun transferLoading(loading:Boolean)
    {
        // handle the visibility of bottom buttons

        if (loading)
        {
            //update_storage_button_layout.visibility = View.GONE
            //empty.visibility = View.VISIBLE
            nav_progress?.visibility = View.VISIBLE
        }
        else
        {
            //update_storage_button_layout.visibility = View.VISIBLE
            //empty.visibility = View.GONE
            nav_progress?.visibility = View.GONE
        }
    }

    private fun putFeesToNegative()
    {
        storage_fee_edittext?.setText("")

        mClickCalculate = false
        storage_fee_edittext?.isEnabled = false
        storage_fee_edittext?.hint = getString(R.string.neutral)

        mUpdateStorageFees = -1

        mUpdateStoragePayload = null
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

    private fun validateConfirmEditionButton(validate: Boolean)
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
            if (mEditMode)
            {
                val i = v.id
                if (i == R.id.daily_spending_limit_edittext && isSpendingLimitAmountDifferent(editable))
                {
                    putSpendingLimitInRed(false)

                    //TODO text changed
                    //TODO load again but only if we don't have any same forged data.

                    if (isInputDataValid())
                    {
                        startInitUpdateStorageLoading()
                    }
                    else
                    {
                        validateConfirmEditionButton(false)

                        cancelRequests(false)
                        transferLoading(false)

                        putFeesToNegative()
                    }
                }
                else if (i != R.id.daily_spending_limit_edittext)
                {
                    throw UnsupportedOperationException(
                            "OnClick has not been implemented for " + resources.getResourceName(v.id))
                }
            }
        }
    }

    private fun isSpendingLimitAmountDifferent(editable: Editable):Boolean
    {
        val isSpendingAmountDifferent = false

        if (!TextUtils.isEmpty(editable))
        {
            try
            {
                val amount = editable.toString().toLong()
                if (amount != -1L && amount != mSpendingLimitAmount)
                {
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                return false
            }
        }
        return isSpendingAmountDifferent
    }

    fun isInputDataValid(): Boolean
    {
        return isP256AddressValid() && isSpendingLimitAmountValid()
    }

    private fun isP256AddressValid(): Boolean
    {
        var isTzAddressValid = false

        if (!TextUtils.isEmpty(public_address_edittext?.text))
        {
            /*
            if (Utils.isTzAddressValid(public_address_edittext.text!!.toString()))
            {
                mUpdateStorageAddress = public_address_edittext.text.toString()
                isTzAddressValid = true
            }
            */
            isTzAddressValid = true
        }

        return isTzAddressValid
    }

    private fun isDelegateFeeValid():Boolean
    {
        val isFeeValid = false

        if (storage_fee_edittext?.text != null && !TextUtils.isEmpty(storage_fee_edittext?.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val fee = storage_fee_edittext.text.toString().toDouble()

                if (fee >= 0.000001f)
                {
                    val longTransferFee = fee*1000000
                    mUpdateStorageFees = longTransferFee.toLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mUpdateStorageFees = -1
                return false
            }
        }

        return isFeeValid
    }

    private fun putEverythingInRed()
    {
        this.putTzAddressInRed(true)
        this.putSpendingLimitInRed(true)
    }

    fun isUpdateButtonValid(): Boolean
    {
        return mUpdateStoragePayload != null
                && isDelegateFeeValid()
                && isInputDataValid()
    }

    private fun putTzAddressInRed(red: Boolean)
    {
        val color: Int

        val tzAddressValid = isP256AddressValid()

        color = if (red && !tzAddressValid)
        {
            R.color.tz_error
        }
        else
        {
            R.color.tz_accent
        }

        public_address_edittext.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun putSpendingLimitInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isSpendingLimitAmountValid()

        color = if (red && !amountValid)
        {
            R.color.tz_error
        }
        else
        {
            R.color.tz_accent
        }

        daily_spending_limit_edittext.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun isSpendingLimitAmountValid():Boolean
    {
        val isAmountValid = false

        if (daily_spending_limit_edittext?.text != null && !TextUtils.isEmpty(daily_spending_limit_edittext.text))
        {
            try
            {
                val amount = daily_spending_limit_edittext.text!!.toString().toLong()

                if (amount in 0..1000)
                {
                    mSpendingLimitAmount = amount
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mSpendingLimitAmount = -1L
                return false
            }
        }

        return isAmountValid
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
            startFinalizeUpdateStorageLoading()
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
            startFinalizeUpdateStorageLoading()
        }
        else
        {
            onDelegateClick()
        }
    }

    private fun cancelRequests(resetBooleans:Boolean)
    {
        if (activity != null)
        {
            val requestQueue = VolleySingleton.getInstance(activity!!.applicationContext).requestQueue
            requestQueue?.cancelAll(UPDATE_STORAGE_INIT_TAG)
            requestQueue?.cancelAll(DELEGATE_FINALIZE_TAG)
            requestQueue?.cancelAll(CONTRACT_SCRIPT_INFO_TAG)

            if (resetBooleans)
            {
                mInitUpdateStorageLoading = false
                mFinalizeDelegateLoading = false
                mStorageInfoLoading = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(UPDATE_STORAGE_INIT_TAG, mInitUpdateStorageLoading)
        outState.putBoolean(DELEGATE_FINALIZE_TAG, mFinalizeDelegateLoading)

        outState.putBoolean(CONTRACT_SCRIPT_INFO_TAG, mStorageInfoLoading)

        outState.putString(DELEGATE_PAYLOAD_KEY, mUpdateStoragePayload)

        outState.putString(DELEGATE_TEZOS_ADDRESS_KEY, mUpdateStorageAddress)

        outState.putLong(DELEGATE_FEE_KEY, mUpdateStorageFees)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)

        outState.putBoolean(WALLET_AVAILABLE_KEY, mWalletEnabled)

        outState.putLong(SPENDING_AMOUNT_KEY, mSpendingLimitAmount)

        outState.putString(STORAGE_DATA_KEY, mStorage)

        outState.putBoolean(EDIT_MODE_KEY, mEditMode)
    }

    private fun mutezToTez(mutez:String):String
    {
        var amountLong: Long = mutez.toLong()
        amountLong /= 1000000

        return amountLong.toString()
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