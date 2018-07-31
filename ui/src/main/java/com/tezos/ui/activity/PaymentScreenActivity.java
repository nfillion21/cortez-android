package com.tezos.ui.activity;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

import com.tezos.core.models.CustomTheme;

/**
 * Created by nfillion on 21/12/2016.
 */

public abstract class PaymentScreenActivity extends AppCompatActivity
{
    public static void start(Activity activity, CustomTheme theme)
    {
        TransferFormActivity.start(activity, theme);
    }
}
