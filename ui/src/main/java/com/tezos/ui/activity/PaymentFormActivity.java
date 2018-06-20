package com.tezos.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tezos.core.models.CustomTheme;
import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.ui.R;
import com.tezos.ui.fragment.AbstractPaymentFormFragment;

/**
 * Created by nfillion on 29/02/16.
 */
public class PaymentFormActivity extends AppCompatActivity
{
    public static int TRANSFER_SELECT_REQUEST_CODE = 0x2100; // arbitrary int

    private AlertDialog mDialog;

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

        /*
        CustomTheme customTheme = CustomTheme.fromBundle(customThemeBundle);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextCompat.getColor(this, customTheme.getTextColorPrimaryId())));

        } else {
            mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, customTheme.getTextColorPrimaryId()), PorterDuff.Mode.SRC_IN);
        }
        */

        if (savedInstanceState == null)
        {
            Bundle paymentPageRequestBundle = getIntent().getBundleExtra(PaymentPageRequest.TAG);
            Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.form_fragment_container, AbstractPaymentFormFragment.newInstance(paymentPageRequestBundle, customThemeBundle)).commit();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
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

    /*
    public CustomTheme getCustomTheme() {
        return customTheme;
    }

    public void setCustomTheme(CustomTheme customTheme) {
        this.customTheme = customTheme;
    }
    */
}
