package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tezos.core.crypto.CryptoUtils;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;

public class CreateWalletActivity extends AppCompatActivity
{
    private static final String MNEMONICS_KEY = "mnemonics_key";
    private static final String BACKUP_CHECKBOX_KEY = "backup_checkbox_key";

    public static int CREATE_WALLET_REQUEST_CODE = 0x2300; // arbitrary int

    private FloatingActionButton mRenewFab;
    private TextView mMnemonicsTextview;
    private String mMnemonicsString;

    private Button mCreateButton;
    private AppCompatCheckBox mBackupCheckbox;
    private FrameLayout mCreateButtonLayout;

    private boolean mBackupChecked;

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

        mMnemonicsTextview = findViewById(R.id.mnemonics_textview);
        //mMnemonicsTextview.setTextColor(ContextCompat.getColor(this, theme.getColorPrimaryDarkId()));
        mMnemonicsTextview.setTextColor(ContextCompat.getColor(this, theme.getColorPrimaryDarkId()));

        mBackupCheckbox = findViewById(R.id.backup_checkbox);

        /*
        mBackupCheckbox.setOnClickListener(view ->
        {
            validateCreateButton(mBackupChecked, null);
        });
        */

        mCreateButton = findViewById(R.id.create_button);
        mCreateButtonLayout = findViewById(R.id.create_button_layout);

        mCreateButtonLayout.setVisibility(View.VISIBLE);

        mCreateButton.setText(R.string.create_wallet);

        mCreateButtonLayout.setOnClickListener(v ->
        {
            //setLoadingMode(true,false);
            //launchRequest();

            if (mMnemonicsString != null)
            {
                Intent intent = getIntent();

                //TODO verify if it does always work
                Bundle keyBundle = CryptoUtils.generateKeys(mMnemonicsString);
                intent.putExtra(CryptoUtils.WALLET_BUNDLE_KEY, keyBundle);
                setResult(R.id.create_wallet_succeed, intent);
                finish();
            }
        });

        mRenewFab = findViewById(R.id.renew);
        mRenewFab.setOnClickListener(v ->
        {
            int i = v.getId();
            if (i == R.id.renew)
            {
                mBackupCheckbox.setEnabled(false);

                mRenewFab.setEnabled(false);
                removeDoneFab(() ->
                {
                    mMnemonicsString = CryptoUtils.generateMnemonics();
                    mMnemonicsTextview.setText(mMnemonicsString);
                    // renew the mnemonic
                    showDoneFab();
                });
            }
            else
            {
                throw new UnsupportedOperationException(
                        "The onClick method has not been implemented for " + getResources()
                                .getResourceEntryName(v.getId()));
            }
        });

        if (savedInstanceState == null)
        {
            mMnemonicsString = CryptoUtils.generateMnemonics();
            if (mMnemonicsString != null)
            {
                mMnemonicsTextview.setText(mMnemonicsString);
            }

            mBackupChecked = false;
        }
        else
        {
            mMnemonicsString = savedInstanceState.getString(MNEMONICS_KEY, null);
            if (mMnemonicsString != null)
            {
                mMnemonicsTextview.setText(mMnemonicsString);
            }

            mBackupChecked = savedInstanceState.getBoolean(BACKUP_CHECKBOX_KEY, false);
            mBackupCheckbox.setChecked(mBackupChecked);
        }

        validateCreateButton(isCreateButtonValid(), null);

        mBackupCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            mBackupChecked  = buttonView.isChecked();
            validateCreateButton(isCreateButtonValid(), theme);
        });
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

    protected boolean isCreateButtonValid()
    {
        if (mBackupChecked)
        {
            return true;
        }

        return false;
    }

    private void removeDoneFab(@Nullable Runnable endAction) {
        ViewCompat.animate(mRenewFab)
                .scaleX(0)
                .scaleY(0)
                .alpha(0)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(endAction)
                .start();
    }

    private void showDoneFab()
    {
        mRenewFab.show();
        mRenewFab.setScaleX(0f);
        mRenewFab.setScaleY(0f);
        ViewCompat.animate(mRenewFab)
                .scaleX(1)
                .scaleY(1)
                .alpha(1)
                .setStartDelay(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        mRenewFab.setEnabled(true);

        mBackupCheckbox.setEnabled(true);
    }

    protected void validateCreateButton(boolean validate, CustomTheme theme)
    {
        if (validate)
        {
            mCreateButton.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
            mCreateButtonLayout.setEnabled(true);
            mCreateButtonLayout.setBackground(makeSelector(theme));

            Drawable[] drawables = mCreateButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

            mRenewFab.setEnabled(false);
            mRenewFab.hide();
        }
        else
        {
            mCreateButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            mCreateButtonLayout.setEnabled(false);

            CustomTheme greyTheme = new CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey);
            mCreateButtonLayout.setBackground(makeSelector(greyTheme));

            Drawable[] drawables = mCreateButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, android.R.color.white));

            mRenewFab.show();
            mRenewFab.setEnabled(true);
        }
    }

    private StateListDrawable makeSelector(CustomTheme theme)
    {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(this, theme.getColorPrimaryDarkId())));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(this, theme.getColorPrimaryId())));
        return res;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString(MNEMONICS_KEY, mMnemonicsString);
        outState.putBoolean(BACKUP_CHECKBOX_KEY, mBackupChecked);
    }
}
