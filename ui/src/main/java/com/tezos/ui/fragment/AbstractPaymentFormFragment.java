package com.tezos.ui.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;

/**
 * Created by nfillion on 29/02/16.
 */

public abstract class AbstractPaymentFormFragment extends Fragment
{
    //private static final String STATE_IS_LOADING = "isLoading";
    //private static final String CURRENT_LOADER_ID = "currentLoaderId";

    protected ProgressBar mProgressBar;
    OnCallbackOrderListener mCallback;
    protected LinearLayout mCardInfoLayout;

    protected boolean mLoadingMode = false;
    protected int mCurrentLoading = -1;

    public abstract void launchRequest();
    public interface OnCallbackOrderListener {

        //void onCallbackOrderReceived(Transaction transaction, Exception exception);
        void updatePaymentProduct(String title);
        void dismissDialogs();
    }

    protected TextInputEditText mAmount;
    protected TextInputLayout mAmountLayout;

    protected abstract boolean isInputDataValid();

    public static AbstractPaymentFormFragment newInstance(Bundle customTheme)
    {
        AbstractPaymentFormFragment fragment;

        fragment = new TransferFormFragment();

        Bundle args = new Bundle();
        args.putBundle(CustomTheme.TAG, customTheme);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //important for the magic lines
        setRetainInstance(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.setLoadingMode(mLoadingMode, false);
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        /*
        try {
            mCallback = (OnCallbackOrderListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
            + " must implement OnCallbackOrderListener");
        }
        */
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // ---- magic lines starting here -----
        // call this to re-connect with an existing
        // loader (after screen configuration changes for e.g!)

        /*
        if (mSecureVaultClient != null && mCurrentLoading == AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue())
        {
            if (mSecureVaultClient.canRelaunch())
            {
                mSecureVaultClient.reLaunchOperations(AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue());

            }
            else
            {
                cancelLoaderId(AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue());

                //check if launchRequest
                cancelOperations();
                launchRequest();
            }
        }

        if (mGatewayClient != null && mCurrentLoading > AbstractClient.RequestLoaderId.GenerateTokenReqLoaderId.getIntegerValue() )
        {
            if (mGatewayClient.canRelaunch())
            {
                mGatewayClient.reLaunchOperations(mCurrentLoading);
            }
            else
            {
                //cancelLoaderId(mCurrentLoading);
                cancelOperations();
                launchRequest();
            }
        }
        */

        //----- end magic lines -----
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public boolean getLoadingMode() {
        return mLoadingMode;
    }

    public abstract void setLoadingMode(boolean loadingMode, boolean delay);


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View contentView = inflater.inflate(R.layout.fragment_payment_form, container, false);
        return contentView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        //outState.putBoolean(STATE_IS_LOADING, mLoadingMode);
        //outState.putInt(CURRENT_LOADER_ID, mCurrentLoading);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        initContentViews(view);
    }

    protected void initContentViews(View view) {

        mProgressBar = (ProgressBar) view.findViewById(R.id.empty);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.colorAccent)));

        } else {
            mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        }
        mCardInfoLayout = view.findViewById(R.id.card_info_layout);

        //validatePayButton(isCreateButtonValid());
    }

    /*
    public void launchBackgroundReload(Transaction transaction) {

        if (transaction != null) {

            String transactionReference = transaction.getTransactionReference();

            Bundle args = getArguments();
            final String signature = args.getString(GatewayClient.SIGNATURE_TAG);

            mCurrentLoading = AbstractClient.RequestLoaderId.TransactionReqLoaderId.getIntegerValue();
            mGatewayClient = new GatewayClient(getActivity());
                    mGatewayClient.getTransactionWithReference(transactionReference, signature, new TransactionDetailsCallback() {

                        @Override
                        public void onSuccess(final Transaction transaction) {

                            if (mCallback != null) {
                                cancelLoaderId(AbstractClient.RequestLoaderId.TransactionReqLoaderId.getIntegerValue());
                                mCallback.onCallbackOrderReceived(transaction, null);
                            }
                        }

                        @Override
                        public void onError(Exception error) {

                            if (mCallback != null) {
                                cancelLoaderId(AbstractClient.RequestLoaderId.TransactionReqLoaderId.getIntegerValue());
                                mCallback.onCallbackOrderReceived(null, error);
                            }
                        }
                    });
        }
        else {

            Bundle args = getArguments();
            final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(args.getBundle(PaymentPageRequest.TAG));
            final String signature = args.getString(GatewayClient.SIGNATURE_TAG);

            String orderId = paymentPageRequest.getOrderId();
            mCurrentLoading = AbstractClient.RequestLoaderId.TransactionsReqLoaderId.getIntegerValue();
            mGatewayClient = new GatewayClient(getActivity());
            mGatewayClient.getTransactionsWithOrderId(orderId, signature, new TransactionsDetailsCallback() {

                @Override
                public void onSuccess(List<Transaction> transactions) {
                    if (mCallback != null) {
                        cancelLoaderId(AbstractClient.RequestLoaderId.TransactionsReqLoaderId.getIntegerValue());
                        mCallback.onCallbackOrderReceived(transactions.get(0), null);
                    }
                }

                @Override
                public void onError(Exception error) {
                    if (mCallback != null) {
                        cancelLoaderId(AbstractClient.RequestLoaderId.TransactionsReqLoaderId.getIntegerValue());
                        mCallback.onCallbackOrderReceived(null, error);
                    }
                }
            });
        }
    }

    protected void cancelLoaderId(int loaderId) {

        mCurrentLoading = -1;

        switch (loaderId) {

            //securevault generateToken
            case 0: {

                if (mSecureVaultClient != null) {
                    mSecureVaultClient.cancelOperation(getActivity());
                    mSecureVaultClient = null;
                }

            } break;

            //anything else
            default: {

                if (mGatewayClient != null) {
                    mGatewayClient.cancelOperation(getActivity());
                    mGatewayClient = null;
                }
            }
        }
    }

    public void cancelOperations() {

        if (mGatewayClient != null) {
            mGatewayClient.cancelOperation(getActivity());
            mGatewayClient = null;
        }

        if (mSecureVaultClient != null) {
            mSecureVaultClient.cancelOperation(getActivity());
            mSecureVaultClient = null;
        }
    }
    */

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        //this.cancelOperations();
    }
}
