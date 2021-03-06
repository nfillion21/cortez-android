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
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
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
import com.tezos.core.utils.*
import com.tezos.ui.R
import com.tezos.ui.activity.AddressBookActivity
import com.tezos.ui.activity.TransferFormActivity
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.fragment_payment_form.*
import kotlinx.android.synthetic.main.payment_form_card_info.*
import kotlinx.android.synthetic.main.payment_form_card_info.no_mnemonics
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.interfaces.ECPublicKey
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.math.roundToLong

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

    private var mClickSourceKT1:Boolean = false
    private var mClickRecipientKT1:Boolean = false

    private var mSourceStorageInfoLoading:Boolean = false

    private var mRecipientStorageInfoLoading:Boolean = false

    private var mStorageSource:String? = null
    private var mStorageRecipient:String? = null

    private var mSourceKT1withCode:Boolean = false

    private var mRecipientKT1withCode:Boolean = false

    private var mSig:String? = null

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

        private const val CLICK_SOURCE_KT1_KEY = "click_source_kt1_key"
        private const val CLICK_RECIPIENT_KT1_KEY = "click_recipient_kt1_key"

        private const val CONTRACT_SCRIPT_SOURCE_INFO_TAG = "contract_script_source_info"

        private const val CONTRACT_SCRIPT_RECIPIENT_INFO_TAG = "contract_script_recipient_info"

        private const val STORAGE_DATA_SOURCE_KEY = "storage_data_source_key"
        private const val STORAGE_DATA_RECIPIENT_KEY = "storage_data_recipient_key"

        private const val TZ_OR_KT1_SOURCE_KEY = "tz_or_kt1_source_key"

        private const val TZ_OR_KT1_RECIPIENT_KEY = "tz_or_kt1_recipient_key"

        private const val CONTRACT_SIG_KEY = "contract_sig_key"
    }

    interface OnTransferListener
    {
        fun onTransferSucceed()
        fun onTransferLoading(loading: Boolean)
        fun onTransferFailed(error: VolleyError?)

        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean

        fun saveFingerprintAllowed(useInFuture: Boolean)

        fun showSnackBar(res:String, color:Int, textColor:Int?)
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
                //startGetRequestLoadContractInfo(false)
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

            mClickSourceKT1 = savedInstanceState.getBoolean(CLICK_SOURCE_KT1_KEY, false)
            mClickRecipientKT1 = savedInstanceState.getBoolean(CLICK_RECIPIENT_KT1_KEY, false)

            mSourceStorageInfoLoading = savedInstanceState.getBoolean(CONTRACT_SCRIPT_SOURCE_INFO_TAG)

            mRecipientStorageInfoLoading = savedInstanceState.getBoolean(CONTRACT_SCRIPT_RECIPIENT_INFO_TAG)

            mStorageSource = savedInstanceState.getString(STORAGE_DATA_SOURCE_KEY, null)
            mStorageRecipient = savedInstanceState.getString(STORAGE_DATA_RECIPIENT_KEY, null)

            mSourceKT1withCode = savedInstanceState.getBoolean(TZ_OR_KT1_SOURCE_KEY, false)

            mRecipientKT1withCode = savedInstanceState.getBoolean(TZ_OR_KT1_RECIPIENT_KEY, false)

            mSig = savedInstanceState.getString(CONTRACT_SIG_KEY, null)

            transferLoading(isLoading())

            //TODO somewhere around here, I need to load the storage.
            //TODO as long as the storage is loading, there is no possible action.

        }
        else
        {
            arguments?.let {

                val srcAddress = it.getString(Address.TAG)
                var dstAddress:String? = null

                val bundle = it.getBundle(TransferFormActivity.DST_ADDRESS_KEY)
                if (bundle != null)
                {
                    val dst = Address.fromBundle(bundle)
                    dstAddress = dst?.pubKeyHash
                }

                if (!srcAddress.isNullOrEmpty() && srcAddress.startsWith("KT1", true))
                {
                    startStorageInfoLoading(false)
                }
                else if (!dstAddress.isNullOrEmpty() && dstAddress.startsWith("kt1", true))
                {
                    startStorageInfoLoading(true)
                }
                else
                {
                    //no need to hide it anymore
                    loading_progress.visibility = View.GONE
                    recipient_area.visibility = View.VISIBLE
                    amount_layout.visibility = View.GONE
                }
            }
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
            onStorageInfoComplete(null, false)

            if (mRecipientStorageInfoLoading)
            {
                startStorageInfoLoading(true)
            }
            else
            {
                if (mDstAccount != null)
                {
                    onStorageInfoComplete(null, true)
                }

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
        if (isRecipient)
        {
            recipient_area.visibility = View.VISIBLE
            amount_layout.visibility = View.GONE
        }
        else
        {
            recipient_area.visibility = View.GONE
            amount_layout.visibility = View.GONE
        }

        loading_progress.visibility = View.VISIBLE
        refresh_KT1_source_layout.visibility = View.GONE
        mClickSourceKT1 = false

        refresh_KT1_recipient_layout.visibility = View.GONE
        mClickRecipientKT1 = false

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
                if (content != null)
                {
                    addContractInfoFromJSON(it, isRecipient)
                    onStorageInfoComplete(null, isRecipient)
                }
            },
                    Response.ErrorListener {

                        if (content != null)
                        {
                            onStorageInfoComplete(it, isRecipient)
                            /*
                            if (isRecipient)
                            {
                                mClickRecipientKT1 = true
                            }
                            else
                            {
                                mClickSourceKT1 = true
                            }
                            */
                        }
                    })

            jsonArrayRequest.tag =

                    if (isRecipient)
                    {
                        CONTRACT_SCRIPT_RECIPIENT_INFO_TAG
                    }
                    else
                    {
                        CONTRACT_SCRIPT_SOURCE_INFO_TAG
                    }

            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    private fun addContractInfoFromJSON(answer: JSONObject, isRecipient: Boolean)
    {
        if (answer.length() > 0)
        {
            if (isRecipient)
            {
                mStorageRecipient = answer.toString()
            }
            else
            {
                mStorageSource = answer.toString()
            }
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
            //TODO need to try if it does work
            //I need to send some tez to an SLC, from an SLC.

            if (mRecipientKT1withCode)
            {
                var canSignWithMaster = false
                val hasMnemonics = Storage(context!!).hasMnemonics()
                if (hasMnemonics)
                {
                    val seed = Storage(activity!!).getMnemonics()
                    canSignWithMaster = !seed.mnemonics.isNullOrEmpty()
                }

                if (canSignWithMaster)
                {

                    val mnemonicsData = Storage(activity!!).getMnemonics()
                    val pk = if (mnemonicsData.pk.isNullOrEmpty())
                    {
                        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                        updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
                    }
                    else
                    {
                        mnemonicsData.pk
                    }

                    val pkh = mnemonicsData.pkh

                    postParams.put("src_pk", pk)
                    postParams.put("src", pkh)

                    val kt1 = arguments!!.getString(Address.TAG)

                    var dstObjects = JSONArray()

                    var dstObject = JSONObject()

                    dstObject.put("dst", kt1)
                    dstObject.put("amount", "0")

                    dstObject.put("entrypoint", "appel_clef_maitresse")


                    // use the tz1 to transfer

                    val dataVisitable = Primitive(
                            Primitive.Name.Right,
                            arrayOf(
                                    Primitive(Primitive.Name.Pair,
                                            arrayOf(
                                                    Visitable.sequenceOf(
                                                            Primitive(
                                                                    Primitive.Name.DIP,
                                                                    arrayOf(

                                                                            Visitable.sequenceOf(

                                                                                    Primitive(Primitive.Name.NIL,
                                                                                            arrayOf(
                                                                                                    Primitive(Primitive.Name.operation)
                                                                                            )
                                                                                    ),
                                                                                    Primitive(Primitive.Name.PUSH,
                                                                                            arrayOf(
                                                                                                    Primitive(Primitive.Name.address),
                                                                                                    Visitable.address(mDstAccount!!)
                                                                                            )
                                                                                    ),
                                                                                    Primitive(Primitive.Name.CONTRACT,
                                                                                            arrayOf(Primitive(Primitive.Name.unit))
                                                                                    ),
                                                                                    Visitable.sequenceOf(
                                                                                            Primitive(Primitive.Name.IF_NONE,
                                                                                                    arrayOf(

                                                                                                            Visitable.sequenceOf(
                                                                                                                    Visitable.sequenceOf(
                                                                                                                            Primitive(Primitive.Name.UNIT),
                                                                                                                            Primitive(Primitive.Name.FAILWITH)
                                                                                                                    )
                                                                                                            ),
                                                                                                            Visitable.sequenceOf()
                                                                                                    )
                                                                                            )
                                                                                    ),
                                                                                    Primitive(Primitive.Name.PUSH,
                                                                                            arrayOf(
                                                                                                    Primitive(Primitive.Name.mutez),
                                                                                                    Visitable.integer((mTransferAmount*1000000).roundToLong())
                                                                                            )
                                                                                    )
                                                                            )
                                                                    )
                                                            ),
                                                            Primitive(Primitive.Name.TRANSFER_TOKENS),
                                                            Primitive(Primitive.Name.CONS)
                                                    ),
                                                    Visitable.keyHash(pkh)
                                            )
                                    )
                            )
                    )

                    val o = ByteArrayOutputStream()
                    o.write(0x05)

                    val dataPacker = Packer(o)
                    dataVisitable.accept(dataPacker)

                    val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

                    val addressAndChainVisitable = Primitive(Primitive.Name.Pair,
                            arrayOf(
                                    Visitable.address(kt1!!),
                                    Visitable.chainID(getString(R.string.chain_ID))
                            )
                    )

                    val output = ByteArrayOutputStream()
                    output.write(0x05)

                    val p = Packer(output)
                    addressAndChainVisitable.accept(p)

                    val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()


                    var saltVisitable: Visitable? = null
                    val salt = getSalt(isRecipient = false)
                    if (salt != null)
                    {
                        saltVisitable = Visitable.integer(salt.toLong())
                    }

                    val outputStream = ByteArrayOutputStream()
                    outputStream.write(0x05)

                    val packer = Packer(outputStream)
                    saltVisitable!!.accept(packer)

                    val saltPack = (packer.output as ByteArrayOutputStream).toByteArray()

                    val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                    val sk = CryptoUtils.generateSk(mnemonics, "")

                    val signature = KeyPair.sign(sk, dataPack + addressAndChainPack + saltPack)

                    val edsig = CryptoUtils.generateEDSig(signature)

                    val spendingLimitFile = "spending_limit_massive_transfer_to_kt1.json"
                    val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                            .use {
                                it.readText()
                            }

                    val value = JSONObject(contract)

                    val argsSig = ((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray
                    val argPk = argsSig[0] as JSONObject
                    argPk.put("string", pk)

                    val argSig = argsSig[1] as JSONObject
                    argSig.put("string", edsig)

                    mSig = edsig

                    val argsMasterKey = (((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                    argsMasterKey.put("string", pkh)

                    val argsTz = ((((((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONArray

                    val argAddress = ((argsTz[1] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                    argAddress.put("string", mDstAccount)

                    val argAmount = ((argsTz[4] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                    argAmount.put("int", (mTransferAmount*1000000).roundToLong().toString())

                    dstObject.put("parameters", value)

                    dstObjects.put(dstObject)

                    postParams.put("dsts", dstObjects)

                }
                else
                {
                    val kt1 = arguments!!.getString(Address.TAG)

                    val ecKeys = retrieveECKeys()
                    if (ecKeys == null)
                    {
                        val volleyError = VolleyError(getString(R.string.generic_error))
                        onInitTransferLoadComplete(volleyError)
                        mClickCalculate = true

                        return
                    }

                    val p2pk = CryptoUtils.generateP2Pk(ecKeys)
                    val tz3 = CryptoUtils.generatePkhTz3(ecKeys)

                    postParams.put("src_pk", p2pk)
                    postParams.put("src", tz3)

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
                                                            Visitable.integer((mTransferAmount*1000000).roundToLong()),
                                                            Visitable.address(mDstAccount!!)
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
                                    Visitable.address(kt1!!),
                                    Visitable.chainID(getString(R.string.chain_ID))
                            )
                    )

                    val output = ByteArrayOutputStream()
                    output.write(0x05)

                    val p = Packer(output)
                    addressAndChainVisitable.accept(p)

                    val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()


                    var saltVisitable: Visitable? = null
                    val salt = getSalt(isRecipient = false)
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

                    val signatureTz3 = EncryptionServices().sign(signedData)
                    val compressedSignature = compressFormat(signatureTz3)
                    val p2sig = CryptoUtils.generateP2Sig(compressedSignature)



                    val spendingLimitFile = "spending_limit_transfer.json"
                    val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                            .use {
                                it.readText()
                            }

                    val value = JSONObject(contract)

                    val argsSend = (((((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONArray)[0] as JSONObject)["args"] as JSONArray

                    val argsSendAmount = argsSend[0] as JSONObject
                    argsSendAmount.put("int", (mTransferAmount*1000000).roundToLong().toString())

                    val argsSendContract = argsSend[1] as JSONObject
                    argsSendContract.put("string", mDstAccount)

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
            }
            else
            {
                var canSignWithMaster = false
                val hasMnemonics = Storage(context!!).hasMnemonics()
                if (hasMnemonics)
                {
                    val seed = Storage(activity!!).getMnemonics()
                    canSignWithMaster = !seed.mnemonics.isNullOrEmpty()
                }

                if (canSignWithMaster)
                {

                    val beginsWith = mDstAccount?.slice(0 until 3)

                    //this one is a standard KT1
                    if (beginsWith?.toLowerCase(Locale.US) == "kt1")
                    {
                        val mnemonicsData = Storage(activity!!).getMnemonics()
                        val pk = if (mnemonicsData.pk.isNullOrEmpty())
                        {
                            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                            updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
                        }
                        else
                        {
                            mnemonicsData.pk
                        }

                        val pkh = mnemonicsData.pkh

                        postParams.put("src_pk", pk)
                        postParams.put("src", pkh)

                        val kt1 = arguments!!.getString(Address.TAG)

                        var dstObjects = JSONArray()

                        var dstObject = JSONObject()

                        dstObject.put("dst", kt1)
                        dstObject.put("amount", "0")

                        dstObject.put("entrypoint", "appel_clef_maitresse")


                        // use the tz1 to transfer

                        val dataVisitable = Primitive(
                                Primitive.Name.Right,
                                arrayOf(
                                        Primitive(Primitive.Name.Pair,
                                                arrayOf(
                                                        Visitable.sequenceOf(
                                                                Primitive(
                                                                        Primitive.Name.DIP,
                                                                        arrayOf(

                                                                                Visitable.sequenceOf(

                                                                                        Primitive(Primitive.Name.NIL,
                                                                                                arrayOf(
                                                                                                        Primitive(Primitive.Name.operation)
                                                                                                )
                                                                                        ),
                                                                                        Primitive(Primitive.Name.PUSH,
                                                                                                arrayOf(
                                                                                                        Primitive(Primitive.Name.address),
                                                                                                        Visitable.address(mDstAccount!!)
                                                                                                )
                                                                                        ),
                                                                                        Primitive(Primitive.Name.CONTRACT,
                                                                                                arrayOf(Primitive(Primitive.Name.unit))
                                                                                        ),
                                                                                        Visitable.sequenceOf(
                                                                                                Primitive(Primitive.Name.IF_NONE,
                                                                                                        arrayOf(

                                                                                                                Visitable.sequenceOf(
                                                                                                                        Visitable.sequenceOf(
                                                                                                                                Primitive(Primitive.Name.UNIT),
                                                                                                                                Primitive(Primitive.Name.FAILWITH)
                                                                                                                        )
                                                                                                                ),
                                                                                                                Visitable.sequenceOf()
                                                                                                        )
                                                                                                )
                                                                                        ),
                                                                                        Primitive(Primitive.Name.PUSH,
                                                                                                arrayOf(
                                                                                                        Primitive(Primitive.Name.mutez),
                                                                                                        Visitable.integer((mTransferAmount*1000000).roundToLong())
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                ),
                                                                Primitive(Primitive.Name.TRANSFER_TOKENS),
                                                                Primitive(Primitive.Name.CONS)
                                                        ),
                                                        Visitable.keyHash(pkh)
                                                )
                                        )
                                )
                        )

                        val o = ByteArrayOutputStream()
                        o.write(0x05)

                        val dataPacker = Packer(o)
                        dataVisitable.accept(dataPacker)

                        val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

                        val addressAndChainVisitable = Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.address(kt1!!),
                                        Visitable.chainID(getString(R.string.chain_ID))
                                )
                        )

                        val output = ByteArrayOutputStream()
                        output.write(0x05)

                        val p = Packer(output)
                        addressAndChainVisitable.accept(p)

                        val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()


                        var saltVisitable: Visitable? = null
                        val salt = getSalt(isRecipient = false)
                        if (salt != null)
                        {
                            saltVisitable = Visitable.integer(salt.toLong())
                        }

                        val outputStream = ByteArrayOutputStream()
                        outputStream.write(0x05)

                        val packer = Packer(outputStream)
                        saltVisitable!!.accept(packer)

                        val saltPack = (packer.output as ByteArrayOutputStream).toByteArray()

                        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                        val sk = CryptoUtils.generateSk(mnemonics, "")

                        val signature = KeyPair.sign(sk, dataPack + addressAndChainPack + saltPack)

                        val edsig = CryptoUtils.generateEDSig(signature)

                        val spendingLimitFile = "spending_limit_massive_transfer_to_kt1.json"
                        val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                                .use {
                                    it.readText()
                                }

                        val value = JSONObject(contract)

                        val argsSig = ((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray
                        val argPk = argsSig[0] as JSONObject
                        argPk.put("string", pk)

                        val argSig = argsSig[1] as JSONObject
                        argSig.put("string", edsig)
                        mSig = edsig

                        val argsMasterKey = (((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                        argsMasterKey.put("string", pkh)

                        val argsTz = ((((((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONArray

                        val argAddress = ((argsTz[1] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                        argAddress.put("string", mDstAccount)

                        val argAmount = ((argsTz[4] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                        argAmount.put("int", (mTransferAmount*1000000).roundToLong().toString())

                        dstObject.put("parameters", value)

                        dstObjects.put(dstObject)

                        postParams.put("dsts", dstObjects)

                    }
                    else
                    {

                        val mnemonicsData = Storage(activity!!).getMnemonics()

                        val pk = if (mnemonicsData.pk.isNullOrEmpty())
                        {
                            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                            updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
                        }
                        else
                        {
                            mnemonicsData.pk
                        }

                        val pkh = mnemonicsData.pkh

                        postParams.put("src_pk", pk)
                        postParams.put("src", pkh)

                        val kt1 = arguments!!.getString(Address.TAG)

                        var dstObjects = JSONArray()

                        var dstObject = JSONObject()

                        dstObject.put("dst", kt1)
                        dstObject.put("amount", "0")

                        dstObject.put("entrypoint", "appel_clef_maitresse")


                        // use the tz1 to transfer

                        val dataVisitable = Primitive(
                                Primitive.Name.Right,
                                arrayOf(
                                        Primitive(
                                                Primitive.Name.Pair,
                                                arrayOf(

                                                        Visitable.sequenceOf(

                                                                Primitive(
                                                                        Primitive.Name.DIP,
                                                                        arrayOf(
                                                                                Visitable.sequenceOf(

                                                                                        Primitive(Primitive.Name.NIL,
                                                                                                arrayOf(Primitive(Primitive.Name.operation))
                                                                                        ),

                                                                                        Primitive(Primitive.Name.PUSH,
                                                                                                arrayOf(
                                                                                                        Primitive(Primitive.Name.key_hash),
                                                                                                        Visitable.keyHash(mDstAccount!!)
                                                                                                )
                                                                                        ),
                                                                                        Primitive(Primitive.Name.IMPLICIT_ACCOUNT),
                                                                                        Primitive(Primitive.Name.PUSH,
                                                                                                arrayOf(
                                                                                                        Primitive(Primitive.Name.mutez),
                                                                                                        Visitable.integer((mTransferAmount*1000000).roundToLong())
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                ),
                                                                Primitive(Primitive.Name.TRANSFER_TOKENS),
                                                                Primitive(Primitive.Name.CONS)
                                                        ),
                                                        Visitable.keyHash(pkh)
                                                )
                                        )
                                )
                        )

                        val o = ByteArrayOutputStream()
                        o.write(0x05)

                        val dataPacker = Packer(o)
                        dataVisitable.accept(dataPacker)

                        val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

                        //val packCompare = "0505080707020000003a051f020000002f053d036d0743035d0a000000150002298c03ed7d454a101eb7022bc95f7e5f41ac78031e0743036a0080c0a8ca9a3a034d031b0a0000001500dbd1087b133e63b9e320d20be9d1469621b6d682".hexToByteArray()

                        val addressAndChainVisitable = Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.address(kt1!!),
                                        Visitable.chainID(getString(R.string.chain_ID))
                                )
                        )

                        val output = ByteArrayOutputStream()
                        output.write(0x05)

                        val p = Packer(output)
                        addressAndChainVisitable.accept(p)

                        val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()


                        var saltVisitable: Visitable? = null
                        val salt = getSalt(isRecipient = false)
                        if (salt != null)
                        {
                            saltVisitable = Visitable.integer(salt.toLong())
                        }

                        val outputStream = ByteArrayOutputStream()
                        outputStream.write(0x05)

                        val packer = Packer(outputStream)
                        saltVisitable!!.accept(packer)

                        val saltPack = (packer.output as ByteArrayOutputStream).toByteArray()

                        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                        val sk = CryptoUtils.generateSk(mnemonics, "")

                        val signature = KeyPair.sign(sk, dataPack + addressAndChainPack + saltPack)

                        val edsig = CryptoUtils.generateEDSig(signature)

                        val spendingLimitFile = "spending_limit_massive_transfer.json"
                        val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                                .use {
                                    it.readText()
                                }

                        val value = JSONObject(contract)

                        val argsSig = ((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray
                        val argPk = argsSig[0] as JSONObject
                        argPk.put("string", pk)

                        val argSig = argsSig[1] as JSONObject
                        argSig.put("string", edsig)

                        mSig = edsig

                        val argsTz = ((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

                        val keyHash = argsTz[1] as JSONObject
                        keyHash.put("string", pkh)

                        val arggs = ((((((((((value["args"] as JSONArray)[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONArray)[0]) as JSONObject)["args"] as JSONArray)[0] as JSONArray)
                        val masterKeyHash = ((arggs[1] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                        masterKeyHash.put("string", mDstAccount)

                        val mutezArgs = ((arggs[3] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                        mutezArgs.put("int", (mTransferAmount*1000000).roundToLong().toString())

                        dstObject.put("parameters", value)

                        dstObjects.put(dstObject)

                        postParams.put("dsts", dstObjects)

                    }
                }
                else
                {
                    // use the tz3 to transfer

                    val ecKeys = retrieveECKeys()
                    if (ecKeys == null)
                    {
                        val volleyError = VolleyError(getString(R.string.generic_error))
                        onInitTransferLoadComplete(volleyError)
                        mClickCalculate = true

                        return
                    }
                    val p2pk = CryptoUtils.generateP2Pk(ecKeys)
                    postParams.put("src_pk", p2pk)
                    val tz3 = CryptoUtils.generatePkhTz3(ecKeys)
                    postParams.put("src", tz3)

                    val kt1 = arguments!!.getString(Address.TAG)

                    var dstObjects = JSONArray()

                    var dstObject = JSONObject()

                    dstObject.put("dst", arguments!!.getString(Address.TAG))
                    dstObject.put("amount", "0")

                    dstObject.put("entrypoint", "transfer")

                    val dataVisitable = Primitive(
                            Primitive.Name.Pair,
                            arrayOf(
                                    Visitable.sequenceOf(
                                            Primitive(
                                                    Primitive.Name.Pair,
                                                    arrayOf(
                                                            Visitable.integer((mTransferAmount*1000000).roundToLong()),
                                                            Visitable.address(mDstAccount!!)
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
                                    Visitable.address(kt1!!),
                                    Visitable.chainID(getString(R.string.chain_ID))
                            )
                    )

                    val output = ByteArrayOutputStream()
                    output.write(0x05)

                    val p = Packer(output)
                    addressAndChainVisitable.accept(p)

                    val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()



                    var saltVisitable: Visitable? = null
                    val salt = getSalt(isRecipient = false)
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

                    val spendingLimitFile = "spending_limit_transfer.json"
                    val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                            .use {
                                it.readText()
                            }

                    val value = JSONObject(contract)

                    val args = value["args"] as JSONArray

                    val firstParamArgs = (args[0] as JSONObject)["args"] as JSONArray

                    val amountAndContract = ((firstParamArgs[0] as JSONArray)[0] as JSONObject)["args"] as JSONArray
                    val amount = amountAndContract[0] as JSONObject
                    amount.put("int", (mTransferAmount*1000000).roundToLong().toString())

                    val contractKT1 = amountAndContract[1] as JSONObject
                    contractKT1.put("string", mDstAccount)

                    val dst = firstParamArgs[1] as JSONObject
                    dst.put("string", tz3)

                    val secondParamArgs = (args[1] as JSONObject)["args"] as JSONArray

                    val pk = secondParamArgs[0] as JSONObject
                    pk.put("string", p2pk)

                    val sig = secondParamArgs[1] as JSONObject
                    sig.put("string", p2sig)

                    mSig = p2sig

                    dstObject.put("parameters", value)

                    dstObjects.put(dstObject)

                    postParams.put("dsts", dstObjects)
                }
            }
        }
        else
        {
            val mnemonicsData = Storage(activity!!).getMnemonics()

            val pk = if (mnemonicsData.pk.isNullOrEmpty())
            {
                val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
            }
            else
            {
                mnemonicsData.pk
            }

            val beginsWith = mSrcAccount?.slice(0 until 3)


            //this one is a standard KT1
            if (beginsWith?.toLowerCase(Locale.US) == "kt1")
            {
                postParams.put("src", mnemonicsData.pkh)
                postParams.put("src_pk", pk)

                var dstObjects = JSONArray()

                var dstObject = JSONObject()
                dstObject.put("dst", mSrcAccount)
                dstObject.put("amount", "0")

                dstObject.put("entrypoint", "do")

                val destBeginsWith = mDstAccount?.slice(0 until 3)

                var value:JSONArray

                if (destBeginsWith?.toLowerCase(Locale.US) == "kt1")
                {
                    val spendingLimitFile = "standard_to_standard_transfer.json"
                    val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                            .use {
                                it.readText()
                            }

                    value = JSONArray(contract)
                    val dstValue = ((value[2] as JSONObject)["args"] as JSONArray)[1] as JSONObject

                    dstValue.put("string", mDstAccount)

                    val dstAmount = ((value[5] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                    dstAmount.put("int", (mTransferAmount*1000000).roundToLong().toString())
                }
                else
                {
                    val spendingLimitFile = "standard_to_implicit_transfer.json"
                    val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                            .use {
                                it.readText()
                            }

                    value = JSONArray(contract)
                    val dstValue = ((value[2] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                    dstValue.put("string", mDstAccount)

                    val dstAmount = ((value[4] as JSONObject)["args"] as JSONArray)[1] as JSONObject
                    dstAmount.put("int", (mTransferAmount*1000000).roundToLong().toString())
                }

                dstObject.put("parameters", value)

                dstObjects.put(dstObject)

                postParams.put("dsts", dstObjects)
            }
            else
            {
                postParams.put("src", mSrcAccount)
                postParams.put("src_pk", pk)

                var dstObjects = JSONArray()

                var dstObject = JSONObject()
                dstObject.put("dst", mDstAccount)

                dstObject.put("amount", (mTransferAmount*1000000).roundToLong().toString())

                dstObjects.put(dstObject)

                postParams.put("dsts", dstObjects)
            }
        }

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (content != null)
            {
                mTransferPayload = answer.getString("result")
                mTransferFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mTransferPayload != null && mTransferFees != -1L)
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
            if (content != null)
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

    private fun retrieveECKeys():ByteArray?
    {
        var keyPair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        if (keyPair != null)
        {
            val ecKey = keyPair!!.public as ECPublicKey
            return ecKeyFormat(ecKey)
        }
        return null
    }

    private fun updateMnemonicsData(data: Storage.MnemonicsData, pk:String):String
    {
        with(Storage(activity!!)) {
            saveSeed(Storage.MnemonicsData(data.pkh, pk, data.mnemonics))
        }
        return pk
    }

    // volley
    private fun startPostRequestLoadFinalizeTransfer()
    {
        val url = getString(R.string.transfer_injection_operation)


        //TODO we got to verify at this very moment.
        if (isPayButtonValid() && mTransferPayload != null)
        {

            val mnemonicsData = Storage(activity!!).getMnemonics()
            var postParams = JSONObject()

            var canSignWithMaster = false
            val hasMnemonics = Storage(context!!).hasMnemonics()
            if (hasMnemonics)
            {
                val seed = Storage(activity!!).getMnemonics()
                canSignWithMaster = !seed.mnemonics.isNullOrEmpty()
            }

            val beginsWith = mSrcAccount?.slice(0 until 3)
            if (beginsWith?.toLowerCase(Locale.US) == "kt1")
            {

                if (mSourceKT1withCode && !canSignWithMaster)
                {
                    val ecKeys = retrieveECKeys()
                    val p2pk = CryptoUtils.generateP2Pk(ecKeys)
                    postParams.put("src_pk", p2pk)
                    val tz3 = CryptoUtils.generatePkhTz3(ecKeys)
                    postParams.put("src", tz3)
                }
                else
                {
                    postParams.put("src", mnemonicsData.pkh)
                    postParams.put("src_pk", mnemonicsData.pk)
                }

                var dstObjects = JSONArray()

                var dstObject = JSONObject()
                dstObject.put("dst", mSrcAccount)
                dstObject.put("dst_account", mDstAccount)
                dstObject.put("amount", 0.toLong())

                val mutezAmount = (mTransferAmount*1000000.0).roundToLong()
                dstObject.put("transfer_amount", mutezAmount)

                dstObject.put("fee", mTransferFees)

                val destBeginsWith = mDstAccount?.slice(0 until 3)


                if (destBeginsWith?.toLowerCase(Locale.US) == "kt1")
                {
                    if (mSourceKT1withCode)
                    {
                        if (mRecipientKT1withCode)
                        {
                            if (canSignWithMaster)
                            {
                                dstObject.put("contract_type", "slc_master_to_kt1")
                            }
                            else
                            {
                                dstObject.put("contract_type", "slc_enclave_transfer")
                            }
                        }
                        else
                        {
                            if (canSignWithMaster)
                            {
                                dstObject.put("contract_type", "slc_master_to_kt1")
                            }
                            else
                            {
                                dstObject.put("contract_type", "slc_enclave_transfer")
                            }
                        }

                        dstObject.put("edsig", mSig)
                    }
                    else
                    {
                        dstObject.put("contract_type", "kt1_to_kt1")
                    }
                }
                else
                {
                    if (mSourceKT1withCode)
                    {
                        if (canSignWithMaster)
                        {
                            dstObject.put("contract_type", "slc_master_to_tz")
                        }
                        else
                        {
                            dstObject.put("contract_type", "slc_enclave_transfer")
                        }
                        dstObject.put("edsig", mSig)
                    }
                    else
                    {
                        dstObject.put("contract_type", "kt1_to_tz")
                    }
                }

                dstObjects.put(dstObject)

                postParams.put("dsts", dstObjects)
            }
            else
            {
                postParams.put("src", mSrcAccount)

                //TODO it won't be pk with contract transfer
                postParams.put("src_pk", mnemonicsData.pk)

                var dstObjects = JSONArray()

                var dstObject = JSONObject()
                dstObject.put("dst", mDstAccount)

                val mutezAmount = (mTransferAmount*1000000.0).roundToLong()
                dstObject.put("amount", mutezAmount)

                dstObject.put("fee", mTransferFees)

                dstObjects.put(dstObject)

                postParams.put("dsts", dstObjects)
            }


            //TODO verify the payloads
            if (isTransferPayloadValid(mTransferPayload!!, postParams))
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mTransferPayload!!.hexToByteArray()

                val xLen = zeroThree.size
                val yLen = byteArrayThree.size
                val result = ByteArray(xLen + yLen)

                System.arraycopy(zeroThree, 0, result, 0, xLen)
                System.arraycopy(byteArrayThree, 0, result, xLen, yLen)


                var canSignWithMaster = false
                val hasMnemonics = Storage(context!!).hasMnemonics()
                if (hasMnemonics)
                {
                    val seed = Storage(activity!!).getMnemonics()
                    canSignWithMaster = !seed.mnemonics.isNullOrEmpty()
                }

                var compressedSignature = if (mSourceKT1withCode && !canSignWithMaster)
                {
                    val bytes = KeyPair.b2b(result)
                    var signature = EncryptionServices().sign(bytes)
                    compressFormat(signature)
                }
                else
                {
                    val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
                    val sk = CryptoUtils.generateSk(mnemonics, "")
                    KeyPair.sign(sk, result)
                }

                val pLen = byteArrayThree.size
                val sLen = compressedSignature.size
                val newResult = ByteArray(pLen + sLen)

                System.arraycopy(byteArrayThree, 0, newResult, 0, pLen)
                System.arraycopy(compressedSignature, 0, newResult, pLen, sLen)

                var payloadsign = newResult.toNoPrefixHexString()

                val stringRequest = object : StringRequest(Method.POST, url,
                        Response.Listener<String> {

                            if (content != null)
                            {
                                onFinalizeTransferLoadComplete(null)
                                listener?.onTransferSucceed()
                            }
                        },
                        Response.ErrorListener
                        {
                            if (content != null)
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

    private fun onStorageInfoComplete(error: VolleyError?, isRecipient: Boolean)
    {
        if (isRecipient)
        {
            mRecipientStorageInfoLoading = false
        }
        else
        {
            mSourceStorageInfoLoading = false
        }

        if (mClickRecipientKT1)
        {
            loading_progress.visibility = View.GONE
            recipient_area.visibility = View.VISIBLE
            refresh_KT1_source_layout.visibility = View.GONE
            refresh_KT1_recipient_layout.visibility = View.VISIBLE
            amount_layout.visibility = View.GONE
        }
        else if (mClickSourceKT1)
        {
            // handle the KT1 source first.
            loading_progress.visibility = View.GONE
            recipient_area.visibility = View.GONE
            refresh_KT1_source_layout.visibility = View.VISIBLE
            refresh_KT1_recipient_layout.visibility = View.GONE
            amount_layout.visibility = View.GONE
        }

        else if (error != null)
        {
            val response = error.networkResponse?.statusCode
            if (response != 404)
            {
                listener?.onTransferFailed(error)

                if (isRecipient)
                {
                    //TODO handle the click KT1 recipient properly
                    //mClickRecipientKT1 = false
                    mClickRecipientKT1 = true

                    loading_progress.visibility = View.GONE
                    recipient_area.visibility = View.VISIBLE
                    refresh_KT1_recipient_layout.visibility = View.VISIBLE
                }
                else
                {
                    // handle the KT1 source first.
                    // KT1 with code or tz1

                    loading_progress.visibility = View.GONE
                    recipient_area.visibility = View.GONE
                    refresh_KT1_source_layout.visibility = View.VISIBLE

                    mClickSourceKT1 = true
                }

                //TODO user needs to retry storage call
                //TODO I will display elements depending on the situation
            }
        }
        else
        {

            //TODO this is a KT1, default one or with code.

            //TODO check if our tz3 is the same as the contract tz3

            //TODO if there is salt, this is a spending limit contract

            val salt = getSalt(isRecipient)
            if (salt != null && salt >= 0)
            {

                if (isRecipient)
                {
                    // the recipient is a KT1 with code
                    mRecipientKT1withCode = true

                    recipient_area.visibility = View.VISIBLE
                    amount_layout.visibility = View.VISIBLE
                }
                else
                {
                    // the source is a KT1 with code

                    mSourceKT1withCode = true

                    recipient_area.visibility = View.VISIBLE
                    amount_layout.visibility = View.GONE
                }

                loading_progress.visibility = View.GONE

                // with this information, handle the code to sign data

                /*
                val tz3 = getTz3()
                val contractTz3 = getContractTz3(isRecipient)

                if (tz3 == null || tz3 != contractTz3)
                {
                    //TODO ERROR, there is no way you can spend money without the right tz3
                    //TODO please update your contract storage in contract tab.
                }
                else
                {
                    //TODO it's ok, this phone has the keys.
                }
                */
            }
            else
            {
                if (isRecipient)
                {
                    mRecipientKT1withCode = false

                    if (!mDstAccount.isNullOrEmpty() && !mDstAccount!!.startsWith("KT1", true))
                    {
                        //this is a standard source tz1/2/3

                        loading_progress.visibility = View.GONE
                        recipient_area.visibility = View.VISIBLE
                        amount_layout.visibility = View.VISIBLE
                    }
                    else
                    {
                        //this is a KT1 with no code in it

                        loading_progress.visibility = View.GONE
                        recipient_area.visibility = View.VISIBLE
                        amount_layout.visibility = View.VISIBLE

                        mClickRecipientKT1 = false
                    }
                }
                else
                {
                    mSourceKT1withCode = false

                    arguments?.let {

                        val srcAddress = it.getString(Address.TAG)
                        if (!srcAddress.isNullOrEmpty() && !srcAddress.startsWith("KT1", true))
                        {
                            //this is a standard source tz1/2/3

                            //no need to hide it anymore
                            loading_progress.visibility = View.GONE
                            recipient_area.visibility = View.VISIBLE
                            amount_layout.visibility = View.GONE
                        }
                        else
                        {
                            //it looks like it's a KT1 with no code in it.

                            mClickSourceKT1 = false

                            // we got to handle if we have the mnemonics.
                            val hasMnemonics = Storage(activity!!).hasMnemonics()
                            if (hasMnemonics)
                            {
                                val seed = Storage(activity!!).getMnemonics()

                                if (seed.mnemonics.isEmpty())
                                {
                                    // TODO write a text to say we cannot transfer anything.
                                    recipient_area.visibility = View.GONE
                                    no_mnemonics.visibility = View.VISIBLE
                                }
                                else
                                {
                                    recipient_area.visibility = View.VISIBLE
                                }

                                loading_progress.visibility = View.GONE
                                amount_layout.visibility = View.GONE
                            }

                        }
                    }
                }
            }
        }

        //refreshLoadingArea()
    }

    private fun getContractTz3(isRecipient: Boolean):String?
    {
        //TODO check if the storage follows our pattern

        if (isRecipient && mStorageRecipient != null)
        {
            val storageJSONObject = JSONObject(mStorageRecipient)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

            val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args") as JSONArray
            val secureKeyJSONObject = argsSecureKey[0] as JSONObject
            val secureKeyJSONArray = DataExtractor.getJSONArrayFromField(secureKeyJSONObject, "args")

            val secureKeyHashField = DataExtractor.getJSONObjectFromField(secureKeyJSONArray, 1)
            return DataExtractor.getStringFromField(secureKeyHashField, "string")
        }

        else if (!isRecipient && mStorageSource != null)
        {
            val storageJSONObject = JSONObject(mStorageSource)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

            val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args") as JSONArray
            val secureKeyJSONObject = argsSecureKey[0] as JSONObject
            val secureKeyJSONArray = DataExtractor.getJSONArrayFromField(secureKeyJSONObject, "args")

            val secureKeyHashField = DataExtractor.getJSONObjectFromField(secureKeyJSONArray, 1)
            return DataExtractor.getStringFromField(secureKeyHashField, "string")
        }

        return null
    }

    private fun getSalt(isRecipient: Boolean):Int?
    {
        //TODO check if the storage follows our pattern
        if (isRecipient && mStorageRecipient != null)
        {
            val storageJSONObject = JSONObject(mStorageRecipient)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")
            if (args != null)
            {
                val argsMasterKey = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray
                val masterKeySaltJSONObject = argsMasterKey[1] as JSONObject

                var canSignWithMaster = false
                val hasMnemonics = Storage(context!!).hasMnemonics()
                if (hasMnemonics)
                {
                    val seed = Storage(activity!!).getMnemonics()
                    canSignWithMaster = !seed.mnemonics.isNullOrEmpty()
                }

                val salt = if (mSourceKT1withCode && !canSignWithMaster)
                {
                    (masterKeySaltJSONObject["args"] as JSONArray)[1] as JSONObject
                }
                else
                {
                    (masterKeySaltJSONObject["args"] as JSONArray)[0] as JSONObject
                }


                return DataExtractor.getStringFromField(salt, "int").toInt()
            }
        }
        else if (!isRecipient && mStorageSource != null)
        {
            val storageJSONObject = JSONObject(mStorageSource)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")
            if (args != null)
            {
                val argsMasterKey = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray
                val masterKeySaltJSONObject = argsMasterKey[1] as JSONObject

                var canSignWithMaster = false
                val hasMnemonics = Storage(context!!).hasMnemonics()
                if (hasMnemonics)
                {
                    val seed = Storage(activity!!).getMnemonics()
                    canSignWithMaster = !seed.mnemonics.isNullOrEmpty()
                }

                val salt = if (mSourceKT1withCode && !canSignWithMaster)
                {
                    (masterKeySaltJSONObject["args"] as JSONArray)[1] as JSONObject
                }
                else
                {
                    (masterKeySaltJSONObject["args"] as JSONArray)[0] as JSONObject
                }

                return DataExtractor.getStringFromField(salt, "int").toInt()
            }
        }

        return null
    }

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

        fee_edittext_new.setOnClickListener {
            startStorageInfoLoading(isRecipient = false)
        }

        fee_edittext_recipient.setOnClickListener {
            startStorageInfoLoading(isRecipient = true)
        }
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
                    mDstAccount = account.pubKeyHash

                    //TODO verify this address is a KT1 and check its storage.
                    //TODO if it's not a KT1, there's no need to
                    if (!mDstAccount.isNullOrEmpty() && mDstAccount!!.startsWith("KT1", true))
                    {
                        /*
                        if (!mSourceKT1withCode)
                        {
                            startStorageInfoLoading(true)
                        }
                        else
                        {
                            listener?.showSnackBar("From a Daily Spending Limit contract, you can't select a KT1 as a recipient", ContextCompat.getColor(activity!!, R.color.tz_accent), Color.YELLOW)

                            //listener?.onTransferFailed(volleyError)
                            mDstAccount = null
                            return
                        }
                        */

                        startStorageInfoLoading(true)
                    }
                    else
                    {
                        //no need to hide it anymore
                        loading_progress.visibility = View.GONE
                        recipient_area.visibility = View.VISIBLE
                        amount_layout.visibility = View.VISIBLE

                    }

                    switchButtonAndLayout(AddressBookActivity.Selection.SelectionAccountsAndAddresses, mDstAccount!!)
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

        if (!TextUtils.isEmpty(editable))
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
                    mTransferFees = longTransferFee.roundToLong()
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
            /*
            val customThemeBundle = arguments!!.getBundle(CustomTheme.TAG)
            val theme = CustomTheme.fromBundle(customThemeBundle)
            */
            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

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

                    if (isInputDataValid())
                    {
                        startInitTransferLoading()
                    }
                    else
                    {
                        validatePayButton(false)

                        cancelRequests(false)
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

        outState.putBoolean(CLICK_SOURCE_KT1_KEY, mClickSourceKT1)
        outState.putBoolean(CLICK_RECIPIENT_KT1_KEY, mClickRecipientKT1)

        outState.putBoolean(CONTRACT_SCRIPT_SOURCE_INFO_TAG, mSourceStorageInfoLoading)

        outState.putBoolean(CONTRACT_SCRIPT_RECIPIENT_INFO_TAG, mRecipientStorageInfoLoading)

        outState.putString(STORAGE_DATA_SOURCE_KEY, mStorageSource)
        outState.putString(STORAGE_DATA_RECIPIENT_KEY, mStorageRecipient)

        outState.putBoolean(TZ_OR_KT1_SOURCE_KEY, mSourceKT1withCode)

        outState.putBoolean(TZ_OR_KT1_RECIPIENT_KEY, mRecipientKT1withCode)

        outState.putString(CONTRACT_SIG_KEY, mSig)
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
