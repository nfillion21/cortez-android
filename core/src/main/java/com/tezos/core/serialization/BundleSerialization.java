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

package com.tezos.core.serialization;

import android.os.Bundle;

import com.tezos.core.utils.Utils;

import java.net.URL;
import java.util.Date;
import java.util.Map;

/**
 * Created by nfillion on 18/03/16.
 */

public class BundleSerialization implements IBundle {

    protected Bundle bundle;

    public BundleSerialization() {

        this.setBundle(new Bundle());
    }

    @Override
    public void putFloat(String key, Float floatNumber) {

        if (floatNumber != null) {
            this.getBundle().putFloat(key, floatNumber);
        }
    }

    @Override
    public void putString(String key, String string) {

        if (string != null) {
            this.getBundle().putString(key, string);
        }
    }

    @Override
    public void putInt(String key, Integer integer) {

        if (integer != null) {
            this.getBundle().putInt(key, integer);
        }
    }

    @Override
    public void putBool(String key, Boolean bool) {

        if (bool != null) {
            this.getBundle().putBoolean(key, bool);
        }
    }

    @Override
    public void putBundle(String key, Bundle bundle) {

        if (bundle != null) {
            this.getBundle().putBundle(key, bundle);
        }
    }

    @Override
    public void putMapJSON(String key, Map<String,String> map) {

        String mapString = Utils.mapToJson(map);
        if (mapString != null) {
            this.getBundle().putString(key, mapString);
        }
    }

    @Override
    public void putDate(String key, Date date) {

        if (date != null) {
            String stringDate = Utils.getStringFromDateISO8601(date);
            this.getBundle().putString(key, stringDate);
        }
    }

    @Override
    public void putUrl(String key, URL url) {

        if (url != null) {
            this.getBundle().putString(key, url.toString());
        }
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

}
