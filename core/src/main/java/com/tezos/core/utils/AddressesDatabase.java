package com.tezos.core.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.tezos.core.models.Account;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by nfillion on 09/07/2018.
 */
public class AddressesDatabase
{
    private static AddressesDatabase mInstance = null;

    final private String SHARED_PREFERENCES_NAME = "TezCore";
    final private String ADDRESSES_KEY = "addresses";

    private AddressesDatabase() {
    }

    public static AddressesDatabase getInstance()
    {
        if(mInstance == null)
        {
            mInstance = new AddressesDatabase();
        }
        return mInstance;
    }

    public void clearAddresses(Context context)
    {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(ADDRESSES_KEY);
        //editor.commit();
        editor.apply();
    }

    public void remove(Context context, Account address)
    {
        Set<String> addresses = this.getAddresses(context);

        if (address != null && !addresses.isEmpty())
        {
            Bundle addressBundle = address.toBundle();
            String addressJSONString = Utils.fromBundle(addressBundle);

            if (addresses.remove(addressJSONString))
            {
                SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                editor.putStringSet(ADDRESSES_KEY, addresses);
                //editor.commit();
                editor.apply();
            }
        }
    }

    public void add(Context context, Account account)
    {
        Bundle accountBundle = account.toBundle();
        String accountJSONString = Utils.fromBundle(accountBundle);

        Set<String> addresses = this.getAddresses(context);

        if (addresses != null && !addresses.isEmpty())
        {
            if (addresses.add(accountJSONString))
            {
                SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                // write on the old version
                editor.putStringSet(ADDRESSES_KEY, addresses);
                //editor.commit();
                editor.apply();
            }
        }
        else
        {
            //create it
            SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putStringSet(ADDRESSES_KEY, new HashSet<>(Arrays.asList(accountJSONString)));
            //editor.commit();
            editor.apply();
        }
    }

    public Set<String> getAddresses(Context context)
    {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        //return a copy to avoid the Android bug
        Set<String> set = preferences.getStringSet(ADDRESSES_KEY, null);
        if (set != null && !set.isEmpty())
        {
            return new HashSet<>(set);
        }

        return null;
    }
}