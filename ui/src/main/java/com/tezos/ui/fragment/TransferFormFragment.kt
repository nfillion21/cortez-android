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
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatSpinner
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.Account
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.activity.PaymentAccountsActivity
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.authentication.SystemServices
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.VolleySingleton
import com.tezos.ui.utils.hexToByteArray
import com.tezos.ui.utils.toNoPrefixHexString
import kotlinx.android.synthetic.main.fragment_payment_form.*
import kotlinx.android.synthetic.main.payment_form_card_info.*
import org.json.JSONObject

/**
 * Created by nfillion on 20/04/16.
 */
class TransferFormFragment : Fragment()
{
    private val TRANSFER_INIT_TAG = "transfer_init"
    private val TRANSFER_FINALIZE_TAG = "transfer_finalize"

    private val TRANSFER_ID_KEY = "transfer_id_key"
    private val TRANSFER_PAYLOAD_KEY = "transfer_payload_key"

    private val systemServices by lazy(LazyThreadSafetyMode.NONE) { SystemServices(activity?.baseContext!!) }
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(activity?.applicationContext!!) }

    private var mPayButton: Button? = null
    private var mPayButtonLayout: FrameLayout? = null

    private var mSrcButton: Button? = null
    private var mDstButton: Button? = null

    private var mTransferSrcFilled: LinearLayout? = null
    private var mTransferDstFilled: LinearLayout? = null

    private var mTransferSrcPkh: TextView? = null
    private var mTransferDstPkh: TextView? = null

    private var mCurrencySpinner: AppCompatSpinner? = null

    private var mAmount:TextInputEditText? = null

    private var mSrcAccount:Account? = null
    private var mDstAccount:Account? = null

    private var listener: OnTransferListener? = null

    private var mInitTransferLoading:Boolean = false
    private var mFinalizeTransferLoading:Boolean = false

    private var mTransferId:Int? = null
    private var mTransferPayload:String? = null

    companion object
    {
        @JvmStatic
        fun newInstance(seedDataBundle:Bundle, customTheme:Bundle) =
                TransferFormFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, customTheme)
                        putBundle(Storage.TAG, seedDataBundle)
                    }
                }

        private const val SRC_ACCOUNT_KEY = "src_account_key"
        private const val DST_ACCOUNT_KEY = "dst_account_key"
    }

    interface OnTransferListener
    {
        fun onTransferSucceed()
        fun onTransferLoading(loading: Boolean)
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnTransferListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException(context.toString() + " must implement OnTransferListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        initContentViews(view)

        if (savedInstanceState != null)
        {
            val srcBundle = savedInstanceState.getParcelable<Bundle>(SRC_ACCOUNT_KEY)
            if (srcBundle != null)
            {
                mSrcAccount = Account.fromBundle(srcBundle)
            }

            val dstBundle = savedInstanceState.getParcelable<Bundle>(DST_ACCOUNT_KEY)
            if (dstBundle != null)
            {
                mDstAccount = Account.fromBundle(dstBundle)
                switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)
            }

            mTransferId = savedInstanceState.getInt(TRANSFER_ID_KEY, -1)
            mTransferPayload = savedInstanceState.getString(TRANSFER_PAYLOAD_KEY, null)

            mInitTransferLoading = savedInstanceState.getBoolean(TRANSFER_INIT_TAG)
            mFinalizeTransferLoading = savedInstanceState.getBoolean(TRANSFER_FINALIZE_TAG)

            transferLoading(isLoading())

            //TODO we got to keep in mind there's an id already.
            if (mInitTransferLoading)
            {
                //TODO make it load
                //refreshTextBalance(false)

                startInitTransferLoading()
            }
            else
            {
                onInitTransferLoadComplete(null)

                if (mFinalizeTransferLoading)
                {
                    //TODO ui stuff.
                    //refreshRecyclerViewAndTextHistory()

                    startFinalizeTransferLoading()
                }
                else
                {
                    onFinalizeTransferLoadComplete(null)
                }
            }
        }
    }

    private fun onInitTransferLoadComplete(error:VolleyError?)
    {
        mInitTransferLoading = false

        if (error != null)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)

            //TODO possibly analyze the error and snackbar it
        }
        else
        {
            // it's signed, looks like it worked.
            //transferLoading(true)
        }
    }

    private fun onFinalizeTransferLoadComplete(error: VolleyError?)
    {
        mFinalizeTransferLoading = false

        if (error != null)
        {
            transferLoading(false)

            //TODO possibly analyze the error and snackbar it
        }
        else
        {
            //TODO it's called in onViewCreated, not only when transfer is succeed

            listener?.onTransferSucceed()
            // if everything
            //transferLoading(true)
        }
    }

    private fun startInitTransferLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        // this fragment always have mnemonics arg
        arguments?.let {
            val seedDataBundle = it.getBundle(Storage.TAG)
            val mnemonicsData = Storage.fromBundle(seedDataBundle!!)

            startPostRequestLoadInitTransfer(mnemonicsData)
        }
    }

    private fun startFinalizeTransferLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        // this fragment always have mnemonics arg
        arguments?.let {
            val seedDataBundle = it.getBundle(Storage.TAG)
            val mnemonicsData = Storage.fromBundle(seedDataBundle!!)

            startPostRequestLoadFinalizeTransfer(mnemonicsData)
        }
    }

    // volley
    private fun startPostRequestLoadInitTransfer(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_url)

        //TODO lock the UI and put this stuff in savedInstance, in case we turn the screen

        var amount = mAmount?.text.toString().toDouble()
        var fee = 0.0
        val selectedItemThreeDS = mCurrencySpinner!!.selectedItemId

        when (selectedItemThreeDS.toInt())
        {
            0 -> { fee = 0.05 }
            1 -> { fee = 0.00 }
            2 -> { fee = 0.01 }
            else -> {}
        }

        // convert in µꜩ
        amount *= 1000000
        fee *= 1000000

        //TODO need to check if activity is not null

        val mnemonics = EncryptionServices(activity!!).decrypt(mnemonicsData.mnemonics, "not useful for marshmallow")
        val pk = CryptoUtils.generatePk(mnemonics, "")

        val pkhSrc = mnemonicsData.pkh
        val pkhDst = mDstAccount?.pubKeyHash

        var postParams = JSONObject()
        postParams.put("src", pkhSrc)
        postParams.put("src_pk", pk)
        postParams.put("dst", pkhDst)
        postParams.put("amount", amount.toInt().toString())
        postParams.put("fee", fee.toInt().toString())

        val jsObjRequest = object : JsonObjectRequest(Request.Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request

            onInitTransferLoadComplete(null)

            mTransferId = answer.getInt("id")
            mTransferPayload = answer.getString("payload")

            startFinalizeTransferLoading()

        }, Response.ErrorListener
        {
            onInitTransferLoadComplete(it)
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

        cancelRequests()

        jsObjRequest.tag = TRANSFER_INIT_TAG
        mInitTransferLoading = true
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
    }

    // volley
    private fun startPostRequestLoadFinalizeTransfer(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_finalize)

        if (mTransferId != null && mTransferId != -1 && mTransferPayload != null)
        {
            val byteArrayThree = mTransferPayload!!.hexToByteArray()

            val mnemonics = EncryptionServices(activity!!).decrypt(mnemonicsData.mnemonics, "not useful for marshmallow")
            val sk = CryptoUtils.generateSk(mnemonics, "")
            val signature = KeyPair.sign(sk, byteArrayThree)

            //TODO verify signature
            //val signVerified = KeyPair.verifySign(signature, byteArrayThree, pk)

            var hexSign = signature.toNoPrefixHexString()

            var postparams = JSONObject()
            postparams.put("id", mTransferId!!)
            postparams.put("payload", hexSign)
            //payload is hex signature, 64 bytes then 128 hex chars

            val jsObjRequest = object : JsonObjectRequest(Request.Method.POST, url, postparams, Response.Listener<JSONObject>
            {
                answer ->

                //TODO check the JSON object before calling success

                onFinalizeTransferLoadComplete(null)

            }, Response.ErrorListener
            {

                onFinalizeTransferLoadComplete(it)
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

            cancelRequests()

            jsObjRequest.tag = TRANSFER_FINALIZE_TAG

            mFinalizeTransferLoading = true
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
        }
        else
        {
            //something wrong happened. we got the finalizeTransfer boolean but not the payload + id
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_payment_form, container, false)
    }

    private fun initContentViews(view: View)
    {
        val args = arguments
        val themeBundle = args!!.getBundle(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        val focusChangeListener = this.focusChangeListener()

        mAmount = view.findViewById(R.id.amount_transfer)
        mAmount?.addTextChangedListener(GenericTextWatcher(mAmount!!))
        mAmount?.onFocusChangeListener = focusChangeListener

        mPayButton = view.findViewById(R.id.pay_button)
        mPayButtonLayout = view.findViewById(R.id.pay_button_layout)

        mCurrencySpinner = view.findViewById(R.id.fee_spinner)
        val adapter = ArrayAdapter.createFromResource(activity!!,
                R.array.array_fee, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mCurrencySpinner!!.adapter = adapter
        mCurrencySpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long)
            {
                putAmountInRed(false)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        mSrcButton = view.findViewById(R.id.transfer_src_button)
        mSrcButton!!.setOnClickListener { _ ->
            PaymentAccountsActivity.start(activity,
                theme,
                PaymentAccountsActivity.FromScreen.FromTransfer,
                PaymentAccountsActivity.Selection.SelectionAccounts)
        }

        mDstButton = view.findViewById(R.id.transfer_dst_button)
        mDstButton!!.setOnClickListener { _ ->
            PaymentAccountsActivity.start(
                    activity,
                    theme,
                    PaymentAccountsActivity.FromScreen.FromTransfer,
                    PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses)
        }

        mTransferSrcFilled = view.findViewById(R.id.transfer_source_filled)
        mTransferDstFilled = view.findViewById(R.id.transfer_destination_filled)

        mTransferSrcPkh = view.findViewById(R.id.src_payment_account_pub_key_hash)
        mTransferDstPkh = view.findViewById(R.id.dst_payment_account_pub_key_hash)

        mPayButtonLayout!!.visibility = View.VISIBLE

        val moneyString = getString(R.string.pay, "ꜩ")

        mPayButton!!.text = moneyString

        mPayButtonLayout!!.setOnClickListener { _ ->
            onPayClick()
        }

        arguments?.let {

            val seedDataBundle = it.getBundle(Storage.TAG)
            val seedData = Storage.fromBundle(seedDataBundle)

            var account = Account()
            account.pubKeyHash = seedData.pkh
            switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccounts, account)
        }

        validatePayButton(isInputDataValid())

        putEverythingInRed()
    }

    private fun isLoading():Boolean
    {
        return mInitTransferLoading || mFinalizeTransferLoading
    }

    private fun transferLoading(loading:Boolean)
    {
        if (loading)
        {
            mPayButtonLayout?.visibility = View.GONE
            empty.visibility = View.VISIBLE
            fee_spinner.isEnabled = false
            amount_transfer.isEnabled = false
        }
        else
        {
            mPayButtonLayout?.visibility = View.VISIBLE
            empty.visibility = View.INVISIBLE
            fee_spinner.isEnabled = true
            amount_transfer.isEnabled = true
        }

        listener?.onTransferLoading(loading)
    }

    private fun onPayClick()
    {
        val dialog = AuthenticationDialog()
        if (storage.isFingerprintAllowed() && systemServices.hasEnrolledFingerprints())
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices(activity?.applicationContext!!).prepareFingerprintCryptoObject()
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
            startInitTransferLoading()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity?.supportFragmentManager, "Authentication")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PaymentAccountsActivity.TRANSFER_SELECT_REQUEST_CODE)
        {
            if (data != null && data.hasExtra(Account.TAG))
            {
                val accountBundle = data.getBundleExtra(Account.TAG)
                val account = Account.fromBundle(accountBundle)

                if (resultCode == R.id.transfer_src_selection_succeed)
                {
                    mSrcAccount = account
                    switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccounts, mSrcAccount!!)
                }
                else if (resultCode == R.id.transfer_dst_selection_succeed)
                {
                    mDstAccount = account
                    switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)
                    validatePayButton(isInputDataValid())
                }
            }
        }
    }

    private fun switchButtonAndLayout(selection: PaymentAccountsActivity.Selection, account: Account)
    {
        when (selection)
        {
            PaymentAccountsActivity.Selection.SelectionAccounts ->
            {
                mSrcButton?.visibility = View.GONE
                mTransferSrcFilled?.visibility = View.VISIBLE

                mTransferSrcPkh?.text = account.pubKeyHash
            }

            PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses ->
            {
                mDstButton?.visibility = View.GONE
                mTransferDstFilled?.visibility = View.VISIBLE
                mTransferDstPkh?.text = account.pubKeyHash
            }

            else ->
            {
                //no-op
            }
        }
    }


    private fun isTransferAmountValid():Boolean
    {
        val isAmountValid = false

        if (mAmount != null && !TextUtils.isEmpty(mAmount?.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val amount = mAmount?.text!!.toString().toDouble()

                if (amount >= 0.000001f)
                {
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                return false
            }
        }

        return isAmountValid
    }
    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id

            if (i == R.id.amount_transfer)
            {
                putAmountInRed(!hasFocus)
            }
            else
            {
                throw UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }

    private fun validatePayButton(validate: Boolean)
    {
        if (validate)
        {
            val customThemeBundle = arguments!!.getBundle(CustomTheme.TAG)
            val theme = CustomTheme.fromBundle(customThemeBundle)

            mPayButton!!.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            mPayButtonLayout!!.isEnabled = true
            mPayButtonLayout!!.background = makeSelector(theme)

            val drawables = mPayButton!!.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
        }
        else
        {
            mPayButton!!.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            mPayButtonLayout!!.isEnabled = false
            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            mPayButtonLayout!!.background = makeSelector(greyTheme)

            val drawables = mPayButton!!.compoundDrawables
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
            }
            else
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }
            validatePayButton(isInputDataValid())
        }
    }

    fun isInputDataValid(): Boolean
    {
        return isTransferAmountValid()
                && mDstAccount != null
    }

    private fun putEverythingInRed()
    {
        this.putAmountInRed(true)
    }

    // put everything in RED

    private fun putAmountInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isTransferAmountValid()

        if (red && !amountValid)
        {
            color = R.color.tz_error
            mPayButton!!.text = getString(R.string.pay, "")
        }
        else
        {
            color = R.color.tz_accent

            if (amountValid)
            {
                val amount = mAmount!!.text.toString()
                this.setTextPayButton(amount)
            }
            else
            {
                mPayButton!!.text = getString(R.string.pay, "")
            }
        }

        mAmount?.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun setTextPayButton(amount: String)
    {
        var amount = amount
        //var amountDouble: Double = java.lang.Double.parseDouble(amount)
        var amountDouble: Double = amount.toDouble()

        val selectedItemThreeDS = mCurrencySpinner!!.selectedItemId

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
        mPayButton!!.text = getString(R.string.pay, moneyFormatted2)
    }

    /**
     * Fingerprint was invalidated, decide what to do in this case.
     */
    private fun onFingerprintInvalidation(useInFuture: Boolean)
    {
        storage.saveFingerprintAllowed(useInFuture)
        if (useInFuture)
        {
            EncryptionServices(activity?.applicationContext!!).createFingerprintKey()
        }
    }

    /**
     * Validate password inputted from Authentication Dialog.
     */
    private fun validatePassword(inputtedPassword: String): Boolean
    {
        val storage = Storage(activity!!)
        return EncryptionServices(activity?.applicationContext!!).decrypt(storage.getPassword(), inputtedPassword) == inputtedPassword
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices(activity?.applicationContext!!).validateFingerprintAuthentication(cryptoObject))
        {
            startInitTransferLoading()
        }
        else
        {
            onPayClick()
        }
    }

    private fun cancelRequests()
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
        requestQueue?.cancelAll(TRANSFER_INIT_TAG)
        requestQueue?.cancelAll(TRANSFER_FINALIZE_TAG)

        mInitTransferLoading = false
        mFinalizeTransferLoading = false
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putParcelable(SRC_ACCOUNT_KEY, mSrcAccount?.toBundle())
        outState.putParcelable(DST_ACCOUNT_KEY, mDstAccount?.toBundle())

        outState.putBoolean(TRANSFER_INIT_TAG, mInitTransferLoading)
        outState.putBoolean(TRANSFER_FINALIZE_TAG, mFinalizeTransferLoading)

        when {
            mTransferId != null -> outState.putInt(TRANSFER_ID_KEY, mTransferId!!)
            else -> outState.putInt(TRANSFER_ID_KEY, -1)
        }
        outState.putString(TRANSFER_PAYLOAD_KEY, mTransferPayload)
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
