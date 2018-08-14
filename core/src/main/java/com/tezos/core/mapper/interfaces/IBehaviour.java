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

import org.json.JSONObject;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by nfillion on 28/01/16.
 */
public interface IBehaviour {

    // Object?
    Object getObjectForKey(String key);
    String getStringForKey(String key);
    Float getFloatForKey(String key);

    String getLowercaseStringForKey(String key);
    Number getNumberForKey(String key);
    Integer getIntegerForKey(String key);

    Date getDateForKey(String key);
    String getEnumCharForKey(String key);

    Bundle getBundleForKey(String key);
    Boolean getBoolNumberForKey(String key);
    JSONObject getJSONObjectForKey(String key);

    Map<String, String> getMapJSONForKey(String key);

    Boolean getBoolForKey(String key);
    Map getMapForKey(String key);

    //public getYearAndMonthForKey

    URL getURLForKey(String key);
    List getArrayFromObject(Object object);

}
