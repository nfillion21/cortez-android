package com.tezos.ui.fragment;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.tezos.core.client.AbstractClient;
import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.ui.R;
import com.tezos.ui.activity.PaymentAccountsActivity;
import com.tezos.ui.activity.PaymentFormActivity;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by nfillion on 20/04/16.
 */
public class PaymentFormFragment extends AbstractPaymentFormFragment
{
    private Button mPayButton;
    private FrameLayout mPayButtonLayout;

    private Button mSrcButton;
    private Button mDstButton;

    private LinearLayout mTransferSrcFilled;
    private LinearLayout mTransferDstFilled;

    private AppCompatSpinner mCurrencySpinner;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    protected void initContentViews(View view)
    {
        super.initContentViews(view);

        Bundle args = getArguments();
        Bundle themeBundle = args.getBundle(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);

        mCurrencySpinner = view.findViewById(R.id.fee_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.array_fee, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCurrencySpinner.setAdapter(adapter);

        mSrcButton = view.findViewById(R.id.transfer_src_button);
        mSrcButton.setOnClickListener(v ->
                PaymentAccountsActivity.start(getActivity(), theme, PaymentAccountsActivity.FromScreen.FromTransfer, PaymentAccountsActivity.Selection.SelectionAccounts));

        mDstButton = view.findViewById(R.id.transfer_dst_button);
        mDstButton.setOnClickListener(v ->
                PaymentAccountsActivity.start(getActivity(), theme, PaymentAccountsActivity.FromScreen.FromTransfer, PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses)
        );

        mTransferSrcFilled = view.findViewById(R.id.transfer_source_filled);
        mTransferDstFilled = view.findViewById(R.id.transfer_destination_filled);

        mPayButton = view.findViewById(R.id.pay_button);
        mPayButtonLayout = view.findViewById(R.id.pay_button_layout);

        mPayButtonLayout.setVisibility(View.VISIBLE);

        mCardInfoLayout.setVisibility(View.VISIBLE);

        //TODO handle the arguments
        //final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        //Currency c = Currency.getInstance(paymentPageRequest.getCurrency());
        //currencyFormatter.setCurrency(c);
        //String moneyFormatted = currencyFormatter.format(paymentPageRequest.getAmount());
        String moneyFormatted = "ꜩ";

        String moneyString = getString(R.string.pay, moneyFormatted);

        mPayButton.setText(moneyString);

        mPayButtonLayout.setOnClickListener(v ->
        {
            //setLoadingMode(true,false);
            //launchRequest();
        });

        View.OnFocusChangeListener focusChangeListener = this.focusChangeListener();

        mAmount = view.findViewById(R.id.amount_transfer);
        mAmount.addTextChangedListener(new GenericTextWatcher(mAmount));
        mAmount.setOnFocusChangeListener(focusChangeListener);

        //mAmountLayout = view.findViewById(R.id.amount_transfer_support);
        //mAmountLayout.setError(" ");

        validatePayButton(isInputDataValid());

        putEverythingInRed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PaymentFormActivity.TRANSFER_SELECT_REQUEST_CODE)
        {
            if (data != null && data.hasExtra(Account.TAG))
            {
                Bundle accountBundle = data.getBundleExtra(Account.TAG);
                Account account = Account.fromBundle(accountBundle);

                if (resultCode == R.id.transfer_src_selection_succeed)
                {
                    switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccounts, account);
                }
                else
                if (resultCode == R.id.transfer_dst_selection_succeed)
                {
                    switchButtonAndLayout(PaymentAccountsActivity.Selection.SelectionAccountsAndAddresses, account);
                }
            }
        }
    }

    private void switchButtonAndLayout(PaymentAccountsActivity.Selection selection, Account account)
    {
        switch (selection)
        {
            case SelectionAccounts:
            {
                mSrcButton.setVisibility(View.GONE);
                mTransferSrcFilled.setVisibility(View.VISIBLE);
            }
            break;

            case SelectionAccountsAndAddresses:
            {
                mDstButton.setVisibility(View.GONE);
                mTransferDstFilled.setVisibility(View.VISIBLE);
            }
            break;

            default:
                //no-op
                break;
        }
    }

    @Override
    public void setLoadingMode(boolean loadingMode, boolean delay)
    {
        if (!delay) {

            if (loadingMode) {

                mPayButtonLayout.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);

            } else {

                mPayButtonLayout.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
            }
        }

        mLoadingMode = loadingMode;
    }

    private View.OnFocusChangeListener focusChangeListener()
    {
        return (v, hasFocus) -> {
            int i = v.getId();

            if (i == R.id.amount_transfer)
            {
                putAmountInRed(!hasFocus);
            }
            else
            {
                throw new UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
        };
    }

    protected void validatePayButton(boolean validate) {

        if (validate) {

            final Bundle customThemeBundle = getArguments().getBundle(CustomTheme.TAG);
            CustomTheme theme = CustomTheme.fromBundle(customThemeBundle);

            mPayButton.setTextColor(ContextCompat.getColor(getActivity(), theme.getTextColorPrimaryId()));
            mPayButtonLayout.setEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mPayButtonLayout.setBackground(makeSelector(theme));

                Drawable[] drawables = mPayButton.getCompoundDrawables();
                Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getActivity(), theme.getTextColorPrimaryId()));
            }

        } else {

            mPayButton.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
            mPayButtonLayout.setEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                CustomTheme greyTheme = new CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey);
                mPayButtonLayout.setBackground(makeSelector(greyTheme));

                Drawable[] drawables = mPayButton.getCompoundDrawables();
                Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getActivity(), android.R.color.white));
            }
        }
    }

    private StateListDrawable makeSelector(CustomTheme theme) {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(getActivity(), theme.getColorPrimaryDarkId())));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(getActivity(), theme.getColorPrimaryId())));
        return res;
    }

    private class GenericTextWatcher implements TextWatcher {

        private int diffLength = 0;
        private int changeStart = 0;
        private boolean isLastSpace = false;
        private int currentLength = 0;

        private View v;
        private GenericTextWatcher(View view) {
            this.v = view;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            diffLength = after - count;
            changeStart = start;

            if (s.length() > 0 && s.charAt(s.length()-1) == ' ') {
                isLastSpace = true;
            } else {
                isLastSpace = false;
            }
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

            diffLength = count - before;
            changeStart = start;

            currentLength = s.length();
        }

        public void afterTextChanged(Editable editable)
        {
            int i = v.getId();

            if (i == R.id.amount_transfer)
            {
                putAmountInRed(false);
            }
            else
            {
                throw new UnsupportedOperationException(
                        "OnClick has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
            validatePayButton(isInputDataValid());
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("onCreate", "onCreate");
    }

    @Override
    public void launchRequest()
    {
        Bundle args = getArguments();

        final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));
        //final PaymentProduct paymentProduct = PaymentProduct.fromBundle(args.getBundle(PaymentProduct.TAG));

        //mSecureVaultClient = new SecureVaultClient(getActivity());
        mCurrentLoading = AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue();

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

    @Override
    protected boolean isInputDataValid()
    {
        if (
                this.isTransferAmountValid()
                )
        {
            return true;
        }

        return false;
    }

    protected void putEverythingInRed()
    {
        this.putAmountInRed(true);
    }

    // put everything in RED

    private void putAmountInRed(boolean red) {

        int color;

        boolean amountValid = this.isTransferAmountValid();

        if (red && !amountValid) {
            color = R.color.tz_error;

        } else {
            color = R.color.tz_accent;
        }

        this.mAmount.setTextColor(ContextCompat.getColor(getActivity(), color));
    }

    private boolean isTransferAmountValid()
    {
        boolean isAmountValid = false;

        if (!TextUtils.isEmpty(mAmount.getText()))
        {
            try
            {
                float amount = Float.parseFloat(mAmount.getText().toString());
                if (amount >= 0.1f)
                {
                    return true;
                }
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }

        return isAmountValid;
    }
}

