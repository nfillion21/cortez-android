package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.TezosUtils;
import com.tezos.ui.R;

public class CreateWalletActivity extends AppCompatActivity
{
    private static final String MNEMONICS_KEY = "mnemonics_key";

    private FloatingActionButton mRenewFab;
    private TextView mMnemonicsTextview;
    private String mMnemonicsString;

    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, CreateWalletActivity.class);
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

        setContentView(R.layout.activity_create_wallet);

        mMnemonicsTextview = findViewById(R.id.mnemonics_textview);

        mRenewFab = findViewById(R.id.renew);
        mRenewFab.setOnClickListener(v ->
        {
            int i = v.getId();
            if (i == R.id.renew)
            {
                mRenewFab.setEnabled(false);
                removeDoneFab(() ->
                {
                    mMnemonicsString = TezosUtils.generateNovaMnemonics();
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
            //Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            //getSupportFragmentManager().beginTransaction()
            //.replace(R.id.form_fragment_container, AbstractPaymentFormFragment.newInstance(paymentPageRequestBundle, customThemeBundle)).commit();
            mMnemonicsString = TezosUtils.generateNovaMnemonics();
            if (mMnemonicsString != null)
            {
                mMnemonicsTextview.setText(mMnemonicsString);
            }
        }
        else
        {
            mMnemonicsString = savedInstanceState.getString(MNEMONICS_KEY, null);
            if (mMnemonicsString != null)
            {
                mMnemonicsTextview.setText(mMnemonicsString);
            }
        }
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
        mRenewFab.setEnabled(true);

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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString(MNEMONICS_KEY, mMnemonicsString);
    }
}
