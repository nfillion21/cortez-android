package com.tezos.core.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public class SeedManager
{
    private static SeedManager mInstance = null;
    final private static String SHARED_PREFERENCES_SEED = "Seed";

    final private static String SEED_KEY = "SeedKey";

    private SeedManager() {}

    public static SeedManager getInstance()
    {
        if(mInstance == null)
        {
            mInstance = new SeedManager();
        }
        return mInstance;
    }

    public void save(Context context, byte[] seed)
    {
        if (seed != null)
        {
            SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_SEED, 0);
            SharedPreferences.Editor editor = preferences.edit();

            String seedString = Base64.encodeToString(seed, Base64.NO_WRAP);
            editor.putString(SEED_KEY, seedString);
            //editor.commit();
            editor.apply();
        }
    }

    public byte[] getSeed(Context context)
    {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_SEED, 0);
        String seedString = preferences.getString(SEED_KEY, null);

        if (seedString != null)
        {
            return Base64.decode(seedString, Base64.NO_WRAP);
        }

        return null;
    }
}
