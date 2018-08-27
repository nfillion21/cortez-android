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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.zxing.Result;
import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.AddressesDatabase;
import com.tezos.ui.R;

import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class AddAddressActivity extends BaseSecureActivity implements ZXingScannerView.ResultHandler
{
    public static int ADD_ADDRESS_REQUEST_CODE = 0x2400; // arbitrary int

    public static int SCAN_PERMISSION_REQUEST_CODE = 0x2800; // arbitrary int
    public static int SCAN_REQUEST_CODE = 0x2900; // arbitrary int

    private TextInputLayout mOwnerLayout;
    private TextInputEditText mOwner;

    private TextInputLayout mTzAddressLayout;
    private TextInputEditText mTzAddress;

    private Button mAddButton;
    private FrameLayout mAddButtonLayout;

    private ZXingScannerView mScannerView;

    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, AddAddressActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    public static void start(Activity activity, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, theme.toBundle());
        ActivityCompat.startActivityForResult(activity, starter, ADD_ADDRESS_REQUEST_CODE, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_address);

        View.OnFocusChangeListener focusChangeListener = this.focusChangeListener();

        mOwnerLayout = findViewById(R.id.address_owner_inputlayout);
        mOwner = findViewById(R.id.address_owner);
        mOwner.addTextChangedListener(new GenericTextWatcher(mOwner));
        mOwner.setOnFocusChangeListener(focusChangeListener);
        mOwnerLayout.setError(" ");

        mTzAddressLayout = findViewById(R.id.tezos_address_inputlayout);
        mTzAddress = findViewById(R.id.tezos_address);
        mTzAddressLayout.setError(" ");
        mTzAddress.addTextChangedListener(new GenericTextWatcher(mTzAddress));
        mTzAddress.setOnFocusChangeListener(focusChangeListener);


        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);
        initToolbar(theme);

        mAddButton = findViewById(R.id.add_button);
        mAddButtonLayout = findViewById(R.id.add_button_layout);

        mAddButtonLayout.setOnClickListener(v ->
        {

            askForScanPermission();

            /*
            String addressName = mOwner.getText().toString();
            String publicKeyHash = mTzAddress.getText().toString();

            if (addressName != null && publicKeyHash != null)
            {
                Account account = new Account();
                account.setDescription(addressName);
                account.setPubKeyHash(publicKeyHash);

                AddressesDatabase.getInstance().add(this, account);

                //Intent intent = getIntent();
                //intent.putExtra(Address.TAG, address.toBundle());

                setResult(R.id.add_address_succeed, null);
                finish();
            }
            */
        });

        validateAddButton(isInputDataValid());

        putEverythingInRed();

        if (savedInstanceState == null)
        {
            //Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            //getSupportFragmentManager().beginTransaction()
                    //.replace(R.id.form_fragment_container, AbstractPaymentFormFragment.newInstance(paymentPageRequestBundle, customThemeBundle)).commit();
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    protected boolean isInputDataValid()
    {
        if (
                this.isOwnerFormValid() &&
                        this.isTzAddressValid()
                )
        {
            return true;
        }

        return false;
    }

    private boolean isOwnerFormValid()
    {
        boolean isOwnerFormValid = false;

        if (!TextUtils.isEmpty(mOwner.getText()))
        {
            isOwnerFormValid = true;
        }

        return isOwnerFormValid;
    }

    private boolean isTzAddressValid()
    {
        boolean isTzAddressValid = false;

        if (!TextUtils.isEmpty(mTzAddress.getText()))
        {
            String addressText = mTzAddress.getText().toString();

            if ((addressText.toLowerCase().startsWith("tz1") ||  addressText.toLowerCase().startsWith("tz2") || addressText.toLowerCase().startsWith("tz3"))
                &&
                    addressText.length() == 36)
            {
                isTzAddressValid = true;
            }
        }

        return isTzAddressValid;
    }

    protected void putEverythingInRed()
    {
        this.putOwnerFormInRed(true);
        this.putTzAddressInRed(true);
    }

    // put everything in RED

    private void putOwnerFormInRed(boolean red) {

        int color;

        boolean ownerFormValid = this.isOwnerFormValid();

        if (red && !ownerFormValid) {
            color = R.color.tz_error;

        } else {
            color = R.color.tz_accent;
        }

        this.mOwner.setTextColor(ContextCompat.getColor(this, color));
    }

    private void putTzAddressInRed(boolean red) {

        int color;

        boolean tzAddressValid = this.isTzAddressValid();

        if (red && !tzAddressValid) {
            color = R.color.tz_error;

        } else {
            color = R.color.tz_accent;
        }

        this.mTzAddress.setTextColor(ContextCompat.getColor(this, color));
    }

    @Override
    public void handleResult(Result result) {

    }

    private class GenericTextWatcher implements TextWatcher
    {
        private View v;
        private GenericTextWatcher(View view)
        {
            this.v = view;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        public void afterTextChanged(Editable editable)
        {
            int i = v.getId();

            if (i == R.id.address_owner)
            {
                putOwnerFormInRed(false);
            }
            else if (i == R.id.tezos_address)
            {
                putTzAddressInRed(false);
            }
            else
            {
                throw new UnsupportedOperationException(
                        "OnClick has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
            validateAddButton(isInputDataValid());
        }
    }

    private View.OnFocusChangeListener focusChangeListener()
    {
        return (v, hasFocus) ->
        {
            int i = v.getId();

            if (i == R.id.address_owner)
            {
                putOwnerFormInRed(!hasFocus);
            }
            else if (i == R.id.tezos_address)
            {
                putTzAddressInRed(!hasFocus);
            }
            else
            {
                throw new UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
        };
    }

    protected void validateAddButton(boolean validate)
    {
        if (validate)
        {
            Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
            CustomTheme theme = CustomTheme.fromBundle(themeBundle);

            mAddButton.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
            mAddButtonLayout.setEnabled(true);
            mAddButtonLayout.setBackground(makeSelector(theme));

            Drawable[] drawables = mAddButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
        }
        else
        {
            mAddButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            mAddButtonLayout.setEnabled(false);

            CustomTheme greyTheme = new CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey);
            mAddButtonLayout.setBackground(makeSelector(greyTheme));

            Drawable[] drawables = mAddButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private StateListDrawable makeSelector(CustomTheme theme)
    {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(this, theme.getColorPrimaryDarkId())));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(this, theme.getColorPrimaryId())));
        return res;
    }

    private void initToolbar(CustomTheme theme)
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.getColorPrimaryId()));
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
    }

    private void askForScanPermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA))
            {
                View.OnClickListener clickListener = v ->
                {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                };

                Snackbar.make(findViewById(R.id.content), getString(R.string.scan_address_permission), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.settings), clickListener)
                        .setActionTextColor(Color.YELLOW)
                        .show();

            }
            else
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        SCAN_PERMISSION_REQUEST_CODE);
            }
        }
        else
        {
            launchScanCard();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        if (requestCode == SCAN_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                launchScanCard();
            }
        }
    }

    private void launchScanCard()
    {
        //SimpleScannerActivity activity = new SimpleScannerActivity()

        Intent starter = new Intent(this, SimpleScannerActivity.class);
        //starter.putExtra(CustomTheme.TAG, themeBundle);
        //starter.putExtra(PKH_KEY, publicKeyHash);

        ActivityCompat.startActivityForResult(this, starter, -1, null);
    }
}
