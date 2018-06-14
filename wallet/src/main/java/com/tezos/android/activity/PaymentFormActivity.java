package com.tezos.android.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tezos.android.R;
import com.tezos.android.errors.exceptions.ApiException;
import com.tezos.android.errors.exceptions.HttpException;
import com.tezos.android.models.CustomTheme;
import com.tezos.android.requests.order.PaymentPageRequest;
import com.tezos.app.R;
import com.tezos.app.errors.Errors;
import com.tezos.app.errors.exceptions.ApiException;
import com.tezos.app.utils.ApiLevelHelper;

/**
 * Created by nfillion on 29/02/16.
 */
public class PaymentFormActivity extends AppCompatActivity {

    public static int SCAN_PERMISSION_REQUEST_CODE = 0x2100; // arbitrary int
    public static int SCAN_REQUEST_CODE = 0x2200; // arbitrary int

    private ImageButton mToolbarBack;
    private AlertDialog mDialog;
    private ProgressBar mProgressBar;

    public static Intent getStartIntent(Context context, Bundle paymentPageRequestBundle, Bundle themeBundle) {

        Intent starter = new Intent(context, PaymentFormActivity.class);

        starter.putExtra(PaymentPageRequest.TAG, paymentPageRequestBundle);

        starter.putExtra(CustomTheme.TAG, themeBundle);

        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_form);

        initToolbar();

        /*
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextCompat.getColor(this, customTheme.getTextColorPrimaryId())));

        } else {
            mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, customTheme.getTextColorPrimaryId()), PorterDuff.Mode.SRC_IN);
        }
        */
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        //it comes from ForwardWebViewActivity, handling 3DS

        if (requestCode == PaymentPageRequest.REQUEST_ORDER) {

            if (resultCode == R.id.transaction_succeed) {

                Bundle transactionBundle = data.getBundleExtra(Transaction.TAG);
                Transaction transaction = Transaction.fromBundle(transactionBundle);

                this.formResultAction(transaction, null);

            } else if (resultCode == R.id.transaction_failed) {

                Bundle exceptionBundle = data.getBundleExtra(Errors.TAG);
                ApiException exception = ApiException.fromBundle(exceptionBundle);

                this.formResultAction(null, exception);

            } else {

                //back pressed
                if (!isPaymentTokenizable()) {
                    forceBackPressed();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void formResultAction(Transaction transaction, Exception exception) {

        FormResult formResult = null;

        if (transaction != null) {
            formResult = this.manageTransactionState(transaction);

        } else if (exception != null) {
            formResult = this.manageTransactionError(exception);

        } else {
            //no-op
        }

        if (formResult != null) {

            switch (formResult) {

                // this never happens for now
                case FormActionReset: {

                    this.setLoadingMode(false, false);

                } break;

                case FormActionReload: {
                    //this is made directly
                    this.setLoadingMode(false, false);

                } break;

                case FormActionBackgroundReload: {

                    //this.setLoadingMode(true);
                    //it's on loading mode already
                    this.transactionNeedsReload(transaction);

                } break;

                case FormActionForward: {

                    this.setLoadingMode(false, true);
                    //do not dismiss the loading thing now
                } break;

                case FormActionQuit: {

                    //not need to stop the loading mode
                    finish();
                } break;

                default: {
                    //nothing
                    this.setLoadingMode(false, true);
                }
            }
        }
    }

    private void setLoadingMode(boolean loadingMode, boolean delay) {

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
        if (fragment != null) {

            AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;
            abstractPaymentFormFragment.setLoadingMode(loadingMode, delay);
        }
    }

    private enum FormResult {

        FormActionReset,
        FormActionReload,
        FormActionBackgroundReload,
        FormActionForward,
        FormActionQuit
    }

    private FormResult manageTransactionError(Exception exception) {

        ApiException apiException = (ApiException) exception;

        // Duplicate order
        if (this.isOrderAlreadyPaid(apiException)) {

            return FormResult.FormActionBackgroundReload;

        }

        // Final error (ex. max attempts exceeded)
        else if (this.isTransactionErrorFinal(apiException)) {

            Intent intent = getIntent();
            intent.putExtra(Errors.TAG, apiException.toBundle());
            setResult(R.id.transaction_failed, intent);

            return FormResult.FormActionQuit;
        }

        // Client error
        else if (apiException.getCause() != null) {

            HttpException httpException = (HttpException)apiException.getCause();

            Integer httpStatusCode = httpException.getStatusCode();
            if (httpStatusCode != null) {

                if (httpStatusCode.equals(Errors.Code.HTTPClient.getIntegerValue())) {

                    //we don't need to backgroundReload

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (!isPaymentTokenizable()) {
                                forceBackPressed();
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error_title_default)
                            .setMessage(R.string.error_body_default)
                            .setNegativeButton(R.string.error_button_dismiss, dialogClickListener)
                            .setCancelable(false)
                            .show();

                    return FormResult.FormActionReload;

                } else if (httpStatusCode.equals(Errors.Code.HTTPNetworkUnavailable.getIntegerValue())) {

                    //we don't need to backgroundReload

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE: {
                                    dialog.dismiss();

                                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
                                    if (fragment != null) {

                                        AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;
                                        abstractPaymentFormFragment.setLoadingMode(true, false);
                                        abstractPaymentFormFragment.launchRequest();
                                    }
                                }
                                break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.dismiss();
                                    if (!isPaymentTokenizable()) {
                                        forceBackPressed();
                                    }
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.error_title_connection)
                            .setMessage(R.string.error_body_default)
                            .setNegativeButton(R.string.error_button_dismiss, dialogClickListener)
                            .setPositiveButton(R.string.error_button_retry, dialogClickListener)
                            .setCancelable(false)
                            .show();

                    return FormResult.FormActionReload;

                }
            }
        }

        // Other connection or server error, unknown error
        this.showCancelRetryDialog();
        return FormResult.FormActionReload;

    }

    private void showCancelRetryDialog() {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE: {
                        dialog.dismiss();

                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
                        if (fragment != null) {

                            AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;
                            abstractPaymentFormFragment.setLoadingMode(true, false);
                            abstractPaymentFormFragment.launchRequest();
                        }
                    }

                    break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        if (!isPaymentTokenizable()) {
                            forceBackPressed();
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_title_default)
                .setMessage(R.string.error_body_default)
                .setNegativeButton(R.string.error_button_dismiss, dialogClickListener)
                .setPositiveButton(R.string.error_button_retry, dialogClickListener)
                .setCancelable(false)
                .show();

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initToolbar()
    {
        mToolbarBack = findViewById(R.id.back);
        //mToolbarBack.setColorFilter((ContextCompat.getColor(this,
                //getCustomTheme().getTextColorPrimaryId())));

        mToolbarBack.setOnClickListener(mOnClickListener);
        TextView titleView = findViewById(R.id.payment_product_title);

        titleView.setText("title");
        //titleView.setTextColor(ContextCompat.getColor(this,
                //getCustomTheme().getTextColorPrimaryId()));

        //titleView.setBackgroundColor(ContextCompat.getColor(this,
                //getCustomTheme().getColorPrimaryId()));
    }

    @SuppressLint("NewApi")
    public void setToolbarElevation(boolean shouldElevate) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            mToolbarBack.setElevation(shouldElevate ?
                    getResources().getDimension(R.dimen.elevation_header) : 0);
        }
    }

    final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v)
        {
            int i = v.getId();
            if (i == R.id.back)
            {
                onBackPressed();
            }
            else {
                throw new UnsupportedOperationException(
                        "OnClick has not been implemented for " + getResources().
                                getResourceName(v.getId()));
            }
        }
    };

    private void forceBackPressed() {

        //is it different from finish?
        super.onBackPressed();
        //finish();
    }

    /*
    @Override
    public void onBackPressed() {

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
        if (fragment != null) {

            AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;

            boolean loadingMode = abstractPaymentFormFragment.getLoadingMode();
            if (loadingMode == true) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE: {
                                dialog.dismiss();

                                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.form_fragment_container);
                                if (fragment != null) {

                                    AbstractPaymentFormFragment abstractPaymentFormFragment = (AbstractPaymentFormFragment)fragment;
                                    abstractPaymentFormFragment.cancelOperations();
                                }

                                forceBackPressed();
                            }
                            break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                dialog.dismiss();
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                mDialog = builder.setTitle(R.string.alert_transaction_loading_title)
                        .setMessage(R.string.alert_transaction_loading_body)
                        .setNegativeButton(R.string.alert_transaction_loading_no, dialogClickListener)
                        .setPositiveButton(R.string.alert_transaction_loading_yes, dialogClickListener)
                        .setCancelable(false)
                        .show();

            } else {

                super.onBackPressed();
            }

        } else {
            super.onBackPressed();
        }
    }
    */

    /*
    public CustomTheme getCustomTheme() {
        return customTheme;
    }

    public void setCustomTheme(CustomTheme customTheme) {
        this.customTheme = customTheme;
    }
    */
}
