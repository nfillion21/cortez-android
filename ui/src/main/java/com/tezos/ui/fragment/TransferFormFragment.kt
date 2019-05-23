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
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.Account
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.R
import com.tezos.ui.activity.AddressBookActivity
import com.tezos.ui.activity.TransferFormActivity
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.fragment_payment_form.*
import kotlinx.android.synthetic.main.payment_form_card_info.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.interfaces.ECPublicKey
import kotlin.collections.HashMap
import kotlin.collections.set

/**
 * Created by nfillion on 20/04/16.
 */
class TransferFormFragment : Fragment()
{
    private var mSrcAccount:String? = null
    private var mDstAccount:String? = null

    private var listener: OnTransferListener? = null

    private var mInitTransferLoading:Boolean = false
    private var mFinalizeTransferLoading:Boolean = false

    private var mTransferPayload:String? = null
    private var mTransferFees:Long = -1

    private var mTransferAmount:Double = -1.0

    private var mClickCalculate:Boolean = false

    private var mSourceStorageInfoLoading:Boolean = false

    private var mRecipientStorageInfoLoading:Boolean = false

    private var mStorage:String? = null

    private var mSourceKT1withCode:Boolean = false

    private var mRecipientKT1withCode:Boolean = false

    companion object
    {
        @JvmStatic
        fun newInstance(srcAddress:String, address:Bundle?, customTheme:Bundle) =
                TransferFormFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, customTheme)
                        putString(Address.TAG, srcAddress)

                        if (address != null)
                        {
                            putBundle(TransferFormActivity.DST_ADDRESS_KEY, address)
                        }
                    }
                }

        private const val SRC_ACCOUNT_KEY = "src_account_key"
        private const val DST_ACCOUNT_KEY = "dst_account_key"

        private const val TRANSFER_INIT_TAG = "transfer_init"
        private const val TRANSFER_FINALIZE_TAG = "transfer_finalize"

        private const val TRANSFER_PAYLOAD_KEY = "transfer_payload_key"

        private const val TRANSFER_AMOUNT_KEY = "transfer_amount_key"
        private const val TRANSFER_FEE_KEY = "transfer_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private const val CONTRACT_SCRIPT_SOURCE_INFO_TAG = "contract_script_source_info"

        private const val CONTRACT_SCRIPT_RECIPIENT_INFO_TAG = "contract_script_recipient_info"

        private const val STORAGE_DATA_KEY = "storage_data_key"

        private const val TZ_OR_KT1_SOURCE_KEY = "tz_or_kt1_source_key"

        private const val TZ_OR_KT1_RECIPIENT_KEY = "tz_or_kt1_recipient_key"
    }

    interface OnTransferListener
    {
        fun onTransferSucceed()
        fun onTransferLoading(loading: Boolean)
        fun onTransferFailed(error: VolleyError?)

        fun noMnemonicsAvailable()

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
            throw RuntimeException("$context must implement OnTransferListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        initContentViews()

        if (savedInstanceState != null)
        {
            mSrcAccount = savedInstanceState.getString(SRC_ACCOUNT_KEY)
            if (mSrcAccount != null)
            {
                switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccounts, mSrcAccount!!)
                startGetRequestLoadContractInfo(false)
            }

            mDstAccount = savedInstanceState.getString(DST_ACCOUNT_KEY)
            if (mDstAccount != null)
            {
                switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)
            }

            mTransferPayload = savedInstanceState.getString(TRANSFER_PAYLOAD_KEY, null)

            mInitTransferLoading = savedInstanceState.getBoolean(TRANSFER_INIT_TAG)
            mFinalizeTransferLoading = savedInstanceState.getBoolean(TRANSFER_FINALIZE_TAG)

            mTransferAmount = savedInstanceState.getDouble(TRANSFER_AMOUNT_KEY, -1.0)

            mTransferFees = savedInstanceState.getLong(TRANSFER_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mSourceStorageInfoLoading = savedInstanceState.getBoolean(CONTRACT_SCRIPT_SOURCE_INFO_TAG)

            mRecipientStorageInfoLoading = savedInstanceState.getBoolean(CONTRACT_SCRIPT_RECIPIENT_INFO_TAG)

            mStorage = savedInstanceState.getString(STORAGE_DATA_KEY, null)

            mSourceKT1withCode = savedInstanceState.getBoolean(TZ_OR_KT1_SOURCE_KEY, false)

            mRecipientKT1withCode = savedInstanceState.getBoolean(TZ_OR_KT1_RECIPIENT_KEY, false)

            transferLoading(isLoading())


            //TODO somewhere around here, I need to load the storage.
            //TODO as long as the storage is loading, there is no possible action.

        }
        else
        {
            startStorageInfoLoading(false)
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (isInputDataValid() && isTransferFeeValid())
        {
            validatePayButton(true)
            this.setTextPayButton()
        }

        //TODO priority with mInitStorageLoading, but only if it's a KT1.

        if (mSourceStorageInfoLoading)
        {
            startStorageInfoLoading(false)
        }
        else
        {
            onStorageInfoComplete()

            if (mRecipientStorageInfoLoading)
            {
                startStorageInfoLoading(true)
            }
            else
            {
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
    }

    private fun onInitTransferLoadComplete(error:VolleyError?)
    {
        mInitTransferLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            //TODO should cancel the payloadTransfer too
            mTransferPayload = null

            //TODO we should give access to the fees button

            fee_edittext.isEnabled = true
            fee_edittext.isFocusable = false
            fee_edittext.isClickable = false
            fee_edittext.isLongClickable = false
            fee_edittext.hint = getString(R.string.click_for_fees)

            fee_edittext.setOnClickListener {
                startInitTransferLoading()
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

    private fun startStorageInfoLoading(isRecipient: Boolean)
    {
        transferLoading(true)

        // validatePay cannot be valid if there is no fees
        //validateConfirmEditionButton(false)

        //swipe_refresh_script_layout?.isEnabled = false

        startGetRequestLoadContractInfo(isRecipient)
    }

    // volley
    private fun startGetRequestLoadContractInfo(isRecipient:Boolean)
    {
        cancelRequests(true)

        if (isRecipient)
        {
            mRecipientStorageInfoLoading = true
        }
        else
        {
            mSourceStorageInfoLoading = true
        }

        /*
        loading_textview.setText(R.string.loading_contract_info)

        nav_progress.visibility = View.VISIBLE
        */

        var pkh :String? = if (isRecipient)
        {
            mDstAccount
        }
        else
        {
            arguments!!.getString(Address.TAG)
        }

        if (pkh != null)
        {
            val url = String.format(getString(R.string.contract_storage_url), pkh)

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<JSONObject>
            {

                //prevents from async crashes
                if (R.id.content != null)
                {
                    addContractInfoFromJSON(it)
                    onStorageInfoComplete()

                    //TODO check if our tz3 is the same as the contract tz3

                    val salt = getSalt()
                    if (salt != null && salt >= 0)
                    {
                        if (isRecipient)
                        {
                            mRecipientKT1withCode = true
                        }
                        else
                        {
                            mSourceKT1withCode = true
                        }

                        // with this information, handle the code to sign data

                        val tz3 = getTz3()
                        val contractTz3 = getContractTz3()

                        if (tz3 == null || tz3 != contractTz3)
                        {
                            //TODO ERROR, there is no way you can spend money without the right tz3
                            //TODO please update your contract storage in contract tab.
                        }
                        else
                        {
                            //TODO it's ok, this phone has the keys.
                        }
                    }
                }
            },
                    Response.ErrorListener {

                        if (R.id.content != null)
                        {
                            onStorageInfoComplete()

                            val response = it.networkResponse?.statusCode
                            if (response != 404)
                            {
                                // 404 happens when there is no storage in this KT1

                                listener?.onTransferFailed(it)

                                //TODO user needs to retry storage call
                            }
                            else
                            {
                                //it means this KT1 had no code
                                //false by default anyway

                                if (isRecipient)
                                {
                                    mRecipientKT1withCode = false
                                }
                                else
                                {
                                    mSourceKT1withCode = false

                                    val hasMnemonics = Storage(activity!!).hasMnemonics()
                                    if (hasMnemonics)
                                    {
                                        val seed = Storage(activity!!).getMnemonics()

                                        if (seed.mnemonics.isEmpty())
                                        {
                                            listener?.noMnemonicsAvailable()
                                        }
                                        else
                                        {
                                            //TODO we can display the screen
                                        }
                                    }
                                }

                                //TODO we can display the elements, or say we don't have the mnemonics
                            }
                        }
                    })

            jsonArrayRequest.tag = if (isRecipient) CONTRACT_SCRIPT_RECIPIENT_INFO_TAG else CONTRACT_SCRIPT_SOURCE_INFO_TAG

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

    private fun startInitTransferLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()
        putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validatePayButton(false)

        startPostRequestLoadInitTransfer()
    }

    private fun startFinalizeTransferLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        startPostRequestLoadFinalizeTransfer()
    }

    // volley
    private fun startPostRequestLoadInitTransfer()
    {
        val url = getString(R.string.transfer_forge)
        var postParams = JSONObject()

        if (mSourceKT1withCode)
        {
            val ecKeys = retrieveECKeys()
            val p2pk = CryptoUtils.generateP2Pk(ecKeys)
            postParams.put("src_pk", p2pk)
            val tz3 = CryptoUtils.generatePkhTz3(ecKeys)
            postParams.put("src", tz3)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()

            dstObject.put("dst", arguments!!.getString(Address.TAG))
            dstObject.put("amount", "0")

            //TODO sign data
            //val signedData = "signedData"
            val signedData0 = "050707020000002107070080897a0a0000001600001c92e58081a9d236c82e3e9d382c64e5642467c00a0000001502c1d46fad69ff8d01b2186aa11d2eb54c04f517f7".hexToByteArray()
            // sending 1 tz to tzNF

            //TODO we got the salt now

            val salt = getSalt()

            val signedData1 = "050001".hexToByteArray()
            val signedData = KeyPair.b2b(signedData0 + signedData1)

            val signature = EncryptionServices().sign(signedData)
            val compressedSignature = compressFormat(signature)

            val p2sig = CryptoUtils.generateP2Sig(compressedSignature)

            val resScript = JSONObject(getString(R.string.spending_limit_contract_evo_spending))

            //montant(mutez)
            //destinataire (tz/KT)
            //signataire (tz3)
            //edpk (p2pk)
            //edsig (p2sig)

            val spendingLimitContract = String.format(resScript.toString(),
                    (mTransferAmount*1000000).toLong().toString(),
                    mDstAccount,
                    tz3,
                    p2pk,
                    p2sig)

            //TODO we need to put a parameter
            val json = JSONObject(spendingLimitContract)
            dstObject.put("parameters", json)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)
        }
        else
        {
            val seed = Storage(activity!!).getMnemonics()

            val mnemonics = EncryptionServices().decrypt(seed.mnemonics)
            val pk = CryptoUtils.generatePk(mnemonics, "")

            postParams.put("src", mSrcAccount)
            postParams.put("src_pk", pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            dstObject.put("dst", mDstAccount)
            dstObject.put("amount", (mTransferAmount*1000000).toLong().toString())


            //TODO handle that, we need to load storage from recipient
            if (mRecipientKT1withCode)
            {
                dstObject.put("parameters", JSONObject(getString(R.string.transfer_args_none).toString()))
            }

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

        }

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (activity != null)
            {
                mTransferPayload = answer.getString("result")
                mTransferFees = answer.getLong("total_fee")

                // get back the object and

                /*
                val dstsArray = postParams["dsts"] as JSONArray
                val dstObj = dstsArray[0] as JSONObject

                dstObj.put("fee", mTransferFees.toString())
                dstsArray.put(0, dstObj)

                postParams.put("dsts", dstsArray)
                */

                // we use this call to ask for payload and fees
                if (mTransferPayload != null && mTransferFees != null)
                {
                    onInitTransferLoadComplete(null)

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
                    onInitTransferLoadComplete(volleyError)
                    mClickCalculate = true

                    //the call failed
                }

            }
        }, Response.ErrorListener
        {
            if (activity != null)
            {
                onInitTransferLoadComplete(it)

                mClickCalculate = true
                //Log.i("mTransferId", ""+mTransferId)
                Log.i("mTransferPayload", ""+mTransferPayload)
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
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
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

    // volley
    private fun startPostRequestLoadFinalizeTransfer()
    {
        val url = getString(R.string.transfer_injection_operation)

        val seed = Storage(activity!!).getMnemonics()

        //TODO we got to verify at this very moment.
        if (isPayButtonValid() && mTransferPayload != null)
        {
            //val pkhSrc = seed.pkh
            val pkhDst = mDstAccount

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

                val pLen = byteArrayThree.size
                val sLen = compressedSignature.size
                val newResult = ByteArray(pLen + sLen)

                System.arraycopy(byteArrayThree, 0, newResult, 0, pLen)
                System.arraycopy(compressedSignature, 0, newResult, pLen, sLen)

                var payloadsign = newResult.toNoPrefixHexString()

                val stringRequest = object : StringRequest(Request.Method.POST, url,
                        Response.Listener<String> { response ->

                            if (activity != null)
                            {
                                onFinalizeTransferLoadComplete(null)
                                listener?.onTransferSucceed()
                            }
                        },
                        Response.ErrorListener
                        {
                            if (activity != null)
                            {
                                onFinalizeTransferLoadComplete(it)
                                listener?.onTransferFailed(it)
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

    /*
    private fun verifySig(data:ByteArray, signature:ByteArray):Boolean
    {
        /*
  * Verify a signature previously made by a PrivateKey in our
  * KeyStore. This uses the X.509 certificate attached to our
  * private key in the KeyStore to validate a previously
  * generated signature.
  */
        val ks = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val entry = ks.getEntry("key1", null) as? KeyStore.PrivateKeyEntry
        if (entry != null)
        {
            return  Signature.getInstance("SHA256withECDSA").run {
                initVerify(entry.certificate)
                update(data)
                verify(signature)
            }
        }

        return false
    }
    */

    private fun onStorageInfoComplete()
    {
        mSourceStorageInfoLoading = false
        empty?.visibility = View.GONE

        listener?.onTransferLoading(false)

        //refreshTextUnderDelegation()
    }

    private fun refreshTextUnderDelegation()
    {
        //TODO for paiements, there is nothing to handle right now, except building a pull to refresh screen

        //this method handles the data and loading texts

        if (mStorage != null)
        {
            //if (mContract!!.delegate != null)
            if (mStorage != JSONObject(getString(R.string.default_storage)).toString())
            {
                //TODO at this point, just show that there is no script.

                val storageJSONObject = JSONObject(mStorage)

                val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

                // get securekey hash

                val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args") as JSONArray
                val secureKeyJSONObject = argsSecureKey[0] as JSONObject
                val secureKeyJSONArray = DataExtractor.getJSONArrayFromField(secureKeyJSONObject, "args")

                val secureKeyHashField = DataExtractor.getJSONObjectFromField(secureKeyJSONArray, 1)
                val secureKeyHash = DataExtractor.getStringFromField(secureKeyHashField, "string")

                val saltSpendingField = DataExtractor.getJSONObjectFromField(secureKeyJSONArray, 0)
                val saltSpending = DataExtractor.getStringFromField(saltSpendingField, "int")
                val saltSpending2 = DataExtractor.getStringFromField(secureKeyHashField, "int")

            }
            else
            {
                /*
                update_storage_form_card?.visibility = View.GONE

                public_address_layout?.visibility = View.VISIBLE

                update_storage_button_layout?.visibility = View.GONE

                storage_info_textview?.visibility = View.VISIBLE
                storage_info_textview?.text = getString(R.string.no_script_info)
                */

                //TODO show everything related to the removing
            }

            //loading_textview?.visibility = View.GONE
            //loading_textview?.text = null
        }
        else
        {
            // mContract is null then just show "-"
            //loading_textview will be hidden behind other textview

            //loading_textview?.visibility = View.VISIBLE
            //loading_textview?.text = "-"
        }
    }

    private fun getContractTz3():String?
    {
        //TODO check if the storage follows our pattern

        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

            val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args") as JSONArray
            val secureKeyJSONObject = argsSecureKey[0] as JSONObject
            val secureKeyJSONArray = DataExtractor.getJSONArrayFromField(secureKeyJSONObject, "args")

            val secureKeyHashField = DataExtractor.getJSONObjectFromField(secureKeyJSONArray, 1)
            return DataExtractor.getStringFromField(secureKeyHashField, "string")
        }

        return null
    }

    private fun getSalt():Int?
    {
        //TODO check if the storage follows our pattern

        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

            val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args") as JSONArray
            val secureKeyJSONObject = argsSecureKey[0] as JSONObject
            val secureKeyJSONArray = DataExtractor.getJSONArrayFromField(secureKeyJSONObject, "args")

            val saltSpendingField = DataExtractor.getJSONObjectFromField(secureKeyJSONArray, 0)
            return DataExtractor.getStringFromField(saltSpendingField, "int").toInt()
        }

        return null
    }

    private fun getTz3():String?
    {
        val keypair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        if (keypair != null)
        {
            val ecKey = keypair.public as ECPublicKey
            val result = ecKeyFormat(ecKey)

            return CryptoUtils.generatePkhTz3(result)
        }

        return null
    }

    /*
    private fun signData(data:ByteArray):ByteArray
    {
        //TODO generic hash 32 bytes

        val bytes = KeyPair.b2b(data)

        /*
        * Use a PrivateKey in the KeyStore to create a signature over
        * some data.
        */

        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val entry: KeyStore.Entry = ks.getEntry("key1", null)
        if (entry is KeyStore.PrivateKeyEntry)
        {
            return Signature.getInstance("NONEwithECDSA").run {
                initSign(entry.privateKey)
                update(bytes)
                sign()
            }
        }
        return ByteArray(0)
    }
    */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_payment_form, container, false)
    }

    private fun initContentViews()
    {
        val args = arguments
        val themeBundle = args!!.getBundle(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        val focusChangeListener = this.focusChangeListener()

        amount_edittext.addTextChangedListener(GenericTextWatcher(amount_edittext))
        amount_edittext.onFocusChangeListener = focusChangeListener

        transfer_src_button.setOnClickListener {
            AddressBookActivity.start(activity,
                    theme,
                    AddressBookActivity.Selection.SelectionAccounts)
        }

        transfer_dst_button.setOnClickListener {
            AddressBookActivity.start(
                    activity,
                    theme,
                    AddressBookActivity.Selection.SelectionAccountsAndAddresses)
        }

        pay_button_layout.visibility = View.VISIBLE

        val moneyString = getString(R.string.pay, "ꜩ")

        pay_button.text = moneyString

        pay_button_layout.setOnClickListener {
            onPayClick()
        }

        putEverythingInRed()

        arguments?.let {

            val srcAddress = it.getString(Address.TAG)
            if (srcAddress != null)
            {
                mSrcAccount = srcAddress
                switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccounts, srcAddress)
            }

            val dstAddress = it.getBundle(TransferFormActivity.DST_ADDRESS_KEY)
            if (dstAddress != null)
            {
                val dst = Address.fromBundle(dstAddress)
                mDstAccount = dst.pubKeyHash
                switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)
            }
        }

        //TODO fragment recreated
        //TODO load again but only if we don't have any same forged data.
        validatePayButton(isInputDataValid() && isTransferFeeValid())
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

    private fun onPayClick()
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
            startFinalizeTransferLoading()
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
                val account = Address.fromBundle(accountBundle)

                /*
                if (resultCode == R.id.transfer_src_selection_succeed)
                {
                    mSrcAccount = account.pubKeyHash
                    switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccounts, mSrcAccount!!)
                }
                else
                    */
                if (resultCode == R.id.transfer_dst_selection_succeed)
                {
                    //TODO verify this address is a KT1 and check its storage.

                    mDstAccount = account.pubKeyHash
                    switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)

                    startStorageInfoLoading(true)
                }
            }

            //TODO no loading transfer anymore after we chose the recipient
            /*

            if (isInputDataValid())
            {
                startInitTransferLoading()
            }
            else
            {
                validatePayButton(false)

                cancelRequests(true)
                transferLoading(false)

                putFeesToNegative()
                putPayButtonToNull()
            }

            */
        }
    }

    private fun switchButtonAndLayout(selection: AddressBookActivity.Selection, address: String)
    {
        when (selection)
        {
            AddressBookActivity.Selection.SelectionAccounts ->
            {
                transfer_src_button.visibility = View.GONE
                transfer_source_filled.visibility = View.VISIBLE

                src_payment_account_pub_key_hash.text = address
            }

            AddressBookActivity.Selection.SelectionAccountsAndAddresses ->
            {
                transfer_dst_button.visibility = View.GONE
                transfer_destination_filled.visibility = View.VISIBLE
                dst_payment_account_pub_key_hash.text = address
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

        if (amount_edittext.text != null && !TextUtils.isEmpty(amount_edittext.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val amount = amount_edittext.text!!.toString().toDouble()

                if (amount >= 0.000001f)
                {
                    mTransferAmount = amount
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mTransferAmount = -1.0
                return false
            }
        }

        return isAmountValid
    }

    private fun isTransferAmountEquals(editable: Editable):Boolean
    {
        val isAmountEquals = false

        if (editable != null && !TextUtils.isEmpty(editable))
        {
            try
            {
                val amount = editable.toString().toDouble()
                if (amount != -1.0 && amount == mTransferAmount)
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

    //TODO need a method to verify if the fees

    private fun isTransferFeeValid():Boolean
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
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id

            if (i == R.id.amount_edittext)
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

            if (i == R.id.amount_edittext)
            {
                if (!isTransferAmountEquals(editable))
                {
                    putAmountInRed(false)

                    //TODO text changed
                    //TODO load again but only if we don't have any same forged data.

                    //val amount = java.lang.Double.parseDouble()

                    //TODO check if it's already

                    if (isInputDataValid()) {
                        startInitTransferLoading()
                    } else {
                        validatePayButton(false)

                        cancelRequests(true)
                        transferLoading(false)

                        putFeesToNegative()
                        putPayButtonToNull()
                    }
                }
            }
            else
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }

    fun isInputDataValid(): Boolean
    {
        val isTransferAmountValid = isTransferAmountValid()
        val isDstAccount = mDstAccount != null
        return isTransferAmountValid && isDstAccount
                //return isTransferAmountValid()
                // mDstAccount != null
    }

    fun isPayButtonValid(): Boolean
    {
        return mTransferPayload != null
        && isTransferFeeValid()
        && isInputDataValid()
    }

    private fun putEverythingInRed()
    {
        this.putAmountInRed(true)
    }

    private fun putFeesToNegative()
    {
        fee_edittext.setText("")

        mClickCalculate = false
        fee_edittext.isEnabled = false
        fee_edittext.hint = getString(R.string.neutral)

        mTransferFees = -1

        mTransferPayload = null
    }

    private fun putPayButtonToNull()
    {
        pay_button.text = getString(R.string.pay, "")
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
        }

        amount_edittext.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun setTextPayButton()
    {
        var amountDouble: Double = mTransferAmount

        //amountDouble += fee_edittext.text.toString().toLong()/1000000
        amountDouble += mTransferFees.toDouble()/1000000.0

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
            onPayClick()
        }
    }

    private fun cancelRequests(resetBooleans:Boolean)
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
        requestQueue?.cancelAll(TRANSFER_INIT_TAG)
        requestQueue?.cancelAll(TRANSFER_FINALIZE_TAG)
        requestQueue?.cancelAll(CONTRACT_SCRIPT_SOURCE_INFO_TAG)
        requestQueue?.cancelAll(CONTRACT_SCRIPT_RECIPIENT_INFO_TAG)

        if (resetBooleans)
        {
            mInitTransferLoading = false
            mFinalizeTransferLoading = false
            mSourceStorageInfoLoading = false
            mRecipientStorageInfoLoading = false
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

        outState.putString(SRC_ACCOUNT_KEY, mSrcAccount)
        outState.putString(DST_ACCOUNT_KEY, mDstAccount)

        outState.putBoolean(TRANSFER_INIT_TAG, mInitTransferLoading)
        outState.putBoolean(TRANSFER_FINALIZE_TAG, mFinalizeTransferLoading)

        outState.putString(TRANSFER_PAYLOAD_KEY, mTransferPayload)

        outState.putDouble(TRANSFER_AMOUNT_KEY, mTransferAmount)

        outState.putLong(TRANSFER_FEE_KEY, mTransferFees)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)

        outState.putBoolean(CONTRACT_SCRIPT_SOURCE_INFO_TAG, mSourceStorageInfoLoading)

        outState.putBoolean(CONTRACT_SCRIPT_RECIPIENT_INFO_TAG, mRecipientStorageInfoLoading)

        outState.putString(STORAGE_DATA_KEY, mStorage)

        outState.putBoolean(TZ_OR_KT1_SOURCE_KEY, mSourceKT1withCode)

        outState.putBoolean(TZ_OR_KT1_RECIPIENT_KEY, mRecipientKT1withCode)
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
