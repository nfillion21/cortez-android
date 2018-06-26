package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.TezosUtils;
import com.tezos.ui.R;

/**
 * Created by nfillion on 21/12/2016.
 */

public abstract class PaymentScreenActivity extends AppCompatActivity
{
    public static void start(Activity activity)
    {
        CustomTheme theme = new CustomTheme(
                R.color.tz_primary,
                R.color.tz_primary_dark,
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

        String words = TezosUtils.generateNovaMnemonic();

        byte[] seed = TezosUtils.generateNovaSeed(words);

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
