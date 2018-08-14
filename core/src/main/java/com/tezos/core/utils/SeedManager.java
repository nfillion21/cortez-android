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
