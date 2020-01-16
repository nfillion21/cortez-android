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

package com.tezos.core.network;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.tezos.core.utils.Utils;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by nfillion on 21/01/16.
 */

public abstract class AbstractHttpClient<T>  {

    protected T mLastData;
    protected Bundle bundle;

    public AbstractHttpClient(Context context, Bundle bundle) {

        this.setBundle(bundle);
    }

    protected abstract HttpURLConnection getHttpURLConnection() throws IOException;
    public abstract HttpMethod getRequestType();
    protected abstract String getSignature();
    protected abstract T buildFromHttpResponse(HttpResult httpResult);
    public abstract String concatUrl();
    protected abstract boolean isV2();

    protected HttpResult backgroundOperation() {

        HttpResult httpResult = new HttpResult();

        boolean isCanceled = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        }

        if (!isCanceled) {

            HttpURLConnection urlConnection;
            try {
                urlConnection = this.getHttpURLConnection();
            } catch (IOException exception) {
                httpResult.setIoException(exception);
                return httpResult;
            }

            try {
                httpResult.setStatusCode(urlConnection.getResponseCode());
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                httpResult.setBodyStream(Utils.readStream(urlConnection.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            httpResult.setErrorStream(Utils.readStream(urlConnection.getErrorStream()));

            if (urlConnection != null) {
                urlConnection.disconnect();
            }

        }

        return httpResult;

    }

    protected void onReleaseResources(T data) {

        mLastData = data;
        mLastData = null;
    }

    public enum HttpMethod
    {
        GET ("GET"),
        POST ("POST");

        protected final String method;
        HttpMethod(String method)
        {
            this.method = method;
        }

        public String getStringValue() {
            return this.method;
        }
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }
}
