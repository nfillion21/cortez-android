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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by nfillion on 25/01/16.
 */

public class DataExtractor {

    private static boolean checkParams(JSONObject jsonObject, String field) {

        if (jsonObject == null || field == null) {
            return false;
        }
        return true;
    }

    private static boolean checkParams(JSONArray jsonArray, int i) {

        if (jsonArray == null || jsonArray.length() <= i ) {
            return false;
        }
        return true;
    }

    public static Object getObjectFromField(JSONObject jsonObject, String field) {

        if (checkParams(jsonObject, field)) {
            return jsonObject.opt(field);
        }
        return null;
    }

    public static String getStringFromField(JSONObject jsonObject, String field) {

        if (checkParams(jsonObject, field)) {
            return jsonObject.optString(field, null);
        }
        return null;
    }

    public static Integer getIntegerFromField(JSONObject jsonObject, String field) {

        if (checkParams(jsonObject, field)) {

            Integer optInt = jsonObject.optInt(field, Integer.MIN_VALUE);
            if (optInt != Integer.MIN_VALUE) {
                return optInt;
            }
        }
        return null;
    }

    public static Boolean getBooleanFromField(JSONObject jsonObject, String field)
    {
        if (checkParams(jsonObject, field))
        {
            Boolean optBoolean = jsonObject.optBoolean(field, false);
            return optBoolean;
        }
        return null;
    }

    public static Integer getIntegerFromField(JSONArray jsonArray, int i) {

        if (checkParams(jsonArray, i)) {
            return jsonArray.optInt(i, -1);
        }
        return null;
    }

    public static JSONObject getJSONObjectFromField(JSONObject jsonObject,
                                                    String field) {

        if (checkParams(jsonObject, field)) {
            return jsonObject.optJSONObject(field);
        }
        return null;
    }

    public static JSONObject getJSONObjectFromField(JSONArray jsonArray,
                                                    int i) {

        if (checkParams(jsonArray, i)) {
            return jsonArray.optJSONObject(i);
        }
        return null;
    }

    public static JSONArray getJSONArrayFromField(JSONObject jsonObject, String field) {

        if (checkParams(jsonObject, field)) {
            return jsonObject.optJSONArray(field);
        }

        return null;
    }

    public static JSONArray getJSONArrayFromField(JSONArray jsonArray, int i) {

        if (checkParams(jsonArray, i)) {
            return jsonArray.optJSONArray(i);
        }

        return null;
    }

    public static String getStringFromField(JSONArray jsonArray, int i) {

        if (checkParams(jsonArray, i)) {
            return jsonArray.optString(i, null);
        }
        return null;
    }

    public static Date getDateFromField(JSONObject jsonObject, String field) {

        //not used in fields, converted to string
        if (checkParams(jsonObject, field)) {
            //return jsonObject.optLong(field, -1);
        }

        return null;
    }

    public static Date getDateFromField(JSONArray jsonArray, int i) {

        //not used in fields, converted to string
        if (checkParams(jsonArray, i)) {
            //return jsonObject.optLong(field, -1);
        }

        return null;
    }
}
