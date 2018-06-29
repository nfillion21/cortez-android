package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.SearchWordDialogFragment;
import com.tezos.ui.fragment.RestoreWalletFragment;

public class RestoreWalletActivity extends AppCompatActivity implements RestoreWalletFragment.OnWordSelectedListener, SearchWordDialogFragment.OnSearchWordSelectedListener
{
    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, RestoreWalletActivity.class);
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

        setContentView(R.layout.activity_restore_wallet);

        if (savedInstanceState == null)
        {
            Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            RestoreWalletFragment restoreWalletFragment = RestoreWalletFragment.newInstance(customThemeBundle);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.restorewallet_container, restoreWalletFragment)
                    .commit();
        }
    }

    @Override
    public void onWordCardNumberClicked(int position)
    {
        SearchWordDialogFragment searchWordDialogFragment = SearchWordDialogFragment.newInstance(position);
        searchWordDialogFragment.show(getSupportFragmentManager(), "searchWordDialog");
    }

    @Override
    public void onSearchWordClicked(String word, int position)
    {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.restorewallet_container);
        if (fragment != null && fragment instanceof RestoreWalletFragment)
        {
            RestoreWalletFragment restoreWalletFragment = (RestoreWalletFragment) fragment;
            restoreWalletFragment.updateCard(word, position);
        }
    }
}
