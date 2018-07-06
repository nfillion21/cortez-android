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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;

public class AddAddressActivity extends AppCompatActivity
{
    private TextInputLayout mOwnerLayout;
    private TextInputEditText mOwner;

    private TextInputLayout mTzAddressLayout;
    private TextInputEditText mTzAddress;

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

        View.OnFocusChangeListener focusChangeListener = this.focusChangeListener();

        mOwnerLayout = findViewById(R.id.address_owner_inputlayout);
        mOwner = findViewById(R.id.address_owner);
        mOwner.addTextChangedListener(new GenericTextWatcher(mOwner));
        mOwner.setOnFocusChangeListener(focusChangeListener);
        mOwnerLayout.setError(" ");

        mTzAddressLayout = findViewById(R.id.tezos_address_inputlayout);
        mTzAddress = findViewById(R.id.tezos_address);
        mTzAddressLayout.setError(" ");
        mTzAddress.addTextChangedListener(new GenericTextWatcher(mTzAddress));
        mTzAddress.setOnFocusChangeListener(focusChangeListener);


        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);
        initToolbar(theme);

        mAddButton = findViewById(R.id.add_button);
        mAddButtonLayout = findViewById(R.id.add_button_layout);

        validateAddButton(isInputDataValid());

        putEverythingInRed();

        if (savedInstanceState == null)
        {
            //Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            //getSupportFragmentManager().beginTransaction()
                    //.replace(R.id.form_fragment_container, AbstractPaymentFormFragment.newInstance(paymentPageRequestBundle, customThemeBundle)).commit();
        }

    }

    protected boolean isInputDataValid()
    {
        if (
                this.isOwnerFormValid() &&
                        this.isTzAddressValid()
                )
        {
            return true;
        }

        return false;
    }

    private boolean isOwnerFormValid()
    {
        boolean isOwnerFormValid = false;

        if (!TextUtils.isEmpty(mOwner.getText()))
        {
            isOwnerFormValid = true;
        }

        return isOwnerFormValid;
    }

    private boolean isTzAddressValid()
    {
        boolean isTzAddressValid = false;

        if (!TextUtils.isEmpty(mTzAddress.getText()))
        {
            String addressText = mTzAddress.getText().toString();

            if ((addressText.startsWith("tz1") ||  addressText.startsWith("tz2") || addressText.startsWith("tz3"))
                &&
                    addressText.length() == 36)
            {
                isTzAddressValid = true;
            }
        }

        return isTzAddressValid;
    }

    protected void putEverythingInRed()
    {
        this.putOwnerFormInRed(true);
        this.putTzAddressInRed(true);
    }

    // put everything in RED

    private void putOwnerFormInRed(boolean red) {

        int color;

        boolean ownerFormValid = this.isOwnerFormValid();

        if (red && !ownerFormValid) {
            color = R.color.tz_error;

        } else {
            color = R.color.tz_accent;
        }

        this.mOwner.setTextColor(ContextCompat.getColor(this, color));
    }

    private void putTzAddressInRed(boolean red) {

        int color;

        boolean tzAddressValid = this.isTzAddressValid();

        if (red && !tzAddressValid) {
            color = R.color.tz_error;

        } else {
            color = R.color.tz_accent;
        }

        this.mTzAddress.setTextColor(ContextCompat.getColor(this, color));
    }

    private class GenericTextWatcher implements TextWatcher
    {
        private View v;
        private GenericTextWatcher(View view)
        {
            this.v = view;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        public void afterTextChanged(Editable editable)
        {
            int i = v.getId();

            if (i == R.id.address_owner)
            {
                putOwnerFormInRed(false);
            }
            else if (i == R.id.tezos_address)
            {
                putTzAddressInRed(false);
            }
            else
            {
                throw new UnsupportedOperationException(
                        "OnClick has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
            validateAddButton(isInputDataValid());
        }
    }

    private View.OnFocusChangeListener focusChangeListener()
    {
        return (v, hasFocus) ->
        {
            int i = v.getId();

            if (i == R.id.address_owner)
            {
                putOwnerFormInRed(!hasFocus);
            }
            else if (i == R.id.tezos_address)
            {
                putTzAddressInRed(!hasFocus);
            }
            else
            {
                throw new UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
        };
    }

    protected void validateAddButton(boolean validate)
    {
        if (validate)
        {
            Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
            CustomTheme theme = CustomTheme.fromBundle(themeBundle);

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
