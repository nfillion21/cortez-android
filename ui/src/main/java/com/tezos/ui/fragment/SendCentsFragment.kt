package com.tezos.ui.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.tezos.ui.fragment.ScriptFragment.Companion.CONTRACT_PUBLIC_KEY
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.dialog_sent_cents.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.interfaces.ECPublicKey

class SendCentsFragment : AppCompatDialogFragment()
{
    private var listener: OnSendCentsInteractionListener? = null

    private var mTransferFees:Long = -1
    private var mClickCalculate:Boolean = false

    private var mTransferPayload:String? = null
    private var mInitTransferLoading:Boolean = false

    private var mFinalizeTransferLoading:Boolean = false

    interface OnSendCentsInteractionListener
    {
        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean
        fun saveFingerprintAllowed(useInFuture: Boolean)

        fun onTransferSucceed()
    }

    companion object
    {
        const val TAG = "send_cents_fragment"

        private const val TRANSFER_FEE_KEY = "transfer_fee_key"
        private const val FEES_CALCULATE_KEY = "calculate_fee_key"
        private const val TRANSFER_PAYLOAD_KEY = "transfer_payload_key"

        private const val TRANSFER_INIT_TAG = "transfer_init"
        private const val TRANSFER_FINALIZE_TAG = "transfer_finalize"

        @JvmStatic
        fun newInstance(contractPkh:String, theme: CustomTheme) =
                SendCentsFragment().apply {
                    arguments = Bundle().apply {

                        val bundleTheme = theme.toBundle()
                        putBundle(CustomTheme.TAG, bundleTheme)
                        putString(CONTRACT_PUBLIC_KEY, contractPkh)
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, 0)
        //isCancelable = false
        arguments?.let {
            val bundleTheme = it.getBundle(CustomTheme.TAG)
        }
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnSendCentsInteractionListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException("$context must implement OnSendCentsInteractionListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        send_cents_button_layout.setOnClickListener {
            onSendClick()
        }

        close_button.setOnClickListener {
            dismiss()
        }

        from_radio_group.setOnCheckedChangeListener { _, i ->

            when (i)
            {
                R.id.from_contract_button ->
                {
                    val i = i
                    val ki = i
                }

                R.id.from_tz1_button ->
                {
                    val seed = Storage(context!!).getMnemonics()
                    if (seed.mnemonics.isEmpty())
                    {
                        showSnackBar(getString(R.string.no_mnemonics_refill_tz3), ContextCompat.getColor(activity!!, R.color.tz_accent), Color.RED)
                        from_contract_button.isChecked = true
                    }
                }
            }
        }

        val tz3 = retrieveTz3()
        tz3_address_textview.text = tz3

        arguments?.let {
            from_contract_button.text = it.getString(CONTRACT_PUBLIC_KEY)
        }

        val seed = Storage(context!!).getMnemonics()
        from_tz1_button.text = seed.pkh
        if (seed.mnemonics.isEmpty())
        {
            //showSnackBar(getString(R.string.no_mnemonics_refill_tz3), ContextCompat.getColor(activity!!, R.color.tz_accent), Color.RED)
            from_tz1_button.isEnabled = false
        }

        if (savedInstanceState != null)
        {
            mTransferFees = savedInstanceState.getLong(TRANSFER_FEE_KEY, -1)
            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mTransferPayload = savedInstanceState.getString(TRANSFER_PAYLOAD_KEY, null)

            mInitTransferLoading = savedInstanceState.getBoolean(TRANSFER_INIT_TAG)
            mFinalizeTransferLoading = savedInstanceState.getBoolean(TRANSFER_FINALIZE_TAG)

            if (mInitTransferLoading)
            {
                refreshTextUnderDelegation(false)
                startPostRequestLoadInitTransfer()
            }
            else
            {
                onInitTransferLoadComplete(null)
            }
        }
        else
        {
            startInitTransferLoading()
        }

        validateSendCentsButton(isTransferFeeValid())
        if (isTransferFeeValid())
        {
            setTextPayButton()
        }
    }

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
            //startFinalizeTransferLoading()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity?.supportFragmentManager, "Authentication")
    }

    private fun setTextPayButton()
    {
        var amountDouble: Double = (mTransferFees.toDouble() + 100000)/1000000.0

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
        send_cents_button.text = getString(R.string.pay, moneyFormatted2)
    }

    override fun onResume()
    {
        super.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_sent_cents, container, false)
    }

    private fun startInitTransferLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        //TODO need to handle transfers from KT1 or from tz1

        putFeesToNegative()
        putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validateSendCentsButton(false)

        startPostRequestLoadInitTransfer()
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
        if (isTransferFeeValid() && mTransferPayload != null)
        {
            //val pkhSrc = seed.pkh
            //val pkhDst = mDstAccount

            /*
            val mnemonics = EncryptionServices().decrypt(seed.mnemonics)
            val pk = CryptoUtils.generatePk(mnemonics, "")

            var postParams = JSONObject()
            postParams.put("src", mSrcAccount)

            //TODO it won't be pk with contract transfer
            postParams.put("src_pk", pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            dstObject.put("dst", pkhDst)

            val mutezAmount = (mTransferAmount*1000000.0).toLong()
            dstObject.put("amount", mutezAmount)

            dstObject.put("fee", mTransferFees)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)
            */

            //TODO verify the payloads
            if (/*!isTransferPayloadValid(mTransferPayload!!, postParams)*/true)
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mTransferPayload!!.hexToByteArray()

                val xLen = zeroThree.size
                val yLen = byteArrayThree.size
                val result = ByteArray(xLen + yLen)

                System.arraycopy(zeroThree, 0, result, 0, xLen)
                System.arraycopy(byteArrayThree, 0, result, xLen, yLen)

                var compressedSignature = ByteArray(64)
                /*
                if (mSourceKT1withCode)
                {
                    val bytes = KeyPair.b2b(result)
                    var signature = EncryptionServices().sign(bytes)

                    if (signature != null)
                    {
                        compressedSignature = compressFormat(signature)
                    }
                }
                else
                {
                    val mnemonics = EncryptionServices().decrypt(seed.mnemonics)
                    val sk = CryptoUtils.generateSk(mnemonics, "")
                    compressedSignature = KeyPair.sign(sk, result)
                }
                */
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
                        Response.Listener<String> { response ->

                            if (rootView != null)
                            {
                                onFinalizeTransferLoadComplete(null)
                                dismiss()
                                listener?.onTransferSucceed()
                            }
                        },
                        Response.ErrorListener
                        {
                            if (rootView != null)
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





    private fun startPostRequestLoadInitTransfer()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
        val pk = CryptoUtils.generatePk(mnemonics, "")

        var postParams = JSONObject()
        postParams.put("src", mnemonicsData.pkh)
        postParams.put("src_pk", pk)

        var dstObject = JSONObject()
        dstObject.put("dst", retrieveTz3())

        //0.1 tez == 100 000 mutez
        dstObject.put("amount", (100000).toLong().toString())

        var dstObjects = JSONArray()
        dstObjects.put(dstObject)
        postParams.put("dsts", dstObjects)

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

                    val feeInTez = mTransferFees?.toDouble()/1000000.0
                    fee_edittext?.setText(feeInTez?.toString())

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

        jsObjRequest.tag = TRANSFER_INIT_TAG
        mInitTransferLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
    }

    private fun onInitTransferLoadComplete(error: VolleyError?)
    {
        mInitTransferLoading = false

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
                startInitTransferLoading()
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

    private fun onFinalizeTransferLoadComplete(error: VolleyError?)
    {
        // everything is over, there's no call to make
        cancelRequests(true)

        if (error != null)
        {
            transferLoading(false)

            var error: String = error?.toString()

            //showSnackBar(error, ContextCompat.getColor(this,
                    //android.R.color.holo_red_light), null)
            //listener?.onTransferFailed(error)
            showSnackBar(error, ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }
        else
        {
            // the finish call is made already
        }
    }

    private fun refreshTextUnderDelegation(animating:Boolean)
    {
        //this method handles the data and loading texts

        /*
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

        */
    }

    fun showSnackBar(res:String, color:Int, textColor:Int)
    {
        val snackbar = Snackbar.make(rootView, res, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(color)
        snackbar.setActionTextColor(textColor)
        snackbar.show()
    }

    private fun transferLoading(loading:Boolean)
    {
        // handle the visibility of bottom buttons

        if (loading)
        {
            fee_progress?.visibility = View.VISIBLE
        }
        else
        {
            fee_progress?.visibility = View.GONE
        }
    }

    private fun putFeesToNegative()
    {
        fee_edittext?.setText("")

        mClickCalculate = false
        fee_edittext?.isEnabled = false
        fee_edittext?.hint = getString(R.string.neutral)

        mTransferFees = -1

        mTransferPayload = null
    }

    private fun putPayButtonToNull()
    {
        send_cents_button?.text = getString(R.string.pay, "")
    }

    private fun isTransferFeeValid():Boolean
    {
        return mTransferFees != -1L

        /*
        val isFeeValid = false

        val fee_edit = fee_edittext
        val text = fee_edittext.text

        if (fee_edittext?.text != null && !TextUtils.isEmpty(fee_edittext?.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val fee = fee_edittext.text.toString().toDouble()

                if (fee >= 0.000001f)
                {
                    val longTransferFee = fee*1000000
                    mTransferFees = longTransferFee.toLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mTransferFees = -1
                return false
            }
        }

        return isFeeValid
        */
    }

    private fun validateSendCentsButton(validate: Boolean)
    {
        val bundleTheme = arguments!!.getBundle(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(bundleTheme)

        if (validate)
        {
            send_cents_button.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            send_cents_button_layout.isEnabled = true
            send_cents_button_layout.background = makeSelector(theme)

            val drawables = send_cents_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
        }
        else
        {
            send_cents_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            send_cents_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            send_cents_button_layout.background = makeSelector(greyTheme)

            val drawables = send_cents_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
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
        var keyPair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        if (keyPair != null)
        {
            val ecKey = keyPair!!.public as ECPublicKey
            return CryptoUtils.generatePkhTz3(ecKeyFormat(ecKey))
        }

        return null
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putLong(TRANSFER_FEE_KEY, mTransferFees)
        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)
        outState.putString(TRANSFER_PAYLOAD_KEY, mTransferPayload)

        outState.putBoolean(TRANSFER_INIT_TAG, mInitTransferLoading)
        outState.putBoolean(TRANSFER_FINALIZE_TAG, mFinalizeTransferLoading)
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
            requestQueue?.cancelAll(TRANSFER_INIT_TAG)
            requestQueue?.cancelAll(TRANSFER_FINALIZE_TAG)

            if (resetBooleans)
            {
                mInitTransferLoading = false
                mFinalizeTransferLoading = false
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
