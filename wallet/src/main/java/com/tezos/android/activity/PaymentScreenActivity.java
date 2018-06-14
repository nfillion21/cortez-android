package com.tezos.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.tezos.android.models.CustomTheme;
import com.tezos.android.requests.order.PaymentPageRequest;

/**
 * Created by nfillion on 21/12/2016.
 */

public abstract class PaymentScreenActivity extends AppCompatActivity
{
    public static void start(Activity activity, PaymentPageRequest paymentPageRequest, CustomTheme theme)
    {
        Bundle paymentPageRequestBundle = paymentPageRequest.toBundle();
        Bundle themeBundle = theme.toBundle();

        Intent paymentFormIntent = PaymentFormActivity.getStartIntent(activity, paymentPageRequestBundle, themeBundle);
        /*
        ActivityCompat.startActivityForResult(activity,
                startIntent,
                PaymentPageRequest.REQUEST_ORDER,
                //transitionBundle);
                //avoid glitch problem
                null);
                */

        ActivityCompat.startActivity(activity, paymentFormIntent, null);
        //PaymentFormActivity.start(activity, paymentPageRequest, theme);
    }
}
