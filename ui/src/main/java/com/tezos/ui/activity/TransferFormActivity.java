package com.tezos.ui.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.TransferFormFragment;
import com.tezos.ui.interfaces.IConfirmCredentialHandler;
import com.tezos.ui.interfaces.IPasscodeHandler;
import com.tezos.ui.utils.ConfirmCredentialHelper;
import com.tezos.ui.utils.ScreenUtils;
import com.tezos.ui.utils.Storage;

/**
 * Created by nfillion on 29/02/16.
 */
public class TransferFormActivity extends AppCompatActivity implements IConfirmCredentialHandler, IPasscodeHandler
{
    public static int TRANSFER_SELECT_REQUEST_CODE = 0x2100; // arbitrary int

    public static Intent getStartIntent(Context context, Bundle seedBundle, Bundle themeBundle)
    {
        Intent starter = new Intent(context, TransferFormActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);
        starter.putExtra(Storage.TAG, seedBundle);

        return starter;
    }

    public static void start(Activity activity, Bundle seedBundle, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, seedBundle, theme.toBundle());
        //TODO remove this request code
        ActivityCompat.startActivityForResult(activity, starter, TransferFormActivity.TRANSFER_SELECT_REQUEST_CODE, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_form);

        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);

        Bundle seedDataBundle = getIntent().getBundleExtra(Storage.TAG);
        //Storage.SeedData seedData = Storage.Companion.fromBundle(seedDataBundle);

        initToolbar(theme);

        if (savedInstanceState == null)
        {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.form_fragment_container, TransferFormFragment.newInstance(seedDataBundle, themeBundle)).commit();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        launchPasscode();
    }

    @Override
    public void launchPasscode() {
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

        TextView mTitleBar = findViewById(R.id.barTitle);
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
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

    @Override
    public void launchConfirmCredential()
    {
        ConfirmCredentialHelper.launchConfirmCredential(this);
    }
}