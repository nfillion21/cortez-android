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

package com.tezos.ui.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.tezos.core.client.GatewayClient;
import com.tezos.core.models.Address;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.AddressesDatabase;
import com.tezos.core.utils.Utils;
import com.tezos.ui.R;
import com.tezos.ui.activity.AddAddressActivity;
import com.tezos.ui.activity.PaymentAccountsActivity;
import com.tezos.ui.adapter.PaymentAccountsAdapter;
import com.tezos.ui.widget.OffsetDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Created by nfillion on 26/02/16.
 */

public class PaymentAccountsFragment extends Fragment implements PaymentAccountsAdapter.OnItemClickListener, PaymentAccountsAdapter.OnItemLongClickListener
{
    private static final String ADDRESSES_ARRAYLIST = "addressList";

    private OnCardSelectedListener mCallback;

    private PaymentAccountsAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private List<Address> mAddressList;

    private FloatingActionButton mAddFab;

    public interface OnCardSelectedListener
    {
        void onCardClicked(Address address);
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AddAddressActivity.ADD_ADDRESS_REQUEST_CODE)
        {
            if (resultCode == R.id.add_address_succeed)
            {

                Snackbar snackbar = Snackbar.make(mAddFab, R.string.address_successfuly_added,
                        Snackbar.LENGTH_SHORT);
                View snackBarView = snackbar.getView();
                snackBarView.setBackgroundColor((ContextCompat.getColor(getActivity(),
                        R.color.tz_green)));
                snackbar.show();
                reloadList();
            }
        }
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

        /*
        mAddFab = view.findViewById(R.id.add);
        mAddFab.setOnClickListener(v ->
        {
            Bundle args = getArguments();

            CustomTheme theme = CustomTheme.fromBundle(args.getBundle(CustomTheme.TAG));
            AddAddressActivity.start(getActivity(), theme);
        });
        */

        if (savedInstanceState != null)
        {
            ArrayList<Bundle> messagesBundle = savedInstanceState.getParcelableArrayList(ADDRESSES_ARRAYLIST);
            mAddressList = bundlesToItems(messagesBundle);
        }
        else
        {
            mAddressList = new ArrayList<>();
        }

        mRecyclerView = view.findViewById(R.id.products);
        setUpAccountGrid(mRecyclerView);
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
        mAdapter.setOnItemLongClickListener(this);

        categoriesView.setAdapter(mAdapter);

        reloadList();
    }

    private void reloadList()
    {
        mAddressList.clear();

        Set<String> set = AddressesDatabase.getInstance().getAddresses(getActivity());

        if (set != null && !set.isEmpty()) {

            for (String addressString : set) {

                Bundle addressBundle = Utils.fromJSONString(addressString);
                if (addressBundle != null) {
                    Address address = Address.fromBundle(addressBundle);
                    mAddressList.add(address);
                }
            }
        }

        mAdapter.updateAddresses(mAddressList);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        ArrayList<Bundle> bundles = itemsToBundles(mAddressList);
        outState.putParcelableArrayList(ADDRESSES_ARRAYLIST, bundles);
    }

    private ArrayList<Bundle> itemsToBundles(List<Address> items)
    {
        if (items != null)
        {
            ArrayList<Bundle> bundles = new ArrayList<>(items.size());
            if (!items.isEmpty())
            {
                for (Address it : items)
                {
                    bundles.add(it.toBundle());
                }
            }
            return bundles;
        }

        return null;
    }

    private ArrayList<Address> bundlesToItems(ArrayList<Bundle> bundles)
    {
        if (bundles != null)
        {
            ArrayList<Address> items = new ArrayList<>(bundles.size());
            if (!bundles.isEmpty())
            {
                for (Bundle bundle : bundles)
                {
                    items.add(Address.fromBundle(bundle));
                }
            }
            return items;
        }

        return null;
    }

    @Override
    public void onClick(View view, Address address)
    {
        if (mCallback != null)
        {
            mCallback.onCardClicked(address);
        }
    }

    @Override
    public void onLongClick(View view, Address address)
    {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which)
            {
                case DialogInterface.BUTTON_POSITIVE:
                {
                    dialog.dismiss();

                    AddressesDatabase.getInstance().remove(getActivity(), address);
                    reloadList();
                }
                break;

                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.alert_deleting_address)
                .setMessage(R.string.alert_deleting_address_body)
                .setNegativeButton(android.R.string.cancel, dialogClickListener)
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setCancelable(false)
                .show();
    }
}
