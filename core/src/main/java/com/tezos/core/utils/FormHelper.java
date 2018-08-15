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

/**
 * Created by nfillion on 08/03/16.
 */

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.tezos.core.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Easy storage and retrieval of preferences.
 */
public class FormHelper {

    private FormHelper() {
        //no instance
    }

    public static boolean isInputDataValid(CharSequence cardNumber, CharSequence cardExpiration, CharSequence cardCVV, CharSequence cardOwner) {
        return  !TextUtils.isEmpty(cardNumber) &&
                !TextUtils.isEmpty(cardExpiration) &&
                !TextUtils.isEmpty(cardCVV) &&
                !TextUtils.isEmpty(cardOwner);
    }

    // http://rosettacode.org/wiki/Luhn_test_of_credit_card_numbers#Java
    public static boolean luhnTest(String number){
        int s1 = 0, s2 = 0;
        String reverse = new StringBuffer(number).reverse().toString();
        for(int i = 0 ;i < reverse.length();i++){
            int digit = Character.digit(reverse.charAt(i), 10);
            if(i % 2 == 0){//this is for odd digits, they are 1-indexed in the algorithm
                s1 += digit;
            }else{//add 2 * digit for 0-4, add 2 * digit - 9 for 5-9
                s2 += 2 * digit;
                if(digit >= 5){
                    s2 -= 9;
                }
            }
        }
        return (s1 + s2) % 10 == 0;
    }

    public static String PACKAGE_NAME = "com.hipay.hipayfullservice";

    public static String getStringResourceByName(String aString, Context context) {

        String resourceName;

        String packageName = context.getPackageName();
        try {
            int resId = context.getResources().getIdentifier(aString.replace("-", "_"), "string", packageName);
            resourceName = context.getString(resId);

        } catch (Resources.NotFoundException exception) {

            resourceName = null;
        }

        return resourceName;
    }

    public static boolean isIndexSpace(Integer index, String productCode, Context context) {

        String cardVisaInfoString = FormHelper.getStringResourceByName("card_"+productCode+"_info", context);

        try {

            JSONObject cardVisaInfo = new JSONObject(cardVisaInfoString);
            JSONArray format = DataExtractor.getJSONArrayFromField(cardVisaInfo, "format");

            Integer integer = 0;
            for (int i = 0; i < format.length(); i++) {

                integer += DataExtractor.getIntegerFromField(format, i);
                if (index.equals(integer+i)) {
                    return true;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;

    }

    public static Integer getMaxCardNumberLength(String productCode, Context context) {

        Integer integer = 0;
        String cardVisaInfoString = FormHelper.getStringResourceByName("card_"+productCode+"_info", context);

        try {

            JSONObject cardVisaInfo = new JSONObject(cardVisaInfoString);
            JSONObject lengths = DataExtractor.getJSONObjectFromField(cardVisaInfo, "lengths");

            integer = DataExtractor.getIntegerFromField(lengths, "length");
            Integer variable = DataExtractor.getIntegerFromField(lengths, "variable");
            if (variable != null) {
                integer += variable;
            }

            JSONArray format = DataExtractor.getJSONArrayFromField(cardVisaInfo, "format");
            integer += format.length();

            return integer;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return integer;
    }

    public static boolean hasValidCardLength(String plainTextNumber, String productCode, Context context) {

        String cardVisaInfoString = FormHelper.getStringResourceByName("card_"+productCode+"_info", context);
        if (cardVisaInfoString == null) {
            return false;
        }

        try {

            JSONObject cardVisaInfo = new JSONObject(cardVisaInfoString);
            JSONObject lengths = DataExtractor.getJSONObjectFromField(cardVisaInfo, "lengths");

            Integer integer = DataExtractor.getIntegerFromField(lengths, "length");
            Set<Integer> integerSet = new HashSet<>(Collections.singletonList(integer));

            Integer variable = DataExtractor.getIntegerFromField(lengths, "variable");
            if (variable != null) {
                for (int j = integer+1; j <= integer+variable; j++) {

                    integerSet.add(j);
                }
            }

            String onlyDigitsString = plainTextNumber.replaceAll(" ", "");
            int length = onlyDigitsString.length();

            if (integerSet.contains(Integer.valueOf(length))) {
                return true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;

    }

    public static boolean validateExpiryDate(String expiryDate) {

        if (expiryDate.length() != 4 || !TextUtils.isDigitsOnly(expiryDate)) {
            return false;
        }

        Integer expMonth = Integer.valueOf(expiryDate.substring(0,2));
        Integer expYear = Integer.valueOf(expiryDate.substring(2,4));

        if (!validateExpMonth(expMonth)) {
            return false;
        }

        if (!validateExpYear(expYear)) {
            return false;
        }

        return !hasMonthPassed(expYear, expMonth);
    }

    public static boolean validateExpMonth(Integer expMonth) {
        if (expMonth == null) {
            return false;
        }
        return (expMonth >= 1 && expMonth <= 12);
    }

    public static boolean validateExpYear(Integer expYear) {
        if (expYear == null) {
            return false;
        }
        return !hasYearPassed(expYear);
    }

    public static boolean hasYearPassed(int year) {
        int normalized = normalizeYear(year);
        Calendar now = Calendar.getInstance();
        return normalized < now.get(Calendar.YEAR);
    }

    public static boolean hasMonthPassed(int year, int month) {
        Calendar now = Calendar.getInstance();
        // Expires at end of specified month, Calendar month starts at 0

        return hasYearPassed(year) ||
                normalizeYear(year) == now.get(Calendar.YEAR) && month < (now.get(Calendar.MONTH) + 1);
    }

    // Convert two-digit year to full year if necessary
    public static int normalizeYear(int year)  {
        if (year < 100 && year >= 0) {
            Calendar now = Calendar.getInstance();
            String currentYear = String.valueOf(now.get(Calendar.YEAR));
            String prefix = currentYear.substring(0, currentYear.length() - 2);
            year = Integer.parseInt(String.format(Locale.US, "%s%02d", prefix, year));
        }
        return year;
    }
}
