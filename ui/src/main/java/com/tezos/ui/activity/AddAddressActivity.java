package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;

public class AddAddressActivity extends AppCompatActivity
{
    private TextInputLayout mAddressOwnerLayout;
    private TextInputEditText mAddressOwner;

    private TextInputLayout mTezosAddressLayout;
    private TextInputEditText mTezosAddress;

    private Button mAddButton;
    private FrameLayout mAddButtonLayout;

    public static Intent getStartIntent(Context context, Bundle themeBundle)
    {
        Intent starter = new Intent(context, AddAddressActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    public static void start(Activity activity, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, theme.toBundle());

        //TODO remove the request code
        ActivityCompat.startActivityForResult(activity, starter, PaymentFormActivity.TRANSFER_SELECT_REQUEST_CODE, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_address);

        mAddressOwnerLayout = findViewById(R.id.address_owner_inputlayout);
        mAddressOwner = findViewById(R.id.address_owner);

        mAddressOwnerLayout.setError(" ");

        mTezosAddressLayout = findViewById(R.id.tezos_address_inputlayout);
        mTezosAddress = findViewById(R.id.tezos_address);

        mTezosAddressLayout.setError(" ");

        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);
        initToolbar(theme);

        mAddButton = findViewById(R.id.add_button);
        mAddButtonLayout = findViewById(R.id.add_button_layout);
        validateCreateButton(true, theme);

        if (savedInstanceState == null)
        {
            //Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            //getSupportFragmentManager().beginTransaction()
                    //.replace(R.id.form_fragment_container, AbstractPaymentFormFragment.newInstance(paymentPageRequestBundle, customThemeBundle)).commit();
        }
    }

    protected void validateCreateButton(boolean validate, CustomTheme theme)
    {
        if (validate)
        {
            mAddButton.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
            mAddButtonLayout.setEnabled(true);
            mAddButtonLayout.setBackground(makeSelector(theme));

            Drawable[] drawables = mAddButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
        }
        else
        {
            mAddButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            mAddButtonLayout.setEnabled(false);

            CustomTheme greyTheme = new CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey);
            mAddButtonLayout.setBackground(makeSelector(greyTheme));

            Drawable[] drawables = mAddButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private StateListDrawable makeSelector(CustomTheme theme)
    {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(this, theme.getColorPrimaryDarkId())));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(this, theme.getColorPrimaryId())));
        return res;
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
}
