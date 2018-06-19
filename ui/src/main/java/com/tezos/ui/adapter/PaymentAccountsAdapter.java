package com.tezos.ui.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;

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
        for (int i = 0; i < 5; i++)
        {
            Account account = new Account();
            account.setDescription("My account " + i );
            account.setPubKeyHash("tz1RPBr6yaJK94JpLDA6Xt31Ju4JJhXFw1sr");
            if (i%3 == 0)
            {
                account.setPrivateKeyHash("TZ1RPBr6yaJK94JpLDA6Xt31Ju4JJhXFw1sr");
            }
            mAccounts.add(account);
        }

        removeStandardAccounts(mAccounts);

        /*
        // sort messages by date, oldest last.
        Collections.sort(mAccounts, new Comparator<Account>()
        {
            @Override
            public int compare(Account o1, Account o2)
            {
                if (o1.getPrivateKeyHash() == null) {
                    return (o2.getPrivateKeyHash() == null) ? 0 : -1;
                }
                if (o2.getPrivateKeyHash() == null)
                {
                    return 1;
                }
                return o1.getPrivateKeyHash().compareTo(o2.getPrivateKeyHash());
            }
        });
        */
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
        //holder.description.setText(account.getDescription());

        CustomTheme theme = null;
        if (theme == null)
        {
            theme = new CustomTheme(R.color.tz_primary,R.color.tz_primary_dark,R.color.tz_light);
        }

        if (account.getPrivateKeyHash() != null)
        {
            holder.keyIcon.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.keyIcon.setVisibility(View.INVISIBLE);
        }

        holder.pubKeyHash.setText(account.getPubKeyHash());

        holder.title.setText(account.getDescription());
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

    private void removeStandardAccounts(List<Account> accountList)
    {
        int size = accountList.size();
        List<Account> itemsToRemove = new ArrayList<>(size);

        for (Account a: accountList)
        {
            if (a.getPrivateKeyHash() == null)
            {
                itemsToRemove.add(a);
            }
        }

        accountList.removeAll(itemsToRemove);
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        final TextView title;
        final TextView pubKeyHash;
        final ImageView keyIcon;

        public ViewHolder(View container)
        {
            super(container);
            title = container.findViewById(R.id.payment_account_title);
            pubKeyHash = container.findViewById(R.id.payment_account_pub_key_hash);
            keyIcon = container.findViewById(R.id.payment_account_key_icon);
        }
    }
}
