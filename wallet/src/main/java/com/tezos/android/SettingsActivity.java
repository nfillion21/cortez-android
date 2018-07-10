package com.tezos.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.tezos.android.activities.PasscodeActivity;
import com.tezos.android.fragments.SettingsFragment;
import com.tezos.android.interfaces.IPasscodeHandler;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.utils.ArchLifecycleApp;

/**
 * Created by nfillion on 3/6/18.
 */

public class SettingsActivity extends AppCompatActivity implements SettingsFragment.OnRowSelectedListener, IPasscodeHandler
{
    private static final String TAG_SETTINGS = "SettingsTag";

    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, SettingsActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    public static void start(Activity activity, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, theme.toBundle());
        ActivityCompat.startActivityForResult(activity, starter, -1, null);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null)
        {
            SettingsFragment settingsFragment = SettingsFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.settings_container, settingsFragment, TAG_SETTINGS)
                    .commit();
        }

        initActionBar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PasscodeActivity.ASK_NEW_CODE_RESULT)
        {
            if (resultCode == R.id.passcode_succeed)
            {
                // success
                String code = data.getStringExtra(PasscodeActivity.BUNDLE_CODE);

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(PasscodeActivity.PASSCODE_KEY, code);
                editor.apply();
            }
            //else if (resultCode == R.id.passcode_failed) {// should not happen actually}
            else
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.settings_container);
                if (fragment != null)
                {
                    SettingsFragment settingsFragment = (SettingsFragment)fragment;
                    settingsFragment.notifyChanged();
                }
                // user just canceled
                // uncheck the password.
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        launchPasscode();
    }

    private void initActionBar()
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } catch (Exception e) {
            Log.getStackTraceString(e);}
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ImageButton mCloseButton = findViewById(R.id.close_button);
        mCloseButton.setColorFilter((ContextCompat.getColor(this, R.color.theme_tezos_text)));
        mCloseButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                //requests stop in onDestroy.
                finish();
            }
        });
    }

    @Override
    public void launchPasscode()
    {
        ArchLifecycleApp archLifecycleApp = (ArchLifecycleApp) getApplication();
        boolean isStarted = archLifecycleApp.isStarted();

        if (isStarted)
        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String codeGuess = sharedPref.getString(PasscodeActivity.PASSCODE_KEY, null);

            if (codeGuess != null)
            {
                Intent intent = new Intent(this, PasscodeActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onItemClicked() {

    }
}
