package com.tezos.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.activity.PasscodeActivity;

public class ScreenUtils
{
    public static StateListDrawable makeSelector(Context context, CustomTheme theme)
    {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(context, theme.getColorPrimaryDarkId())));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(context, theme.getColorPrimaryId())));
        return res;
    }

    public static void launchPasscode(Activity activity)
    {
        ArchLifecycleApp archLifecycleApp = (ArchLifecycleApp) activity.getApplication();
        boolean isStarted = archLifecycleApp.isStarted();

        if (isStarted)
        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
            String codeGuess = sharedPref.getString(PasscodeActivity.PASSCODE_KEY, null);

            if (codeGuess != null)
            {
                Intent intent = new Intent(activity, PasscodeActivity.class);
                activity.startActivity(intent);
            }
        }
    }
}
