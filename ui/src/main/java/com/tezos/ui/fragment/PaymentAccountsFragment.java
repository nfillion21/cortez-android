package com.tezos.ui.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.tezos.core.client.GatewayClient;
import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.adapter.PaymentAccountsAdapter;
import com.tezos.ui.widget.OffsetDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nfillion on 26/02/16.
 */

public class PaymentAccountsFragment extends Fragment implements PaymentAccountsAdapter.OnItemClickListener
{
    private static final String STATE_IS_LOADING = "isLoading";

    private PaymentAccountsAdapter mAdapter;
    private GatewayClient mGatewayClient;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;

    private List<Account> accountList;

    protected boolean mLoadingMode;
    protected int mCurrentLoading = -1;

    public static PaymentAccountsFragment newInstance(Bundle customTheme)
    {
        PaymentAccountsFragment fragment = new PaymentAccountsFragment();

        Bundle bundle = new Bundle();
        bundle.putBundle(CustomTheme.TAG, customTheme);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if (mGatewayClient != null)
        {
            mGatewayClient.reLaunchOperations(mCurrentLoading);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //necessary to handle the request
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_payment_accounts, container, false);
    }

    private void launchRequest()
    {
        //mAdapter.updateAccounts(accountList);

        /*
        setLoadingMode(true);

        final Bundle paymentPageRequestBundle = getArguments().getBundle(PaymentPageRequest.TAG);
        final PaymentPageRequest paymentPageRequest = PaymentPageRequest.fromBundle(paymentPageRequestBundle);

        mGatewayClient = new GatewayClient(getActivity());
        mCurrentLoading = AbstractClient.RequestLoaderId.PaymentProductsReqLoaderId.getIntegerValue();
        mGatewayClient.getPaymentProducts(paymentPageRequest, new PaymentProductsCallback()
        {
            @Override
            public void onSuccess(List<PaymentProduct> pProducts)
            {
                cancelOperations();

                if (pProducts != null && !pProducts.isEmpty())
                {
                    accountList = updatedAccounts(pProducts, paymentPageRequest.isPaymentCardGroupingEnabled());
                    if (accountList != null) {
                        mAdapter.updatePaymentProducts(accountList);
                    }

                }
                else
                {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            if (getActivity() != null)
                            {
                                getActivity().finish();
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setDescription(R.string.error_title_default)
                            .setMessage(R.string.error_body_payment_products)
                            .setNegativeButton(R.string.error_button_dismiss, dialogClickListener)
                            .setCancelable(false)
                            .show();
                }
            }

            @Override
            public void onError(Exception error)
            {
                // an error occurred
                cancelOperations();

                if (getActivity() != null)
                {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            if (getActivity() != null)
                            {
                                getActivity().finish();
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setDescription(R.string.error_title_default)
                            .setMessage(R.string.error_body_default)
                            .setNegativeButton(R.string.error_button_dismiss, dialogClickListener)
                            .setCancelable(false)
                            .show();
                }
            }
        });
        */
    }

    private List<Account> updatedAccounts(List<Account> accounts)
    {
        /*
        boolean atLeastOneCard = false;
        boolean atLeastOneNoCard = false;

        if (isGroupingCard != null && isGroupingCard.equals(Boolean.TRUE))
        {
            Iterator<PaymentProduct> iter = accounts.iterator();
            while (iter.hasNext())
            {
                PaymentProduct p = iter.next();
                if (p.isTokenizable())
                {
                    iter.remove();
                    atLeastOneCard = true;
                }
                else
                {
                    atLeastOneNoCard = true;
                }
            }

            if (atLeastOneCard)
            {
                PaymentProduct cardProduct = new PaymentProduct();
                cardProduct.setCode(PaymentProduct.PaymentProductCategoryCodeCard);
                cardProduct.setPaymentProductDescription(getActivity().getString(R.string.payment_product_card_description));
                cardProduct.setTokenizable(true);

                // there are other payment products
                if (atLeastOneNoCard)
                {
                    accounts.add(0, cardProduct);

                }
                else
                {
                    // just one card, represented by card product
                    onClick(null, cardProduct);
                    return null;
                }
            }
        }
        else
        {
            // do something only if there is one tokenizable
            if (accounts.size() == 1)
            {
                PaymentProduct paymentProduct = accounts.get(0);
                if (paymentProduct.isTokenizable())
                {
                    onClick(null, paymentProduct);
                    return null;
                }
            }
        }
*/
        return accounts;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mProgressBar = view.findViewById(R.id.progress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.tz_light)));
        }
        else
        {
            mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(getActivity(), R.color.tz_light), PorterDuff.Mode.SRC_IN);
        }

        mRecyclerView = view.findViewById(R.id.products);
        setUpAccountGrid(mRecyclerView);

        if (savedInstanceState == null)
        {
            launchRequest();
        }
        else
        {
            if (accountList != null && !accountList.isEmpty())
            {
                mAdapter.updateAccounts(accountList);
            }
        }
    }

    private void setLoadingMode(boolean loadingMode)
    {
        if (loadingMode)
        {
            mProgressBar.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        }
        else
        {
            mProgressBar.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }

        mLoadingMode = loadingMode;
    }

    private void setUpAccountGrid(final RecyclerView categoriesView)
    {
        final int spacing = getContext().getResources()
                .getDimensionPixelSize(R.dimen.spacing_nano);
        categoriesView.addItemDecoration(new OffsetDecoration(spacing));

        mAdapter = new PaymentAccountsAdapter(getActivity());
        mAdapter.setOnItemClickListener(this);

        categoriesView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {

        getActivity().supportStartPostponedEnterTransition();
        super.onResume();

        setLoadingMode(mLoadingMode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_LOADING, mLoadingMode);
    }

    public void cancelOperations()
    {
        if (mGatewayClient != null)
        {
            mGatewayClient.cancelOperation(getActivity());
            mGatewayClient = null;
        }

        setLoadingMode(false);
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<Account> accountList) {
        this.accountList = accountList;
    }

    @Override
    public void onClick(View view, Account account)
    {
        if (getActivity() != null)
        {
            getActivity().finish();
        }
        /*
        final Bundle paymentPageRequestBundle = getArguments().getBundle(PaymentPageRequest.TAG);
        final Bundle customThemeBundle = getArguments().getBundle(CustomTheme.TAG);
        final String signature = getArguments().getString(GatewayClient.SIGNATURE_TAG);

        Activity activity = getActivity();
        startPaymentFormActivityWithTransition(activity, view == null ? null :
                        view.findViewById(R.id.payment_product_title),
                paymentPageRequestBundle,
                customThemeBundle,
                paymentProduct,
                signature);
                */
    }
}
