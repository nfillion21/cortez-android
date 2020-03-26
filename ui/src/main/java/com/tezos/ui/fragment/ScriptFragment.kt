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


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.*
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.text.style.BulletSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.core.crypto.Base58
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.crypto.KeyPair
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.*
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.utils.*
import kotlinx.android.synthetic.main.fragment_delegation.*
import kotlinx.android.synthetic.main.fragment_script.*
import kotlinx.android.synthetic.main.fragment_script.nav_progress
import kotlinx.android.synthetic.main.multisig_form_card_info_signatories.*
import kotlinx.android.synthetic.main.update_multisig_form_card.*
import kotlinx.android.synthetic.main.update_storage_form_card.*
import kotlinx.android.synthetic.main.update_storage_form_card.gas_textview
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.interfaces.ECPublicKey
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.roundToLong

class ScriptFragment : Fragment(), AddSignatoryDialogFragment.OnSignatorySelectorListener
{
    private var mCallback: OnUpdateScriptListener? = null

    private var mInitUpdateMultisigStorageLoading:Boolean = false

    private var mInitUpdateStorageLoading:Boolean = false
    private var mFinalizeDelegateLoading:Boolean = false

    private var mFinalizeMultisigLoading:Boolean = false

    private var mSecureHashBalanceLoading:Boolean = false

    private var mStorageInfoLoading:Boolean = false


    private var mUpdateMultisigStoragePayload:String? = null
    private var mUpdateMultisigStorageFees:Long = -1L

    private var mUpdateStoragePayload:String? = null
    private var mUpdateStorageFees:Long = -1L

    private var mMultisigFees:Long = -1

    private var mSignatoriesList:ArrayList<String> = ArrayList(SIGNATORIES_CAPACITY)

    private var mThreshold:Long = 0

    private var mClickCalculate:Boolean = false

    private var mStorage:String? = null
    private var mWalletEnabled:Boolean = false

    private var mSpendingLimitEditMode:Boolean = false

    private var mMultisigEditMode:Boolean = false

    private var mSpendingLimitAmount:Long = -1L

    private var mSecureHashBalance:Long = -1L

    private var mSig:String? = null

    companion object
    {
        const val CONTRACT_PUBLIC_KEY = "contract_public_key"


        private const val UPDATE_MULTISIG_STORAGE_INIT_TAG = "update_multisig_storage_init"

        private const val UPDATE_STORAGE_INIT_TAG = "update_storage_init"
        private const val DELEGATE_FINALIZE_TAG = "delegate_finalize"

        private const val MULTISIG_FINALIZE_TAG = "multisig_finalize"

        private const val CONTRACT_SCRIPT_INFO_TAG = "contract_script_info"

        private const val LOAD_SECURE_HASH_BALANCE_TAG = "load_secure_hash_balance"

        private const val DELEGATE_PAYLOAD_KEY = "transfer_payload_key"

        private const val MULTISIG_PAYLOAD_KEY = "multisig_payload_key"

        private const val DELEGATE_FEE_KEY = "delegate_fee_key"

        private const val FEES_CALCULATE_KEY = "calculate_fee_key"

        private const val WALLET_AVAILABLE_KEY = "wallet_available_key"

        private const val STORAGE_DATA_KEY = "storage_data_key"

        private const val SPENDING_AMOUNT_KEY = "spending_amount_key"

        private const val THRESHOLD_KEY = "threshold_key"

        private const val EDIT_SPENDING_LIMIT_MODE_KEY = "edit_spending_limit_mode_key"

        private const val EDIT_MULTISIG_MODE_KEY = "edit_multisig_mode_key"

        private const val BALANCE_LONG_KEY = "balance_long_key"

        private const val CONTRACT_SIG_KEY = "contract_sig_key"

        private const val SIGNATORIES_CAPACITY = 10

        private const val SIGNATORIES_LIST_KEY = "signatories_list"

        @JvmStatic
        fun newInstance(theme: CustomTheme, contract: String?) =
                ScriptFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(CONTRACT_PUBLIC_KEY, contract)
                    }
                }
    }

    interface OnUpdateScriptListener
    {
        fun showSnackBar(res:String, color:Int, textColor:Int)
        fun finish(res:Int)

        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean
        fun saveFingerprintAllowed(useInFuture: Boolean)
    }

    override fun onAttach(context: Context?)
    {
        super.onAttach(context)

        try
        {
            mCallback = context as OnUpdateScriptListener?
        }
        catch (e: ClassCastException)
        {
            throw ClassCastException(context!!.toString() + " must implement OnUpdateScriptListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_script, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        validateConfirmEditionButton(isSpendingLimitInputDataValid() && isUpdateStorageFeeValid())

        validateConfirmEditionMultisigButton(isMultisigInputDataValid() && isMultisigFeeValid())

        update_storage_button_layout.setOnClickListener {
            onUpdateSlcStorageClick()
        }

        update_multisig_button_layout.setOnClickListener {
            onUpdateMultisigStorageClick()
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
                                refreshSignatories(editMode = true)
                            }

                            DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                        }
                    }

                    val builder = AlertDialog.Builder(activity!!)
                    builder.setTitle(R.string.alert_remove_own_signatory_title)
                            .setMessage(String.format(getString(R.string.alert_remove_own_signatory_body), pkhtz1()))

                            .setNegativeButton(android.R.string.cancel, dialogClickListener)
                            .setPositiveButton(android.R.string.yes, dialogClickListener)
                            .setCancelable(false)
                            .show()
                }
                else
                {
                    mSignatoriesList.removeAt(i)
                    refreshSignatories(editMode = true)
                }
            }
        }

        send_cents_button.setOnClickListener {

            val seed = Storage(context!!).getMnemonics()
            if (mSecureHashBalance <= 0 && seed.mnemonics.isEmpty())
            {
                mCallback?.showSnackBar(getString(R.string.no_mnemonics_send_cents), ContextCompat.getColor(activity!!, R.color.tz_accent), ContextCompat.getColor(context!!, R.color.tz_light))
            }
            else
            {
                arguments?.let {

                    val themeBundle = it.getBundle(CustomTheme.TAG)
                    val theme = CustomTheme.fromBundle(themeBundle)

                    val sendCentsFragment = SendCentsFragment.newInstance(pkh()!!, mSecureHashBalance > 0, mStorage!!, theme)
                    sendCentsFragment.show(activity!!.supportFragmentManager, SendCentsFragment.TAG)
                }
            }
        }

        fab_edit_storage.setOnClickListener {

            val hasMnemonics = Storage(activity!!).hasMnemonics()
            if (hasMnemonics)
            {
                val seed = Storage(activity!!).getMnemonics()

                if (seed.mnemonics.isEmpty())
                {
                    mCallback?.showSnackBar(getString(R.string.no_mnemonics_script), ContextCompat.getColor(activity!!, R.color.tz_accent), ContextCompat.getColor(context!!, R.color.tz_light))
                }
                else
                {
                    animateFabEditMode(true)
                }
            }
        }

        fab_edit_multisig_storage.setOnClickListener {

            val hasMnemonics = Storage(activity!!).hasMnemonics()
            if (hasMnemonics)
            {
                val seed = Storage(activity!!).getMnemonics()

                if (seed.mnemonics.isEmpty())
                {
                    mCallback?.showSnackBar(getString(R.string.no_mnemonics_script), ContextCompat.getColor(activity!!, R.color.tz_accent), ContextCompat.getColor(context!!, R.color.tz_light))
                }
                else
                {
                    animateFabMultisigEditMode(editMode = true)
                }
            }
        }

        fab_undo_storage.setOnClickListener {
            animateFabEditMode(false)
        }

        fab_undo_multisig_storage.setOnClickListener {
            animateFabMultisigEditMode(false)
        }

        swipe_refresh_script_layout.setOnRefreshListener {
            startStorageInfoLoading()
        }

        add_signatory_button.setOnClickListener {
            /*
            AddressBookActivity.start(
                    this,
                    theme,
                    AddressBookActivity.Selection.SelectionAddresses)
            */
            var dialog = AddSignatoryDialogFragment.newInstance()
            dialog.setTargetFragment(this, 1)
            dialog.show(activity?.supportFragmentManager, "AddSignatory")
        }

        daily_spending_limit_edittext.addTextChangedListener(GenericTextWatcher(daily_spending_limit_edittext))
        //daily_spending_limit_edittext.onFocusChangeListener = focusChangeListener()

        val focusListener = this.focusChangeListener()
        threshold_edittext.addTextChangedListener(GenericTextWatcher(threshold_edittext))
        threshold_edittext.onFocusChangeListener = focusListener

        if (savedInstanceState != null)
        {
            mUpdateStoragePayload = savedInstanceState.getString(DELEGATE_PAYLOAD_KEY, null)

            mUpdateMultisigStoragePayload = savedInstanceState.getString(MULTISIG_PAYLOAD_KEY, null)

            mInitUpdateMultisigStorageLoading = savedInstanceState.getBoolean(UPDATE_MULTISIG_STORAGE_INIT_TAG)

            mInitUpdateStorageLoading = savedInstanceState.getBoolean(UPDATE_STORAGE_INIT_TAG)
            mFinalizeDelegateLoading = savedInstanceState.getBoolean(DELEGATE_FINALIZE_TAG)

            mFinalizeMultisigLoading = savedInstanceState.getBoolean(MULTISIG_FINALIZE_TAG)

            mSecureHashBalanceLoading = savedInstanceState.getBoolean(LOAD_SECURE_HASH_BALANCE_TAG)

            mStorageInfoLoading = savedInstanceState.getBoolean(CONTRACT_SCRIPT_INFO_TAG)

            mUpdateStorageFees = savedInstanceState.getLong(DELEGATE_FEE_KEY, -1)

            mClickCalculate = savedInstanceState.getBoolean(FEES_CALCULATE_KEY, false)

            mWalletEnabled = savedInstanceState.getBoolean(WALLET_AVAILABLE_KEY, false)

            mStorage = savedInstanceState.getString(STORAGE_DATA_KEY, null)

            mSpendingLimitAmount = savedInstanceState.getLong(SPENDING_AMOUNT_KEY, -1L)

            mThreshold = savedInstanceState.getLong(THRESHOLD_KEY, -1L)

            mSpendingLimitEditMode = savedInstanceState.getBoolean(EDIT_SPENDING_LIMIT_MODE_KEY, false)

            mMultisigEditMode = savedInstanceState.getBoolean(EDIT_MULTISIG_MODE_KEY, false)

            mSecureHashBalance = savedInstanceState.getLong(BALANCE_LONG_KEY, -1)

            mSig = savedInstanceState.getString(CONTRACT_SIG_KEY, null)

            mSignatoriesList = savedInstanceState.getStringArrayList(SIGNATORIES_LIST_KEY)

            if (mStorageInfoLoading)
            {
                refreshTextUnderDelegation(false)
                mWalletEnabled = true
                startStorageInfoLoading()
            }
            else
            {
                onStorageInfoComplete(false)

                if (mSecureHashBalanceLoading)
                {
                    startGetRequestBalance()
                }
                else
                {
                    onBalanceLoadComplete()

                    //TODO we got to keep in mind there's an id already.
                    if (mInitUpdateStorageLoading)
                    {
                        startInitUpdateStorageLoading()
                    }
                    else
                    {
                        onInitEditLoadComplete(null)

                        if (mFinalizeDelegateLoading)
                        {
                            startFinalizeUpdateStorageLoading()
                        }
                        else
                        {
                            onFinalizeLoadComplete(null)

                            if (mInitUpdateMultisigStorageLoading)
                            {
                                startInitUpdateMultisigStorageLoading()
                            }
                            else
                            {
                                onInitEditLoadMultisigComplete(error = null)

                                if (mFinalizeMultisigLoading)
                                {
                                    startFinalizeUpdateMultisigStorageLoading()
                                }
                                else
                                {
                                    onFinalizeLoadComplete(null)
                                }
                            }
                        }
                    }
                }
            }
        }
        else
        {
            mWalletEnabled = true
            startStorageInfoLoading()

            val ss = SpannableString(" ")
            ss.setSpan(BulletSpan(8, ContextCompat.getColor(activity!!, R.color.colorAccent)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

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
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (mSpendingLimitEditMode)
        {
            putEverythingInRed()

            if (isUpdateStorageFeeValid())
            {
                if (isSpendingLimitInputDataValid())
                {
                    validateConfirmEditionButton(true)
                }
            }
        }
        else if (mMultisigEditMode)
        {
            putEverythingInRed()

            if (isMultisigFeeValid())
            {
                if (isMultisigInputDataValid())
                {
                    validateConfirmEditionMultisigButton(true)
                }
            }
        }


        if (!mWalletEnabled)
        {
            mWalletEnabled = true

            startStorageInfoLoading()
        }
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            when (v.id) {
                //R.id.public_address_edittext -> putTzAddressInRed(!hasFocus)
                R.id.threshold_edittext -> putThresholdInRed(!hasFocus)
                else -> throw UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }

    private fun pkh():String?
    {
        var pkh:String? = null

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            pkh = arguments!!.getString(CONTRACT_PUBLIC_KEY)
            if (pkh == null)
            {
                //should not happen
                //val seed = Storage(activity!!).getMnemonics()
                //pkh = seed.pkh
            }
        }

        return pkh
    }

    private fun pk():String?
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()
        return mnemonicsData.pk
    }

    private fun pkhtz1():String?
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()
        return mnemonicsData.pkh
    }

    private fun switchToEditMode(editMode:Boolean)
    {
        if (editMode)
        {
            val secureKeyHash = getStorageSecureKeyHash()
            if (!secureKeyHash.isNullOrEmpty())
            {
                val tz3 = retrieveTz3()
                if (tz3 != secureKeyHash)
                {
                    public_address_edittext.setText("")
                    public_address_edittext.isEnabled = true
                    public_address_edittext.isFocusable = false
                    public_address_edittext.isClickable = false
                    public_address_edittext.isLongClickable = false
                    public_address_edittext.hint = getString(R.string.click_for_p2pk)

                    public_address_edittext.setOnClickListener {
                        val ecKeys = retrieveECKeys()
                        val tz3 = CryptoUtils.generatePkhTz3(ecKeys)
                        public_address_edittext.setText(tz3)
                    }
                }
            }


            update_storage_button_relative_layout.visibility = View.VISIBLE

            gas_textview.visibility = View.VISIBLE
            gas_layout.visibility = View.VISIBLE


            limit_infos_layout.visibility = View.GONE

            redelegate_address_textview.setText(R.string.secure_enclave_generated)

            daily_spending_limit_textview.setText(R.string.daily_spending_limit_from_0_to_1k)

            daily_spending_limit_edittext.isEnabled = true
            daily_spending_limit_edittext.setText("")
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(daily_spending_limit_edittext, InputMethodManager.SHOW_IMPLICIT)

            send_cents_button.visibility = View.GONE

            secure_hash_balance_layout.visibility = View.GONE

            fab_edit_storage.hide()
            fab_undo_storage.show()
        }
        else
        {
            validateConfirmEditionButton(false)
            cancelRequests(true)
            transferLoading(false)
            putFeesToNegative()

            update_storage_button_relative_layout?.visibility = View.GONE

            gas_textview?.visibility = View.GONE
            gas_layout?.visibility = View.GONE

            limit_infos_layout.visibility = View.VISIBLE

            daily_spending_limit_textview.setText(R.string.daily_spending_limit)

            daily_spending_limit_edittext?.isEnabled = false

            redelegate_address_textview.setText(R.string.secure_enclave)


            if (mStorage != JSONObject(getString(R.string.default_storage)).toString())
            {
                val storageJSONObject = JSONObject(mStorage)

                val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

                // get securekey hash

                val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args") as JSONArray
                val secureKeyJSONObject = argsSecureKey[0] as JSONObject
                val secureKeyHash = DataExtractor.getStringFromField(secureKeyJSONObject, "string")

                public_address_edittext.setText(secureKeyHash)
                public_address_edittext.isFocusableInTouchMode = false
                public_address_edittext.hint = null
                public_address_edittext.isClickable = false
                public_address_edittext.isLongClickable = false
                public_address_edittext.isClickable = false
                public_address_edittext.isEnabled = true
                public_address_edittext.isFocusable = false

                // get daily spending limit

                val historyPayment = getHistoryPayment()
                daily_spending_limit_edittext?.setText(mutezToTez(historyPayment?.get(0) as Long))


                // 100 000 mutez == 0.1 tez
                if (mSecureHashBalance != -1L && mSecureHashBalance < 100000)
                {
                    if (isSecureKeyHashIdentical())
                    {
                        send_cents_button.visibility = View.VISIBLE
                    }
                    else
                    {
                        send_cents_button.visibility = View.GONE
                    }
                }
                else
                {
                    send_cents_button.visibility = View.GONE
                }

                putSpendingLimitInRed(false)
            }

            secure_hash_balance_layout.visibility = View.VISIBLE

            fab_undo_storage.hide()
            fab_edit_storage.show()
        }
    }

    private fun switchToMultisigEditMode(editMode:Boolean)
    {
        if (editMode)
        {
            threshold_edittext.setText("")
            threshold_edittext.isFocusableInTouchMode = true
            threshold_edittext.isEnabled = true
            threshold_edittext.isFocusable = true
            threshold_edittext.isClickable = true
            threshold_edittext.isLongClickable = true

            val threshold = getThreshold()
            threshold_edittext.hint = threshold

            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(threshold_edittext, InputMethodManager.SHOW_IMPLICIT)


            //TODO check if we are allowed to modify or we need to ask for a change.


            if (askingForSignatories())
            {
                update_multisig_button_relative_layout.visibility = View.GONE
                request_update_multisig_button_relative_layout.visibility = View.VISIBLE
            }
            else
            {
                update_multisig_button_relative_layout.visibility = View.VISIBLE
                request_update_multisig_button_relative_layout.visibility = View.GONE
            }

            gas_multisig_textview.visibility = View.VISIBLE
            gas_multisig_layout.visibility = View.VISIBLE

            add_signatory_button.visibility = View.VISIBLE

            refreshSignatories(editMode = editMode)

            fab_edit_multisig_storage.hide()
            fab_undo_multisig_storage.show()
        }
        else
        {
            validateConfirmEditionMultisigButton(false)

            cancelRequests(true)
            transferLoading(false)
            putFeesMultisigToNegative()

            update_multisig_button_relative_layout?.visibility = View.GONE
            request_update_multisig_button_relative_layout.visibility = View.GONE

            gas_multisig_layout.visibility = View.GONE
            gas_multisig_textview?.visibility = View.GONE

            val threshold = getThreshold()

            threshold_edittext.setText(threshold)
            threshold_edittext.isFocusableInTouchMode = false
            threshold_edittext.hint = null
            threshold_edittext.isClickable = false
            threshold_edittext.isLongClickable = false
            threshold_edittext.isClickable = false
            threshold_edittext.isEnabled = true
            threshold_edittext.isFocusable = false

            add_signatory_button.visibility = View.GONE

            refreshSignatories(editMode = editMode)

            // eventually put threshold in red
            /*
            putSpendingLimitInRed(false)
            */

            fab_undo_multisig_storage.hide()
            fab_edit_multisig_storage.show()
        }
    }

    private fun hideClearButtons(editMode:Boolean)
    {
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

        val signatoriesList =

                if (editMode)
                {
                    mSignatoriesList
                }
                else
                {
                    getSignatoriesList()
                }


        if (!signatoriesList.isNullOrEmpty() && editMode)
        {
            val length = signatoriesList!!.size

            for (i in 0 until length)
            {
                clearButtons[i].visibility = View.VISIBLE
            }

            for (i in length until SIGNATORIES_CAPACITY)
            {
                clearButtons[i].visibility = View.GONE
            }
        }
        else
        {
            for (it in clearButtons)
            {
                it.visibility = View.GONE
            }
        }
    }

    private fun refreshSignatories(editMode:Boolean)
    {
        hideClearButtons(editMode = editMode)

        if (!editMode)
        {
            mSignatoriesList = getSignatoriesList()
        }

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

        val signatoriesList =
                if (editMode)
                {
                    mSignatoriesList
                }
                else
                {
                    getSignatoriesList()
                }

        if (!signatoriesList.isNullOrEmpty())
        {
            val length = signatoriesList.size

            for (i in 0 until length)
            {
                bulletEditTexts[i].text = signatoriesList[i]
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

        if (editMode)
        {
            putThresholdInRed(false)

            if (isMultisigInputDataValid())
            {
                startInitUpdateMultisigStorageLoading()
            }
            else
            {
                validateConfirmEditionMultisigButton(false)

                cancelRequests(false)
                transferLoading(false)

                putFeesMultisigToNegative()
            }
        }

        /*
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
        */
    }

    private fun animateFabEditMode(editMode:Boolean)
    {
        mSpendingLimitEditMode = editMode

        if (editMode)
        {
            val textViewAnimator = ObjectAnimator.ofFloat(fab_edit_storage, View.SCALE_X, 0f)
            val textViewAnimator2 = ObjectAnimator.ofFloat(fab_edit_storage, View.SCALE_Y, 0f)
            val textViewAnimator3 = ObjectAnimator.ofFloat(fab_edit_storage, View.ALPHA, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.interpolator = FastOutSlowInInterpolator()
            animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
            animatorSet.addListener(object : AnimatorListenerAdapter()
            {
                override fun onAnimationEnd(animation: Animator)
                {
                    super.onAnimationEnd(animation)
                    switchToEditMode(editMode)
                }
            })
            animatorSet.start()
        }
        else
        {
            val textViewAnimator = ObjectAnimator.ofFloat(fab_undo_storage, View.SCALE_X, 0f)
            val textViewAnimator2 = ObjectAnimator.ofFloat(fab_undo_storage, View.SCALE_Y, 0f)
            val textViewAnimator3 = ObjectAnimator.ofFloat(fab_undo_storage, View.ALPHA, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.interpolator = FastOutSlowInInterpolator()
            animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
            animatorSet.addListener(object : AnimatorListenerAdapter()
            {
                override fun onAnimationEnd(animation: Animator)
                {
                    super.onAnimationEnd(animation)
                    switchToEditMode(editMode)
                }
            })
            animatorSet.start()
        }
    }


    private fun animateFabMultisigEditMode(editMode:Boolean)
    {
        mMultisigEditMode = editMode

        if (editMode)
        {
            val textViewAnimator = ObjectAnimator.ofFloat(fab_edit_multisig_storage, View.SCALE_X, 0f)
            val textViewAnimator2 = ObjectAnimator.ofFloat(fab_edit_multisig_storage, View.SCALE_Y, 0f)
            val textViewAnimator3 = ObjectAnimator.ofFloat(fab_edit_multisig_storage, View.ALPHA, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.interpolator = FastOutSlowInInterpolator()
            animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
            animatorSet.addListener(object : AnimatorListenerAdapter()
            {
                override fun onAnimationEnd(animation: Animator)
                {
                    super.onAnimationEnd(animation)
                    switchToMultisigEditMode(editMode)
                }
            })
            animatorSet.start()
        }
        else
        {
            val textViewAnimator = ObjectAnimator.ofFloat(fab_undo_multisig_storage, View.SCALE_X, 0f)
            val textViewAnimator2 = ObjectAnimator.ofFloat(fab_undo_multisig_storage, View.SCALE_Y, 0f)
            val textViewAnimator3 = ObjectAnimator.ofFloat(fab_undo_multisig_storage, View.ALPHA, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.interpolator = FastOutSlowInInterpolator()
            animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
            animatorSet.addListener(object : AnimatorListenerAdapter()
            {
                override fun onAnimationEnd(animation: Animator)
                {
                    super.onAnimationEnd(animation)
                    switchToMultisigEditMode(editMode)
                }
            })
            animatorSet.start()
        }
    }

    private fun startStorageInfoLoading()
    {
        transferLoading(true)

        // validatePay cannot be valid if there is no fees
        validateConfirmEditionButton(false)

        //swipe_refresh_script_layout?.isEnabled = false

        startGetRequestLoadContractInfo()
    }

    private fun startInitUpdateStorageLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesToNegative()

        // validatePay cannot be valid if there is no fees
        validateConfirmEditionButton(false)

        startPostRequestLoadInitUpdateStorage()
    }

    private fun startInitUpdateMultisigStorageLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        putFeesMultisigToNegative()

        // validatePay cannot be valid if there is no fees
        validateConfirmEditionMultisigButton(false)


        // at this point, we will update the good request.

        if (askingForSignatories())
        {
            startPostRequestLoadInitRequestUpdateStorage()
        }
        else
        {
            startPostRequestLoadInitUpdateMultisigStorage()
        }
    }

    private fun startFinalizeUpdateStorageLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        startPostRequestLoadFinalizeUpdateStorage()
    }

    private fun startFinalizeUpdateMultisigStorageLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        startPostRequestLoadFinalizeUpdateMultisigStorage()
    }

    // volley
    private fun startGetRequestLoadContractInfo()
    {
        cancelRequests(true)

        mStorageInfoLoading = true

        loading_textview.setText(R.string.loading_contract_info)

        nav_progress.visibility = View.VISIBLE

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.contract_storage_url), pkh)

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<JSONObject>
            {

                //prevents from async crashes
                if (swipe_refresh_script_layout != null)
                {
                    addContractInfoFromJSON(it)
                    onStorageInfoComplete(true)

                    //TODO control more precisely the three different contracts

                    if (getStorageSecureKeyHash() != null)
                    {
                        //TODO I don't need to forge a transfer for now
                        //TODO hide the whole thing

                        //I need the right data inputs before.
                        //startInitRemoveDelegateLoading()

                        validateConfirmEditionButton(isSpendingLimitInputDataValid() && isUpdateStorageFeeValid())

                        startGetRequestBalance()
                    }
                    else if (getThreshold() != null)
                    {
                        // here I need to check the user is notary or signatory only.
                        startGetRequestManagerKey()
                    }
                    else
                    {
                        val mnemonicsData = Storage(activity!!).getMnemonics()
                        val defaultContract = JSONObject().put("string", mnemonicsData.pkh)
                        val isDefaultContract = mStorage.toString() == defaultContract.toString()

                        if (isDefaultContract)
                        {
                        }
                    }
                }
            },
                    Response.ErrorListener {

                        if (swipe_refresh_script_layout != null)
                        {
                            val response = it.networkResponse?.statusCode
                            if (response == 404)
                            {
                                //TODO this doesn't exist anymore
                                mStorage = JSONObject(getString(R.string.default_storage)).toString()
                            }
                            else
                            {
                                // 404 happens when there is no storage in this KT1
                                showSnackBar(it, null, ContextCompat.getColor(activity!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
                            }

                            onStorageInfoComplete(false)
                        }
                    })

            jsonArrayRequest.tag = CONTRACT_SCRIPT_INFO_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    private fun startGetRequestBalance()
    {
        //it's the same key, we can check for the tz3 balance

        cancelRequests(true)

        mSecureHashBalanceLoading = true

        //TODO handle loading
        /*
        mEmptyLoadingOperationsTextView?.setText(R.string.loading_list_operations)
        mEmptyLoadingBalanceTextview?.setText(R.string.loading_balance)

        mNavProgressBalance?.visibility = View.VISIBLE
        */

        secure_hash_progress.visibility = View.VISIBLE

        val url = String.format(getString(R.string.balance_url), getStorageSecureKeyHash())

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->

                    if (swipe_refresh_script_layout != null)
                    {
                        val balance = response.replace("[^0-9]".toRegex(), "")

                        mSecureHashBalance = balance.toLong()

                        // refresh UI

                        onBalanceLoadComplete()

                        /*
                        mSecureHashBalance = balance?.toDouble()/1000000
                        if (mSecureHashBalance != null)
                        {
                            animateBalance(mSecureHashBalance)
                        }

                        onBalanceLoadComplete(true)
                        startGetRequestLoadOperations()
                        */
                    }
                },
                Response.ErrorListener
                {
                    if (swipe_refresh_script_layout != null)
                    {
                        onBalanceLoadComplete()
                        showSnackBar(it, null, ContextCompat.getColor(activity!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
                    }
                })

        stringRequest.tag = LOAD_SECURE_HASH_BALANCE_TAG
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(stringRequest)

    }

    // volley
    private fun startGetRequestManagerKey()
    {
        cancelRequests(resetBooleans = true)

        val url = String.format(getString(R.string.manager_key_url), pkh())

        // Request a string response from the provided URL.
        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
        {
            if (swipe_refresh_script_layout != null)
            {
                val managerKey = it.getJSONObject(0)["manager"] as String
                if (managerKey == pkhtz1())
                {
                    val k = "ok"
                    val k2 = "ok"
                }
                else
                {
                    val k = "ok"
                    val k2 = "ok"

                    fdkjg
                    // dans ces cas-là il faut afficher le texte qui dit qu'on est signataire mais on ne peut pas faire de modification


                }

                //addContractAddressesFromJSON(it, pkh)

                //reloadList()
                //onDelegatedAddressesComplete(true)

                //startGetRequestLoadMultisigAsSignatoryContracts()
            }
        },
                Response.ErrorListener {

                    if (swipe_refresh_script_layout != null)
                    {
                        //onDelegatedAddressesComplete(false)
                        showSnackBar(it, null, ContextCompat.getColor(activity!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
                    }
                })

        //jsonArrayRequest.tag = ContractsFragment.LOAD_DELEGATED_ADDRESSES_TAG
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
    }

    private fun onBalanceLoadComplete()
    {
        mSecureHashBalanceLoading = false
        secure_hash_progress.visibility = View.INVISIBLE
        secure_hash_balance_textview.text =

                if (mSecureHashBalance != -1L)
                {
                    if (isSecureKeyHashIdentical())
                    {
                        "Balance : " + mutezToTez(mSecureHashBalance) + " " + getString(R.string.tez) + "." + "\nMaintain this balance between about 0.1 and 0.5 ꜩ."
                    }
                    else
                    {
                        "Balance : " + mutezToTez(mSecureHashBalance) + " " + getString(R.string.tez) + "."
                    }
                    //"\nThere is enough tez to make transfers from this contract."

                }
                else
                {
                    getString(R.string.neutral)
                }

        // 100 000 mutez == 0.1 tez
        if (mSecureHashBalance != -1L && mSecureHashBalance < 100000)
        {
            if (isSecureKeyHashIdentical())
            {
                warning_empty_secure_key_info.visibility = View.VISIBLE
                send_cents_button.visibility = View.VISIBLE
            }
            else
            {
                send_cents_button.visibility = View.GONE
                warning_empty_secure_key_info.visibility = View.GONE
            }
        }
        else
        {
            warning_empty_secure_key_info.visibility = View.GONE
            send_cents_button.visibility = View.GONE
        }
    }

    private fun addContractInfoFromJSON(answer: JSONObject)
    {
        if (answer.length() > 0)
        {
            mStorage = answer.toString()
        }
    }

    private fun onStorageInfoComplete(animating:Boolean)
    {
        mStorageInfoLoading = false
        nav_progress?.visibility = View.GONE

        //TODO handle the swipe refresh
        swipe_refresh_script_layout?.isEnabled = true
        swipe_refresh_script_layout?.isRefreshing = false

        refreshTextUnderDelegation(animating)
    }

    private fun refreshTextUnderDelegation(animating:Boolean)
    {
        //this method handles the data and loading texts

        if (mStorage != null)
        {
            //check the JSON storage
            val mnemonicsData = Storage(activity!!).getMnemonics()
            val defaultContract = JSONObject().put("string", mnemonicsData.pkh)
            val isDefaultContract = mStorage.toString() == defaultContract.toString()

            if (isDefaultContract)
            {
                //This is a default contract

                update_storage_form_card?.visibility = View.GONE

                public_address_layout?.visibility = View.VISIBLE

                update_storage_button_layout?.visibility = View.GONE

                storage_info_textview?.visibility = View.VISIBLE
                storage_info_textview?.text = getString(R.string.no_script_info)

                //TODO show everything related to the removing
            }
            else if (getStorageSecureKeyHash() != null)
            {
                //DAILY SPENDING LIMIT

                val secureKeyHash = getStorageSecureKeyHash()

                val listStorageData = getHistoryPayment()

                val dailySpendingLimit = listStorageData[0] as Long
                val dailySpendingLimitInTez = mutezToTez(dailySpendingLimit)
                daily_spending_limit_edittext?.setText(dailySpendingLimitInTez)

                //remaining_spending_limit.text = "You can still spend \$ " + dailySpendingLimitInTez + "on the \$ " + mutezToTez(listStorageData[0] as Long) + "limit of your SLC contract"

                val remainingDailySpendingLimit = listStorageData[1] as Long
                val remainingDailySpendingLimitInTez = mutezToTez(remainingDailySpendingLimit)
                remaining_daily_spending_limit_edittext.setText(remainingDailySpendingLimitInTez)

                remaining_time_daily_spending_limit_textview.text = String.format(getString(R.string.remaining_time_daily_spending_limit), dailySpendingLimitInTez)


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                {
                    view_timer.isCountDown = true
                    view_timer.base = SystemClock.elapsedRealtime() + (listStorageData[3] as Long)*1000L
                    if (listStorageData[3] as Long == 0L)
                    {
                        view_timer.stop()
                        view_timer.text = getString(R.string.spending_limit_reset)
                    }
                    else
                    {
                        view_timer.setOnChronometerTickListener {

                            if (it.base - SystemClock.elapsedRealtime() <= 0L)
                            {
                                view_timer.stop()
                                view_timer.text = getString(R.string.spending_limit_reset)

                                remaining_daily_spending_limit_edittext.setText(dailySpendingLimitInTez)
                            }
                        }
                        view_timer.start()
                    }
                }
                else
                {
                    remaining_time_daily_spending_limit_textview.visibility = View.GONE
                    view_timer.visibility = View.GONE
                }


                update_storage_form_card?.visibility = View.VISIBLE

                public_address_layout?.visibility = View.VISIBLE

                public_address_edittext?.setText(secureKeyHash)

                update_storage_button_layout?.visibility = View.VISIBLE

                storage_info_textview?.visibility = View.VISIBLE
                storage_info_textview?.text = getString(R.string.contract_storage_info)

                switchToEditMode(mSpendingLimitEditMode)

                val tz3 = retrieveTz3()
                if (tz3 == null || tz3 != secureKeyHash)
                {
                    warning_p2pk_info?.visibility = View.VISIBLE
                }
                else
                {
                    warning_p2pk_info?.visibility = View.GONE

                    //TODO at this point, the secure enclave key is the good one. better check if there is enough fees to make a transfer.
                }
            }
            else if (getThreshold() != null)
            {
                // MULTISIG CONTRACT

                update_multisig_form_card.visibility = View.VISIBLE

                update_storage_form_card?.visibility = View.GONE


                update_storage_button_layout?.visibility = View.GONE

                storage_info_textview?.visibility = View.VISIBLE
                storage_info_textview?.text = getString(R.string.multisig_script_info)

                //need to check if our edpk is contained in the signatories
                //even if he's contained, we need to check the threshold
                var threshold = getThreshold()
                val numberAndSpotPair = getNumberAndSpot(mnemonicsData.pk)
                if (numberAndSpotPair.first != -1)
                {
                    // our edpk is in the list. great. it's fine if we have a threshold of 1.
                    if (threshold!!.toInt() == 1)
                    {
                        warning_signatory_info.visibility = View.GONE
                    }
                    else
                    {
                        // we are on the list but it needs another signatory
                        warning_signatory_textview.text = String.format(getString(R.string.warning_other_signatories_info), threshold.toInt()-1)
                        warning_signatory_info.visibility = View.VISIBLE
                    }
                }
                else
                {
                    warning_signatory_textview.text = String.format(getString(R.string.warning_not_signatory_info), threshold)
                    warning_signatory_info.visibility = View.VISIBLE
                }

                switchToMultisigEditMode(mMultisigEditMode)
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
    }

    // volley
    private fun startPostRequestLoadFinalizeUpdateStorage()
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isUpdateButtonValid() && mUpdateStoragePayload != null && mUpdateStorageFees != -1L)
        {
            val mnemonicsData = Storage(activity!!).getMnemonics()
            var postParams = JSONObject()

            postParams.put("src", mnemonicsData.pkh)

            //TODO it won't be pk with contract transfer
            postParams.put("src_pk", mnemonicsData.pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            dstObject.put("dst", pkh())

            dstObject.put("amount", 0.toLong())

            val mutezAmount = (mSpendingLimitAmount*1000000.0).roundToLong()
            dstObject.put("transfer_amount", mutezAmount)

            dstObject.put("fee", mUpdateStorageFees)

            dstObject.put("edsig", mSig)

            dstObject.put("tz3", retrieveTz3())

            dstObject.put("contract_type", "slc_update_storage")

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)

            if (isTransferPayloadValid(mUpdateStoragePayload!!, postParams))
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mUpdateStoragePayload!!.hexToByteArray()

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
                            if (swipe_refresh_script_layout != null)
                            {
                                //there's no need to do anything because we call finish()
                                onFinalizeLoadComplete(null)

                                mCallback?.finish(R.id.update_storage_succeed)
                            }
                        },
                        Response.ErrorListener
                        {
                            if (swipe_refresh_script_layout != null)
                            {
                                onFinalizeLoadComplete(it)
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

                stringRequest.tag = DELEGATE_FINALIZE_TAG

                mFinalizeDelegateLoading = true
                VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(stringRequest)
            }
            else
            {
                val volleyError = VolleyError(getString(R.string.generic_error))
                onFinalizeLoadComplete(volleyError)
            }
        }
        else
        {
            val volleyError = VolleyError(getString(R.string.generic_error))
            onFinalizeLoadComplete(volleyError)
        }
    }


    // volley
    private fun startPostRequestLoadFinalizeUpdateMultisigStorage()
    {
        val url = getString(R.string.transfer_injection_operation)

        if (isUpdateMultisigButtonValid())
        {
            val mnemonicsData = Storage(activity!!).getMnemonics()
            var postParams = JSONObject()

            /*
            postParams.put("src", mnemonicsData.pkh)

            //TODO it won't be pk with contract transfer
            postParams.put("src_pk", mnemonicsData.pk)

            var dstObjects = JSONArray()

            var dstObject = JSONObject()
            dstObject.put("dst", pkh())

            dstObject.put("amount", 0.toLong())

            val mutezAmount = (mSpendingLimitAmount*1000000.0).roundToLong()
            dstObject.put("transfer_amount", mutezAmount)

            dstObject.put("fee", mUpdateStorageFees)

            dstObject.put("edsig", mSig)

            dstObject.put("tz3", retrieveTz3())

            dstObject.put("contract_type", "slc_update_storage")

            dstObjects.put(dstObject)

            postParams.put("dsts", dstObjects)
            */

            if (/*isTransferPayloadValid(mUpdateMultisigStoragePayload!!, postParams)*/true)
            {
                val zeroThree = "0x03".hexToByteArray()

                val byteArrayThree = mUpdateMultisigStoragePayload!!.hexToByteArray()

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
                            if (swipe_refresh_script_layout != null)
                            {
                                //there's no need to do anything because we call finish()
                                onFinalizeLoadComplete(null)

                                mCallback?.finish(R.id.update_multisig_storage_succeed)
                            }
                        },
                        Response.ErrorListener
                        {
                            if (swipe_refresh_script_layout != null)
                            {
                                onFinalizeLoadComplete(it)
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

                stringRequest.tag = MULTISIG_FINALIZE_TAG

                mFinalizeMultisigLoading = true
                VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(stringRequest)
            }
            else
            {
                val volleyError = VolleyError(getString(R.string.generic_error))
                onFinalizeLoadComplete(volleyError)
            }
        }
        else
        {
            val volleyError = VolleyError(getString(R.string.generic_error))
            onFinalizeLoadComplete(volleyError)
        }
    }

    private fun onFinalizeLoadComplete(error: VolleyError?)
    {
        // everything is over, there's no call to make
        cancelRequests(true)

        if (error != null)
        {
            transferLoading(false)

            showSnackBar(error, null, ContextCompat.getColor(activity!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
        }
        else
        {
            // there is no finish
            transferLoading(false)
        }
    }

    private fun onInitEditLoadComplete(error:VolleyError?)
    {
        mInitUpdateStorageLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            mUpdateStoragePayload = null

            storage_fee_edittext.isEnabled = true
            storage_fee_edittext.isFocusable = false
            storage_fee_edittext.isClickable = false
            storage_fee_edittext.isLongClickable = false
            storage_fee_edittext.hint = getString(R.string.click_for_fees)

            storage_fee_edittext.setOnClickListener {
                startInitUpdateStorageLoading()
            }

            if(error != null)
            {
                //TODO handle the show snackbar
                showSnackBar(error, null, ContextCompat.getColor(activity!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
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

    private fun onInitEditLoadMultisigComplete(error:VolleyError?)
    {
        mInitUpdateMultisigStorageLoading = false

        if (error != null || mClickCalculate)
        {
            // stop the moulinette only if an error occurred
            transferLoading(false)
            cancelRequests(true)

            mUpdateMultisigStoragePayload = null

            fee_limit_edittext.isEnabled = true
            fee_limit_edittext.isFocusable = false
            fee_limit_edittext.isClickable = false
            fee_limit_edittext.isLongClickable = false
            fee_limit_edittext.hint = getString(R.string.click_for_fees)

            fee_limit_edittext.setOnClickListener {
                startInitUpdateMultisigStorageLoading()
            }

            if(error != null)
            {
                //TODO handle the show snackbar
                showSnackBar(error, null, ContextCompat.getColor(activity!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
            }
        }
        else
        {
            transferLoading(false)
            cancelRequests(true)
        }
    }

    private fun updateMnemonicsData(data: Storage.MnemonicsData, pk:String):String
    {
        with(Storage(activity!!)) {
            saveSeed(Storage.MnemonicsData(data.pkh, pk, data.mnemonics))
        }
        return pk
    }


    // volley
    private fun startPostRequestLoadInitUpdateMultisigStorage()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val pk = if (mnemonicsData.pk.isNullOrEmpty())
        {
            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
        }
        else
        {
            mnemonicsData.pk
        }

        val tz1 = mnemonicsData.pkh

        var postParams = JSONObject()
        postParams.put("src", tz1)
        postParams.put("src_pk", pk)

        var dstObject = JSONObject()
        dstObject.put("dst", pkh())
        dstObject.put("amount", "0")

        dstObject.put("entrypoint", "default")

        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
        val sk = CryptoUtils.generateSk(mnemonics, "")

        val dataVisitable = Primitive(
                Primitive.Name.Pair,
                arrayOf(
                        Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.chainID(getString(R.string.chain_ID)),
                                        Visitable.address(pkh()!!)
                                )
                        ),
                        Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.integer(getCounter()!!.toLong()),
                                        Primitive(Primitive.Name.Right,
                                                arrayOf(
                                                        Primitive(Primitive.Name.Right,
                                                                arrayOf(
                                                                        Primitive(Primitive.Name.Pair,
                                                                                arrayOf(
                                                                                        Visitable.integer(mThreshold),
                                                                                        VisitableSequence(mSignatoriesList.map { Visitable.publicKey(it) }.toTypedArray())
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        )

        val o = ByteArrayOutputStream()
        o.write(0x05)

        val dataPacker = Packer(o)
        dataVisitable.accept(dataPacker)

        val dataPack = (dataPacker.output as ByteArrayOutputStream).toByteArray()

        val signature = KeyPair.sign(sk, dataPack)
        val edsig = CryptoUtils.generateEDSig(signature)

        val spendingLimitFile = "multisig_update_storage.json"
        val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                .use {
                    it.readText()
                }

        val value = JSONObject(contract)

        val args = ((value["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

        val counter = args[0] as JSONObject
        counter.put("int", getCounter())


        val argsb = (((((args[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray

        val signatoriesCount = argsb[0] as JSONObject

        // TODO get the textfield as signatoryCount
        signatoriesCount.put("int", mThreshold.toString())

        // on recupere le signatory ici

        // JSONArray

        val signatories = argsb[1] as JSONArray
        signatories.remove(0)

        for (it in mSignatoriesList)
        {
            //val signatory = (argsb[1] as JSONArray)[0] as JSONObject

            var decodedValue = Base58.decode(it)
            var bytes = decodedValue.slice(4 until (decodedValue.size - 4)).toByteArray()
            bytes = byteArrayOf(0x00) + bytes

            val signatory = JSONObject()
            signatory.put("bytes", bytes.toNoPrefixHexString())

            signatories.put(signatory)
        }

        val sigs = (value["args"] as JSONArray)[1] as JSONArray
        sigs.remove(0)

        val numberAndSpot = getNumberAndSpot(pk)

        for (i in 0 until numberAndSpot.second)
        {
            val sigParam = JSONObject()

            if (i == numberAndSpot.first)
            {
                sigParam.put("prim", "Some")

                val argSig = JSONArray()
                val argSigStr = JSONObject()

                argSigStr.put("string", edsig)
                argSig.put(argSigStr)
                sigParam.put("args", argSig)
            }
            else
            {
                sigParam.put("prim", "None")
            }

            sigs.put(sigParam)
        }

        dstObject.put("parameters", value)

        var dstObjects = JSONArray()
        dstObjects.put(dstObject)

        postParams.put("dsts", dstObjects)

        //*
        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (swipe_refresh_script_layout != null)
            {
                mUpdateMultisigStoragePayload = answer.getString("result")
                mUpdateMultisigStorageFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mUpdateMultisigStoragePayload != null && mUpdateMultisigStorageFees != -1L && activity != null)
                {
                    //TODO handle this one, need to make one for multisig
                    onInitEditLoadMultisigComplete(null)

                    val feeInTez = mUpdateMultisigStorageFees?.toDouble()/1000000.0
                    fee_limit_edittext?.setText(feeInTez.toString())

                    validateConfirmEditionMultisigButton(isMultisigInputDataValid() && isMultisigFeeValid())
                }
                else
                {
                    val volleyError = VolleyError(getString(R.string.generic_error))
                    onInitEditLoadMultisigComplete(volleyError)
                    mClickCalculate = true

                    //the call failed
                }
            }

        }, Response.ErrorListener
        {
            if (swipe_refresh_script_layout != null)
            {
                onInitEditLoadMultisigComplete(it)

                mClickCalculate = true
                //Log.i("mTransferId", ""+mTransferId)
                //Log.i("mUpdateStoragePayload", ""+mUpdateStoragePayload)
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

        jsObjRequest.tag = UPDATE_MULTISIG_STORAGE_INIT_TAG
        mInitUpdateMultisigStorageLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)

        //*/
    }


    // volley
    private fun startPostRequestLoadInitUpdateStorage()
    {
        val mnemonicsData = Storage(activity!!).getMnemonics()

        val url = getString(R.string.transfer_forge)

        val pk = if (mnemonicsData.pk.isNullOrEmpty())
        {
            val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
            updateMnemonicsData(mnemonicsData, CryptoUtils.generatePk(mnemonics, ""))
        }
        else
        {
            mnemonicsData.pk
        }

        val tz1 = mnemonicsData.pkh

        var postParams = JSONObject()
        postParams.put("src", tz1)
        postParams.put("src_pk", pk)


        var dstObject = JSONObject()
        dstObject.put("dst", pkh())
        dstObject.put("amount", "0")

        dstObject.put("entrypoint", "appel_clef_maitresse")

        //TODO I need to insert a signature into parameters

        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)
        val sk = CryptoUtils.generateSk(mnemonics, "")

        val tz3 = retrieveTz3()

        val dataVisitable = Primitive(
                Primitive.Name.Left,
                arrayOf(
                        Primitive(
                                Primitive.Name.Pair,
                                arrayOf(
                                        Primitive(
                                                Primitive.Name.Pair,
                                                arrayOf(
                                                        Visitable.keyHash(tz3!!),
                                                        Primitive(
                                                                Primitive.Name.Pair,
                                                                arrayOf(
                                                                        Primitive(
                                                                                Primitive.Name.Pair,
                                                                                arrayOf(
                                                                                        Visitable.integer(mSpendingLimitAmount*1000000),
                                                                                        Visitable.integer(86400)
                                                                                )
                                                                        ),
                                                                        Primitive(
                                                                                Primitive.Name.Pair,
                                                                                arrayOf(
                                                                                        Visitable.sequenceOf(),
                                                                                        Visitable.sequenceOf()
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        ),
                                        Visitable.keyHash(tz1)
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
                        Visitable.address(pkh()!!),
                        Visitable.chainID(getString(R.string.chain_ID))
                )
        )

        val output = ByteArrayOutputStream()
        output.write(0x05)

        val p = Packer(output)
        addressAndChainVisitable.accept(p)

        val addressAndChainPack = (p.output as ByteArrayOutputStream).toByteArray()


        var saltVisitable:Visitable? = null
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

        val signature = KeyPair.sign(sk, dataPack + addressAndChainPack + saltPack)

        val edsig = CryptoUtils.generateEDSig(signature)


        val spendingLimitFile = "spending_limit_update_storage.json"
        val contract = context!!.assets.open(spendingLimitFile).bufferedReader()
                .use {
                    it.readText()
                }

        val value = JSONObject(contract)

        val args = value["args"] as JSONArray

        val sigPart = args[0] as JSONObject
        val argsSigPart = sigPart["args"] as JSONArray

        val pkArgsSigPart = argsSigPart[0] as JSONObject
        pkArgsSigPart.put("string", pk)

        val sigArgsSigPart = argsSigPart[1] as JSONObject
        sigArgsSigPart.put("string", edsig)

        mSig = edsig


        val keysPart = args[1] as JSONObject
        val argsKeyPart = keysPart["args"] as JSONArray
        val firstArgsKeyPart = argsKeyPart[0] as JSONObject
        val argsFirstArgsKeyPart = firstArgsKeyPart["args"] as JSONArray

        val argsTz3AndValues = (argsFirstArgsKeyPart[0] as JSONObject)["args"] as JSONArray

        val tz3Args = argsTz3AndValues[0] as JSONObject
        tz3Args.put("string", tz3)

        val valuesArgs = (((argsTz3AndValues[1] as JSONObject)["args"] as JSONArray)[0] as JSONObject)["args"] as JSONArray
        val spendingLimitOne = valuesArgs[0] as JSONObject
        spendingLimitOne.put("int", (mSpendingLimitAmount*1000000).toString())

        val spendingLimitTwo = valuesArgs[1] as JSONObject
        spendingLimitTwo.put("int", "86400")

        val masterKeyArgs = argsFirstArgsKeyPart[1] as JSONObject
        masterKeyArgs.put("string", tz1)

        dstObject.put("parameters", value)

        var dstObjects = JSONArray()
        dstObjects.put(dstObject)


//send 0.1 tz if this tz3 is a new one
        if (!isSecureKeyHashIdentical())
        {
            val dstCentsObject = JSONObject()
            dstCentsObject.put("dst", tz3)
            dstCentsObject.put("amount", "100000")

            dstObjects.put(dstCentsObject)
        }

        postParams.put("dsts", dstObjects)

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (swipe_refresh_script_layout != null)
            {
                mUpdateStoragePayload = answer.getString("result")
                mUpdateStorageFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mUpdateStoragePayload != null && mUpdateStorageFees != -1L && activity != null)
                {
                    onInitEditLoadComplete(null)

                    val feeInTez = mUpdateStorageFees?.toDouble()/1000000.0
                    storage_fee_edittext?.setText(feeInTez.toString())

                    validateConfirmEditionButton(isSpendingLimitInputDataValid() && isUpdateStorageFeeValid())

                    if (isSpendingLimitInputDataValid() && isUpdateStorageFeeValid())
                    {
                        validateConfirmEditionButton(true)
                    }
                    else
                    {
                        // should no happen
                        validateConfirmEditionButton(false)
                    }
                }
                else
                {
                    val volleyError = VolleyError(getString(R.string.generic_error))
                    onInitEditLoadComplete(volleyError)
                    mClickCalculate = true

                    //the call failed
                }
            }

        }, Response.ErrorListener
        {
            if (swipe_refresh_script_layout != null)
            {
                onInitEditLoadComplete(it)

                mClickCalculate = true
                //Log.i("mTransferId", ""+mTransferId)
                //Log.i("mUpdateStoragePayload", ""+mUpdateStoragePayload)
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

        jsObjRequest.tag = UPDATE_STORAGE_INIT_TAG
        mInitUpdateStorageLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
    }


    // volley
    private fun startPostRequestLoadInitRequestUpdateStorage()
    {
        //val mnemonicsData = Storage(activity!!).getMnemonics()
        //val url = String.format(getString(R.string.manager_key_url), mnemonicsData.pk)

        // TODO
        // this call will be made to Mezos to ask for a transaction instance.
        //

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.manager_key_url), pkh)

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            {
                if (swipe_refresh_script_layout != null)
                {
                    //addContractAddressesFromJSON(it, pkh)

                    //reloadList()
                    //onDelegatedAddressesComplete(true)

                    //startGetRequestLoadMultisigAsSignatoryContracts()
                }
            },
                    Response.ErrorListener {

                        if (swipe_refresh_script_layout != null)
                        {
                            //onDelegatedAddressesComplete(false)
                            //showSnackbarError(it)
                        }
                    })

            //jsonArrayRequest.tag = ContractsFragment.LOAD_DELEGATED_ADDRESSES_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }

        /*

        val jsObjRequest = object : JsonObjectRequest(Method.POST, url, postParams, Response.Listener<JSONObject>
        { answer ->

            //TODO check if the JSON is fine then launch the 2nd request
            if (swipe_refresh_script_layout != null)
            {
                mUpdateStoragePayload = answer.getString("result")
                mUpdateStorageFees = answer.getLong("total_fee")

                // we use this call to ask for payload and fees
                if (mUpdateStoragePayload != null && mUpdateStorageFees != -1L && activity != null)
                {
                    onInitEditLoadComplete(null)

                    val feeInTez = mUpdateStorageFees?.toDouble()/1000000.0
                    storage_fee_edittext?.setText(feeInTez.toString())

                    validateConfirmEditionButton(isSpendingLimitInputDataValid() && isUpdateStorageFeeValid())

                    if (isSpendingLimitInputDataValid() && isUpdateStorageFeeValid())
                    {
                        validateConfirmEditionButton(true)
                    }
                    else
                    {
                        // should no happen
                        validateConfirmEditionButton(false)
                    }
                }
                else
                {
                    val volleyError = VolleyError(getString(R.string.generic_error))
                    onInitEditLoadComplete(volleyError)
                    mClickCalculate = true

                    //the call failed
                }
            }

        }, Response.ErrorListener
        {
            if (swipe_refresh_script_layout != null)
            {
                onInitEditLoadComplete(it)

                mClickCalculate = true
                //Log.i("mTransferId", ""+mTransferId)
                //Log.i("mUpdateStoragePayload", ""+mUpdateStoragePayload)
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

        jsObjRequest.tag = UPDATE_STORAGE_INIT_TAG
        mInitUpdateStorageLoading = true
        VolleySingleton.getInstance(activity!!.applicationContext).addToRequestQueue(jsObjRequest)
        */
    }

    private fun getSalt():Int?
    {
//TODO check if the storage follows our pattern

        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

// get masterkey hash

            val argsMasterKey = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray
            val masterKeySaltJSONObject = argsMasterKey[1] as JSONObject

            val saltLeft = (masterKeySaltJSONObject["args"] as JSONArray)[0] as JSONObject
            val saltRight = (masterKeySaltJSONObject["args"] as JSONArray)[1] as JSONObject

            return DataExtractor.getStringFromField(saltLeft, "int").toInt()
        }

        return null
    }


    private fun transferLoading(loading:Boolean)
    {
// handle the visibility of bottom buttons

        if (loading)
        {
//update_storage_button_layout.visibility = View.GONE
//empty.visibility = View.VISIBLE
            nav_progress?.visibility = View.VISIBLE
        }
        else
        {
//update_storage_button_layout.visibility = View.VISIBLE
//empty.visibility = View.GONE
            nav_progress?.visibility = View.GONE
        }
    }

    private fun putFeesToNegative()
    {
        storage_fee_edittext?.setText("")

        mClickCalculate = false
        storage_fee_edittext?.isEnabled = false
        storage_fee_edittext?.hint = getString(R.string.neutral)

        mUpdateStorageFees = -1

        mUpdateStoragePayload = null
    }

    private fun putFeesMultisigToNegative()
    {
        fee_limit_edittext?.setText("")

        mClickCalculate = false
        fee_limit_edittext?.isEnabled = false
        fee_limit_edittext?.hint = getString(R.string.neutral)

        mMultisigFees = -1

        mUpdateMultisigStoragePayload = null
    }

    private fun showSnackBar(error:VolleyError?, message:String?, color:Int, textColor: Int)
    {
        if (error != null)
        {
//mCallback?.showSnackBar(error.toString(), ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))
            mCallback?.showSnackBar(error.toString(), color, textColor)

            loading_textview?.text = getString(R.string.generic_error)
        }
        else if (message != null)
        {
//mCallback?.showSnackBar(message, ContextCompat.getColor(context!!, android.R.color.holo_green_light), ContextCompat.getColor(context!!, R.color.tz_light))
            mCallback?.showSnackBar(message, color, textColor)
        }
    }

    private fun validateConfirmEditionButton(validate: Boolean)
    {
        if (activity != null)
        {
            //val themeBundle = arguments!!.getBundle(CustomTheme.TAG)
            //val theme = CustomTheme.fromBundle(themeBundle)
            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

            if (validate)
            {
                update_storage_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                update_storage_button_layout?.isEnabled = true
                update_storage_button_layout?.background = makeSelector(theme)

                val drawables = update_storage_button?.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                }
            }
            else
            {
                update_storage_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                update_storage_button_layout?.isEnabled = false

                val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
                update_storage_button_layout?.background = makeSelector(greyTheme)

                val drawables = update_storage_button?.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }
            }

        }
    }


    private fun validateConfirmEditionMultisigButton(validate: Boolean)
    {
        if (activity != null)
        {
            //val themeBundle = arguments!!.getBundle(CustomTheme.TAG)
            //val theme = CustomTheme.fromBundle(themeBundle)
            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

            if (validate)
            {
                update_multisig_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                update_multisig_button_layout?.isEnabled = true
                update_multisig_button_layout?.background = makeSelector(theme)

                val drawables = update_multisig_button?.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                }


                request_update_multisig_button?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                request_update_multisig_button_layout?.isEnabled = true
                request_update_multisig_button_layout?.background = makeSelector(theme)

                val drawablesRequest = request_update_multisig_button?.compoundDrawables
                if (drawablesRequest != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawablesRequest[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
                }

            }
            else
            {
                update_multisig_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                update_multisig_button_layout?.isEnabled = false

                val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
                update_multisig_button_layout?.background = makeSelector(greyTheme)

                val drawables = update_multisig_button?.compoundDrawables
                if (drawables != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawables[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }

                request_update_multisig_button?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                request_update_multisig_button_layout?.isEnabled = false

                request_update_multisig_button_layout?.background = makeSelector(greyTheme)

                val drawablesRequest = request_update_multisig_button?.compoundDrawables
                if (drawablesRequest != null)
                {
                    val wrapDrawable = DrawableCompat.wrap(drawablesRequest[0])
                    DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
                }
            }
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
            if (mSpendingLimitEditMode || mMultisigEditMode)
            {
                val i = v.id
                if (
                        i == R.id.daily_spending_limit_edittext && isSpendingLimitAmountDifferent(editable)
                        ||
                        i == R.id.threshold_edittext && !isThresholdAmountEquals(editable)
                //TODO check about the threshold edittext
                )
                {
                    if (i == R.id.daily_spending_limit_edittext)
                    {
                        putSpendingLimitInRed(false)

                        if (isSpendingLimitInputDataValid())
                        {
                            startInitUpdateStorageLoading()
                        }
                        else
                        {
                            validateConfirmEditionButton(false)

                            cancelRequests(false)
                            transferLoading(false)

                            putFeesToNegative()
                        }
                    }

                    if (i == R.id.threshold_edittext )
                    {
                        putThresholdInRed(false)

                        if (isMultisigInputDataValid())
                        {
                            startInitUpdateMultisigStorageLoading()
                        }
                        else
                        {
                            validateConfirmEditionMultisigButton(false)

                            cancelRequests(false)
                            transferLoading(false)

                            putFeesMultisigToNegative()
                        }
                    }

                    //TODO text changed
                    //TODO load again but only if we don't have any same forged data.

                }
                else if (i != R.id.daily_spending_limit_edittext && i != R.id.threshold_edittext)
                {
                    throw UnsupportedOperationException(
                            "OnClick has not been implemented for " + resources.getResourceName(v.id))
                }
            }
        }
    }

    private fun isSpendingLimitAmountDifferent(editable: Editable):Boolean
    {
        val isSpendingAmountDifferent = false

        if (!TextUtils.isEmpty(editable))
        {
            try
            {
                val amount = editable.toString().toLong()
                if (amount != -1L && amount != mSpendingLimitAmount)
                {
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                return false
            }
        }
        return isSpendingAmountDifferent
    }

    private fun isThresholdAmountEquals(editable: Editable):Boolean
    {
        val isThresholdAmountEquals = false

        if (editable != null && !TextUtils.isEmpty(editable))
        {
            try
            {
                val limit = editable.toString().toLong()
                if (limit != 0L && limit == mThreshold)
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

    private fun isSpendingLimitInputDataValid(): Boolean
    {
        return isP256AddressValid() && isSpendingLimitAmountValid()
    }

    private fun isMultisigInputDataValid(): Boolean
    {
        return isThresholdValid()
    }

    private fun isThresholdValid():Boolean
    {
        val isThresholdValid = false

        if (threshold_edittext.text != null && !TextUtils.isEmpty(threshold_edittext.text))
        {
            try
            {
                val threshold = threshold_edittext.text.toString().toLong()

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

    private fun isSecureKeyHashIdentical(): Boolean
    {
        val secureKeyHash = getStorageSecureKeyHash()
        if (!secureKeyHash.isNullOrEmpty())
        {
            val tz3 = retrieveTz3()
            if (!tz3.isNullOrEmpty() && tz3 == secureKeyHash)
            {
                return true
            }
        }

        return false
    }

    private fun getHistoryPayment(): List<Any>
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)

            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")
            val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args") as JSONArray

            val historyPaymentJSONObject = ((argsSecureKey[1] as JSONObject)["args"]) as JSONArray

            var spendingLimit:Long = 0
            var remainingSpendingLimit:Long = -1L
            var timingInSec:Long = -1L

            val nowInEpoch =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        Instant.now().epochSecond
                    }
            else
                    {
                        System.currentTimeMillis()/1000
                    }


            var sortedDatesList = mutableListOf<Long>()


            if (historyPaymentJSONObject != null && historyPaymentJSONObject.length() > 0)
            {
                for (i in 0 until historyPaymentJSONObject.length())
                {
                    //first object (0) contains spending limit and global timing
                    if (i == 0)
                    {
                        val spendingLimitAndTiming = (historyPaymentJSONObject[0] as JSONObject)["args"] as JSONArray

                        val sLimit = ((spendingLimitAndTiming[0] as JSONObject)["int"] as String).toLong()
                        remainingSpendingLimit = sLimit

                        spendingLimit += sLimit

                        val timing = ((spendingLimitAndTiming[1] as JSONObject)["int"] as String).toLong()
                        timingInSec = timing

                    }
                    else
                    {
                        // there will be a second index, not a third one.

                        val allOfThem = (historyPaymentJSONObject[1] as JSONObject)["args"] as JSONArray

                        var cumulatedAmount:Long = 0

                        if (allOfThem.length() != null && allOfThem.length() > 0)
                        {
                            for (j in 0 until allOfThem.length())
                            {
                                if (j == 0)
                                {
                                    // the element necessary to know when it resets the limit
                                    //TODO the necessary element could be in the second index of allOfThem array

                                    val necessaryElement = allOfThem[j] as JSONArray

                                    if (!necessaryElement.isNull(0))
                                    {
                                        val necessaryElement = ((allOfThem[j] as JSONArray)[0] as JSONObject)["args"] as JSONArray

                                        val necessaryElementAmount = ((necessaryElement[1] as JSONObject)["int"] as String).toLong()
                                        cumulatedAmount += necessaryElementAmount

                                        val necessaryElementDate = (necessaryElement[0] as JSONObject)["string"] as String

                                        val time = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                        {
                                            Instant.parse(necessaryElementDate).epochSecond
                                        }
                                        else
                                        {
                                            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                            format.timeZone = TimeZone.getTimeZone("UTC")
                                            format.parse(necessaryElementDate).time/1000
                                        }

                                        sortedDatesList.add(time)

                                        if (time <= nowInEpoch)
                                        {
                                            remainingSpendingLimit += necessaryElementAmount
                                        }
                                    }
                                }
                                else
                                {
                                    //the rest of it with j == 1
                                    val otherPayments = allOfThem[j] as JSONArray
                                    if (!otherPayments.isNull(0))
                                    {
                                        for (k in 0 until otherPayments.length())
                                        {
                                            val payment = (otherPayments[k] as JSONObject)["args"] as JSONArray

                                            val paymentAmount = ((payment[1] as JSONObject)["int"] as String).toLong()

                                            cumulatedAmount += paymentAmount

                                            val paymentDate = (payment[0] as JSONObject)["string"] as String

                                            val time = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                            {
                                                Instant.parse(paymentDate).epochSecond
                                            }
                                            else
                                            {
                                                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                                format.timeZone = TimeZone.getTimeZone("UTC")
                                                format.parse(paymentDate).time/1000
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                            {

                                                sortedDatesList.add(time)

                                                if (time <= nowInEpoch)
                                                {
                                                    remainingSpendingLimit += paymentAmount
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            spendingLimit += cumulatedAmount
                        }
                    }
                }

                val myList = mutableListOf<Any>()

                myList.add(spendingLimit)
                myList.add(remainingSpendingLimit)
                myList.add(timingInSec)

                if (sortedDatesList.isNotEmpty())
                {
                    sortedDatesList.sort()

                    val lastDate = sortedDatesList.last()
                    //TODO maybe the lastDate is already <= now, then it's unlocked already.

                    val remainingTime = lastDate - nowInEpoch

                    when {
                        remainingTime > 0 ->
                        {
                            myList.add(remainingTime)
                        }
                        else ->
                        {
                            myList.add(0L)
                        }
                    }
                }
                else
                {
                    myList.add(0L)
                }

                return myList
            }
        }

        return listOf()
    }

    private fun getStorageSecureKeyHash(): String?
    {
        if (mStorage != null)
        {
//TODO at this point, just show that there is no script.

            val storageJSONObject = JSONObject(mStorage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")

            // get securekey hash
            if (args != null)
            {
                val argsSecureKey = DataExtractor.getJSONArrayFromField(args[0] as JSONObject, "args")
                if (argsSecureKey != null)
                {
                    val secureKeyJSONObject = argsSecureKey[0] as JSONObject
                    return DataExtractor.getStringFromField(secureKeyJSONObject, "string")
                }
            }
        }

        return null
    }

    private fun getThreshold(): String?
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args")
            if (args != null)
            {
                val counter = DataExtractor.getStringFromField(args[0] as JSONObject, "int")
                if (counter != null)
                {
                    val argsPk = DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray
                    return DataExtractor.getStringFromField(argsPk[0] as JSONObject, "int")
                }
            }
        }

        return null
    }

    private fun askingForSignatories():Boolean
    {
        var threshold = getThreshold()

        val mnemonicsData = Storage(activity!!).getMnemonics()
        val numberAndSpotPair = getNumberAndSpot(mnemonicsData.pk)
        if (numberAndSpotPair.first != -1)
        {
            // our edpk is in the list. great. it's fine if we have a threshold of 1.
            if (threshold!!.toInt() == 1)
            {
                return false
            }
        }

        return true
    }

    private fun getCounter(): String?
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args") as JSONArray

            return DataExtractor.getStringFromField(args[0] as JSONObject, "int")
        }

        return null
    }

    private fun getSignatoriesList(): ArrayList<String>
    {
        if (mStorage != null)
        {
            val storageJSONObject = JSONObject(mStorage)
            val args = DataExtractor.getJSONArrayFromField(storageJSONObject, "args") as JSONArray

            val counter = DataExtractor.getStringFromField(args[0] as JSONObject, "int")
            if (counter != null)
            {
                val argsPk = (DataExtractor.getJSONArrayFromField(args[1] as JSONObject, "args") as JSONArray)[1] as JSONArray

                val list = ArrayList<String> ()

                for (it in 0 until argsPk.length())
                {
                    val item = argsPk.getJSONObject(it)

                    val pk = DataExtractor.getStringFromField(item, "string")
                    list.add(pk)
                }

                return list
            }
        }

        return ArrayList(SIGNATORIES_CAPACITY)
    }

    private fun getNumberAndSpot(publicKey:String): Pair<Int, Int>
    {
        val signatories = getSignatoriesList()
        if (!signatories.isNullOrEmpty())
        {
            return Pair(signatories.indexOf(publicKey), signatories.size)
        }

        return Pair(-1, -1)
    }

    private fun isP256AddressValid(): Boolean
    {
        var isTzAddressValid = false

        if (!TextUtils.isEmpty(public_address_edittext?.text))
        {
/*
if (Utils.isTzAddressValid(public_address_edittext.text!!.toString()))
{
    mUpdateStorageAddress = public_address_edittext.text.toString()
    isTzAddressValid = true
}
*/
            isTzAddressValid = true
        }

        return isTzAddressValid
    }

    private fun isUpdateStorageFeeValid():Boolean
    {
        val isFeeValid = false

        if (storage_fee_edittext?.text != null && !TextUtils.isEmpty(storage_fee_edittext?.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val fee = storage_fee_edittext.text.toString().toDouble()

                if (fee >= 0.000001f)
                {
                    val longTransferFee = fee*1000000
                    mUpdateStorageFees = longTransferFee.roundToLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mUpdateStorageFees = -1
                return false
            }
        }

        return isFeeValid
    }

    private fun isMultisigFeeValid():Boolean
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
                    mMultisigFees = longTransferFee.roundToLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mMultisigFees = -1
                return false
            }
        }

        return isFeeValid
    }

    private fun putEverythingInRed()
    {
        this.putTzAddressInRed(true)
        this.putSpendingLimitInRed(true)
    }

    private fun isUpdateButtonValid(): Boolean
    {
        return mUpdateStoragePayload != null
                && isUpdateStorageFeeValid()
                && isSpendingLimitInputDataValid()
    }

    private fun isUpdateMultisigButtonValid(): Boolean
    {
        return mUpdateMultisigStoragePayload != null
                && isMultisigFeeValid()
                && isMultisigInputDataValid()
    }

    private fun putTzAddressInRed(red: Boolean)
    {
        val color: Int

        val tzAddressValid = isP256AddressValid()

        color = if (red && !tzAddressValid)
        {
            R.color.tz_error
        }
        else
        {
            R.color.tz_accent
        }

        public_address_edittext.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun putSpendingLimitInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isSpendingLimitAmountValid()

        color = if (red && !amountValid)
        {
            R.color.tz_error
        }
        else
        {
            R.color.tz_accent
        }

        daily_spending_limit_edittext.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun putThresholdInRed(red: Boolean)
    {

        val thresholdValid = isThresholdValid()

        val color = if (red && !thresholdValid)
        {
            R.color.tz_error
        }
        else
        {
            R.color.tz_accent
        }

        threshold_edittext.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun isSpendingLimitAmountValid():Boolean
    {
        val isAmountValid = false

        if (daily_spending_limit_edittext?.text != null && !TextUtils.isEmpty(daily_spending_limit_edittext.text))
        {
            try
            {
                val amount = daily_spending_limit_edittext.text!!.toString().toLong()

                if (amount in 0..1000)
                {
                    mSpendingLimitAmount = amount
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mSpendingLimitAmount = -1L
                return false
            }
        }

        return isAmountValid
    }

    private fun onUpdateSlcStorageClick()
    {
        val dialog = AuthenticationDialog()
        if (isFingerprintAllowed()!! && hasEnrolledFingerprints()!!)
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices().prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = {
                validateKeyAuthentication(it, isSlcContract = true)
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
            startFinalizeUpdateStorageLoading()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity!!.supportFragmentManager, "Authentication")
    }

    private fun onUpdateMultisigStorageClick()
    {
        val dialog = AuthenticationDialog()
        if (isFingerprintAllowed()!! && hasEnrolledFingerprints()!!)
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices().prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = {
                validateKeyAuthentication(it, isSlcContract = false)
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
            startFinalizeUpdateMultisigStorageLoading()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity!!.supportFragmentManager, "Authentication")
    }

    private fun isFingerprintAllowed():Boolean
    {
        return mCallback!!.isFingerprintAllowed()
    }

    private fun hasEnrolledFingerprints():Boolean
    {
        return mCallback!!.hasEnrolledFingerprints()
    }

    private fun saveFingerprintAllowed(useInFuture:Boolean)
    {
        mCallback?.saveFingerprintAllowed(useInFuture)
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
        val storage = Storage(activity!!)
        return EncryptionServices().decrypt(storage.getPassword()) == inputtedPassword
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject, isSlcContract:Boolean)
    {
        if (EncryptionServices().validateFingerprintAuthentication(cryptoObject))
        {
            if (isSlcContract)
            {
                startFinalizeUpdateStorageLoading()
            }
            else
            {
                startFinalizeUpdateMultisigStorageLoading()
            }
        }
        else
        {
            onUpdateSlcStorageClick()
        }
    }

    private fun cancelRequests(resetBooleans:Boolean)
    {
        if (activity != null)
        {
            val requestQueue = VolleySingleton.getInstance(activity!!.applicationContext).requestQueue
            requestQueue?.cancelAll(UPDATE_STORAGE_INIT_TAG)
            requestQueue?.cancelAll(UPDATE_MULTISIG_STORAGE_INIT_TAG)
            requestQueue?.cancelAll(DELEGATE_FINALIZE_TAG)
            requestQueue?.cancelAll(MULTISIG_FINALIZE_TAG)
            requestQueue?.cancelAll(LOAD_SECURE_HASH_BALANCE_TAG)
            requestQueue?.cancelAll(CONTRACT_SCRIPT_INFO_TAG)

            if (resetBooleans)
            {
                mInitUpdateMultisigStorageLoading = false
                mInitUpdateStorageLoading = false
                mFinalizeDelegateLoading = false
                mFinalizeMultisigLoading = false
                mStorageInfoLoading = false
                mSecureHashBalanceLoading = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(UPDATE_MULTISIG_STORAGE_INIT_TAG, mInitUpdateMultisigStorageLoading)

        outState.putBoolean(UPDATE_STORAGE_INIT_TAG, mInitUpdateStorageLoading)
        outState.putBoolean(DELEGATE_FINALIZE_TAG, mFinalizeDelegateLoading)

        outState.putBoolean(MULTISIG_FINALIZE_TAG, mFinalizeMultisigLoading)

        outState.putBoolean(LOAD_SECURE_HASH_BALANCE_TAG, mSecureHashBalanceLoading)

        outState.putBoolean(CONTRACT_SCRIPT_INFO_TAG, mStorageInfoLoading)

        outState.putString(DELEGATE_PAYLOAD_KEY, mUpdateStoragePayload)

        outState.putLong(DELEGATE_FEE_KEY, mUpdateStorageFees)

        outState.putBoolean(FEES_CALCULATE_KEY, mClickCalculate)

        outState.putBoolean(WALLET_AVAILABLE_KEY, mWalletEnabled)

        outState.putLong(SPENDING_AMOUNT_KEY, mSpendingLimitAmount)

        outState.putLong(THRESHOLD_KEY, mThreshold)

        outState.putString(STORAGE_DATA_KEY, mStorage)

        outState.putBoolean(EDIT_SPENDING_LIMIT_MODE_KEY, mSpendingLimitEditMode)

        outState.putBoolean(EDIT_MULTISIG_MODE_KEY, mMultisigEditMode)

        outState.putLong(BALANCE_LONG_KEY, mSecureHashBalance)

        outState.putString(CONTRACT_SIG_KEY, mSig)

        outState.putStringArrayList(SIGNATORIES_LIST_KEY, mSignatoriesList)
    }

    private fun mutezToTez(mutez:String):String
    {
        var amountDouble: Double = mutez.toDouble()
        amountDouble /= 1000000

        return amountDouble.toString()
    }

    private fun mutezToTez(mutez:Long):String
    {
        var amountDouble: Double = mutez.toDouble()
        amountDouble /= 1000000

        return amountDouble.toString()
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

    private fun addSignatory(signatory:String)
    {
        when {

            mSignatoriesList.size >= 10 ->
            {
                //TODO show snackbar
                //showSnackBar(null, getString(R.string.max_10_signatories))
            }

            mSignatoriesList.contains(signatory) ->
            {
                //TODO show snackbar
                //showSnackBar(null, getString(R.string.signatory_already_in_list))
            }

            else ->
            {
                mSignatoriesList.add(signatory)
                refreshSignatories(editMode = true)
            }
        }
    }

    override fun onPublicKeyClicked(publicKey:String)
    {
        addSignatory(publicKey)

        /*
        val fragment = supportFragmentManager.findFragmentById(R.id.create_wallet_container)
        if (fragment != null && fragment is VerifyCreationWalletFragment) {
            val verifyCreationWalletFragment = fragment as VerifyCreationWalletFragment?
            verifyCreationWalletFragment!!.updateCard(word, position)
        }
        */
    }

    override fun onDetach()
    {
        super.onDetach()
        mCallback = null
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequests(false)
    }
}
