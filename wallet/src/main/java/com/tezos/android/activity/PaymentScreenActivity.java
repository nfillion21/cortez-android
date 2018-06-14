package com.tezos.android.activity;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

import com.hipay.fullservice.core.client.config.ClientConfig;
import com.hipay.fullservice.core.requests.order.PaymentPageRequest;
import com.hipay.fullservice.core.utils.PaymentCardTokenDatabase;
import com.tezos.android.models.CustomTheme;
import com.tezos.android.requests.order.PaymentPageRequest;
import com.tezos.app.models.CustomTheme;
import com.tezos.app.requests.order.PaymentPageRequest;

import java.util.Set;

/**
 * Created by nfillion on 21/12/2016.
 */

public abstract class PaymentScreenActivity extends AppCompatActivity {

    public static void start(Activity activity, PaymentPageRequest paymentPageRequest, CustomTheme theme) {

        PaymentFormActivity.start(activity, paymentPageRequest, theme);
    }
}
