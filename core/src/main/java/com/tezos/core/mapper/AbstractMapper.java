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

package com.tezos.core.mapper;

import android.net.Uri;
import android.os.Bundle;

import com.tezos.core.mapper.interfaces.BundleMapper;
import com.tezos.core.mapper.interfaces.IBehaviour;
import com.tezos.core.mapper.interfaces.MapMapper;
import com.tezos.core.mapper.interfaces.UriMapper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.Map;

/**
 * Created by nfillion on 25/01/16.
 */
public abstract class AbstractMapper<T> {

    protected IBehaviour behaviour;

    protected abstract Object mappedObjectFromJSON();
    protected abstract Object mappedObjectFromBundle();
    protected abstract boolean isValid();

    public AbstractMapper(T rawData) {

        try {
            this.isMapperValid(rawData);

        } catch (InvalidParameterException exception) {

            // json/bundle is not valid

        } finally {
            //continue
        }
    }

    public AbstractMapper() {}

    private void isMapperValid(T rawData) {

        if (rawData == null) {
            throw new InvalidParameterException();

        } else {

            if (rawData instanceof JSONObject) {

                JSONObject map = (JSONObject)rawData;
                this.setBehaviour(new MapMapper(map));

            } else if (rawData instanceof JSONArray) {

                //JSONArray list = (JSONArray)this.getRawData();
                //this.setBehaviour(new ListMapper(list));

            } else if (rawData instanceof Bundle) {

                Bundle bundle = (Bundle)rawData;
                this.setBehaviour(new BundleMapper(bundle));

            } else if (rawData instanceof Uri) {

                Uri uri = (Uri)rawData;
                this.setBehaviour(new UriMapper(uri));
            }
        }

        if (!this.isValid()) {

            throw new InvalidParameterException();
        }
    }

    protected String getStringForKey(String key) {

        return this.getBehaviour().getStringForKey(key);
    }

    protected Float getFloatForKey(String key) {

        return this.getBehaviour().getFloatForKey(key);
    }
    protected Integer getIntegerForKey(String key) {

        return this.getBehaviour().getIntegerForKey(key);
    }

    protected String getLowercaseStringForKey(String key) {

        return this.getBehaviour().getLowercaseStringForKey(key);
    }

    protected URL getURLForKey(String key) {

        return this.getBehaviour().getURLForKey(key);
    }

    protected Boolean getBoolForKey(String key) {

        return this.getBehaviour().getBoolForKey(key);
    }

    protected String getEnumCharForKey(String key) {

        return this.getBehaviour().getEnumCharForKey(key);
    }

    protected Date getDateForKey(String key) {

        return this.getBehaviour().getDateForKey(key);
    }

    protected JSONObject getJSONObjectForKey(String key) {

        return this.getBehaviour().getJSONObjectForKey(key);
    }

    protected Bundle getBundleForKey(String key) {

        return this.getBehaviour().getBundleForKey(key);
    }

    protected Map<String, String> getMapJSONForKey(String key) {

        return this.getBehaviour().getMapJSONForKey(key);
    }

    public IBehaviour getBehaviour() {
        return behaviour;
    }

    public void setBehaviour(IBehaviour behaviour) {
        this.behaviour = behaviour;
    }

}
