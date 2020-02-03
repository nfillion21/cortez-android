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

package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tezos.core.models.Account;
import com.tezos.core.models.Address;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.AddressBookFragment;

/**
 * Created by nfillion on 25/02/16.
 */
public class AddressBookActivity extends BaseSecureActivity implements AddressBookFragment.OnCardSelectedListener
{
    public static int TRANSFER_SELECT_REQUEST_CODE = 0x2300; // arbitrary int

    public static String SELECTED_REQUEST_CODE_KEY = "selectedRequestCodeKey";

    private FloatingActionButton mAddFab;

    public static void start(Activity activity, CustomTheme theme, Selection selection)
    {
        Intent starter = getStartIntent(activity, theme, selection);
        ActivityCompat.startActivityForResult(activity, starter, TRANSFER_SELECT_REQUEST_CODE, null);
    }

    @NonNull
    static Intent getStartIntent(Context context, CustomTheme theme, Selection selection)
    {
        Intent starter = new Intent(context, AddressBookActivity.class);
        starter.putExtra(CustomTheme.TAG, theme.toBundle());
        starter.putExtra(SELECTED_REQUEST_CODE_KEY, selection.getStringValue());

        return starter;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AddAddressActivity.ADD_ADDRESS_REQUEST_CODE)
        {
            if (resultCode == R.id.add_address_succeed)
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.payment_products_container);
                if (fragment != null)
                {
                    Snackbar snackbar = Snackbar.make(mAddFab, R.string.address_successfully_added,
                            Snackbar.LENGTH_SHORT);
                    View snackBarView = snackbar.getView();
                    snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                            R.color.tz_green)));
                    snackbar.show();

                    AddressBookFragment addressBookFragment = (AddressBookFragment) fragment;
                    addressBookFragment.reloadList();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_address_book);

        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);

        initToolbar(theme);

        mAddFab = findViewById(R.id.add);
        mAddFab.setOnClickListener(v ->
                AddAddressActivity.start(this, theme));

        if (savedInstanceState == null)
        {
            attachAccountGridFragment();
        }
    }

    private void initToolbar(CustomTheme theme)
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.getColorPrimaryDarkId()));
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this,
                theme.getColorPrimaryDarkId()));
        try
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        catch (Exception e)
        {
            Log.getStackTraceString(e);
        }

        ImageButton mCloseButton = findViewById(R.id.close_button);
        mCloseButton.setColorFilter((ContextCompat.getColor(this, theme.getTextColorPrimaryId())));
        mCloseButton.setOnClickListener(v -> {
            //requests stop in onDestroy.
            finish();
        });

        TextView mTitleBar = findViewById(R.id.barTitle);
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        String selectionString = getIntent().getStringExtra(AddressBookActivity.SELECTED_REQUEST_CODE_KEY);

        Selection selection = Selection.fromStringValue(selectionString);
        if (selection.equals(Selection.SelectionAccounts))
        {
            mTitleBar.setText(getString(R.string.select_source_title));
        }
        else if (selection.equals(Selection.SelectionAddresses))
        {
            //mTitleBar.setText(getString(R.string.select_destination_title));
            mTitleBar.setText(getString(R.string.select_an_address));
        }
    }

    private void attachAccountGridFragment()
    {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.payment_products_container);
        if (!(fragment instanceof AddressBookFragment))
        {
            Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            Intent intent = getIntent();
            String selectionString = intent.getStringExtra(SELECTED_REQUEST_CODE_KEY);
            fragment = AddressBookFragment.newInstance(CustomTheme.fromBundle(customThemeBundle), null, Selection.fromStringValue(selectionString));
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.payment_products_container, fragment)
                .commit();
    }

    @Override
    public void onCardClicked(Address address)
    {
        Intent intent = getIntent();
        String selectionString = intent.getStringExtra(SELECTED_REQUEST_CODE_KEY);

        Selection selection = Selection.fromStringValue(selectionString);
        intent.putExtra(Account.TAG, address.toBundle());

        switch (selection)
        {
            case SelectionAccounts:
            {
                setResult(R.id.transfer_src_selection_succeed, intent);
            }
            break;

            case SelectionAccountsAndAddresses:
            {
                setResult(R.id.transfer_dst_selection_succeed, intent);
            }
            break;

            case SelectionAddresses:
            {
                setResult(R.id.multisig_address_selection_succeed, intent);
            }
            break;

            default: //no-op;
                break;
        }

        finish();
    }

    public enum Selection
    {
        SelectionAccounts ("SelectionAccounts"),
        SelectionAddresses ("SelectionAddresses"),
        SelectionAccountsAndAddresses("SelectionAccountsAndAddresses");

        protected final String selection;
        Selection(String method)
        {
            this.selection = method;
        }

        public String getStringValue()
        {
            return this.selection;
        }

        public static Selection fromStringValue(String value)
        {
            if (value == null) return null;

            if (value.equalsIgnoreCase(SelectionAccounts.getStringValue()))
            {
                return SelectionAccounts;
            }

            if (value.equalsIgnoreCase(SelectionAddresses.getStringValue()))
            {
                return SelectionAddresses;
            }

            if (value.equalsIgnoreCase(SelectionAccountsAndAddresses.getStringValue()))
            {
                return SelectionAccountsAndAddresses;
            }
            return null;
        }
    }
}