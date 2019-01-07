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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.VolleySingleton
import kotlinx.android.synthetic.main.activity_add_delegate.*
import kotlinx.android.synthetic.main.delegate_form_card_info.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by nfillion on 20/11/18.
 */
class AddDelegateActivity : BaseSecureActivity()
{
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    private var mInitDelegateLoading:Boolean = false
    private var mFinalizeDelegateLoading:Boolean = false

    private var mDelegatePayload:String? = null
    private var mDelegateFees:Long = -1

    private var mDelegateAmount:Double = -1.0

    private var mClickCalculate:Boolean = false

    companion object
    {
        var ADD_DELEGATE_REQUEST_CODE = 0x3100 // arbitrary int

        private const val DELEGATE_INIT_TAG = "delegate_init"
        private const val DELEGATE_FINALIZE_TAG = "delegate_finalize"

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val DELEGATE_AMOUNT_KEY = "delegate_amount_key"
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

        validateAddButton(isInputDataValid())

        add_delegate_button_layout.setOnClickListener {
            onDelegateClick()
        }

        initToolbar(theme)

        amount_transfer.addTextChangedListener(GenericTextWatcher(amount_transfer))

        val focusChangeListener = this.focusChangeListener()
        amount_transfer.onFocusChangeListener = focusChangeListener

        tezos_address.addTextChangedListener(GenericTextWatcher(tezos_address))

        tezos_address.onFocusChangeListener = focusChangeListener

        val adapter = ArrayAdapter.createFromResource(this,
                R.array.array_fee, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        if (savedInstanceState != null)
        {
            mDelegatePayload = savedInstanceState.getString(DELEGATE_PAYLOAD_KEY, null)

            mInitDelegateLoading = savedInstanceState.getBoolean(DELEGATE_INIT_TAG)
            mFinalizeDelegateLoading = savedInstanceState.getBoolean(DELEGATE_FINALIZE_TAG)

            mDelegateAmount = savedInstanceState.getDouble(DELEGATE_AMOUNT_KEY, -1.0)

            mDelegateFees = savedInstanceState.getLong(DELEGATE_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)
        }
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id

            if (i == R.id.amount_transfer)
            {
                putAmountInRed(!hasFocus)
            }
            else if (i == R.id.tezos_address)
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

        val mCloseButton = findViewById<ImageButton>(R.id.close_button)
        mCloseButton.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId))
        mCloseButton.setOnClickListener { _ ->
            //requests stop in onDestroy.
            finish()
        }

        //nav_progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId), PorterDuff.Mode.SRC_IN)

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
        validatePayButton(false)

        startPostRequestLoadInitDelegation()
    }

    private fun onInitTransferLoadComplete(error:VolleyError?)
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
                startInitDelegateLoading()
            }

            if(error != null)
            {
                listener?.onTransferFailed(error)
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

        val mnemonics = EncryptionServices(this).decrypt(mnemonicsData.mnemonics, "not useful for marshmallow")
        val pk = CryptoUtils.generatePk(mnemonics, "")

        val pkhSrc = mnemonicsData.pkh
        //val pkhDst = mDstAccount?.pubKeyHash

        var postParams = JSONObject()
        postParams.put("src", pkhSrc)
        postParams.put("src_pk", pk)

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        dstObject.put("manager", pk)

        //TODO put the delegate element
        dstObject.put("delegate", pk)

        //TODO put the right amount
        //dstObject.put("credit", (mTransferAmount*1000000).toLong().toString())
        dstObject.put("credit", 1000000.toString())
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
            if (mDelegatePayload != null && mDelegateFees != null)
            {
                onInitDelegateLoadComplete(null)

                val feeInTez = mTransferFees.toDouble()/1000000.0
                fee_edittext.setText(feeInTez.toString())

                validatePayButton(isInputDataValid() && isTransferFeeValid())

                if (isInputDataValid() && isTransferFeeValid())
                {
                    validatePayButton(true)

                    this.setTextPayButton()
                }
                else
                {
                    // should no happen
                    validatePayButton(false)
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
            onInitTransferLoadComplete(it)

            mClickCalculate = true
            //Log.i("mTransferId", ""+mTransferId)
            Log.i("mTransferPayload", ""+mTransferPayload)
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
            pay_button_layout.visibility = View.GONE
            empty.visibility = View.VISIBLE
            //amount_transfer.isEnabled = false
        }
        else
        {
            pay_button_layout.visibility = View.VISIBLE
            empty.visibility = View.INVISIBLE
            //amount_transfer.isEnabled = true
        }

        listener?.onTransferLoading(loading)
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
        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        if (validate)
        {
            add_delegate_button.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
            add_delegate_button_layout.isEnabled = true
            add_delegate_button_layout.background = makeSelector(theme)

            val drawables = add_delegate_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.textColorPrimaryId))
        }
        else
        {
            add_delegate_button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            add_delegate_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            add_delegate_button_layout.background = makeSelector(greyTheme)

            val drawables = add_delegate_button.compoundDrawables
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

            if (i == R.id.amount_transfer)
            {
                putAmountInRed(false)
                mSpinnerPosition = i
            }

            else if (i == R.id.tezos_address)
            {
                putTzAddressInRed(false)
            }
            else
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }

            validateAddButton(isInputDataValid())
        }
    }

    fun isInputDataValid(): Boolean
    {
        return isTransferAmountValid() && isTzAddressValid()
    }

    private fun isTzAddressValid(): Boolean
    {
        val isTzAddressValid = false

        return if (!TextUtils.isEmpty(tezos_address.text))
        {
            Utils.isTzAddressValid(tezos_address.text!!.toString())
        }

        else isTzAddressValid
    }


    private fun isTransferAmountValid():Boolean
    {
        val isAmountValid = false

        if (amount_transfer != null && !TextUtils.isEmpty(amount_transfer.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val amount = amount_transfer.text!!.toString().toDouble()

                //TODO there's no need to put money in there.
                if (amount >= 0.000001f)
                {
                    mAmountCache = amount
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mAmountCache = -1.0
                return false
            }
        }

        return isAmountValid
    }

    override fun onResume()
    {
        super.onResume()

        putEverythingInRed()
    }

    private fun putEverythingInRed()
    {
        this.putAmountInRed(true)
        this.putTzAddressInRed(true)
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

        tezos_address.setTextColor(ContextCompat.getColor(this, color))
    }

// put everything in RED

    private fun putAmountInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isTransferAmountValid()

        if (red && !amountValid)
        {
            color = R.color.tz_error
            add_delegate_button.text = getString(R.string.delegate_format, "")
        }
        else
        {
            color = R.color.tz_accent

            if (amountValid)
            {
                val amount = amount_transfer.text.toString()
                this.setTextPayButton(amount)
            }
            else
            {
                add_delegate_button.text = getString(R.string.delegate_format, "")
            }
        }

        amount_transfer.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun setTextPayButton(amount: String)
    {
        var amount = amount
        var amountDouble: Double = amount.toDouble()

        //val selectedItemThreeDS = fee_spinner.selectedItemId

        /*
        when (selectedItemThreeDS.toInt())
        {
            0 -> {
                amountDouble += 0.01
            }

            1 -> {
                amountDouble += 0.00
            }

            2 -> {
                amountDouble += 0.05
            }

            else -> {
                //no-op
            }
        }
        */

//amount = java.lang.Double.toString(amountDouble)
        amount = amountDouble.toString()

//check the correct amount
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
        add_delegate_button.text = getString(R.string.delegate_format, moneyFormatted2)
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
        if (isFingerprintAllowed()!! && hasEnrolledFingerprints()!!)
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
            //startInitTransferLoading()
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
        return EncryptionServices(this).decrypt(storage.getPassword(), inputtedPassword) == inputtedPassword
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices(this).validateFingerprintAuthentication(cryptoObject))
        {
            //startInitTransferLoading()
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
            mInitTransferLoading = false
            mFinalizeTransferLoading = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle?)
    {
        super.onSaveInstanceState(outState)

        outState?.putDouble(TRANSFER_AMOUNT_KEY, mAmountCache)
        outState?.putInt(TRANSFER_SPINNER_POS_KEY, mSpinnerPosition)
    }
}
