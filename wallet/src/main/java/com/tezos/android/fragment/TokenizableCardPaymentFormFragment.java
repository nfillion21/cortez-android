package com.tezos.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.tezos.android.R;
import com.tezos.android.fragment.interfaces.CardBehaviour;
import com.tezos.core.client.AbstractClient;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.core.utils.FormHelper;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;

/**
 * Created by nfillion on 20/04/16.
 */
public class TokenizableCardPaymentFormFragment extends AbstractPaymentFormFragment
{
    private Button mPayButton;
    private FrameLayout mPayButtonLayout;

    private CardBehaviour mCardBehaviour;

    private String mCardNumberCache;

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
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void initContentViews(View view)
    {
        super.initContentViews(view);

        mPayButton = view.findViewById(R.id.pay_button);
        mPayButtonLayout = view.findViewById(R.id.pay_button_layout);

        mPayButtonLayout.setVisibility(View.VISIBLE);

        mCardInfoLayout.setVisibility(View.VISIBLE);

        //TODO handle the arguments
        Bundle args = getArguments();
        //final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        //Currency c = Currency.getInstance(paymentPageRequest.getCurrency());
        //currencyFormatter.setCurrency(c);
        //String moneyFormatted = currencyFormatter.format(paymentPageRequest.getAmount());
        String moneyFormatted = "ꜩ";

        String moneyString = getString(R.string.pay, moneyFormatted);

        mPayButton.setText(moneyString);

        mPayButtonLayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setLoadingMode(true,false);
                launchRequest();
            }
        });

        View.OnFocusChangeListener focusChangeListener = this.focusChangeListener();

        mAmount = view.findViewById(R.id.amount_transfer);
        mAmount.addTextChangedListener(new GenericTextWatcher(mAmount));
        mAmount.setOnFocusChangeListener(focusChangeListener);

        mFees = view.findViewById(R.id.fees_transfer);
        mFees.addTextChangedListener(new GenericTextWatcher(mFees));
        mFees.setOnFocusChangeListener(focusChangeListener);

        mFeesLayout = view.findViewById(R.id.fees_transfer_support);
        mAmountLayout = view.findViewById(R.id.amount_transfer_support);

        mFeesLayout.setError(" ");
        mAmountLayout.setError(" ");

        if (mCardBehaviour == null) {
            //mCardBehaviour = new CardBehaviour(paymentProduct);
            mCardBehaviour = new CardBehaviour();
        }

        //mCardBehaviour.updateForm(mFees, mCardCVV, mCardExpiration, mCardCVVLayout, mSecurityCodeInfoTextview, mSecurityCodeInfoImageview, isPaymentCardStorageSwitchVisible ? mCardStorageSwitchLayout : null, false, getActivity());

        mAmount.requestFocus();

        setElementsCache(true);

        validatePayButton(isInputDataValid());

        putEverythingInRed();

    }

    @Override
    public void setLoadingMode(boolean loadingMode, boolean delay) {

        setElementsCache(loadingMode);

        if (!delay) {

            if (loadingMode) {

                mPayButtonLayout.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);

                mFees.setEnabled(false);

            } else {

                mPayButtonLayout.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);

                mFees.setEnabled(true);
            }
        }

        mLoadingMode = loadingMode;
    }

    private View.OnFocusChangeListener focusChangeListener()
    {
        return new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                int i = v.getId();

                if (i == R.id.amount_transfer)
                {
                    //putCardNumberInRed(!hasFocus);
                }
                else if (i == R.id.fees_transfer)
                {
                    //putCardNumberInRed(!hasFocus);
                }
                else
                {
                    throw new UnsupportedOperationException(
                            "onFocusChange has not been implemented for " + getResources().
                                    getResourceName(v.getId()));
                }
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

            String version = editable.toString();

            if (i == R.id.amount_transfer)
            {
                //mFees.requestFocus();
            }
            else if (i == R.id.fees_transfer)
            {
            }

            else {
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

    private void setElementsCache(boolean bool)
    {
        if (bool)
        {
            if (mCardNumberCache == null)
            {
                mCardNumberCache = mFees.getText().toString().replaceAll(" ", "");
            }

        } else
        {
            mCardNumberCache = null;
        }
    }

    @Override
    public void launchRequest()
    {
        Bundle args = getArguments();

        final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));
        //final PaymentProduct paymentProduct = PaymentProduct.fromBundle(args.getBundle(PaymentProduct.TAG));

        //mSecureVaultClient = new SecureVaultClient(getActivity());
        mCurrentLoading = AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue();

        setElementsCache(true);

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

    private String getMonthFromExpiry(String expiryString) {

        if (expiryString != null && !expiryString.isEmpty()) {
            return expiryString.substring(0,2);
        }

        return null;
    }

    @Override
    protected boolean isInputDataValid() {

        if (
                this.isCVVValid() &&
                this.isCardNumberValid()

                ) {

            return true;
        }

        return false;
    }

    protected boolean isCardNumberValid() {

        if (!TextUtils.isEmpty(mFees.getText())) {

            //luhn first
            if (!FormHelper.luhnTest(mFees.getText().toString().replaceAll(" ", ""))) {
                return false;
            }

            //format then
            Set<String> paymentProducts = FormHelper.getPaymentProductCodes(mFees.getText().toString(), getActivity());
            if (paymentProducts.isEmpty()) {
                return false;
            }

            if (paymentProducts.size() == 1) {

                String[] things = paymentProducts.toArray(new String[1]);

                if (FormHelper.hasValidCardLength(mFees.getText().toString(), things[0], getActivity())) {

                    return true;
                }
            }
        }

        return false;
    }

    protected boolean isCVVValid() {

        //return mCardBehaviour.isSecurityCodeValid(mCardCVV);
        return true;
    }

    protected boolean hasSecurityCode() {

        return mCardBehaviour.hasSecurityCode();
    }

    protected boolean hasSpaceAtIndex(int index) {

        return mCardBehaviour.hasSpaceAtIndex(index, getActivity());
    }

    private boolean isPaymentCardStorageConfigEnabled()
    {
        /*
        boolean paymentCardEnabled = ClientConfig.getInstance().isPaymentCardStorageEnabled();

        Bundle args = getArguments();
        PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));
        boolean paymentPageRequestECI = paymentPageRequest.getEci() == Transaction.ECI.SecureECommerce ? true : false;

        return paymentCardEnabled && paymentPageRequestECI;
        */

        return false;
    }

    protected void putEverythingInRed() {

        this.putCVVInRed(true);
    }

    private void putCVVInRed(boolean red) {

        int color;

        boolean securityCodeValid = this.isCVVValid();

        if (red && !securityCodeValid) {
            color = R.color.hpf_error;

        } else {
            color = R.color.hpf_accent;
        }
    }

    private boolean isInferedOrCardDomesticNetwork(String product) {
        return true;
    }

    private boolean isDomesticNetwork() {
        return true;
    }

    private boolean isDomesticNetwork(String paymentProductCode) {

        return false;
    }
}

