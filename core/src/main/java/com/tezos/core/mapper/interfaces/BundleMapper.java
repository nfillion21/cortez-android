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

import android.os.Bundle;

import com.tezos.core.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by nfillion on 21/03/16.
 */

public class BundleMapper implements IBehaviour {

    protected Bundle bundleObject;

    public BundleMapper(Bundle bundle) {
        this.setBundleObject(bundle);
    }

    @Override
    public Float getFloatForKey(String key) {

        float floatValue = this.getBundleObject().getFloat(key, -1.f);
        if (floatValue != -1.f) {
            return Float.valueOf(floatValue);
        }
        return null;
    }

    @Override
    public String getStringForKey(String key) {

        return this.getBundleObject().getString(key);
    }

    @Override
    public Boolean getBoolForKey(String key) {

        return this.getBundleObject().getBoolean(key);
    }

    @Override
    public Map<String, String> getMapJSONForKey(String key) {

        String customDataString = this.getStringForKey(key);
        if (customDataString != null) {

            Map<String, String> map = null;
            try {
                map = Utils.jsonToMap(customDataString);

            } catch (JSONException e) {
                e.printStackTrace();
                map = null;

            } finally {

                return map;
            }
        }

        return null;
    }

    @Override
    public Integer getIntegerForKey(String key) {

        int intValue = this.getBundleObject().getInt(key, -1);
        if (intValue != -1.f) {
            return Integer.valueOf(intValue);
        }
        return null;
    }

    @Override
    public URL getURLForKey(String key) {

        String string = this.getStringForKey(key);
        if (string != null && !string.isEmpty()) {

            URL url = null;
            try {
                url = new URL(string);
                return new URL(string);

            } catch (MalformedURLException e) {
                e.printStackTrace();
                url = null;

            } finally {
                return url;
            }
        }

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
        return this.getBundleObject().getBundle(key);
    }

    @Override
    public String getLowercaseStringForKey(String key) {
        return null;
    }

    @Override
    public Number getNumberForKey(String key) {
        return null;
    }

    @Override
    public Date getDateForKey(String key) {

        String stringDate = this.getStringForKey(key);

        Date date = Utils.getBasicDateFromString(stringDate);

        if (date == null) {
            date = Utils.getDateISO8601FromString(stringDate);
        }

        return date;
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
    public Map getMapForKey(String key) {
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

    public Bundle getBundleObject() {
        return bundleObject;
    }

    public void setBundleObject(Bundle bundleObject) {
        this.bundleObject = bundleObject;
    }
}
