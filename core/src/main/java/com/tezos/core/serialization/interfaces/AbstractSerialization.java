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

package com.tezos.core.serialization.interfaces;

import android.os.Bundle;

import com.tezos.core.serialization.BundleSerialization;
import com.tezos.core.serialization.IBundle;

import java.net.URL;
import java.util.Date;
import java.util.Map;

/**
 * Created by nfillion on 04/02/16.
 */
public abstract class AbstractSerialization<T> implements ISerialization {

    protected T model;

    IBundle bundleBehaviour;

    public AbstractSerialization(T model) {
        this.setModel(model);
    }

    //add the Map<String,String> behaviour

    public abstract Map<String, String> getSerializedRequest();

    public Bundle getSerializedBundle() {

        this.setBundleBehaviour(new BundleSerialization());
        return this.getBundle();
    }

    public abstract String getQueryString();

    //it should be Bundle or Map behaviour
    private IBundle getBundleBehaviour() {
        return bundleBehaviour;
    }

    private void setBundleBehaviour(IBundle bundleBehaviour) {
        this.bundleBehaviour = bundleBehaviour;
    }

    protected T getModel() {
        return model;
    }

    private void setModel(T model) {
        this.model = model;
    }

    protected void putFloatForKey(String key, Float floatNumber) {
        this.getBundleBehaviour().putFloat(key, floatNumber);
    }

    protected void putStringForKey(String key, String string) {
        this.getBundleBehaviour().putString(key, string);
    }

    protected void putUrlForKey(String key, URL url) {
        this.getBundleBehaviour().putUrl(key, url);
    }

    protected void putIntForKey(String key, Integer integer) {
        this.getBundleBehaviour().putInt(key, integer);
    }

    protected void putDateForKey(String key, Date date) {
        this.getBundleBehaviour().putDate(key, date);
    }

    protected void putBoolForKey(String key, Boolean bool) {
        this.getBundleBehaviour().putBool(key, bool);
    }

    protected void putBundleForKey(String key, Bundle bundle) {
        this.getBundleBehaviour().putBundle(key, bundle);
    }

    protected void putMapJSONForKey(String key, Map<String, String> map) {
        this.getBundleBehaviour().putMapJSON(key, map);
    }

    protected Bundle getBundle() {
       return this.getBundleBehaviour().getBundle();
    }
}