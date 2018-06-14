package com.tezos.android.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.tezos.android.R;
import com.tezos.android.models.CustomTheme;
import com.tezos.android.requests.order.PaymentPageRequest;
import com.tezos.android.utils.ApiLevelHelper;

/**
 * Created by nfillion on 29/02/16.
 */
public class PaymentFormActivity extends AppCompatActivity
{
    public static int SCAN_PERMISSION_REQUEST_CODE = 0x2100; // arbitrary int
    public static int SCAN_REQUEST_CODE = 0x2200; // arbitrary int

    private ImageButton mToolbarBack;
    private AlertDialog mDialog;
    private ProgressBar mProgressBar;

    public static Intent getStartIntent(Context context, Bundle paymentPageRequestBundle, Bundle themeBundle)
    {
        Intent starter = new Intent(context, PaymentFormActivity.class);

        starter.putExtra(PaymentPageRequest.TAG, paymentPageRequestBundle);

        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_form);

        initToolbar();

        /*
        Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

        CustomTheme customTheme = CustomTheme.fromBundle(customThemeBundle);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextCompat.getColor(this, customTheme.getTextColorPrimaryId())));

        } else {
            mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, customTheme.getTextColorPrimaryId()), PorterDuff.Mode.SRC_IN);
        }
        */
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    private void setLoadingMode(boolean loadingMode, boolean delay) {

        /*
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
        if (fragment != null) {

            AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;
            abstractPaymentFormFragment.setLoadingMode(loadingMode, delay);
        }
        */
    }

    private enum FormResult {

        FormActionReset,
        FormActionReload,
        FormActionBackgroundReload,
        FormActionForward,
        FormActionQuit
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initToolbar()
    {
        //mToolbarBack = findViewById(R.id.back);
        //mToolbarBack.setColorFilter((ContextCompat.getColor(this,
                //getCustomTheme().getTextColorPrimaryId())));

        //mToolbarBack.setOnClickListener(mOnClickListener);
        //TextView titleView = findViewById(R.id.payment_product_title);

        //titleView.setText("title");
        //titleView.setTextColor(ContextCompat.getColor(this,
                //getCustomTheme().getTextColorPrimaryId()));

        //titleView.setBackgroundColor(ContextCompat.getColor(this,
                //getCustomTheme().getColorPrimaryId()));
    }

    @SuppressLint("NewApi")
    public void setToolbarElevation(boolean shouldElevate) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            mToolbarBack.setElevation(shouldElevate ?
                    getResources().getDimension(R.dimen.elevation_header) : 0);
        }
    }

    final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v)
        {
            int i = v.getId();
            /*
            if (i == R.id.back)
            {
                onBackPressed();
            }
            else {
                throw new UnsupportedOperationException(
                        "OnClick has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
            */
        }
    };

    private void forceBackPressed()
    {
        //is it different from finish?
        super.onBackPressed();
        //finish();
    }

    /*
    @Override
    public void onBackPressed() {

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
        if (fragment != null) {

            AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;

            boolean loadingMode = abstractPaymentFormFragment.getLoadingMode();
            if (loadingMode == true) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE: {
                                dialog.dismiss();

                                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
                                if (fragment != null) {

                                    AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;
                                    abstractPaymentFormFragment.cancelOperations();
                                }

                                forceBackPressed();
                            }
                            break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                dialog.dismiss();
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                mDialog = builder.setTitle(R.string.alert_transaction_loading_title)
                        .setMessage(R.string.alert_transaction_loading_body)
                        .setNegativeButton(R.string.alert_transaction_loading_no, dialogClickListener)
                        .setPositiveButton(R.string.alert_transaction_loading_yes, dialogClickListener)
                        .setCancelable(false)
                        .show();

            } else {

                super.onBackPressed();
            }

        } else {
            super.onBackPressed();
        }
    }
    */

    /*
    public CustomTheme getCustomTheme() {
        return customTheme;
    }

    public void setCustomTheme(CustomTheme customTheme) {
        this.customTheme = customTheme;
    }
    */
}
