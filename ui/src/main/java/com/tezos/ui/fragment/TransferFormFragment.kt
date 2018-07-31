package com.tezos.ui.fragment

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
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
import com.tezos.core.models.Account
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.activity.PaymentAccountsActivity
import com.tezos.ui.activity.TransferFormActivity
import com.tezos.ui.utils.Storage
import java.util.ArrayList

/**
 * Created by nfillion on 20/04/16.
 */
class TransferFormFragment : Fragment()
{
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
    //private var mAmountLayout: TextInputLayout? = null

    private var mSrcAccount:Account? = null
    private var mDstAccount:Account? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            }
        }

        initContentViews(view)
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
        mSrcButton!!.setOnClickListener { v -> PaymentAccountsActivity.start(activity, theme, PaymentAccountsActivity.FromScreen.FromTransfer, PaymentAccountsActivity.Selection.SelectionAccounts) }

        mDstButton = view.findViewById(R.id.transfer_dst_button)
        mDstButton!!.setOnClickListener { v -> PaymentAccountsActivity.start(activity, theme, PaymentAccountsActivity.FromScreen.FromTransfer, PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses) }

        mTransferSrcFilled = view.findViewById(R.id.transfer_source_filled)
        mTransferDstFilled = view.findViewById(R.id.transfer_destination_filled)

        mTransferSrcPkh = view.findViewById(R.id.src_payment_account_pub_key_hash)
        mTransferDstPkh = view.findViewById(R.id.dst_payment_account_pub_key_hash)

        mPayButtonLayout!!.visibility = View.VISIBLE

        val moneyFormatted = "ꜩ"

        val moneyString = getString(R.string.pay, moneyFormatted)

        mPayButton!!.text = moneyString

        mPayButtonLayout!!.setOnClickListener { v ->

            val seedDataBundle = arguments?.getBundle(Storage.TAG)
            val seedData = Storage.fromBundle(seedDataBundle!!)
            val pkhSrc = seedData.pkh

            val pkhDst = mDstAccount?.pubKeyHash
            val pkhDst2 = mDstAccount?.pubKeyHash
            val pkhDst3 = mDstAccount?.pubKeyHash

        }

        //mAmountLayout = view.findViewById(R.id.amount_transfer_support);
        //mAmountLayout.setError(" ");

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == TransferFormActivity.TRANSFER_SELECT_REQUEST_CODE)
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
                }
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
                amountDouble += 0.05
            }

            1 -> {
                amountDouble += 0.00
            }

            2 -> {
                amountDouble += 0.1
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

            if (elements.length > 7)
            {
                amount = String.format("%.6f", amount.toDouble())
                val d = amount.toDouble()
                amount = d.toString()
            }
            else if (elements.length > 3)
            {
                //                        int length = elements.length() - 1;
                //                        String format = "%." + length + "f";
                //                        Float f = Float.parseFloat(amount);
                //                        amount = String.format(format, f);
            }
            else
            {
                amount = String.format("%.2f", amount.toDouble())
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

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putParcelable(SRC_ACCOUNT_KEY, mSrcAccount?.toBundle())
        outState.putParcelable(DST_ACCOUNT_KEY, mDstAccount?.toBundle())
    }
}
