package com.tezos.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tezos.android.R;
import com.tezos.android.fragment.interfaces.CardBehaviour;
import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.core.client.AbstractClient;
import com.tezos.core.client.GatewayClient;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.FormHelper;
import com.tezos.core.utils.Utils;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * Created by nfillion on 20/04/16.
 */
public class TokenizableCardPaymentFormFragment extends AbstractPaymentFormFragment {

    private Button mScanButton;
    private Button mScanNfcButton;
    private LinearLayout mScanNfcInfoLayout;

    private Button mPayButton;
    private FrameLayout mPayButtonLayout;

    private String inferedPaymentProduct;

    private CardBehaviour mCardBehaviour;

    private SwitchCompat mCardStorageSwitch;
    private LinearLayout mCardStorageSwitchLayout;

    private String mCardNumberCache;
    private String mMonthExpiryCache;
    private String mYearExpiryCache;
    private String mCardOwnerCache;
    private String mCardCVVCache;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
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

        mPayButton = (Button) view.findViewById(R.id.pay_button);
        mPayButtonLayout = (FrameLayout) view.findViewById(R.id.pay_button_layout);

        mPayButtonLayout.setVisibility(View.VISIBLE);

        mCardInfoLayout.setVisibility(View.VISIBLE);

        mSecurityCodeInfoLayout = (LinearLayout) view.findViewById(R.id.card_cvv_info);
        mSecurityCodeInfoTextview = (TextView) view.findViewById(R.id.card_cvv_info_text);
        mSecurityCodeInfoImageview = (ImageView) view.findViewById(R.id.card_cvv_info_src);

        Bundle args = getArguments();
        //final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        //Currency c = Currency.getInstance(paymentPageRequest.getCurrency());
        //currencyFormatter.setCurrency(c);
        //String moneyFormatted = currencyFormatter.format(paymentPageRequest.getAmount());
        String moneyFormatted = "ꜩ";

        String moneyString = getString(R.string.pay, moneyFormatted);

        mPayButton.setText(moneyString);

        mPayButtonLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                setLoadingMode(true,false);
                launchRequest();
            }
        });

        View.OnFocusChangeListener focusChangeListener = this.focusChangeListener();

        mCardOwner = (TextInputEditText) view.findViewById(R.id.card_owner);

        mCardOwner.setOnFocusChangeListener(focusChangeListener);
        mCardOwner.addTextChangedListener(new GenericTextWatcher(mCardOwner));

        mCardNumber = (TextInputEditText) view.findViewById(R.id.card_number);
        mCardNumber.addTextChangedListener(new GenericTextWatcher(mCardNumber));
        mCardNumber.setOnFocusChangeListener(focusChangeListener);

        mCardExpiration = (TextInputEditText) view.findViewById(R.id.card_expiration);
        mCardExpiration.setOnFocusChangeListener(focusChangeListener);
        mCardExpiration.addTextChangedListener(new GenericTextWatcher(mCardExpiration));

        mCardCVV = (TextInputEditText) view.findViewById(R.id.card_cvv);
        mCardCVV.setOnFocusChangeListener(focusChangeListener);
        mCardCVV.addTextChangedListener(new GenericTextWatcher(mCardCVV));

        mCardCVVLayout = (TextInputLayout) view.findViewById(R.id.card_cvv_support);
        mCardOwnerLayout = (TextInputLayout) view.findViewById(R.id.card_owner_support);
        mCardExpirationLayout = (TextInputLayout) view.findViewById(R.id.card_expiration_support);
        mCardNumberLayout = (TextInputLayout) view.findViewById(R.id.card_number_support);

        mCardOwnerLayout.setError(" ");
        mCardNumberLayout.setError(" ");
        mCardExpirationLayout.setError(" ");
        mCardCVVLayout.setError(" ");

        mCardStorageSwitch = (SwitchCompat) view.findViewById(R.id.card_storage_switch);
        mCardStorageSwitchLayout = (LinearLayout) view.findViewById(R.id.card_storage_layout);

        //switch visible or gone
        boolean isPaymentCardStorageSwitchVisible = this.isPaymentCardStorageConfigEnabled();
        mCardStorageSwitchLayout.setVisibility(isPaymentCardStorageSwitchVisible ? View.VISIBLE : View.GONE);

        if (inferedPaymentProduct == null) {
            //inferedPaymentProduct = paymentProduct.getCode();
        }

        if (mCardBehaviour == null) {
            //mCardBehaviour = new CardBehaviour(paymentProduct);
            mCardBehaviour = new CardBehaviour();
        }

        //mCardBehaviour.updateForm(mCardNumber, mCardCVV, mCardExpiration, mCardCVVLayout, mSecurityCodeInfoTextview, mSecurityCodeInfoImageview, isPaymentCardStorageSwitchVisible ? mCardStorageSwitchLayout : null, false, getActivity());

        //CustomerInfoRequest customerInfoRequest = paymentPageRequest.getCustomer();
        //String displayName = customerInfoRequest.getDisplayName();
        //if (displayName != null) {
            mCardOwner.setText("Nico");
        //}

        mCardNumber.requestFocus();

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

                mCardOwner.setEnabled(false);
                mCardCVV.setEnabled(false);
                mCardExpiration.setEnabled(false);
                mCardNumber.setEnabled(false);
                mCardStorageSwitch.setEnabled(false);

            } else {

                mPayButtonLayout.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);

                mCardOwner.setEnabled(true);
                mCardCVV.setEnabled(true);
                mCardExpiration.setEnabled(true);
                mCardNumber.setEnabled(true);
                mCardStorageSwitch.setEnabled(true);

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

                if (i == R.id.card_owner) {

                    putCardOwnerInRed(hasFocus);

                } else if (i == R.id.card_cvv) {

                    putCVVInRed(!hasFocus);

                    mSecurityCodeInfoLayout.setVisibility(hasFocus? View.VISIBLE : View.GONE);

                } else if (i == R.id.card_expiration) {

                    putExpiryDateInRed(!hasFocus);

                } else if (i == R.id.card_number) {

                    putCardNumberInRed(!hasFocus);

                } else {
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

    public void fillCardNumber(String cardNumberText, Date expiryDate) {

        mCardNumber.setFilters( new InputFilter[] { new InputFilter.LengthFilter(Integer.MAX_VALUE)});
        mCardNumber.setText(Utils.formatCardNumber(cardNumberText));

        if (expiryDate != null) {
            mCardExpiration.setText(Utils.getPaymentFormStringFromDate(expiryDate));
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

        public void afterTextChanged(Editable editable) {

            int i = v.getId();

            String version = editable.toString();

            if (i == R.id.card_owner) {

                putCardOwnerInRed(false);

            } else if (i == R.id.card_cvv) {

                // the condition to say it is wrong
                putCVVInRed(false);
                if (isCVVValid()) {

                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mCardCVV.getWindowToken(), 0);
                }

            } else if (i == R.id.card_expiration) {

            } else if (i == R.id.card_number) {

            } else {
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

    private void setElementsCache(boolean bool) {

        if (bool) {

            if (mCardNumberCache == null) {

                mCardNumberCache = mCardNumber.getText().toString().replaceAll(" ", "");
            }

            if (mMonthExpiryCache == null) {
                mMonthExpiryCache = this.getMonthFromExpiry(mCardExpiration.getText().toString());
            }

            if (mYearExpiryCache == null) {
                //mYearExpiryCache = this.getYearFromExpiry(mCardExpiration.getText().toString());
            }

            if (mCardCVVCache == null) {
                mCardCVVCache = mCardCVV.getText().toString();
            }

            if (mCardOwnerCache == null) {
                mCardOwnerCache = mCardOwner.getText().toString();
            }

        } else {

            mCardNumberCache = null;
            mMonthExpiryCache = null;
            mYearExpiryCache = null;
            mCardCVVCache = null;
            mCardOwnerCache = null;
        }
    }

    @Override
    public void launchRequest() {

        Bundle args = getArguments();

        final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));
        //final PaymentProduct paymentProduct = PaymentProduct.fromBundle(args.getBundle(PaymentProduct.TAG));

        final String signature = args.getString(GatewayClient.SIGNATURE_TAG);

        if (this.isPaymentCardStorageConfigEnabled() && this.isPaymentCardStorageEnabled() && mCardStorageSwitch.isChecked())
        {
            //paymentPageRequest.setMultiUse(true);
        }

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
                this.isExpiryDateValid() &&
                this.isCVVValid() &&
                this.isCardOwnerValid() &&
                this.isCardNumberValid()

                ) {

            return true;
        }

        return false;
    }

    protected boolean isCardNumberValid() {

        if (!TextUtils.isEmpty(mCardNumber.getText())) {

            //luhn first
            if (!FormHelper.luhnTest(mCardNumber.getText().toString().replaceAll(" ", ""))) {
                return false;
            }

            //format then
            Set<String> paymentProducts = FormHelper.getPaymentProductCodes(mCardNumber.getText().toString(), getActivity());
            if (paymentProducts.isEmpty()) {
                return false;
            }

            if (paymentProducts.size() == 1) {

                String[] things = paymentProducts.toArray(new String[1]);

                if (FormHelper.hasValidCardLength(mCardNumber.getText().toString(), things[0], getActivity())) {

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

    protected boolean isCardOwnerValid() {

        if (!TextUtils.isEmpty(mCardOwner.getText())) {
            return true;
        }
        return false;
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

    private boolean isPaymentCardStorageEnabled()
    {
        String paymentProduct;

        if (inferedPaymentProduct != null) {
            paymentProduct = inferedPaymentProduct;
        } else {
            paymentProduct = mCardBehaviour.getProductCode();
        }

        return true;
    }

    protected boolean isExpiryDateValid() {

        return true;

        /*
        if (!TextUtils.isEmpty(mCardExpiration.getText())) {

            String expiryDateString = mCardExpiration.getText().toString().trim();

            if (expiryDateString.length() == 5 && expiryDateString.charAt(2) == '/') {

                String firstPart = expiryDateString.substring(0, 2);
                String secondPart = expiryDateString.substring(3,5);

                StringBuilder stringBuilder = new StringBuilder(firstPart).append(secondPart);
                String expiryDate = stringBuilder.toString();

                if (TextUtils.isDigitsOnly(expiryDate)) {

                    return FormHelper.validateExpiryDate(expiryDate);
                }

            } else if (expiryDateString.length() == 4 && TextUtils.isDigitsOnly(expiryDateString)) {

                return FormHelper.validateExpiryDate(expiryDateString);
            }
        }
        return false;
        */
    }

    protected void putEverythingInRed() {

        this.putExpiryDateInRed(true);
        this.putCVVInRed(true);
        this.putCardOwnerInRed(true);
        this.putCardNumberInRed(true);
    }

    private void putExpiryDateInRed(boolean red) {

        int color;

        boolean expiryDateValid = this.isExpiryDateValid();
        if (red && !expiryDateValid) {
            color = R.color.hpf_error;
        } else {
            color = R.color.hpf_accent;
        }

        this.mCardExpiration.setTextColor(ContextCompat.getColor(getActivity(), color));
    }
    private void putCVVInRed(boolean red) {

        int color;

        boolean securityCodeValid = this.isCVVValid();

        if (red && !securityCodeValid) {
            color = R.color.hpf_error;

        } else {
            color = R.color.hpf_accent;
        }

        this.mCardCVV.setTextColor(ContextCompat.getColor(getActivity(), color));
    }
    private void putCardOwnerInRed(boolean red) {

        int color;

        if (red && !this.isCardOwnerValid()) {
            color = R.color.hpf_error;

        } else {
            color = R.color.hpf_accent;
        }

        this.mCardOwner.setTextColor(ContextCompat.getColor(getActivity(), color));
    }

    private void putCardNumberInRed(boolean red) {

        int color;

        if (red && !this.isCardNumberValid()) {
            color = R.color.hpf_error;

        } else {

            color = R.color.hpf_accent;

            //every time the user types
            if (!TextUtils.isEmpty(mCardNumber.getText()) && inferedPaymentProduct != null) {

                Set<String> paymentProductCodes = FormHelper.getPaymentProductCodes(mCardNumber.getText().toString(), getActivity());
                if (paymentProductCodes.size() == 1 && !paymentProductCodes.contains(inferedPaymentProduct)) {

                    String[] things = paymentProductCodes.toArray(new String[1]);

                    inferedPaymentProduct = things[0];

                    // we do pass switchLayout as a parameter if config is enabled
                    LinearLayout switchLayout = this.isPaymentCardStorageConfigEnabled() ? mCardStorageSwitchLayout : null;

                    if (isDomesticNetwork(inferedPaymentProduct)) {

                        //on garde le inferedPaymentProduct (VISA) mais on met l'image et titre de CB
                        //mCallback.updatePaymentProduct(basicPaymentProduct.getPaymentProductDescription());
                        mCardBehaviour.updatePaymentProduct(inferedPaymentProduct);

                        mCardBehaviour.updateForm(mCardNumber, mCardCVV, mCardExpiration, mCardCVVLayout, mSecurityCodeInfoTextview, mSecurityCodeInfoImageview, switchLayout, false, getActivity());

                    } else {

                        mCallback.updatePaymentProduct(inferedPaymentProduct);
                        mCardBehaviour.updatePaymentProduct(inferedPaymentProduct);
                        mCardBehaviour.updateForm(mCardNumber, mCardCVV, mCardExpiration, mCardCVVLayout, mSecurityCodeInfoTextview, mSecurityCodeInfoImageview, switchLayout, false, getActivity());
                    }


                    this.putEverythingInRed();

                }

                // on va essayer d'atteindre la taille max.

            } else {

                //textfield is empty
                mCardStorageSwitch.setChecked(false);
            }

        }

        this.mCardNumber.setTextColor(ContextCompat.getColor(getActivity(), color));
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

