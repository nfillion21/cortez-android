package com.tezos.ui.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.tezos.core.client.GatewayClient;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.SeedManager;
import com.tezos.ui.R;
import com.tezos.ui.activity.AddAddressActivity;
import com.tezos.ui.activity.PaymentAccountsActivity;
import com.tezos.ui.adapter.PaymentAccountsAdapter;
import com.tezos.ui.widget.OffsetDecoration;

import java.util.List;

import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.ExtendedPublicKey;
import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip44.AddressIndex;

import static io.github.novacrypto.bip44.BIP44.m;


/**
 * Created by nfillion on 26/02/16.
 */

public class PaymentAccountsFragment extends Fragment implements PaymentAccountsAdapter.OnItemClickListener
{
    private OnCardSelectedListener mCallback;

    private static final String STATE_IS_LOADING = "isLoading";

    private PaymentAccountsAdapter mAdapter;
    private GatewayClient mGatewayClient;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;

    private List<com.tezos.core.models.Account> accountList;

    protected boolean mLoadingMode;
    protected int mCurrentLoading = -1;

    private FloatingActionButton mAddFab;

    public interface OnCardSelectedListener
    {
        void onCardClicked(com.tezos.core.models.Account account);
    }

    public static PaymentAccountsFragment newInstance(Bundle customThemeBundle, PaymentAccountsActivity.Selection selection)
    {
        PaymentAccountsFragment fragment = new PaymentAccountsFragment();

        Bundle bundle = new Bundle();
        bundle.putBundle(CustomTheme.TAG, customThemeBundle);
        bundle.putString(PaymentAccountsActivity.SELECTED_REQUEST_CODE_KEY, selection.getStringValue());

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        /*
        if (mGatewayClient != null)
        {
            mGatewayClient.reLaunchOperations(mCurrentLoading);
        }
        */
        try
        {
            mCallback = (OnCardSelectedListener) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString()
                    + " must implement OnWordSelectedListener");
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mProgressBar = view.findViewById(R.id.progress);

        mAddFab = view.findViewById(R.id.add);
        mAddFab.setOnClickListener(v ->
        {
            Bundle args = getArguments();

            CustomTheme theme = CustomTheme.fromBundle(args.getBundle(CustomTheme.TAG));
            AddAddressActivity.start(getActivity(), theme);

            //TODO add an account

            byte[] seed = SeedManager.getInstance().getSeed(getActivity());

            ExtendedPrivateKey root = ExtendedPrivateKey.fromSeed(seed, Bitcoin.TEST_NET);

            final io.github.novacrypto.bip44.Account account =
                    m().purpose44()
                            .coinType(1729)
                            .account(0);
            final ExtendedPublicKey accountKey = root.derive(account, io.github.novacrypto.bip44.Account.DERIVATION)
                    .neuter();

            for (int i = 0; i < 20; i++)
            {
                final AddressIndex derivationPath = account.external().address(i);
                final ExtendedPublicKey publicKey = accountKey.derive(derivationPath, AddressIndex.DERIVATION_FROM_ACCOUNT);
                System.out.println(derivationPath + " = " + publicKey.p2pkhAddress());
            }
        });

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
            //launchRequest();
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

        Bundle args = getArguments();
        String selectionString = args.getString(PaymentAccountsActivity.SELECTED_REQUEST_CODE_KEY);
        Bundle customThemeBundle = args.getBundle(CustomTheme.TAG);
        CustomTheme customTheme = CustomTheme.fromBundle(customThemeBundle);

        mAdapter = new PaymentAccountsAdapter(getActivity(), PaymentAccountsActivity.Selection.fromStringValue(selectionString), customTheme);

        mAdapter.setOnItemClickListener(this);

        categoriesView.setAdapter(mAdapter);
    }

    @Override
    public void onResume()
    {
        //getActivity().supportStartPostponedEnterTransition();
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

    public List<com.tezos.core.models.Account> getAccountList()
    {
        return accountList;
    }

    public void setAccountList(List<com.tezos.core.models.Account> accountList)
    {
        this.accountList = accountList;
    }

    @Override
    public void onClick(View view, com.tezos.core.models.Account account)
    {
        if (mCallback != null)
        {
            mCallback.onCardClicked(account);
        }
    }
}
