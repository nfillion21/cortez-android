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

package com.tezos.core.mapper.interfaces;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;

import org.json.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by nfillion on 12/01/2017.
 */

public class UriMapper implements IBehaviour {

    protected Uri uriObject;
    protected UrlQuerySanitizer mSanitizer;

    public UriMapper(Uri uri) {

        this.setUriObject(uri);
        mSanitizer = new UrlQuerySanitizer(uri.toString());
    }

    @Override
    public Float getFloatForKey(String key) {

        String value = mSanitizer.getValue(key);

        if (value != null) {

            Float floatForKey;
            try {
                floatForKey = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                floatForKey = null;
            }
            return floatForKey;
        }

        return null;
    }

    @Override
    public String getStringForKey(String key) {

        return mSanitizer.getValue(key);
    }

    @Override
    public Boolean getBoolForKey(String key) {

        String value = mSanitizer.getValue(key);

        if (value != null) {

            Boolean booleanForKey;

            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("1")) {
                booleanForKey = Boolean.TRUE;
            } else {
                booleanForKey = Boolean.FALSE;
            }

            return booleanForKey;
        }

        return null;
    }

    @Override
    public Integer getIntegerForKey(String key) {

        String value = mSanitizer.getValue(key);

        if (value != null) {

            Integer integerForKey;
            try {
                integerForKey = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                integerForKey = null;
            }
            return integerForKey;
        }

        return null;
    }

    @Override
    public Date getDateForKey(String key) {
        return null;
    }

    public String getEnumCharForKey(String key) {

        String string = this.getStringForKey(key);
        if (string != null && string.length() == 1)  {
            return string;
        }

        return null;
    }

    @Override
    public Bundle getBundleForKey(String key) {
        return null;
    }


    @Override
    public String getLowercaseStringForKey(String key) {
        String string = this.getStringForKey(key);
        if (string != null) {
            return string.toLowerCase(Locale.US);
        }

        return null;
    }

    @Override
    public Number getNumberForKey(String key) {
        return null;
    }

    @Override
    public Boolean getBoolNumberForKey(String key) {
        return null;
    }

    @Override
    public JSONObject getJSONObjectForKey(String key) {
        return null;
    }


    @Override
    public Map<String, String> getMapJSONForKey(String key) {
        return null;
    }

    @Override
    public Map getMapForKey(String key) {
        return null;
    }

    @Override
    public URL getURLForKey(String key) {
        return null;
    }

    @Override
    public List getArrayFromObject(Object object) {
        return null;
    }

    @Override
    public Object getObjectForKey(String key) {
        return null;
    }

    //public Uri getUriObject() {
        //return uriObject;
    //}

    public void setUriObject(Uri uriObject) {
        this.uriObject = uriObject;
    }
}
