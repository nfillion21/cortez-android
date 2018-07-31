package com.tezos.ui.fragment

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatSpinner
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.tezos.core.client.AbstractClient
import com.tezos.core.models.Account
import com.tezos.core.models.CustomTheme
import com.tezos.core.requests.order.PaymentPageRequest
import com.tezos.ui.R
import com.tezos.ui.activity.PaymentAccountsActivity
import com.tezos.ui.activity.TransferFormActivity

/**
 * Created by nfillion on 20/04/16.
 */
class TransferFormFragment : AbstractPaymentFormFragment()
{
    private var mPayButton: Button? = null
    private var mPayButtonLayout: FrameLayout? = null

    private var mSrcButton: Button? = null
    private var mDstButton: Button? = null

    private var mTransferSrcFilled: LinearLayout? = null
    private var mTransferDstFilled: LinearLayout? = null

    private var mCurrencySpinner: AppCompatSpinner? = null

    private val isTransferAmountValid: Boolean
        get()
        {
            val isAmountValid = false

            if (!TextUtils.isEmpty(mAmount.text))
            {
                try
                {
                    val amount = java.lang.Double.parseDouble(mAmount.text!!.toString())

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

    override fun initContentViews(view: View)
    {
        super.initContentViews(view)

        val args = arguments
        val themeBundle = args!!.getBundle(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        mCurrencySpinner = view.findViewById(R.id.fee_spinner)
        val adapter = ArrayAdapter.createFromResource(activity!!,
                R.array.array_fee, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mCurrencySpinner!!.adapter = adapter
        mCurrencySpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long)
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

        mPayButton = view.findViewById(R.id.pay_button)
        mPayButtonLayout = view.findViewById(R.id.pay_button_layout)

        mPayButtonLayout!!.visibility = View.VISIBLE

        mCardInfoLayout.visibility = View.VISIBLE

        val moneyFormatted = "ꜩ"

        val moneyString = getString(R.string.pay, moneyFormatted)

        mPayButton!!.text = moneyString

        mPayButtonLayout!!.setOnClickListener { v ->
            //setLoadingMode(true,false);
            //launchRequest();
        }

        val focusChangeListener = this.focusChangeListener()

        mAmount = view.findViewById(R.id.amount_transfer)
        mAmount.addTextChangedListener(GenericTextWatcher(mAmount))
        mAmount.onFocusChangeListener = focusChangeListener

        //mAmountLayout = view.findViewById(R.id.amount_transfer_support);
        //mAmountLayout.setError(" ");

        validatePayButton(isInputDataValid)

        switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccounts, Account())

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
                    switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccounts, account)
                }
                else if (resultCode == R.id.transfer_dst_selection_succeed)
                {
                    switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses, account)
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
            }

            PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses ->
            {
                mDstButton?.visibility = View.GONE
                mTransferDstFilled?.visibility = View.VISIBLE
            }

            else ->
            {
                //no-op
            }
        }
    }

    override fun setLoadingMode(loadingMode: Boolean, delay: Boolean)
    {
        if (!delay)
        {
            if (loadingMode)
            {
                mPayButtonLayout!!.visibility = View.GONE
                mProgressBar.visibility = View.VISIBLE
            }
            else
            {
                mPayButtonLayout!!.visibility = View.VISIBLE
                mProgressBar.visibility = View.GONE
            }
        }

        mLoadingMode = loadingMode
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
            validatePayButton(isInputDataValid)
        }
    }

    override fun launchRequest()
    {
        val args = arguments

        val paymentPageRequest = PaymentPageRequest.fromBundle(args!!.getBundle(PaymentPageRequest.TAG))
        //final PaymentProduct paymentProduct = PaymentProduct.fromBundle(args.getBundle(PaymentProduct.TAG));

        //mSecureVaultClient = new SecureVaultClient(getActivity());
        mCurrentLoading = AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.integerValue!!

        /*
        mSecureVaultClient.generateToken(

                mCardNumberCache,
                mMonthExpiryCache,
                mYearExpiryCache,
                mCardOwnerCache,
                mCardCVVCache,
                paymentPageRequest.getMultiUse(),

                new SecureVaultRequestCallback() {
                    @Override
                    public void onSuccess(PaymentCardToken paymentCardToken) {

                        mPaymentCardToken = paymentCardToken;

                        //secure vault
                        cancelLoaderId(AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue());

                        OrderRequest orderRequest = new OrderRequest(paymentPageRequest);

                        String productCode = paymentProduct.getCode();
                        if (productCode.equals(PaymentProduct.PaymentProductCategoryCodeCard) || !productCode.equals(inferedPaymentProduct)) {
                            productCode = mCardBehaviour.getProductCode();
                        }

                        orderRequest.setPaymentProductCode(productCode);

                        CardTokenPaymentMethodRequest cardTokenPaymentMethodRequest =
                                new CardTokenPaymentMethodRequest(
                                        mPaymentCardToken.getToken(),
                                        paymentPageRequest.getEci(),
                                        paymentPageRequest.getAuthenticationIndicator());

                        orderRequest.setPaymentMethod(cardTokenPaymentMethodRequest);

                        //check if activity is still available
                        if (getActivity() != null) {

                            mGatewayClient = new GatewayClient(getActivity());
                            mCurrentLoading = AbstractClient.RequestLoaderId.OrderReqLoaderId.getIntegerValue();

                            mGatewayClient.requestNewOrder(orderRequest, signature, new OrderRequestCallback() {

                                @Override
                                public void onSuccess(final Transaction transaction) {
                                    //Log.i("transaction success", transaction.toString());

                                    if (mCallback != null) {
                                        cancelLoaderId(AbstractClient.RequestLoaderId.OrderReqLoaderId.getIntegerValue());
                                        mCallback.onCallbackOrderReceived(transaction, null);
                                    }

                                }

                                @Override
                                public void onError(Exception error) {
                                    //Log.i("transaction failed", error.getLocalizedMessage());
                                    if (mCallback != null) {
                                        cancelLoaderId(AbstractClient.RequestLoaderId.OrderReqLoaderId.getIntegerValue());
                                        mCallback.onCallbackOrderReceived(null, error);
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Exception error) {

                        if (mCallback != null) {
                            cancelLoaderId(AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue());
                            mCallback.onCallbackOrderReceived(null, error);
                        }
                    }
                }
        );
        */
    }

    override fun isInputDataValid(): Boolean
    {
        return this.isTransferAmountValid
    }

    private fun putEverythingInRed()
    {
        this.putAmountInRed(true)
    }

    // put everything in RED

    private fun putAmountInRed(red: Boolean)
    {
        val color: Int

        val amountValid = this.isTransferAmountValid

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
                val amount = mAmount.text!!.toString()
                this.setTextPayButton(amount)
            }
            else
            {
                mPayButton!!.text = getString(R.string.pay, "")
            }
        }

        this.mAmount.setTextColor(ContextCompat.getColor(activity!!, color))
    }

    private fun setTextPayButton(amount: String)
    {
        var amount = amount
        var amountDouble: Double = java.lang.Double.parseDouble(amount)

        val selectedItemThreeDS = mCurrencySpinner!!.selectedItemId

        when (selectedItemThreeDS.toInt())
        {
            0 -> {
                amountDouble += 0.05
            }

            1 -> {
                amountDouble += 0.01
            }

            2 -> {
                amountDouble += 0.1
            }

            else -> {
                //no-op
            }
        }

        amount = java.lang.Double.toString(amountDouble!!)

        //check the correct amount
        if (amount.contains("."))
        {
            val elements = amount.substring(amount.indexOf("."))

            if (elements.length > 7)
            {
                amount = String.format("%.6f", java.lang.Double.parseDouble(amount))
                val d = java.lang.Double.parseDouble(amount)
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
                amount = String.format("%.2f", java.lang.Double.parseDouble(amount))
            }
        }
        else
        {
            amount = String.format("%.2f", java.lang.Double.parseDouble(amount))
            //amount = Double.parseDouble(amount).toString();
        }

        val moneyFormatted2 = "$amount ꜩ"
        //String moneyFormatted3 = Double.toString(amountDouble) + " ꜩ";
        mPayButton!!.text = getString(R.string.pay, moneyFormatted2)
    }
}
