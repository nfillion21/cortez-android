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
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.text.*
import android.text.style.BulletSpan
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.Account
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.fragment.AddSignatoryDialogFragment
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.activity_add_limits.*
import kotlinx.android.synthetic.main.multisig_form_card_info.*
import kotlinx.android.synthetic.main.multisig_form_card_info_signatories.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.interfaces.ECPublicKey
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.Map
import kotlin.collections.set
import kotlin.math.roundToLong

/**
 * Created by nfillion on 26/02/19.
 */
class AddMultisigActivity : BaseSecureActivity(), AddSignatoryDialogFragment.OnSignatorySelectorListener
{
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    private var mInitLoading:Boolean = false
    private var mFinalizeLoading:Boolean = false

    private var mPayload:String? = null
    private var mFees:Long = -1

    private var mDepositAmount:Double = -1.0
    private var mThreshold:Int = 0

    private var mClickCalculate:Boolean = false

    private var mIsTracking:Boolean = false
    private var mIsTyping:Boolean = false

    private var mSignatoriesList:ArrayList<String> = ArrayList(SIGNATORIES_CAPACITY)

    companion object
    {
        var ADD_DSL_REQUEST_CODE = 0x3500 // arbitrary int

        private const val DELEGATE_INIT_TAG = "delegate_init"
        private const val DELEGATE_FINALIZE_TAG = "delegate_finalize"

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val DELEGATE_AMOUNT_KEY = "delegate_amount_key"
        private const val THRESHOLD_KEY = "threshold_key"

        private const val DELEGATE_FEE_KEY = "delegate_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private const val SIGNATORIES_LIST_KEY = "signatories_list"

        private const val SIGNATORIES_CAPACITY = 10


        private fun getStartIntent(context: Context, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, AddMultisigActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, ADD_DSL_REQUEST_CODE, null)
        }
    }

    private fun pk():String
    {
        val seed = Storage(context = this).getMnemonics()
        return seed.pk
    }

    private fun pkh():String
    {
        val seed = Storage(context = this).getMnemonics()
        return seed.pkh
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_multisig)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        validateAddButton(isInputDataValid() && isFeeValid())

        create_limit_contract_button_layout.setOnClickListener {
            onDelegateClick()
        }

        add_signatory_button.setOnClickListener {
            /*
            AddressBookActivity.start(
                    this,
                    theme,
                    AddressBookActivity.Selection.SelectionAddresses)
            */
            val dialog = AddSignatoryDialogFragment.newInstance()
            dialog.show(supportFragmentManager, "AddSignatory")
        }

        val clearButtons = listOf<ImageButton>(
                delete_address_button_01,
                delete_address_button_02,
                delete_address_button_03,
                delete_address_button_04,
                delete_address_button_05,
                delete_address_button_06,
                delete_address_button_07,
                delete_address_button_08,
                delete_address_button_09,
                delete_address_button_10
        )

        for (i in clearButtons.indices)
        {
            clearButtons[i].setOnClickListener {

                if (mSignatoriesList[i] == pk())
                {
                    val dialogClickListener = { dialog: DialogInterface, which:Int ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                dialog.dismiss()

                                mSignatoriesList.removeAt(i)
                                refreshSignatories()
                            }

                            DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                        }
                    }

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.alert_remove_own_signatory_title)
                            .setMessage(String.format(getString(R.string.alert_remove_own_signatory_body), pkh()))

                            .setNegativeButton(android.R.string.cancel, dialogClickListener)
                            .setPositiveButton(android.R.string.yes, dialogClickListener)
                            .setCancelable(false)
                            .show()
                }
                else
                {
                    mSignatoriesList.removeAt(i)
                    refreshSignatories()
                }
            }
        }

        initToolbar(theme)


        amount_limit_edittext.addTextChangedListener(GenericTextWatcher(amount_limit_edittext))

        val focusChangeListener = this.focusChangeListener()
        amount_limit_edittext.onFocusChangeListener = focusChangeListener

        threshold_edittext.addTextChangedListener(GenericTextWatcher(threshold_edittext))
        threshold_edittext.onFocusChangeListener = focusChangeListener

        if (savedInstanceState != null)
        {
            mPayload = savedInstanceState.getString(DELEGATE_PAYLOAD_KEY, null)

            mInitLoading = savedInstanceState.getBoolean(DELEGATE_INIT_TAG)
            mFinalizeLoading = savedInstanceState.getBoolean(DELEGATE_FINALIZE_TAG)

            mDepositAmount = savedInstanceState.getDouble(DELEGATE_AMOUNT_KEY, -1.0)

            mThreshold = savedInstanceState.getInt(THRESHOLD_KEY, 0)

            mFees = savedInstanceState.getLong(DELEGATE_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mSignatoriesList = savedInstanceState.getStringArrayList(SIGNATORIES_LIST_KEY)
        }
        else
        {
            val ss = SpannableString(" ")
            ss.setSpan(BulletSpan(8, ContextCompat.getColor(this, R.color.colorAccent)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            bullet_textview_01.text = ss
            bullet_textview_02.text = ss
            bullet_textview_03.text = ss
            bullet_textview_04.text = ss
            bullet_textview_05.text = ss
            bullet_textview_06.text = ss
            bullet_textview_07.text = ss
            bullet_textview_08.text = ss
            bullet_textview_09.text = ss
            bullet_textview_10.text = ss

            val seed = Storage(context = this).getMnemonics()
            mSignatoriesList.add(seed.pk)
        }
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->

            when (v.id)
            {
                R.id.amount_limit_edittext -> putAmountInRed(!hasFocus)

                R.id.threshold_edittext -> putThresholdInRed(!hasFocus)

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

    private fun refreshSignatories()
    {
        val signatoryLayouts = listOf<LinearLayout>(
                signatory_layout_01,
                signatory_layout_02,
                signatory_layout_03,
                signatory_layout_04,
                signatory_layout_05,
                signatory_layout_06,
                signatory_layout_07,
                signatory_layout_08,
                signatory_layout_09,
                signatory_layout_10
        )

        val bulletEditTexts = listOf<TextView>(
                bullet_address_edittext_01,
                bullet_address_edittext_02,
                bullet_address_edittext_03,
                bullet_address_edittext_04,
                bullet_address_edittext_05,
                bullet_address_edittext_06,
                bullet_address_edittext_07,
                bullet_address_edittext_08,
                bullet_address_edittext_09,
                bullet_address_edittext_10
        )

        if (!mSignatoriesList.isNullOrEmpty())
        {
            val length = mSignatoriesList.size

            for (i in 0 until length)
            {
                bulletEditTexts[i].text = mSignatoriesList[i]
                signatoryLayouts[i].visibility = View.VISIBLE
            }

            for (i in length until SIGNATORIES_CAPACITY)
            {
                bulletEditTexts[i].text = getString(R.string.neutral)
                signatoryLayouts[i].visibility = View.GONE
            }
        }
        else
        {
            for (it in signatoryLayouts)
            {
                it.visibility = View.GONE
            }
        }
    }

    private fun addSignatory(signatory:String)
    {
        when {

            mSignatoriesList.size >= 10 ->
            {
                showSnackBar(null, getString(R.string.max_10_signatories))
            }

            mSignatoriesList.contains(signatory) ->
            {
                showSnackBar(null, getString(R.string.signatory_already_in_list))
            }

            else ->
            {
                mSignatoriesList.add(signatory)
                refreshSignatories()
            }
        }
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

        if (isAddButtonValid() && mPayload != null)
        {
            var postParams = JSONObject()
            postParams.put("src", mnemonicsData.pkh)
            postParams.put("src_pk", mnemonicsData.pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            //dstObject.put("dst", pkhDst)

            val mutezAmount = (mDepositAmount*1000000.0).roundToLong()
            dstObject.put("balance", mutezAmount)

            dstObject.put("fee", mFees)

            val ecKeys = retrieveECKeys()
            val tz3 = CryptoUtils.generatePkhTz3(ecKeys)

            dstObject.put("tz3", tz3)
            //dstObject.put("limit", mLimitAmount*1000000L)

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

            if (isOriginateSlcPayloadValid(mPayload!!, postParams))
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mPayload!!.hexToByteArray()

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

                mFinalizeLoading = true
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

            showSnackBar(error, null)
        }
        else
        {
            // the finish call is made already
        }
    }


    private fun onInitDelegateLoadComplete(error:VolleyError?)
    {
        mInitLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            //TODO should cancel the payloadTransfer too
            mPayload = null

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
        dstObject.put("credit", (mDepositAmount*1000000L).roundToLong().toString())

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
        //firstParamArgsFirstParamArgsSecondAndThirdParamsArgsStorageOne.put("int", (mLimitAmount*1000000L).toString())

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

            mPayload = answer.getString("result")
            mFees = answer.getLong("total_fee")

            // get back the object and

            val dstsArray = postParams["dsts"] as JSONArray
            val dstObj = dstsArray[0] as JSONObject

            dstObj.put("fee", mFees.toString())
            dstsArray.put(0, dstObj)

            postParams.put("dsts", dstsArray)

            // we use this call to ask for payload and fees
            if (mPayload != null && mFees != -1L)
            {
                onInitDelegateLoadComplete(null)

                val feeInTez = mFees.toDouble()/1000000.0
                fee_limit_edittext.setText(feeInTez.toString())

                validateAddButton(isInputDataValid() && isFeeValid())

                if (isInputDataValid() && isFeeValid())
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
            Log.i("mDelegatePayload", ""+mPayload)
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
        mInitLoading = true
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

        mFees = -1

        mPayload = null
    }

    private fun showSnackBar(volleyError:VolleyError?, errorMessage:String?)
    {
        var error: String =
                volleyError?.toString()
                        ?: errorMessage
                                ?: getString(R.string.generic_error)

        val snackbar = Snackbar.make(findViewById(R.id.content), error, Snackbar.LENGTH_LONG)
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

            if (
                    (i == R.id.amount_limit_edittext && !isDepositAmountEquals(editable))
                    ||
                    (i == R.id.threshold_edittext && !isThresholdAmountEquals(editable))
            )
            {
                if (i == R.id.amount_limit_edittext )
                {
                    putAmountInRed(false)
                }

                if (i == R.id.threshold_edittext )
                {
                    putThresholdInRed(false)
                }

                //TODO text changed
                //TODO load again but only if we don't have any same forged data.

                //val amount = java.lang.Double.parseDouble()

                //TODO check if it's already

                if (isInputDataValid())
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
            else if (i != R.id.amount_limit_edittext && i != R.id.threshold_edittext)
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }
        }

        private fun isDepositAmountEquals(editable: Editable):Boolean
        {
            val isAmountEquals = false

            if (!TextUtils.isEmpty(editable))
            {
                try
                {
                    val amount = editable.toString().toDouble()
                    if (amount != -1.0 && amount == mDepositAmount)
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

        private fun isThresholdAmountEquals(editable: Editable):Boolean
        {
            val isThresholdAmountEquals = false

            if (editable != null && !TextUtils.isEmpty(editable))
            {
                try
                {
                    val limit = editable.toString().toInt()
                    if (limit != 0 && limit == mThreshold)
                    {
                        return true
                    }
                }
                catch (e: NumberFormatException)
                {
                    return false
                }
            }
            return isThresholdAmountEquals
        }
    }

    private fun isInputDataValid(): Boolean
    {
        return isDepositAmountValid()
                && isThresholdValid()
    }

    private fun isFeeValid():Boolean
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
                    mFees = longTransferFee.roundToLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mFees = -1
                return false
            }
        }

        return isFeeValid
    }

    private fun isDepositAmountValid():Boolean
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
                    mDepositAmount = amount
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mDepositAmount = -1.0
                return false
            }
        }
        else
        {
            mDepositAmount = -1.0
        }

        return isAmountValid
    }

    private fun isThresholdValid():Boolean
    {
        val isThresholdValid = false

        if (threshold_edittext.text != null && !TextUtils.isEmpty(threshold_edittext.text))
        {
            try
            {
                val threshold = threshold_edittext.text.toString().toInt()

                if (threshold in 1..10 && threshold <= mSignatoriesList.size)
                {
                    mThreshold = threshold
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mThreshold = 0
                return false
            }

        }
        else
        {
            mThreshold = 0
        }

        return isThresholdValid
    }

    override fun onResume()
    {
        super.onResume()

        putEverythingInRed()

        //TODO show the right list of signatories
        refreshSignatories()


        if (isInputDataValid() && isFeeValid())
        {
            validateAddButton(true)
        }

        //TODO we got to keep in mind there's an id already.
        if (mInitLoading)
        {
            startInitOriginateContractLoading()
        }
        else
        {
            onInitDelegateLoadComplete(null)

            if (mFinalizeLoading)
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
        this.putThresholdInRed(true)
    }

    private fun isAddButtonValid(): Boolean
    {
        return mPayload != null
                && isFeeValid()
                && isInputDataValid()
    }

// put everything in RED

    private fun putAmountInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isDepositAmountValid()

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

    private fun putThresholdInRed(red: Boolean)
    {
        val color: Int

        val thresholdValid = isThresholdValid()

        if (red && !thresholdValid)
        {
            color = R.color.tz_error
            //update_storage_button.text = getString(R.string.delegate_format, "")
        }
        else
        {
            color = R.color.tz_accent

            if (thresholdValid)
            {
                //val amount = delegate_transfer_edittext.text.toString()
                //this.setTextPayButton()
            }
            else
            {
                //update_storage_button.text = getString(R.string.delegate_format, "")
            }
        }

        threshold_edittext.setTextColor(ContextCompat.getColor(this, color))
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AddressBookActivity.TRANSFER_SELECT_REQUEST_CODE)
        {
            if (data != null && data.hasExtra(Account.TAG) && resultCode == R.id.multisig_address_selection_succeed)
            {
                val accountBundle = data.getBundleExtra(Account.TAG)
                val account = Address.fromBundle(accountBundle)
                val tz = account.pubKeyHash
                addSignatory(tz)
            }
        }

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
            mInitLoading = false
            mFinalizeLoading = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(DELEGATE_INIT_TAG, mInitLoading)
        outState.putBoolean(DELEGATE_FINALIZE_TAG, mFinalizeLoading)

        outState.putString(DELEGATE_PAYLOAD_KEY, mPayload)

        outState.putDouble(DELEGATE_AMOUNT_KEY, mDepositAmount)

        outState.putInt(THRESHOLD_KEY, mThreshold)

        outState.putLong(DELEGATE_FEE_KEY, mFees)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)

        outState.putStringArrayList(SIGNATORIES_LIST_KEY, mSignatoriesList)
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests(false)
    }

    override fun onPublicKeyClicked(publicKey:String)
    {
        addSignatory(publicKey)
    }
}
