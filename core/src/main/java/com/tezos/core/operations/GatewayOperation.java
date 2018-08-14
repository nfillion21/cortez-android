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

package com.tezos.core.operations;

import android.content.Context;
import android.os.Bundle;

import com.tezos.core.network.HttpResult;
import com.tezos.core.utils.Utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by nfillion on 22/01/16.
 */

public abstract class GatewayOperation extends AbstractOperation {

    public GatewayOperation(Context context, Bundle bundle) {
        super(context, bundle);
    }

    protected URL getRequestURL() {

        URL requestUrl;
        try {
            requestUrl = new URL(this.getBaseUrl());
            requestUrl = Utils.concatenatePath(requestUrl, this.concatUrl());

            if (this.getRequestType().equals(HttpMethod.GET)) {

                String params = this.getQueryParams();
                if (params != null) {

                    //if the request contains params like transaction?orderid=transactionOrderId
                    if (params.contains("=")) {
                        requestUrl = Utils.concatenateParams(requestUrl, params);
                    }

                    //if the request is just an url like transaction/referenceTransaction
                    else {
                        requestUrl = Utils.concatenatePath(requestUrl, params);
                    }
                }
            }

        } catch (URISyntaxException e) {
            requestUrl = null;
        } catch (MalformedURLException e) {
            requestUrl = null;
        }

        return requestUrl;
    }

    private String getBaseUrl() {

        /*
        switch (ClientConfig.getInstance().getEnvironment()) {

            case Stage: {

                if (this.isV2()) {
                    return ClientConfig.GatewayClientBaseURLNewStage;

                } else {
                    return ClientConfig.GatewayClientBaseURLStage;
                }
            }

            case Production: {

                if (this.isV2()) {
                    return ClientConfig.GatewayClientBaseURLNewProduction;

                } else {
                    return ClientConfig.GatewayClientBaseURLProduction;
                }
            }

            default: return null;
        }
        */

        return null;
    }

    protected String getQueryParams() {

        if (this.getBundle() != null) {

            return this.getBundle().getString("queryParams");
        }
        return null;
    }

    @Override
    protected String getSignature() {

        if (this.getBundle() != null) {

            return this.getBundle().getString("HS_signature");
        }
        return null;
    }

    @Override
    protected HttpURLConnection getHttpURLConnection() throws IOException {

        HttpURLConnection urlConnection = (HttpURLConnection) this.getRequestURL().openConnection();

        urlConnection.setRequestMethod(this.getRequestType().getStringValue());
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setRequestProperty("Authorization", this.getAuthHeader());

        if (this.getRequestType().equals(HttpMethod.POST)) {

            String queryParams = this.getQueryParams();
            if (queryParams != null) {

                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(queryParams);
                wr.flush();
                wr.close();
            }
        }

        return urlConnection;
    }

    @Override
    protected HttpResult buildFromHttpResponse(HttpResult httpResult) {

        return httpResult;
    }
}
