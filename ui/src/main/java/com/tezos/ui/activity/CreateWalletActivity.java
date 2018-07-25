package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.CreateWalletFragment;
import com.tezos.ui.fragment.RestoreWalletFragment;
import com.tezos.ui.fragment.SearchWordDialogFragment;
import com.tezos.ui.fragment.VerifyCreationWalletFragment;
import com.tezos.ui.interfaces.IPasscodeHandler;
import com.tezos.ui.utils.ScreenUtils;

public class CreateWalletActivity extends AppCompatActivity implements IPasscodeHandler, CreateWalletFragment.OnCreateWalletListener, VerifyCreationWalletFragment.OnVerifyWalletCreationListener, SearchWordDialogFragment.OnWordSelectedListener
{
    public static int CREATE_WALLET_REQUEST_CODE = 0x2300; // arbitrary int

    public static String MNEMONICS_STR = "mnemonics_str";

    private TextView mTitleBar;

    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, CreateWalletActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    public static void start(Activity activity, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, theme.toBundle());
        ActivityCompat.startActivityForResult(activity, starter, CREATE_WALLET_REQUEST_CODE, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_wallet);

        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);
        initToolbar(theme);

        if (savedInstanceState == null)
        {
            CreateWalletFragment createWalletFragment = CreateWalletFragment.newInstance(theme);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.create_wallet_container, createWalletFragment)
                    .commit();

            //TextView mTitleBar = findViewById(R.id.barTitle);
            //TitleBar.setText(R.string.create_wallet_title_1);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        launchPasscode();
    }

    @Override
    public void updateTitle()
    {
        // Update your UI here.
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.create_wallet_container);
        if (fragment != null)
        {
            String titleScreen = null;

            if (fragment instanceof VerifyCreationWalletFragment)
            {
                titleScreen = getString(R.string.create_wallet_title_2);
            }
            else
            if (fragment instanceof CreateWalletFragment)
            {
                titleScreen = getString(R.string.create_wallet_title_1);
            }
            if (mTitleBar != null)
            {
                mTitleBar.setText(titleScreen);
            }
        }
    }

    @Override
    public void launchPasscode()
    {
        ScreenUtils.launchPasscode(this);
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

        mTitleBar = findViewById(R.id.barTitle);
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateWalletValidated(String mnemonics)
    {
        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);

        VerifyCreationWalletFragment verifyCreationWalletFragment = VerifyCreationWalletFragment.newInstance(theme, mnemonics);
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.create_wallet_container, verifyCreationWalletFragment)
                .addToBackStack(null)
                .commit();

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener()
                {
                    public void onBackStackChanged()
                    {
                        updateTitle();
                    }
                });
    }

    @Override
    public void onVerifyWalletCardNumberClicked(int position)
    {
        SearchWordDialogFragment searchWordDialogFragment = SearchWordDialogFragment.newInstance(position);
        searchWordDialogFragment.show(getSupportFragmentManager(), SearchWordDialogFragment.TAG);
    }

    @Override
    public void onWordClicked(String word, int position)
    {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.create_wallet_container);
        if (fragment != null && fragment instanceof VerifyCreationWalletFragment)
        {
            VerifyCreationWalletFragment verifyCreationWalletFragment = (VerifyCreationWalletFragment) fragment;
            verifyCreationWalletFragment.updateCard(word, position);
        }
    }

    @Override
    public void mnemonicsVerified() {

    }
}
