package com.tezos.ui.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.AbstractPaymentFormFragment;
import com.tezos.ui.interfaces.IConfirmCredentialHandler;
import com.tezos.ui.utils.ConfirmCredentialHelper;

/**
 * Created by nfillion on 29/02/16.
 */
public class PaymentFormActivity extends AppCompatActivity implements IConfirmCredentialHandler
{
    public static int TRANSFER_SELECT_REQUEST_CODE = 0x2100; // arbitrary int

    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, PaymentFormActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    public static void start(Activity activity, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, theme.toBundle());
        //TODO remove this request code
        ActivityCompat.startActivityForResult(activity, starter, PaymentFormActivity.TRANSFER_SELECT_REQUEST_CODE, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_form);

        if (savedInstanceState == null)
        {
            Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.form_fragment_container, AbstractPaymentFormFragment.newInstance(customThemeBundle)).commit();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        this.launchConfirmCredential();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ConfirmCredentialHelper.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS)
        {
            ConfirmCredentialHelper.onActivityResult(requestCode, resultCode, data);
        }
        else
        {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
            if (fragment != null)
            {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    private void setLoadingMode(boolean loadingMode, boolean delay)
    {
        /*
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
        if (fragment != null) {

            AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;
            abstractPaymentFormFragment.setLoadingMode(loadingMode, delay);
        }
        */
    }

    @Override
    public void launchConfirmCredential()
    {
        ConfirmCredentialHelper.launchConfirmCredential(this);
    }
}
