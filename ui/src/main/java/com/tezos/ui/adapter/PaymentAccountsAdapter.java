package com.tezos.ui.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.tezos.core.models.Account;
import com.tezos.core.models.Address;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.AddressesDatabase;
import com.tezos.ui.R;
import com.tezos.ui.activity.PaymentAccountsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by nfillion on 29/02/16.
 */

public class PaymentAccountsAdapter extends RecyclerView.Adapter<PaymentAccountsAdapter.ViewHolder>
{
    private final Resources mResources;
    private LayoutInflater mLayoutInflater;
    private Activity mActivity;
    private List<Address> mAddresses;

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;
    private CustomTheme mCustomTheme;

    public interface OnItemClickListener
    {
        void onClick(View view, Address paymentProduct);
    }

    public interface OnItemLongClickListener
    {
        void onLongClick(View view, Address address);
    }

    public PaymentAccountsAdapter(Activity activity, PaymentAccountsActivity.Selection selection, CustomTheme customTheme)
    {
        mActivity = activity;
        mResources = mActivity.getResources();
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());
        mCustomTheme = customTheme;

        mAddresses = new ArrayList<>();

        /*

        if (selection.equals(PaymentAccountsActivity.Selection.SelectionAccounts))
        {
            removeAddresses(mAddresses);
        }
        else if (selection.equals(PaymentAccountsActivity.Selection.SelectionAddresses))
        {
            removeAccounts(mAddresses);
        }
        else
        {
            //do not remove anything.
        }
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
        Address address = mAddresses.get(holder.getAdapterPosition());

        /*
        if (address.getPrivateKeyHash() != null)
        {
            holder.keyIcon.setImageResource(R.drawable.vpn_key_black);
        }
        else
        {
            holder.keyIcon.setImageResource(R.drawable.redeem_black_24);
        }
        */

        holder.pubKeyHash.setText(address.getPubKeyHash());

        holder.title.setText(address.getDescription());
        holder.title.setTextColor(getColor(mCustomTheme.getTextColorPrimaryId()));
        holder.title.setBackgroundColor(getColor(mCustomTheme.getColorPrimaryId()));

        holder.itemView.setBackgroundColor(getColor(android.R.color.background_light));

        holder.itemView.setOnClickListener((View v) -> {
            mOnItemClickListener.onClick(v, getItem(holder.getAdapterPosition()));
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view)
            {
                mOnItemLongClickListener.onLongClick(view, getItem(holder.getAdapterPosition()));
                return false;
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return mAddresses.size();
    }

    public Address getItem(int position)
    {
        return mAddresses.get(position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener)
    {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener)
    {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    public void updateAddresses(List<Address> addresses)
    {
        mAddresses.clear();
        if (addresses != null && !addresses.isEmpty())
        {
            mAddresses.addAll(addresses);
        }
        notifyDataSetChanged();
    }

    /**
     * Convenience method for color loading.
     *
     * @param colorRes The resource id of the color to load.
     * @return The loaded color.
     */
    private int getColor(@ColorRes int colorRes)
    {
        return ContextCompat.getColor(mActivity, colorRes);
    }

    private void removeAccounts(List<Address> addressList)
    {
        int size = addressList.size();
        List<Address> itemsToRemove = new ArrayList<>(size);

        for (Address a: addressList)
        {
            /*
            if (a.getPrivateKeyHash() != null)
            {
                itemsToRemove.add(a);
            }
            */
        }

        addressList.removeAll(itemsToRemove);
    }

    private void removeAddresses(List<Address> accountList)
    {
        int size = accountList.size();
        List<Address> itemsToRemove = new ArrayList<>(size);

        for (Address a: accountList)
        {
            /*
            if (a.getPrivateKeyHash() == null)
            {
                itemsToRemove.add(a);
            }
            */
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
            pubKeyHash = container.findViewById(R.id.src_payment_account_pub_key_hash);
            keyIcon = container.findViewById(R.id.payment_account_key_icon);
        }
    }
}
