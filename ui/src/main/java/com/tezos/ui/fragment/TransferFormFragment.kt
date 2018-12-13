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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.Account
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.activity.AddressBookActivity
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.VolleySingleton
import com.tezos.ui.utils.hexToByteArray
import com.tezos.ui.utils.toNoPrefixHexString
import kotlinx.android.synthetic.main.fragment_payment_form.*
import kotlinx.android.synthetic.main.payment_form_card_info.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList

/**
 * Created by nfillion on 20/04/16.
 */
class TransferFormFragment : Fragment()
{
    private val TRANSFER_INIT_TAG = "transfer_init"
    private val TRANSFER_FINALIZE_TAG = "transfer_finalize"

    private val TRANSFER_PAYLOAD_KEY = "transfer_payload_key"

    private val TRANSFER_AMOUNT_KEY = "transfer_amount_key"
    private val TRANSFER_FEE_KEY = "transfer_fee_key"

    private var mSrcAccount:Address? = null
    private var mDstAccount:Address? = null

    private var listener: OnTransferListener? = null

    private var mInitTransferLoading:Boolean = false
    private var mFinalizeTransferLoading:Boolean = false

    //private var mTransferId:Int? = null
    private var mTransferPayload:String? = null

    private var mAmountCache:Double = -1.0

    companion object
    {
        @JvmStatic
        fun newInstance(seedDataBundle:Bundle, address:Bundle?, customTheme:Bundle) =
                TransferFormFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, customTheme)
                        putBundle(Storage.TAG, seedDataBundle)

                        if (address != null)
                        {
                            putBundle(Address.TAG, address)
                        }
                    }
                }

        private const val SRC_ACCOUNT_KEY = "src_account_key"
        private const val DST_ACCOUNT_KEY = "dst_account_key"
    }

    interface OnTransferListener
    {
        fun onTransferSucceed()
        fun onTransferLoading(loading: Boolean)
        fun onTransferFailed(error: VolleyError?)

        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean

        fun saveFingerprintAllowed(useInFuture: Boolean)
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
        initContentViews(view, savedInstanceState)

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
                switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)
            }

            //mTransferId = savedInstanceState.getInt(TRANSFER_ID_KEY, -1)
            mTransferPayload = savedInstanceState.getString(TRANSFER_PAYLOAD_KEY, null)

            mInitTransferLoading = savedInstanceState.getBoolean(TRANSFER_INIT_TAG)
            mFinalizeTransferLoading = savedInstanceState.getBoolean(TRANSFER_FINALIZE_TAG)

            mAmountCache = savedInstanceState.getDouble(TRANSFER_AMOUNT_KEY, -1.0)

            transferLoading(isLoading())

            //TODO view created
            //TODO load again but only if we don't have any same forged data.
            validatePayButton(isInputDataValid())

            //TODO we got to keep in mind there's an id already.
            if (mInitTransferLoading)
            {
                startInitTransferLoading()
            }
            else
            {
                onInitTransferLoadComplete(null)

                if (mFinalizeTransferLoading)
                {
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
            cancelRequests(true)

            listener?.onTransferFailed(error)
        }
        else
        {
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

            listener?.onTransferFailed(error)
        }
        else
        {
            // the finish call is made already
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
        val url = getString(R.string.transfer_forge)

        //TODO lock the UI and put this stuff in savedInstance, in case we turn the screen

        var amount = mAmountCache

        //TODO handle the new fees
        var fee = 0.0

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

        var dstObjects = JSONArray()

        var dstObject = JSONObject()
        dstObject.put("dst", pkhDst)
        dstObject.put("amount", amount.toLong().toString())
        dstObject.put("fee", fee.toLong().toString())

        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Request.Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request

            mTransferPayload = answer.getString("result")

            if (mTransferPayload != null && isPayloadValid(mTransferPayload!!, postParams))
            {
                onInitTransferLoadComplete(null)

                startFinalizeTransferLoading()
            }
            else
            {
                val volleyError = VolleyError(getString(R.string.generic_error))
                onInitTransferLoadComplete(volleyError)
            }

        }, Response.ErrorListener
        {
            onInitTransferLoadComplete(it)

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

        jsObjRequest.tag = TRANSFER_INIT_TAG
        mInitTransferLoading = true
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
    }

    private fun isPayloadValid(payload:String, params:JSONObject):Boolean
    {
        var isValid = false
        if (payload != null && params != null)
        {
            val data = payload.hexToByteArray()

            /*
            var postParams = JSONObject()
            postParams.put("src", pkhSrc)
            postParams.put("src_pk", pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            dstObject.put("dst", pkhDst)
            dstObject.put("amount", amount.toLong().toString())
            dstObject.put("fee", fee.toLong().toString())

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)
            */

            val obj = params["dsts"] as JSONArray
            val dstObj = obj[0] as JSONObject

            var i: Int
            i = when {
                data.size > 100 -> 94
                else -> 32
            }

            val isHeightValid = data[i].compareTo(8) == 0
            if (!isHeightValid)
            {
                return false
            }


            val src = data.slice((i+3)..(i+3+19)).toByteArray()

            val pkh = params["src"]



            val isSrcValid = pkh == CryptoUtils.genericHashToPkh(src)

            if (!isSrcValid)
            {
                return false
            }

            val size = data.size
            val fee = data.slice((i+3+19+1) until size).toByteArray()

            val feeList = ArrayList<Int>()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(fee[i])

                feeList.add(bytePos)
                i++

            } while (bytePos > 128)

            val dstFees = dstObj["fee"] as String

            val isFeesValid = addBytesLittleEndian(feeList) == dstFees.toLong()

            if (!isFeesValid)
            {
                return false
            }


            val counter = fee.slice(i until fee.size).toByteArray()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(counter[i])
                i++

            } while (bytePos >= 128)

            val gasLimit = counter.slice(i until counter.size).toByteArray()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(gasLimit[i])
                i++

            } while (bytePos >= 128)


            val storageLimit = gasLimit.slice(i until gasLimit.size).toByteArray()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(storageLimit[i])
                i++

            } while (bytePos >= 128)


            val amount = storageLimit.slice(i until storageLimit.size).toByteArray()

            val amountList = ArrayList<Int>()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(amount[i])

                amountList.add(bytePos)
                i++

            } while (bytePos >= 128)

            val dstAmount = dstObj["amount"] as String

            val isAmountValid = addBytesLittleEndian(amountList) == dstAmount.toLong()
            if (!isAmountValid)
            {
                return false
            }


            val dst = amount.slice(i+2 until amount.size).toByteArray()
            //TODO handle the first two bytes

            val dstPkh = dstObj["dst"]
            val isDstValid = dstPkh == CryptoUtils.genericHashToPkh(dst)

            isValid = isHeightValid && isSrcValid && isFeesValid && isAmountValid && isDstValid

        }
        return isValid
    }

    private fun addBytesLittleEndian(bytes:ArrayList<Int>):Long
    {
        val reversed = bytes.reversed()

        var accum = 0L

        for (i in reversed.indices)
        {
            val bytePos = reversed[i]

            if (bytePos < 128L)
            {
                accum += bytePos
                if (i != reversed.size - 1)
                {
                    accum *= 128
                }
            }
            else
            {
                accum += bytePos - 128
                if (i != reversed.size - 1)
                {
                    accum *= 128
                }
            }
        }

        return accum
    }

    // volley
    private fun startPostRequestLoadFinalizeTransfer(mnemonicsData: Storage.MnemonicsData)
    {
        val url = getString(R.string.transfer_injection_operation)

        if (/*mTransferId != null && mTransferId != -1 && */mTransferPayload != null)
        {
            val zeroThree = "0x03".hexToByteArray()

            val byteArrayThree = mTransferPayload!!.hexToByteArray()

            val xLen = zeroThree.size
            val yLen = byteArrayThree.size
            val result = ByteArray(xLen + yLen)

            System.arraycopy(zeroThree, 0, result, 0, xLen)
            System.arraycopy(byteArrayThree, 0, result, xLen, yLen)

            val mnemonics = EncryptionServices(activity!!).decrypt(mnemonicsData.mnemonics, "not useful for marshmallow")
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

                        onFinalizeTransferLoadComplete(null)
                        listener?.onTransferSucceed()
                    },
                    Response.ErrorListener
                    {
                        onFinalizeTransferLoadComplete(it)
                        listener?.onTransferFailed(it)
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

//something wrong happened. we got the finalizeTransfer boolean but not the payload + id
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_payment_form, container, false)
    }

    private fun initContentViews(view: View, savedInstanceState: Bundle?)
    {
        val args = arguments
        val themeBundle = args!!.getBundle(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        val focusChangeListener = this.focusChangeListener()

        amount_transfer.addTextChangedListener(GenericTextWatcher(amount_transfer))
        amount_transfer.onFocusChangeListener = focusChangeListener

        transfer_src_button.setOnClickListener { _ ->
            AddressBookActivity.start(activity,
                    theme,
                    AddressBookActivity.Selection.SelectionAccounts)
        }

        transfer_dst_button.setOnClickListener { _ ->
            AddressBookActivity.start(
                    activity,
                    theme,
                    AddressBookActivity.Selection.SelectionAccountsAndAddresses)
        }

        pay_button_layout.visibility = View.VISIBLE

        val moneyString = getString(R.string.pay, "ꜩ")

        pay_button.text = moneyString

        pay_button_layout.setOnClickListener { _ ->
            onPayClick()
        }

        putEverythingInRed()

        arguments?.let {

            val seedDataBundle = it.getBundle(Storage.TAG)
            val seedData = Storage.fromBundle(seedDataBundle)

            var account = Address()
            account.pubKeyHash = seedData.pkh
            switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccounts, account)

            val address = it.getBundle(Address.TAG)
            if (address != null)
            {
                mDstAccount = Address.fromBundle(address)
                switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)
            }
        }

        //TODO fragment recreated
        //TODO load again but only if we don't have any same forged data.
        validatePayButton(isInputDataValid())
    }

    private fun isLoading():Boolean
    {
        return mInitTransferLoading || mFinalizeTransferLoading
    }

    private fun transferLoading(loading:Boolean)
    {
        if (loading)
        {
            pay_button_layout.visibility = View.GONE
            empty.visibility = View.VISIBLE
            amount_transfer.isEnabled = false
        }
        else
        {
            pay_button_layout.visibility = View.VISIBLE
            empty.visibility = View.INVISIBLE
            amount_transfer.isEnabled = true
        }

        listener?.onTransferLoading(loading)
    }

    private fun onPayClick()
    {
        val dialog = AuthenticationDialog()
        if (listener?.isFingerprintAllowed()!! && listener?.hasEnrolledFingerprints()!!)
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

        if (requestCode == AddressBookActivity.TRANSFER_SELECT_REQUEST_CODE)
        {
            if (data != null && data.hasExtra(Account.TAG))
            {
                val accountBundle = data.getBundleExtra(Account.TAG)
                val account = Account.fromBundle(accountBundle)

                if (resultCode == R.id.transfer_src_selection_succeed)
                {
                    mSrcAccount = account
                    switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccounts, mSrcAccount!!)
                }
                else if (resultCode == R.id.transfer_dst_selection_succeed)
                {
                    mDstAccount = account
                    switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)

                    //TODO destination loaded
                    //TODO load again but only if we don't have any same forged data.
                    validatePayButton(isInputDataValid())
                }
            }
        }
    }

    private fun switchButtonAndLayout(selection: AddressBookActivity.Selection, address: Address)
    {
        when (selection)
        {
            AddressBookActivity.Selection.SelectionAccounts ->
            {
                transfer_src_button.visibility = View.GONE
                transfer_source_filled.visibility = View.VISIBLE

                src_payment_account_pub_key_hash.text = address.pubKeyHash
            }

            AddressBookActivity.Selection.SelectionAccountsAndAddresses ->
            {
                transfer_dst_button.visibility = View.GONE
                transfer_destination_filled.visibility = View.VISIBLE
                dst_payment_account_pub_key_hash.text = address.pubKeyHash
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

        if (amount_transfer != null && !TextUtils.isEmpty(amount_transfer.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val amount = amount_transfer.text!!.toString().toDouble()

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

            pay_button.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            pay_button_layout.isEnabled = true
            pay_button_layout.background = makeSelector(theme)

            val drawables = pay_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
        }
        else
        {
            pay_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            pay_button_layout.isEnabled = false
            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            pay_button_layout.background = makeSelector(greyTheme)

            val drawables = pay_button.compoundDrawables
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

            //TODO text changed
            //TODO load again but only if we don't have any same forged data.
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
            pay_button.text = getString(R.string.pay, "")
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
                pay_button.text = getString(R.string.pay, "")
            }
        }

        amount_transfer.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun setTextPayButton(amount: String)
    {
        var amount = amount
//var amountDouble: Double = java.lang.Double.parseDouble(amount)
        var amountDouble: Double = amount.toDouble()

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
        pay_button.text = getString(R.string.pay, moneyFormatted2)
    }

    /**
     * Fingerprint was invalidated, decide what to do in this case.
     */
    private fun onFingerprintInvalidation(useInFuture: Boolean)
    {
        listener?.saveFingerprintAllowed(useInFuture)
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

    private fun cancelRequests(resetBooleans:Boolean)
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
        requestQueue?.cancelAll(TRANSFER_INIT_TAG)
        requestQueue?.cancelAll(TRANSFER_FINALIZE_TAG)

        if (resetBooleans)
        {
            mInitTransferLoading = false
            mFinalizeTransferLoading = false
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests(false)
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putParcelable(SRC_ACCOUNT_KEY, mSrcAccount?.toBundle())
        outState.putParcelable(DST_ACCOUNT_KEY, mDstAccount?.toBundle())

        outState.putBoolean(TRANSFER_INIT_TAG, mInitTransferLoading)
        outState.putBoolean(TRANSFER_FINALIZE_TAG, mFinalizeTransferLoading)

        outState.putString(TRANSFER_PAYLOAD_KEY, mTransferPayload)

        outState.putDouble(TRANSFER_AMOUNT_KEY, mAmountCache)
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
