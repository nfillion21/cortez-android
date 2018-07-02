package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.tezos.core.models.CustomTheme;
import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.ui.R;

/**
 * Created by nfillion on 21/12/2016.
 */

public abstract class PaymentScreenActivity extends AppCompatActivity
{
    public static void start(Activity activity, CustomTheme theme)
    {
        PaymentFormActivity.start(activity, theme);
    }
}
