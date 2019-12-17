/*
(*****************************************************************************)
(*                                                                           *)
(* Open Source License                                                       *)
(* Copyright (c) 2018 Nomadic Development, Inc. <contact@tezcore.com>        *)
(*                                                                           *)
(* Permission is hereby granted, free of charge, to any person obtaining a   *)
(* copy of this software and associated documentation files (the "Software"),*)
(* to deal in the Software without restriction, including without limitation *)
(* the rights to use, copy, modify, merge, publish, distribute, sublicense,  *)
(* and/or sell copies of the Software, and to permit persons to whom the     *)
(* Software is furnished to do so, subject to the following conditions:      *)
(*                                                                           *)
(* The above copyright notice and this permission notice shall be included   *)
(* in all copies or substantial portions of the Software.                    *)
(*                                                                           *)
(* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR*)
(* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  *)
(* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL   *)
(* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER*)
(* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING   *)
(* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER       *)
(* DEALINGS IN THE SOFTWARE.                                                 *)
(*                                                                           *)
(*****************************************************************************)
*/

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

import com.tezos.core.models.Address;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.activity.AddressBookActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nfillion on 29/02/16.
 */

public class AddressBookAdapter extends RecyclerView.Adapter<AddressBookAdapter.ViewHolder>
{
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

    public AddressBookAdapter(Activity activity, AddressBookActivity.Selection selection, CustomTheme customTheme)
    {
        mActivity = activity;
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());
        mCustomTheme = customTheme;

        mAddresses = new ArrayList<>();

        /*

        if (selection.equals(AddressBookActivity.Selection.SelectionAccounts))
        {
            removeAddresses(mAddresses);
        }
        else if (selection.equals(AddressBookActivity.Selection.SelectionAddresses))
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
                .inflate(R.layout.item_account_addressbook, parent, false));
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
