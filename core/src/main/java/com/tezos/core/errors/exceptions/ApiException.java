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

package com.tezos.core.errors.exceptions;

import android.os.Bundle;

import com.tezos.core.mapper.AbstractMapper;
import com.tezos.core.mapper.interfaces.BundleMapper;
import com.tezos.core.mapper.interfaces.MapMapper;
import com.tezos.core.serialization.AbstractSerializationMapper;
import com.tezos.core.serialization.interfaces.AbstractSerialization;

import java.util.Map;

/**
 * Created by nfillion on 05/04/16.
 */
public class ApiException extends AbstractException {

    private Integer apiCode;

    public ApiException(String message, Integer statusCode, Integer apiCode, Throwable throwable) {
        super(message, statusCode, throwable);
        this.apiCode = apiCode;
    }

    public static ApiException fromBundle(Bundle bundle) {

        ApiExceptionMapper mapper = new ApiExceptionMapper(bundle);
        return mapper.mappedObjectFromBundle();
    }

    public Bundle toBundle() {

        ApiExceptionSerializationMapper mapper = new ApiExceptionSerializationMapper(this);
        return mapper.getSerializedBundle();
    }

    protected static class ApiExceptionSerializationMapper extends AbstractSerializationMapper {

        protected ApiExceptionSerializationMapper(ApiException apiException) {
            super(apiException);
        }

        @Override
        protected String getQueryString() {
            return super.getQueryString();
        }

        @Override
        protected Bundle getSerializedBundle() {
            return super.getSerializedBundle();
        }
    }

    public static class ApiExceptionMapper extends AbstractMapper {

        public ApiExceptionMapper(Object rawData) {
            super(rawData);
        }

        @Override
        protected boolean isValid() {

            if (this.getBehaviour() instanceof MapMapper) {

                return true;

            } else if (getBehaviour() instanceof BundleMapper) {

                return true;
            }

            return true;
        }

        @Override
        protected ApiException mappedObjectFromJSON() {

            return null;
        }

        @Override
        protected ApiException mappedObjectFromBundle() {

            Bundle exceptionBundle = this.getBundleForKey("cause");
            HttpException httpException = null;
            if (exceptionBundle != null) {
                httpException = HttpException.fromBundle(exceptionBundle);
            }

            ApiException object = new ApiException(
                    this.getStringForKey("message"),
                    this.getIntegerForKey("code"),
                    this.getIntegerForKey("apiCode"),
                    httpException
            );

            return object;
        }
    }

    public static class ApiExceptionSerialization extends AbstractSerialization {

        public ApiExceptionSerialization(Exception exception) {
            super(exception);
        }

        @Override
        public Map<String, String> getSerializedRequest() {
            return null;
        }

        @Override
        public Bundle getSerializedBundle() {

            super.getSerializedBundle();

            ApiException apiException = (ApiException)this.getModel();

            this.putStringForKey("message", apiException.getMessage());
            this.putIntForKey("code", apiException.getStatusCode());
            this.putIntForKey("apiCode", apiException.getApiCode());

            Throwable exception = apiException.getCause();
            if (exception != null) {

                HttpException httpSubException = (HttpException) exception;
                Bundle bundle = httpSubException.toBundle();
                this.putBundleForKey("cause", bundle);
            }

            return this.getBundle();
        }

        @Override
        public String getQueryString() {
            return null;
        }
    }

    public Integer getApiCode() {
        return apiCode;
    }
}
