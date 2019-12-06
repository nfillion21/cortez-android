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

package com.tezos.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.activity_add_limits.*
import kotlinx.android.synthetic.main.limits_form_card_info.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.interfaces.ECPublicKey
import kotlin.math.roundToLong

/**
 * Created by nfillion on 26/02/19.
 */
class AddLimitsActivity : BaseSecureActivity()
{
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    private var mInitDelegateLoading:Boolean = false
    private var mFinalizeDelegateLoading:Boolean = false

    private var mDelegatePayload:String? = null
    private var mDelegateFees:Long = -1

    private var mDelegateAmount:Double = -1.0

    private var mLimitAmount:Long = -1L

    private var mClickCalculate:Boolean = false

    private var mIsTracking:Boolean = false
    private var mIsTyping:Boolean = false

    companion object
    {
        var ADD_DSL_REQUEST_CODE = 0x3500 // arbitrary int

        private const val DELEGATE_INIT_TAG = "delegate_init"
        private const val DELEGATE_FINALIZE_TAG = "delegate_finalize"

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val DELEGATE_AMOUNT_KEY = "delegate_amount_key"
        private const val LIMIT_AMOUNT_KEY = "limit_amount_key"

        private const val DELEGATE_FEE_KEY = "delegate_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private fun getStartIntent(context: Context, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, AddLimitsActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, ADD_DSL_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_limits)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        validateAddButton(isInputDataValid() && isDelegateFeeValid())

        create_limit_contract_button_layout.setOnClickListener {
            onDelegateClick()
        }

        initToolbar(theme)

        limits_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            private var currentValue:Int = 1

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean)
            {
                if (!mIsTyping)
                {
                    limit_edittext.setText( (i+1).toString())
                    currentValue = i+1
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar)
            {
                if (!mIsTyping)
                {
                    mIsTracking = true
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar)
            {
                if (!mIsTyping)
                {
                    mIsTracking = false
                    limit_edittext.setText( (currentValue).toString())
                }
            }
        })

        amount_limit_edittext.addTextChangedListener(GenericTextWatcher(amount_limit_edittext))

        val focusChangeListener = this.focusChangeListener()
        amount_limit_edittext.onFocusChangeListener = focusChangeListener

        limit_edittext.addTextChangedListener(GenericTextWatcher(limit_edittext))
        limit_edittext.onFocusChangeListener = focusChangeListener

        if (savedInstanceState != null)
        {
            mDelegatePayload = savedInstanceState.getString(DELEGATE_PAYLOAD_KEY, null)

            mInitDelegateLoading = savedInstanceState.getBoolean(DELEGATE_INIT_TAG)
            mFinalizeDelegateLoading = savedInstanceState.getBoolean(DELEGATE_FINALIZE_TAG)

            mDelegateAmount = savedInstanceState.getDouble(DELEGATE_AMOUNT_KEY, -1.0)

            mLimitAmount = savedInstanceState.getLong(LIMIT_AMOUNT_KEY, -1L)

            mDelegateFees = savedInstanceState.getLong(DELEGATE_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)
        }
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id

            when (i)
            {
                R.id.amount_limit_edittext -> putAmountInRed(!hasFocus)

                R.id.limit_edittext -> putLimitInRed(!hasFocus)

                else -> throw UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }

    private fun initToolbar(theme: CustomTheme)
    {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryDarkId))
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        val window = window
        window.statusBarColor = ContextCompat.getColor(this,
                theme.colorPrimaryDarkId)
        try
        {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        }
        catch (e: Exception)
        {
            Log.getStackTraceString(e)
        }

        close_button.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId))
        close_button.setOnClickListener {
            //requests stop in onDestroy.
            finish()
        }

        nav_progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId), PorterDuff.Mode.SRC_IN)

        val mTitleBar = findViewById<TextView>(R.id.barTitle)
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
    }

    private fun startInitOriginateContractLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()

        // validatePay cannot be valid if there is no fees
        validateAddButton(false)

        startPostRequestLoadInitOriginateContract()
    }

    private fun startFinalizeLoadingOriginateContract()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        val mnemonicsData = Storage(baseContext).getMnemonics()
        startPostRequestLoadFinalizeDelegate(mnemonicsData)
    }

    private fun updateMnemonicsData(data: Storage.MnemonicsData, pk:String):String
    {
        with(Storage(this)) {
            saveSeed(Storage.MnemonicsData(data.pkh, pk, data.mnemonics))
        }
        return pk
    }

    // volley
    private fun startPostRequestLoadFinalizeDelegate(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isAddButtonValid() && mDelegatePayload != null)
        {
            var postParams = JSONObject()
            postParams.put("src", mnemonicsData.pkh)
            postParams.put("src_pk", mnemonicsData.pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            //dstObject.put("dst", pkhDst)

            val mutezAmount = (mDelegateAmount*1000000.0).roundToLong()
            dstObject.put("balance", mutezAmount)

            dstObject.put("fee", mDelegateFees)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

            if (true/*!isAddDelegatePayloadValid(mDelegatePayload!!, postParams)*/)
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

                            //there's no need to do anything because we call finish()
                            //onFinalizeDelegationLoadComplete(null)

                            setResult(R.id.add_dsl_succeed, null)
                            finish()

                            //TODO create the spending key
                            //EncryptionServices.createAndroidAsymmetricKey

                        },
                        Response.ErrorListener
                        {
                            onFinalizeDelegationLoadComplete(it)
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
                VolleySingleton.getInstance(applicationContext).addToRequestQueue(stringRequest)
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

            showSnackBar(error)
        }
        else
        {
            // the finish call is made already
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

            //TODO should cancel the payloadTransfer too
            mDelegatePayload = null

            //TODO we should give access to the fees button

            fee_limit_edittext.isEnabled = true
            fee_limit_edittext.isFocusable = false
            fee_limit_edittext.isClickable = false
            fee_limit_edittext.isLongClickable = false
            fee_limit_edittext.hint = getString(R.string.click_for_fees)

            fee_limit_edittext.setOnClickListener {
                startInitOriginateContractLoading()
            }

            if(error != null)
            {
                showSnackBar(error)
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
    private fun startPostRequestLoadInitOriginateContract()
    {
        val mnemonicsData = Storage(baseContext).getMnemonics()

        val url = getString(R.string.originate_account_url)

        val pk = if (mnemonicsData.pk.isNullOrEmpty())
        {
            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
        }
        else
        {
            mnemonicsData.pk
        }

        val pkhSrc = mnemonicsData.pkh
        //val pkhDst = mDstAccount?.pubKeyHash

        var postParams = JSONObject()
        postParams.put("src", pkhSrc)
        postParams.put("src_pk", pk)

        var dstObject = JSONObject()

        //TODO be careful, do it in mutez.
        dstObject.put("credit", (mDelegateAmount*1000000L).roundToLong().toString())

        //dstObject.put("manager", pkhSrc)
        val ecKeys = retrieveECKeys()
        val tz3 = CryptoUtils.generatePkhTz3(ecKeys)

        val spendingLimitFile = "spending_limit_double_salt.json"
        val contract = application.assets.open(spendingLimitFile).bufferedReader()
                .use {
                    it.readText()
                }

        val jsonContract = JSONObject(contract)

        //val jsonScript = String.format(jsonContract.toString(), tz3, "86400", (mLimitAmount*1000000L).toString(), pkhSrc)
        dstObject.put("script", jsonContract)

        val storage = jsonContract["storage"] as JSONObject
        val argsStorage = storage["args"] as JSONArray

        val storageOne = argsStorage[0] as JSONObject
        val argsStorageOne = storageOne["args"] as JSONArray

        // storage 1 and 2-3 separate here

        val firstParamArgsStorageOne = argsStorageOne[0] as JSONObject
        firstParamArgsStorageOne.put("string", tz3)

        val secondAndThirdParamsArgsStorageOne = argsStorageOne[1] as JSONObject
        val argsSecondAndThirdParamsArgsStorageOne = secondAndThirdParamsArgsStorageOne["args"] as JSONArray
        val firstParamArgsSecondAndThirdParamsArgsStorageOne = argsSecondAndThirdParamsArgsStorageOne[0] as JSONObject
        val argsFirstParamArgsSecondAndThirdParamsArgsStorageOne = firstParamArgsSecondAndThirdParamsArgsStorageOne["args"] as JSONArray

        val firstParamArgsFirstParamArgsSecondAndThirdParamsArgsStorageOne = argsFirstParamArgsSecondAndThirdParamsArgsStorageOne[0] as JSONObject
        firstParamArgsFirstParamArgsSecondAndThirdParamsArgsStorageOne.put("int", (mLimitAmount*1000000L).toString())

        val secondParamArgsFirstParamArgsSecondAndThirdParamsArgsStorageOne = argsFirstParamArgsSecondAndThirdParamsArgsStorageOne[1] as JSONObject
        secondParamArgsFirstParamArgsSecondAndThirdParamsArgsStorageOne.put("int", "86400")

        val storageTwo = argsStorage[1] as JSONObject
        val argsStorageTwo = storageTwo["args"] as JSONArray

        val firstParamArgsStorageTwo = argsStorageTwo[0] as JSONObject
        firstParamArgsStorageTwo.put("string", pkhSrc)


        var dstObjects = JSONArray()

        //dstObject.put("delegatable", true)

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request

            mDelegatePayload = answer.getString("result")
            mDelegateFees = answer.getLong("total_fee")

            // get back the object and

            val dstsArray = postParams["dsts"] as JSONArray
            val dstObj = dstsArray[0] as JSONObject

            dstObj.put("fee", mDelegateFees.toString())
            dstsArray.put(0, dstObj)

            postParams.put("dsts", dstsArray)

            // we use this call to ask for payload and fees
            if (mDelegatePayload != null && mDelegateFees != -1L)
            {
                onInitDelegateLoadComplete(null)

                val feeInTez = mDelegateFees.toDouble()/1000000.0
                fee_limit_edittext.setText(feeInTez.toString())

                validateAddButton(isInputDataValid() && isDelegateFeeValid())

                if (isInputDataValid() && isDelegateFeeValid())
                {
                    validateAddButton(true)
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

        }, Response.ErrorListener
        {
            onInitDelegateLoadComplete(it)

            mClickCalculate = true
            //Log.i("mTransferId", ""+mTransferId)
            Log.i("mDelegatePayload", ""+mDelegatePayload)
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
        VolleySingleton.getInstance(applicationContext).addToRequestQueue(jsObjRequest)
    }

    private fun retrieveECKeys():ByteArray
    {
        var keyPair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        if (keyPair == null)
        {
            EncryptionServices().createSpendingKey()
            keyPair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        }

        val ecKey = keyPair!!.public as ECPublicKey
        return ecKeyFormat(ecKey)
    }

    private fun transferLoading(loading:Boolean)
    {
        if (loading)
        {
            create_limit_contract_button_layout.visibility = View.GONE
            nav_progress.visibility = View.VISIBLE
            //amount_transfer.isEnabled = false
        }
        else
        {
            create_limit_contract_button_layout.visibility = View.VISIBLE
            nav_progress.visibility = View.INVISIBLE
            //amount_transfer.isEnabled = true
        }
    }

    private fun putFeesToNegative()
    {
        fee_limit_edittext.setText("")

        mClickCalculate = false
        fee_limit_edittext.isEnabled = false
        fee_limit_edittext.hint = getString(R.string.neutral)

        mDelegateFees = -1

        mDelegatePayload = null
    }

    private fun showSnackBar(error:VolleyError?)
    {
        var error: String? = if (error != null)
        {
            error.toString()
        }
        else
        {
            getString(R.string.generic_error)
        }

        val snackbar = Snackbar.make(findViewById(R.id.content), error.toString(), Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                android.R.color.holo_red_light)))
        snackbar.show()
    }

    private fun validateAddButton(validate: Boolean)
    {
        //eBundle = intent.getBundleExtra(CustomTheme.TAG)
        //val theme = CustomTheme.fromBundle(themeBundle)
        val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

        if (validate)
        {
            create_limit_contract_button.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
            create_limit_contract_button_layout.isEnabled = true
            create_limit_contract_button_layout.background = makeSelector(theme)

            val drawables = create_limit_contract_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.textColorPrimaryId))
        }
        else
        {
            create_limit_contract_button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            create_limit_contract_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            create_limit_contract_button_layout.background = makeSelector(greyTheme)

            val drawables = create_limit_contract_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(this, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(this, theme.colorPrimaryId)))
        return res
    }

    private inner class GenericTextWatcher internal constructor(private val v: View) : TextWatcher
    {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable)
        {
            val i = v.id

            if ((i == R.id.amount_limit_edittext && !isDelegateAmountEquals(editable))
                    ||
                    (i == R.id.limit_edittext && !isLimitAmountEquals(editable)))
            {
                if (i == R.id.amount_limit_edittext )
                {
                    putAmountInRed(false)
                }

                if (i == R.id.limit_edittext )
                {
                    if (!mIsTracking)
                    {
                        putLimitInRed(false)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        {
                            val progress = limits_seekbar.progress

                            val change:Int = if (TextUtils.isEmpty(limit_edittext.text))
                            {
                                0
                            } else
                            {
                                limit_edittext.text.toString().toInt() - 1
                            }

                            if (progress != change)
                            {
                                mIsTyping = true
                                limits_seekbar.setProgress(change-1, true)
                                mIsTyping = false
                            }
                        }
                    }
                }

                //TODO text changed
                //TODO load again but only if we don't have any same forged data.

                //val amount = java.lang.Double.parseDouble()

                //TODO check if it's already

                if (!mIsTracking && isInputDataValid())
                {
                    startInitOriginateContractLoading()
                }
                else
                {
                    validateAddButton(false)

                    cancelRequests(false)
                    transferLoading(false)

                    putFeesToNegative()
                }
            }
            else if (i != R.id.amount_limit_edittext && i != R.id.limit_edittext)
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }
        }

        private fun isDelegateAmountEquals(editable: Editable):Boolean
        {
            val isAmountEquals = false

            if (!TextUtils.isEmpty(editable))
            {
                try
                {
                    val amount = editable.toString().toDouble()
                    if (amount != -1.0 && amount == mDelegateAmount)
                    {
                        return true
                    }
                }
                catch (e: NumberFormatException)
                {
                    return false
                }
            }
            return isAmountEquals
        }

        private fun isLimitAmountEquals(editable: Editable):Boolean
        {
            val isLimitAmountEquals = false

            if (editable != null && !TextUtils.isEmpty(editable))
            {
                try
                {
                    val limit = editable.toString().toLong()
                    if (limit != -1L && limit == mLimitAmount)
                    {
                        return true
                    }
                }
                catch (e: NumberFormatException)
                {
                    return false
                }
            }
            return isLimitAmountEquals
        }

    }

    fun isInputDataValid(): Boolean
    {
        return isDelegateAmountValid() && isDailySpendingLimitValid()
    }

    private fun isDelegateFeeValid():Boolean
    {
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

        return isFeeValid
    }

    private fun isDelegateAmountValid():Boolean
    {
        val isAmountValid = false

        if (amount_limit_edittext.text != null && !TextUtils.isEmpty(amount_limit_edittext.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val amount = amount_limit_edittext.text!!.toString().toDouble()

                //no need
                if (amount >= 0.0f)
                {
                    mDelegateAmount = amount
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mDelegateAmount = -1.0
                return false
            }
        }

        return isAmountValid
    }

    private fun isDailySpendingLimitValid():Boolean
    {
        val isLimitValid = false

        if (limit_edittext.text != null && !TextUtils.isEmpty(limit_edittext.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val limit = limit_edittext.text!!.toString().toLong()

                //no need
                if (limit in 1..1000)
                {
                    mLimitAmount = limit
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mLimitAmount = -1
                return false
            }
        }

        return isLimitValid
    }

    override fun onResume()
    {
        super.onResume()

        putEverythingInRed()

        if (isInputDataValid() && isDelegateFeeValid())
        {
            validateAddButton(true)
        }

        //TODO we got to keep in mind there's an id already.
        if (mInitDelegateLoading)
        {
            startInitOriginateContractLoading()
        }
        else
        {
            onInitDelegateLoadComplete(null)

            if (mFinalizeDelegateLoading)
            {
                startFinalizeLoadingOriginateContract()
            }
            else
            {
                onFinalizeDelegationLoadComplete(null)
            }
        }
    }

    private fun putEverythingInRed()
    {
        this.putAmountInRed(true)
        this.putLimitInRed(true)
    }

    fun isAddButtonValid(): Boolean
    {
        return mDelegatePayload != null
                && isDelegateFeeValid()
                && isInputDataValid()
    }

// put everything in RED

    private fun putAmountInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isDelegateAmountValid()

        if (red && !amountValid)
        {
            color = R.color.tz_error
            //update_storage_button.text = getString(R.string.delegate_format, "")
        }
        else
        {
            color = R.color.tz_accent

            if (amountValid)
            {
                //val amount = delegate_transfer_edittext.text.toString()
                //this.setTextPayButton()
            }
            else
            {
                //update_storage_button.text = getString(R.string.delegate_format, "")
            }
        }

        amount_limit_edittext.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun putLimitInRed(red: Boolean)
    {
        val color: Int

        val limitValid = isDailySpendingLimitValid()

        if (red && !limitValid)
        {
            color = R.color.tz_error
            create_limit_contract_button.text = getString(R.string.delegate_format, "")
        }
        else
        {
            color = R.color.tz_accent
        }

        limit_edittext.setTextColor(ContextCompat.getColor(this, color))
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        //val fragment = supportFragmentManager.findFragmentById(R.id.form_fragment_container)
        //fragment?.onActivityResult(requestCode, resultCode, data)
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
            startFinalizeLoadingOriginateContract()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(supportFragmentManager, "Authentication")
    }

    private fun isFingerprintAllowed():Boolean
    {
        return storage.isFingerprintAllowed()
    }

    private fun hasEnrolledFingerprints():Boolean
    {
        return systemServices.hasEnrolledFingerprints()
    }

    private fun saveFingerprintAllowed(useInFuture:Boolean)
    {
        storage.saveFingerprintAllowed(useInFuture)
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
        val storage = Storage(this)
        return EncryptionServices().decrypt(storage.getPassword()) == inputtedPassword
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices().validateFingerprintAuthentication(cryptoObject))
        {
            startFinalizeLoadingOriginateContract()
        }
        else
        {
            onDelegateClick()
        }
    }

    private fun cancelRequests(resetBooleans:Boolean)
    {
        val requestQueue = VolleySingleton.getInstance(applicationContext).requestQueue
        requestQueue?.cancelAll(DELEGATE_INIT_TAG)
        requestQueue?.cancelAll(DELEGATE_FINALIZE_TAG)

        if (resetBooleans)
        {
            mInitDelegateLoading = false
            mFinalizeDelegateLoading = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(DELEGATE_INIT_TAG, mInitDelegateLoading)
        outState.putBoolean(DELEGATE_FINALIZE_TAG, mFinalizeDelegateLoading)

        outState.putString(DELEGATE_PAYLOAD_KEY, mDelegatePayload)

        outState.putDouble(DELEGATE_AMOUNT_KEY, mDelegateAmount)

        outState.putLong(LIMIT_AMOUNT_KEY, mLimitAmount)

        outState.putLong(DELEGATE_FEE_KEY, mDelegateFees)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests(false)
    }
}
