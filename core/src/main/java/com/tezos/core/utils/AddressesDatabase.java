/*
(*****************************************************************************)
(*                                                                           *)
(* Open Source License                                                       *)
(* Copyright (c) 2018 Nomadic Development, Inc. <contact@tezcore.com>        *)
(*                                                                           *)
(* Permission is hereby granted, free of charge, to any person obtaining a   *)
(* copy of this software and associated documentation files (the "Software"),*)
(* to deal in the Software without restriction, including without limitation *)
(* the rights to use, copy, modify, merge, publish, distribute, sublicense,  *)
(* and/or sell copies of the Software, and to permit persons to whom the     *)
(* Software is furnished to do so, subject to the following conditions:      *)
(*                                                                           *)
(* The above copyright notice and this permission notice shall be included   *)
(* in all copies or substantial portions of the Software.                    *)
(*                                                                           *)
(* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR*)
(* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  *)
(* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL   *)
(* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER*)
(* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING   *)
(* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER       *)
(* DEALINGS IN THE SOFTWARE.                                                 *)
(*                                                                           *)
(*****************************************************************************)
*/

package com.tezos.core.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.tezos.core.models.Account;
import com.tezos.core.models.Address;

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

    public void remove(Context context, Address address)
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

    public void logOut(Context context)
    {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(ADDRESSES_KEY);
        //editor.commit();
        editor.apply();
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