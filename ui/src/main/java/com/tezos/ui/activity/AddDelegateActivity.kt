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
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.activity_add_delegate.*
import kotlinx.android.synthetic.main.delegate_form_card_info.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Created by nfillion on 20/11/18.
 */
class AddDelegateActivity : BaseSecureActivity()
{
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    private var mInitDelegateLoading:Boolean = false
    private var mFinalizeDelegateLoading:Boolean = false

    private var mDelegatePayload:String? = null
    private var mDelegateFees:Long = -1L

    private var mDelegateAmount:Double = -1.0
    private var mDelegateTezosAddress:String? = null

    private var mClickCalculate:Boolean = false

    companion object
    {
        var ADD_DELEGATE_REQUEST_CODE = 0x3100 // arbitrary int

        private const val DELEGATE_INIT_TAG = "delegate_init"
        private const val DELEGATE_FINALIZE_TAG = "delegate_finalize"

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val DELEGATE_AMOUNT_KEY = "delegate_amount_key"
        private const val DELEGATE_TEZOS_ADDRESS_KEY = "delegate_tezos_address_key"
        private const val DELEGATE_FEE_KEY = "delegate_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private fun getStartIntent(context: Context, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, AddDelegateActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, ADD_DELEGATE_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_delegate)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        validateAddButton(isInputDataValid() && isDelegateFeeValid())

        update_storage_button_layout.setOnClickListener {
            onDelegateClick()
        }

        initToolbar(theme)

        amount_edittext.addTextChangedListener(GenericTextWatcher(amount_edittext))

        val focusChangeListener = this.focusChangeListener()
        amount_edittext.onFocusChangeListener = focusChangeListener

        public_address_edittext.addTextChangedListener(GenericTextWatcher(public_address_edittext))

        public_address_edittext.onFocusChangeListener = focusChangeListener

        if (savedInstanceState != null)
        {
            mDelegatePayload = savedInstanceState.getString(DELEGATE_PAYLOAD_KEY, null)

            mInitDelegateLoading = savedInstanceState.getBoolean(DELEGATE_INIT_TAG)
            mFinalizeDelegateLoading = savedInstanceState.getBoolean(DELEGATE_FINALIZE_TAG)

            mDelegateAmount = savedInstanceState.getDouble(DELEGATE_AMOUNT_KEY, -1.0)

            mDelegateTezosAddress = savedInstanceState.getString(DELEGATE_TEZOS_ADDRESS_KEY, null)

            mDelegateFees = savedInstanceState.getLong(DELEGATE_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)
        }
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id

            if (i == R.id.amount_edittext)
            {
                putAmountInRed(!hasFocus)
            }
            else if (i == R.id.public_address_edittext)
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

    private fun initToolbar(theme: CustomTheme)
    {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryId))
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

    private fun startInitDelegationLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()
        putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validateAddButton(false)

        startPostRequestLoadInitDelegation()
    }

    private fun startFinalizeDelegationLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        val mnemonicsData = Storage(baseContext).getMnemonics()
        startPostRequestLoadFinalizeDelegate(mnemonicsData)
    }

    // volley
    private fun startPostRequestLoadFinalizeDelegate(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isAddButtonValid() && mDelegatePayload != null)
        {
            val pkhSrc = mnemonicsData.pkh
            //val pkhDst = mDstAccount?.pubKeyHash

            val mnemonics = EncryptionServices(this).decrypt(mnemonicsData.mnemonics)
            val pk = CryptoUtils.generatePk(mnemonics, "")

            var postParams = JSONObject()
            postParams.put("src", pkhSrc)
            postParams.put("src_pk", pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            //dstObject.put("dst", pkhDst)

            val mutezAmount = (mDelegateAmount*1000000.0).toLong()
            dstObject.put("balance", mutezAmount)

            dstObject.put("fee", mDelegateFees)

            dstObject.put("delegate", mDelegateTezosAddress)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

            if (isAddDelegatePayloadValid(mDelegatePayload!!, postParams))
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

                            //there's no need to do anything because we call finish()
                            //onFinalizeDelegationLoadComplete(null)

                            setResult(R.id.add_address_succeed, null)
                            finish()
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

    fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String
    {
        return this.bufferedReader(charset).use { it.readText() }
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
    private fun startPostRequestLoadInitDelegation()
    {
        val mnemonicsData = Storage(baseContext).getMnemonics()

        val url = getString(R.string.originate_account_url)

        val mnemonics = EncryptionServices(this).decrypt(mnemonicsData.mnemonics)
        val pk = CryptoUtils.generatePk(mnemonics, "")

        val pkhSrc = mnemonicsData.pkh
        //val pkhDst = mDstAccount?.pubKeyHash

        var postParams = JSONObject()
        postParams.put("src", pkhSrc)
        postParams.put("src_pk", pk)

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        dstObject.put("manager", pkhSrc)


        dstObject.put("delegate", mDelegateTezosAddress)

        //TODO be careful, do it in mutez.
        dstObject.put("credit", (mDelegateAmount*1000000).toLong().toString())

        dstObject.put("delegatable", true)

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Request.Method.POST, url, postParams, Response.Listener<JSONObject>
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
                fee_edittext.setText(feeInTez.toString())

                validateAddButton(isInputDataValid() && isDelegateFeeValid())

                if (isInputDataValid() && isDelegateFeeValid())
                {
                    validateAddButton(true)

                    this.setTextPayButton()
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

    private fun transferLoading(loading:Boolean)
    {
        if (loading)
        {
            update_storage_button_layout.visibility = View.GONE
            nav_progress.visibility = View.VISIBLE
            //amount_transfer.isEnabled = false
        }
        else
        {
            update_storage_button_layout.visibility = View.VISIBLE
            nav_progress.visibility = View.INVISIBLE
            //amount_transfer.isEnabled = true
        }
    }

    private fun putFeesToNegative()
    {
        fee_edittext.setText("")

        mClickCalculate = false
        fee_edittext.isEnabled = false
        fee_edittext.hint = getString(R.string.neutral)

        mDelegateFees = -1

        mDelegatePayload = null
    }

    private fun putPayButtonToNull()
    {
        update_storage_button.text = getString(R.string.pay, "")
    }

    private fun showSnackBar(error:VolleyError?)
    {
        var errorStr: String? = error?.toString() ?: getString(R.string.generic_error)

        val snackbar = Snackbar.make(findViewById(R.id.content), errorStr.toString(), Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                android.R.color.holo_red_light)))
        snackbar.show()
    }

    private fun validateAddButton(validate: Boolean)
    {
        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        if (validate)
        {
            update_storage_button.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
            update_storage_button_layout.isEnabled = true
            update_storage_button_layout.background = makeSelector(theme)

            val drawables = update_storage_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.textColorPrimaryId))
        }
        else
        {
            update_storage_button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            update_storage_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            update_storage_button_layout.background = makeSelector(greyTheme)

            val drawables = update_storage_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
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

            if ((i == R.id.amount_edittext && !isDelegateAmountEquals(editable))
                            ||
                 (i == R.id.public_address_edittext && !isDelegateTezosAddressEquals(editable)))
            {
                if (i == R.id.amount_edittext )
                {
                    putAmountInRed(false)
                }

                if (i == R.id.public_address_edittext)
                {
                    putTzAddressInRed(false)
                }

                //TODO text changed
                //TODO load again but only if we don't have any same forged data.

                //val amount = java.lang.Double.parseDouble()

                //TODO check if it's already

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
            else if (i != R.id.amount_edittext && i != R.id.public_address_edittext)
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
        return isDelegateAmountValid() && isTzAddressValid()
    }

    private fun isTzAddressValid(): Boolean
    {
        var isTzAddressValid = false

        if (!TextUtils.isEmpty(public_address_edittext.text))
        {
            if (Utils.isTzAddressValid(public_address_edittext.text!!.toString()))
            {
                mDelegateTezosAddress = public_address_edittext.text.toString()
                isTzAddressValid = true
            }
        }

        return isTzAddressValid
    }


    private fun isDelegateFeeValid():Boolean
    {
        val isFeeValid = false

        if (fee_edittext.text != null && !TextUtils.isEmpty(fee_edittext.text))
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

    private fun isDelegateAmountValid():Boolean
    {
        val isAmountValid = false

        if (amount_edittext.text != null && !TextUtils.isEmpty(amount_edittext.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val amount = amount_edittext.text!!.toString().toDouble()

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

    override fun onResume()
    {
        super.onResume()

        putEverythingInRed()

        if (isInputDataValid() && isDelegateFeeValid())
        {
            validateAddButton(true)
            this.setTextPayButton()
        }

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
                startFinalizeDelegationLoading()
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
        this.putTzAddressInRed(true)
    }

    fun isAddButtonValid(): Boolean
    {
        return mDelegatePayload != null
                && isDelegateFeeValid()
                && isInputDataValid()
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

        public_address_edittext.setTextColor(ContextCompat.getColor(this, color))
    }

// put everything in RED

    private fun putAmountInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isDelegateAmountValid()

        if (red && !amountValid)
        {
            color = R.color.tz_error
            update_storage_button.text = getString(R.string.delegate_format, "")
        }
        else
        {
            color = R.color.tz_accent

            if (amountValid)
            {
                //val amount = delegate_transfer_edittext.text.toString()
                this.setTextPayButton()
            }
            else
            {
                update_storage_button.text = getString(R.string.delegate_format, "")
            }
        }

        amount_edittext.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun setTextPayButton()
    {
        var amountDouble: Double = mDelegateAmount

        //amountDouble += fee_edittext.text.toString().toLong()/1000000
        amountDouble += mDelegateFees.toDouble()/1000000.0

        var amount = amountDouble.toString()

        if (amount.contains("."))
        {
            val elements = amount.substring(amount.indexOf("."))

            when
            {
                elements.length > 7 ->
                {
                    amount = String.format("%.6f", amount.toDouble())
                    val d = amount.toDouble()
                    amount = d.toString()
                }

                elements.length <= 3 ->
                {
                    amount = String.format("%.2f", amount.toDouble())
                }
                else ->
                {
                    //                        int length = elements.length() - 1;
                    //                        String format = "%." + length + "f";
                    //                        Float f = Float.parseFloat(amount);
                    //                        amount = String.format(format, f);
                }
            }
        }
        else
        {
            amount = String.format("%.2f", amount.toDouble())
//amount = Double.parseDouble(amount).toString();
        }

        val moneyFormatted2 = "$amount ꜩ"
//String moneyFormatted3 = Double.toString(amountDouble) + " ꜩ";
        update_storage_button.text = getString(R.string.pay, moneyFormatted2)
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
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices(this).prepareFingerprintCryptoObject()
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
            startFinalizeDelegationLoading()
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
            EncryptionServices(this).createFingerprintKey()
        }
    }

    /**
     * Validate password inputted from Authentication Dialog.
     */
    private fun validatePassword(inputtedPassword: String): Boolean
    {
        val storage = Storage(this)
        return EncryptionServices(this).decrypt(storage.getPassword()) == inputtedPassword
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices(this).validateFingerprintAuthentication(cryptoObject))
        {
            startFinalizeDelegationLoading()
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

        outState.putString(DELEGATE_TEZOS_ADDRESS_KEY, mDelegateTezosAddress)

        outState.putLong(DELEGATE_FEE_KEY, mDelegateFees)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests(false)
    }
}
