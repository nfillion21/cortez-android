package com.tezos.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.tezos.android.R;
import com.tezos.android.requests.order.PaymentPageRequest;
import com.tezos.core.models.CustomTheme;

/**
 * Created by nfillion on 21/12/2016.
 */

public abstract class PaymentScreenActivity extends AppCompatActivity
{
    public static void start(Activity activity)
    {
        CustomTheme theme = new CustomTheme(
                R.color.hpf_primary,
                R.color.hpf_primary_dark,
                R.color.theme_blue_text);
        Bundle themeBundle = theme.toBundle();

        PaymentPageRequest paymentPageRequest = buildPageRequest("1");
        Bundle paymentPageRequestBundle = paymentPageRequest.toBundle();

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

    private static PaymentPageRequest buildPageRequest(String orderId)
    {
        PaymentPageRequest paymentPageRequest = new PaymentPageRequest();

        paymentPageRequest.setOrderId(orderId);

        String amount = "4.5";
        paymentPageRequest.setAmount(Float.parseFloat(amount));

        return paymentPageRequest;
    }
}
