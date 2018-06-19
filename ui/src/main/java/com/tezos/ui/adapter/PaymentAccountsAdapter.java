package com.tezos.ui.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.activity.PaymentAccountsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nfillion on 29/02/16.
 */

public class PaymentAccountsAdapter extends RecyclerView.Adapter<PaymentAccountsAdapter.ViewHolder>
{
    private final Resources mResources;
    private LayoutInflater mLayoutInflater;
    private Activity mActivity;
    private List<Account> mAccounts;

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener
    {
        void onClick(View view, Account paymentProduct);
    }

    public PaymentAccountsAdapter(Activity activity)
    {
        mActivity = activity;
        mResources = mActivity.getResources();
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());

        mAccounts = new ArrayList<>();
        for (int i = 0; i < 15; i++)
        {
            Account account = new Account();
            account.setTitle("My account " + i );
            if (i == 5)
            account.setPubKeyHash("ttz1RPBr6yaJK94JpLDA6Xt31Ju4JJhXFw1srtz1RPBr6yaJK94JpLDA6Xt31Ju4JJhXFw1srtz1RPBr6yaJK94JpLDA6Xt31Ju4JJhXFw1srz1RPBr6yaJK94JpLDA6Xt31Ju4JJhXFw1sr");
            else
            account.setPubKeyHash("tz1RPBr6yaJK94JpLDA6Xt31Ju4JJhXFw1sr");
            mAccounts.add(account);
        }

        //updateAccounts(activity);
        //emptyPaymentProducts();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mLayoutInflater
                .inflate(R.layout.item_payment_account, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        Account account = mAccounts.get(holder.getAdapterPosition());


        // break this dependency
        //PaymentAccountsActivity activity = (PaymentAccountsActivity)mActivity;

        //holder.itemView.setBackgroundColor(getColor(android.R.color.background_light));
        //holder.title.setText(account.getTitle());

        CustomTheme theme = null;
        if (theme == null)
        {
            theme = new CustomTheme(R.color.tz_primary,R.color.tz_primary_dark,R.color.tz_light);
        }

        holder.pubKeyHash.setText(account.getPubKeyHash());

        holder.title.setText(account.getTitle());
        holder.title.setTextColor(getColor(theme.getTextColorPrimaryId()));
        holder.title.setBackgroundColor(getColor(theme.getColorPrimaryId()));

        holder.itemView.setBackgroundColor(getColor(android.R.color.background_light));
        holder.itemView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mOnItemClickListener.onClick(v, getItem(holder.getAdapterPosition()));
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return mAccounts.size();
    }

    public Account getItem(int position)
    {
        return mAccounts.get(position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener)
    {
        mOnItemClickListener = onItemClickListener;
    }

    public void updateAccounts(List<Account> accounts)
    {
        mAccounts.clear();
        mAccounts.addAll(accounts);
        notifyDataSetChanged();
    }

    /**
     * Convenience method for color loading.
     *
     * @param colorRes The resource id of the color to load.
     * @return The loaded color.
     */
    private int getColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(mActivity, colorRes);
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        final TextView title;
        final TextView pubKeyHash;

        public ViewHolder(View container)
        {
            super(container);
            title = container.findViewById(R.id.payment_account_title);
            pubKeyHash = container.findViewById(R.id.payment_account_pub_key_hash);
        }
    }
}
