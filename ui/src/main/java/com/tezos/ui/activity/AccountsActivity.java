package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.tezos.core.models.CustomTheme;
import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.ui.R;
import com.tezos.ui.fragment.PaymentAccountsFragment;

/**
 * Created by nfillion on 25/02/16.
 */
public class AccountsActivity extends AppCompatActivity
{
    public static void start(Activity activity, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, theme);

        ActivityOptionsCompat activityOptions = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, null);

        ActivityCompat.startActivityForResult(activity, starter, PaymentPageRequest.REQUEST_ORDER, activityOptions.toBundle());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        //no-op
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        //no-op
    }

    @NonNull
    static Intent getStartIntent(Context context, CustomTheme theme)
    {
        Intent starter = new Intent(context, AccountsActivity.class);

        if (theme == null)
        {
            theme = new CustomTheme(R.color.tz_primary,R.color.tz_primary_dark,R.color.tz_light);
        }
        starter.putExtra(CustomTheme.TAG, theme.toBundle());

        return starter;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PaymentPageRequest.REQUEST_ORDER)
        {
            /*
            if (resultCode == R.id.transaction_succeed)
            {
                setResult(R.id.transaction_succeed, data);
                finish();
            }
            else if (resultCode == R.id.transaction_failed)
            {
                setResult(R.id.transaction_failed, data);
                finish();
            }
            else
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.payment_products_container);
                if (fragment != null)
                {
                    PaymentProductsFragment paymentProductsFragment = (PaymentProductsFragment) fragment;
                    List<PaymentProduct> paymentProducts = paymentProductsFragment.getAccountList();
                    if (paymentProducts == null || paymentProducts.isEmpty())
                    {
                        finish();
                    }
                }
            }
            */
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_accounts);

        Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme customTheme = CustomTheme.fromBundle(customThemeBundle);

        if (savedInstanceState == null)
        {
            attachAccountGridFragment();
        }

        //useful when this activity is gonna be called with makeTransition
        supportPostponeEnterTransition();
    }

    private void attachAccountGridFragment()
    {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.payment_products_container);
        if (!(fragment instanceof PaymentAccountsFragment))
        {
            Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            fragment = PaymentAccountsFragment.newInstance(customThemeBundle);

            //fragment.setArguments(paymentPageRequestBundle);
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.payment_products_container, fragment)
                .commit();
    }
}

