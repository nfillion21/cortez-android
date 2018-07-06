package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.SearchWordDialogFragment;
import com.tezos.ui.fragment.RestoreWalletFragment;

import net.glxn.qrgen.android.QRCode;

public class RestoreWalletActivity extends AppCompatActivity implements RestoreWalletFragment.OnWordSelectedListener, SearchWordDialogFragment.OnSearchWordSelectedListener
{
    public static int RESTORE_WALLET_REQUEST_CODE = 0x2400; // arbitrary int

    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, RestoreWalletActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    public static void start(Activity activity, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, theme.toBundle());
        ActivityCompat.startActivityForResult(activity, starter, RESTORE_WALLET_REQUEST_CODE, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_restore_wallet);

        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);
        initToolbar(theme);

        if (savedInstanceState == null)
        {
            RestoreWalletFragment restoreWalletFragment = RestoreWalletFragment.newInstance(themeBundle);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.restorewallet_container, restoreWalletFragment)
                    .commit();
        }
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
