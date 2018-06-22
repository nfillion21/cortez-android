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
import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.activity.AddAccountActivity;
import com.tezos.ui.activity.PaymentAccountsActivity;
import com.tezos.ui.adapter.PaymentAccountsAdapter;
import com.tezos.ui.widget.OffsetDecoration;

import java.util.List;


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

    private List<Account> accountList;

    protected boolean mLoadingMode;
    protected int mCurrentLoading = -1;

    private FloatingActionButton mAddFab;

    public interface OnCardSelectedListener
    {
        void onCardClicked(Account account);
    }

    public static PaymentAccountsFragment newInstance(Bundle customTheme, PaymentAccountsActivity.Selection selection)
    {
        PaymentAccountsFragment fragment = new PaymentAccountsFragment();

        Bundle bundle = new Bundle();
        bundle.putBundle(CustomTheme.TAG, customTheme);
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
                    + " must implement OnCardSelectedListener");
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
        mAddFab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Bundle args = getArguments();

                CustomTheme theme = CustomTheme.fromBundle(args.getBundle(CustomTheme.TAG));
                AddAccountActivity.start(getActivity(), theme);
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

        mAdapter = new PaymentAccountsAdapter(getActivity(), PaymentAccountsActivity.Selection.fromStringValue(selectionString));

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

    public List<Account> getAccountList()
    {
        return accountList;
    }

    public void setAccountList(List<Account> accountList)
    {
        this.accountList = accountList;
    }

    @Override
    public void onClick(View view, Account account)
    {
        if (mCallback != null)
        {
            mCallback.onCardClicked(account);
        }
    }
}
