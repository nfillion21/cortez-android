package com.tezos.ui.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.*
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.fragment.ScriptFragment.Companion.CONTRACT_PUBLIC_KEY
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.dialog_sent_cents.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.interfaces.ECPublicKey

class SendCentsFragment : AppCompatDialogFragment()
{
    private var listener: OnSendCentsInteractionListener? = null

    private var mTransferFees:Long = -1
    private var mClickCalculate:Boolean = false

    private var mTransferPayload:String? = null
    private var mInitTransferLoading:Boolean = false

    private var mFinalizeTransferLoading:Boolean = false

    private var mStorage:String? = null

    private var mIsFromContract:Boolean = true

    private var mSig:String? = null

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

        private const val STORAGE_DATA_KEY = "storage_data_key"

        private const val IS_FROM_CONTRACT_KEY = "is_from_contract_key"

        private const val IS_CONTRACT_AVAILABLE_KEY = "is_contract_available_key"

        private const val CONTRACT_SIG_KEY = "contract_sig_key"

        @JvmStatic
        fun newInstance(contractPkh:String, contractAvailable:Boolean, storage:String, theme: CustomTheme) =
                SendCentsFragment().apply {
                    arguments = Bundle().apply {

                        val bundleTheme = theme.toBundle()
                        putBundle(CustomTheme.TAG, bundleTheme)
                        putString(CONTRACT_PUBLIC_KEY, contractPkh)
                        putString(STORAGE_DATA_KEY, storage)
                        putBoolean(IS_CONTRACT_AVAILABLE_KEY, contractAvailable)
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, 0)
        arguments?.let {
            mStorage = it.getString(STORAGE_DATA_KEY)

            val isContractAvailable = it.getBoolean(IS_CONTRACT_AVAILABLE_KEY)
            if (!isContractAvailable)
            {
                mIsFromContract = false
            }
        }
        //isCancelable = false
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
                    mIsFromContract = true
                    startInitTransferLoading(true)
                }

                R.id.from_tz1_button ->
                {
                    mIsFromContract = false
                    startInitTransferLoading(false)
                    /*
                    val seed = Storage(context!!).getMnemonics()
                    if (seed.mnemonics.isEmpty())
                    {
                        showSnackBar(getString(R.string.no_mnemonics_refill_tz3), ContextCompat.getColor(activity!!, R.color.tz_accent), Color.RED)
                        from_contract_button.isChecked = true
                    }
                    */
                }
            }
        }

        val tz3 = retrieveTz3()
        tz3_address_textview.text = tz3

        //startInitTransferLoading(mIsFromContract)
        //from_tz1_button.isChecked = !from_contract_button.isChecked

        arguments?.let {
            from_contract_button.text = it.getString(CONTRACT_PUBLIC_KEY)
            from_contract_button.isEnabled = it.getBoolean(IS_CONTRACT_AVAILABLE_KEY)
        }

        val seed = Storage(context!!).getMnemonics()
        from_tz1_button.text = seed.pkh
        from_tz1_button.isEnabled = seed.mnemonics.isNotEmpty()


        if (savedInstanceState != null)
        {
            mTransferFees = savedInstanceState.getLong(TRANSFER_FEE_KEY, -1)
            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mTransferPayload = savedInstanceState.getString(TRANSFER_PAYLOAD_KEY, null)

            mInitTransferLoading = savedInstanceState.getBoolean(TRANSFER_INIT_TAG)
            mFinalizeTransferLoading = savedInstanceState.getBoolean(TRANSFER_FINALIZE_TAG)

            mStorage = savedInstanceState.getString(STORAGE_DATA_KEY, null)

            mIsFromContract = savedInstanceState.getBoolean(IS_FROM_CONTRACT_KEY)

            mSig = savedInstanceState.getString(CONTRACT_SIG_KEY, null)

            if (mInitTransferLoading)
            {
                startInitTransferLoading(mIsFromContract)
            }
            else
            {
                onInitTransferLoadComplete(null)
                if (mFinalizeTransferLoading)
                {
                    startFinalizeTransferLoading(mIsFromContract)
                }
                else
                {
                    onFinalizeTransferLoadComplete(null)
                }
            }
        }
        else
        {
            arguments?.let {
                from_contract_button.isChecked = it.getBoolean(IS_CONTRACT_AVAILABLE_KEY)
                from_tz1_button.isChecked = !from_contract_button.isChecked && seed.mnemonics.isNotEmpty()
            }
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
            startFinalizeTransferLoading(mIsFromContract)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_sent_cents, container, false)
    }

    private fun startInitTransferLoading(fromContract:Boolean)
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        //TODO need to handle transfers from KT1 or from tz1

        putFeesToNegative()
        putPayButtonToNull()

        validateSendCentsButton(false)

        startPostRequestLoadInitTransfer(fromContract)
    }


    private fun startFinalizeTransferLoading(isFromContract: Boolean)
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        startPostRequestLoadFinalizeTransfer(isFromContract)
    }

    // REQUESTS

    // volley
    private fun startPostRequestLoadFinalizeTransfer(mIsFromContract: Boolean)
    {
        val url = getString(R.string.transfer_injection_operation)

        val seed = Storage(activity!!).getMnemonics()

        //TODO we got to verify at this very moment.
        if (isTransferFeeValid() && mTransferPayload != null)
        {
            var postParams = JSONObject()
            var dstObjects = JSONArray()
            var dstObject = JSONObject()

            if (mIsFromContract)
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

                dstObject.put("edsig", mSig)

                //0.1 tez == 100 000 mutez
                dstObject.put("transfer_amount", "100000".toLong())
                dstObject.put("fee", mTransferFees)
            }
            else
            {
                val mnemonicsData = Storage(activity!!).getMnemonics()

                postParams.put("src", mnemonicsData.pkh)
                postParams.put("src_pk", mnemonicsData.pk)

                dstObject.put("dst", retrieveTz3())

                dstObject.put("amount", "100000".toLong())

                dstObject.put("fee", mTransferFees)

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
                if (mIsFromContract)
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

    private fun startPostRequestLoadInitTransfer(fromContract: Boolean)
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        var postParams = JSONObject()
        if (fromContract)
        {
            val ecKeys = retrieveECKeys()
            val p2pk = CryptoUtils.generateP2Pk(ecKeys)
            postParams.put("src_pk", p2pk)
            val tz3 = CryptoUtils.generatePkhTz3(ecKeys)
            postParams.put("src", tz3)

            val kt1 = arguments!!.getString(CONTRACT_PUBLIC_KEY)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()

            dstObject.put("dst", kt1)
            dstObject.put("amount", "0")

            dstObject.put("entrypoint", "transfer")

            val dataVisitable = Primitive(
                    Primitive.Name.Pair,
                    arrayOf(
                            Visitable.sequenceOf(
                                    Primitive(
                                            Primitive.Name.Pair,
                                            arrayOf(
                                                    Visitable.integer(100000),
                                                    Visitable.address(tz3)
                                            )
                                    )
                            ),
                            Visitable.keyHash(tz3)
                    )
            )


            val o = ByteArrayOutputStream()
            o.write(0x05)

            val dataPacker = Packer(o)
            dataVisitable.accept(dataPacker)

            val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

            val addressAndChainVisitable = Primitive(Primitive.Name.Pair,
                    arrayOf(
                            Visitable.address(kt1),
                            Visitable.chainID(getString(R.string.chain_ID))
                    )
            )

            val output = ByteArrayOutputStream()
            output.write(0x05)

            val p = Packer(output)
            addressAndChainVisitable.accept(p)

            val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()



            var saltVisitable: Visitable? = null
            val salt = getSalt()
            if (salt != null)
            {
                saltVisitable = Visitable.integer(salt.toLong())
            }

            val outputStream = ByteArrayOutputStream()
            outputStream.write(0x05)

            val packer = Packer(outputStream)
            saltVisitable!!.accept(packer)

            val saltPack = (packer.output as ByteArrayOutputStream).toByteArray()


            val signedData = KeyPair.b2b("0x".hexToByteArray()+dataPack + addressAndChainPack + saltPack)

            val signature = EncryptionServices().sign(signedData)
            val compressedSignature = compressFormat(signature)

            val p2sig = CryptoUtils.generateP2Sig(compressedSignature)


            //val signature = KeyPair.sign(sk, dataPack + addressAndChainPack + saltPack)

            //val p2sig = CryptoUtils.generateEDSig(signature)


            val spendingLimitFile = "spending_limit_transfer.json"
            val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                    .use {
                        it.readText()
                    }

            val value = JSONObject(contract)

            val argsSend = (((((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONArray)[0] as JSONObject)["args"] as JSONArray

            val argsSendAmount = argsSend[0] as JSONObject
            argsSendAmount.put("int", "100000")

            val argsSendContract = argsSend[1] as JSONObject
            argsSendContract.put("string", tz3)

            val argsSendTz = (((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[1] as JSONObject
            argsSendTz.put("string", tz3)

            val argsSig = ((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray

            val argsSigPk = argsSig[0] as JSONObject
            argsSigPk.put("string", p2pk)

            val argsSigSig = argsSig[1] as JSONObject
            argsSigSig.put("string", p2sig)

            mSig = p2sig

            dstObject.put("parameters", value)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)
        }
        else
        {
            val pk = if (mnemonicsData.pk.isNullOrEmpty())
            {
                val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
            }
            else
            {
                mnemonicsData.pk
            }

            postParams.put("src", mnemonicsData.pkh)
            postParams.put("src_pk", pk)

            var dstObject = JSONObject()
            dstObject.put("dst", retrieveTz3())

            //0.1 tez == 100 000 mutez
            dstObject.put("amount", "100000")

            var dstObjects = JSONArray()
            dstObjects.put(dstObject)
            postParams.put("dsts", dstObjects)
        }


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

        jsObjRequest.tag = TRANSFER_INIT_TAG
        mInitTransferLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
    }

    private fun updateMnemonicsData(data: Storage.MnemonicsData, pk:String):String
    {
        with(Storage(activity!!)) {
            saveSeed(Storage.MnemonicsData(data.pkh, pk, data.mnemonics))
        }
        return pk
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
                startInitTransferLoading(mIsFromContract)
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
        //val bundleTheme = arguments!!.getBundle(CustomTheme.TAG)
        //val theme = CustomTheme.fromBundle(bundleTheme)

        val theme = CustomTheme(R.color.colorPrimaryDark, R.color.colorPrimaryVeryDark, R.color.colorTitleText)

        if (validate)
        {
            send_cents_button.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            send_cents_button_layout.isEnabled = true
            send_cents_button_layout.background = makeSelector(theme)

            val drawables = send_cents_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
        }
        else
        {
            send_cents_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            send_cents_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            send_cents_button_layout.background = makeSelector(greyTheme)

            val drawables = send_cents_button.compoundDrawables
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
                val saltRight = (masterKeySaltJSONObject["args"] as JSONArray)[1] as JSONObject

                return DataExtractor.getStringFromField(saltRight, "int").toInt()
            }
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

        outState.putString(STORAGE_DATA_KEY, mStorage)
        outState.putBoolean(IS_FROM_CONTRACT_KEY, mIsFromContract)

        outState.putString(CONTRACT_SIG_KEY, mSig)
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
            startFinalizeTransferLoading(mIsFromContract)
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
