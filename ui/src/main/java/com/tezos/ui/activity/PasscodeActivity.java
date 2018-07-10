package com.tezos.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tezos.ui.R;
import com.tezos.ui.interfaces.IPasscodeHandler;
import com.tezos.ui.utils.ScreenUtils;

/**
 * Created by nfillion on 3/7/18.
 */

public class PasscodeActivity extends AppCompatActivity implements IPasscodeHandler
{
    private TextInputEditText mCode1;
    private TextInputEditText mCode2;
    private TextInputEditText mCode3;
    private TextInputEditText mCode4;

    private TextView mErrorTextview;
    private TextView mInfoTextview;

    public static final int ASK_NEW_CODE_RESULT = 0x2300;
    public static final String ASK_NEW_CODE_PARAMETER = "ask_new_code_parameter";
    public static final String BUNDLE_CODE = "bundle_code";

    public static final String PASSCODE_KEY = "passcode_key";

    private static final String TURNS_KEY = "turns_key";
    private static final String CODE_CACHE_KEY = "code_cache_key";

    private static final String INFO_KEY = "info_key";
    private static final String ERROR_KEY = "error_key";

    private boolean mAskNewCode;
    private int mTurns;
    private String mCodeCache;

    private String mInfoCache;
    private String mErrorCache;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_passcode);

        mCode1 = findViewById(R.id.code1_edittext);
        mCode2 = findViewById(R.id.code2_edittext);
        mCode3 = findViewById(R.id.code3_edittext);
        mCode4 = findViewById(R.id.code4_edittext);

        mCode1.addTextChangedListener(new GenericTextWatcher(mCode1));
        mCode2.addTextChangedListener(new GenericTextWatcher(mCode2));
        mCode3.addTextChangedListener(new GenericTextWatcher(mCode3));
        mCode4.addTextChangedListener(new GenericTextWatcher(mCode4));

        mErrorTextview = findViewById(R.id.error_textview);
        mInfoTextview = findViewById(R.id.info_textview);

        //mCode1.requestFocus();

        initActionBar();

        Intent intent = getIntent();
        mAskNewCode = intent.getBooleanExtra(ASK_NEW_CODE_PARAMETER, false);

        if (savedInstanceState != null)
        {
            mInfoCache = savedInstanceState.getString(INFO_KEY);
            mInfoTextview.setVisibility( mInfoCache != null ? View.VISIBLE : View.GONE );
            mInfoTextview.setText(mInfoCache);

            mErrorCache = savedInstanceState.getString(ERROR_KEY);
            mErrorTextview.setVisibility( mErrorCache != null ? View.VISIBLE : View.GONE );
            mErrorTextview.setText(mErrorCache);

            if (mAskNewCode)
            {
                mTurns = savedInstanceState.getInt(TURNS_KEY);
                mCodeCache = savedInstanceState.getString(CODE_CACHE_KEY);
            }
        }
        else
        {
            // first tour
            mTurns = 1;
            mErrorTextview.setVisibility(View.GONE);
            mErrorTextview.setText(null);

            if (mAskNewCode)
            {
                mInfoCache = getString(R.string.general_passcode);
                mInfoTextview.setText(mInfoCache);
                mInfoTextview.setVisibility(View.VISIBLE);
            }
            else
            {
                mInfoCache = null;
                mInfoTextview.setText(null);
                mInfoTextview.setVisibility(View.GONE);
            }
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

    private void initActionBar()
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } catch (Exception e) {
            Log.getStackTraceString(e);}
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void verifyNumber()
    {
        if (!TextUtils.isEmpty(mCode1.getText()) &&
                !TextUtils.isEmpty(mCode2.getText()) &&
                !TextUtils.isEmpty(mCode3.getText()) &&
                !TextUtils.isEmpty(mCode4.getText()) )
        {
            String code1 = mCode1.getText().toString();
            String code2 = mCode2.getText().toString();
            String code3 = mCode3.getText().toString();
            String code4 = mCode4.getText().toString();

            String code = code1 + code2 + code3 + code4;

            if (mAskNewCode)
            {
                this.definePasscode(code);
            }
            else
            {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String codeGuess = sharedPref.getString(PASSCODE_KEY, null);

                // it should never ask for code if preference_passcode_key value is null
                if (codeGuess != null && !codeGuess.equalsIgnoreCase(code))
                {
                    this.clearTexts();

                    mErrorCache = getString(R.string.incorrect_passcode);
                    mErrorTextview.setText(mErrorCache);
                    mErrorTextview.setVisibility(View.VISIBLE);

                    mInfoCache = null;
                    mInfoTextview.setVisibility(View.GONE);
                    mInfoTextview.setText(null);
                }
                else
                {
                    // user found the code
                    finish();
                }
            }
        }
    }

    private void clearTexts()
    {
        // wrong password
        mCode1.getText().clear();
        mCode2.getText().clear();
        mCode3.getText().clear();
        mCode4.getText().clear();

        mCode1.requestFocus();
    }

    private void definePasscode(String codeStr)
    {
        switch (mTurns)
        {
            case 1:
            {
                mCodeCache = codeStr;
                mTurns = 2;

                mErrorCache = null;
                mErrorTextview.setText(null);
                mErrorTextview.setVisibility(View.GONE);

                mInfoCache = getString(R.string.confirm_passcode);
                mInfoTextview.setText(mInfoCache);
                mInfoTextview.setVisibility(View.VISIBLE);

                this.clearTexts();

            } break;

            case 2:
            {
                if (mCodeCache.equalsIgnoreCase(codeStr))
                {
                    Intent data = getIntent();
                    data.putExtra(BUNDLE_CODE, codeStr);

                    setResult(R.id.passcode_succeed, data);
                    finish();
                }
                else
                {
                    mErrorCache = getString(R.string.error_different_passcodes);
                    mErrorTextview.setText(mErrorCache);
                    mErrorTextview.setVisibility(View.VISIBLE);

                    mInfoCache = getString(R.string.general_passcode);
                    mInfoTextview.setText(mInfoCache);
                    mInfoTextview.setVisibility(View.VISIBLE);

                    this.clearTexts();
                    mTurns = 1;
                }

            } break;

            default:
                //no-op
        }
    }

    private class GenericTextWatcher implements TextWatcher
    {
        private int enteringText;
        private int changeStart;

        private final View v;

        private GenericTextWatcher(View view)
        {
            this.v = view;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
            enteringText = after;
            changeStart = start;
        }

        public void onTextChanged(CharSequence s, int start, int before, int count)
        {}

        public void afterTextChanged(Editable editable)
        {
            // if 2 numbers are entering, then it was empty before.
            if (enteringText == 2)
            {
                editable.delete(1,2);
            }
            else if (enteringText == 1)
            {
                // now we need to know if there is 2 characters.
                if (editable.length() != 1)
                {
                    // then we can remove the bad one
                    if (changeStart == 0)
                    {
                        // remove the 2nd character
                        editable.delete(1,2);
                    }
                    else
                    {
                        // remove the 1st character
                        editable.delete(0,1);
                    }
                }
            }

            String version = editable.toString();
            if (!version.isEmpty())
            {
                int i = v.getId();
                if (i == R.id.code1_edittext)
                {
                    mCode2.requestFocus();
                }
                else if (i == R.id.code2_edittext)
                {
                    mCode3.requestFocus();
                }
                else if (i == R.id.code3_edittext)
                {
                    mCode4.requestFocus();
                }
                else if (i == R.id.code4_edittext) {}
                else
                {
                    throw new UnsupportedOperationException(
                            "OnClick has not been implemented for " + getResources().
                                    getResourceName(v.getId()));
                }

                verifyNumber();
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = getIntent();
        boolean askNewCode = intent.getBooleanExtra(ASK_NEW_CODE_PARAMETER, false);
        if (askNewCode)
        {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putInt(TURNS_KEY, mTurns);
        outState.putString(CODE_CACHE_KEY, mCodeCache);

        outState.putString(INFO_KEY, mInfoCache);
        outState.putString(ERROR_KEY, mErrorCache);
    }
}
