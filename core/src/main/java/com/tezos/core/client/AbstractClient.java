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

package com.tezos.core.client;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.tezos.core.client.interfaces.IReqHandler;
import com.tezos.core.client.interfaces.callbacks.AbstractRequestCallback;
import com.tezos.core.network.HttpResult;
import com.tezos.core.operations.AbstractOperation;

import java.lang.ref.WeakReference;

/**
 * Created by nfillion on 21/01/16.
 */
public abstract class AbstractClient<T> implements LoaderManager.LoaderCallbacks<HttpResult>{

    private IReqHandler reqHandler;

    protected WeakReference<Context> contextWeakReference = null;
    private AbstractOperation operation;

    public AbstractClient(Context ctx) {

        contextWeakReference = new WeakReference<>(ctx);
    }

    protected void createRequest(T request, AbstractRequestCallback callback) {

        if (this.initReqHandler(request, callback)) {
            this.launchOperation();

        } else {
            throw new IllegalArgumentException();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void cancelOperation(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            //it cancels the loading
            AbstractOperation operation = this.getOperation();
            if (operation != null) {
                operation.cancelLoad();
            }
        }

        //...and it actually brutally destroys the loader
        if (this.getContext() == null) {
            contextWeakReference = new WeakReference<>(context);
        }

        if (this.getContext() instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) this.getContext();
            activity.getSupportLoaderManager().destroyLoader(this.getLoaderId());
        }
    }

    protected void launchOperation() {

        Bundle queryBundle = new Bundle();
        queryBundle.putString("queryParams", this.getQueryParams());
        queryBundle.putString("HS_signature", this.getSignature());

        AppCompatActivity activity = (AppCompatActivity)this.getContext();
        //activity.getSupportLoaderManager().enableDebugLogging(true);
        if (activity != null) {
            activity.getSupportLoaderManager().initLoader(this.getLoaderId(), queryBundle, this);
        }
    }



    public void reLaunchOperations(int loaderId) {

        AppCompatActivity activity = (AppCompatActivity)this.getContext();
        if (activity != null) {

            LoaderManager supportLoaderManager = activity.getSupportLoaderManager();
            if (supportLoaderManager.getLoader(loaderId) != null) {
                this.launchOperation();
            }
        }
    }

    public boolean canRelaunch() {

        Context context = this.getContext();
        if (context != null) {
            return true;
        }

        return false;
    }

    private boolean initReqHandler(T request, AbstractRequestCallback callback)
    {
        /*
        if (request instanceof GenerateTokenRequest
                && callback instanceof SecureVaultRequestCallback) {

            GenerateTokenRequest generateTokenRequest = (GenerateTokenRequest) request;
            SecureVaultRequestCallback secureVaultRequestCallback = (SecureVaultRequestCallback)callback;

            this.setReqHandler(new SecureVaultReqHandler(generateTokenRequest, secureVaultRequestCallback));

            return true;

        } if (request instanceof UpdatePaymentCardRequest
                && callback instanceof UpdatePaymentCardRequestCallback) {

            UpdatePaymentCardRequest updatePaymentCardRequest = (UpdatePaymentCardRequest) request;
            UpdatePaymentCardRequestCallback updatePaymentCardRequestCallback = (UpdatePaymentCardRequestCallback)callback;

            this.setReqHandler(new UpdatePaymentCardReqHandler(updatePaymentCardRequest, updatePaymentCardRequestCallback));

            return true;

        } if (request instanceof LookupPaymentCardRequest
                && callback instanceof LookupPaymentCardRequestCallback) {

            LookupPaymentCardRequest lookupPaymentCardRequest = (LookupPaymentCardRequest) request;
            LookupPaymentCardRequestCallback lookupPaymentCardRequestCallback = (LookupPaymentCardRequestCallback)callback;

            this.setReqHandler(new LookupPaymentCardReqHandler(lookupPaymentCardRequest, lookupPaymentCardRequestCallback));

            return true;

        } else if (request instanceof PaymentPageRequest
                && callback instanceof PaymentProductsCallback) {

            PaymentPageRequest paymentPageRequest = (PaymentPageRequest) request;
            PaymentProductsCallback paymentProductsCallback = (PaymentProductsCallback) callback;

            this.setReqHandler(new PaymentProductsReqHandler(paymentPageRequest, paymentProductsCallback));

            return true;
        }
        return false;
        */

        return true;
    }

    @Override
    public Loader<HttpResult> onCreateLoader(int id, Bundle bundle) {

        if (this.getContext() != null) {

            this.setOperation(this.getOperation(this.getContext(), bundle));

            //performs http request
            AbstractOperation operation = this.getOperation();

            String httpMethod = operation.getRequestType().getStringValue();
            String path = operation.concatUrl();

            //Logger.d(new StringBuilder("<HTTP:> Performs ").append(httpMethod).append(" ").append(path).toString());

            return operation;
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<HttpResult> loader, HttpResult data) {

        if (this.getContext() != null) {
            this.getReqHandler().handleCallback(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<HttpResult> loader) {

        Log.i("reset", "reset");
        Log.i("reset", "reset");
    }

    protected String getQueryParams() {

        return this.getReqHandler().getReqQueryString();
    }

    protected String getSignature() {

        return this.getReqHandler().getReqSignatureString();
    }

    protected AbstractOperation getOperation(Context context, Bundle bundle) {

        return this.getReqHandler().getReqOperation(context, bundle);
    }

    protected int getLoaderId() {

        return this.getReqHandler().getLoaderId();
    }

    public enum RequestLoaderId {

        GenerateTokenReqLoaderId(0),
        OrderReqLoaderId(1),
        PaymentPageReqLoaderId(2),
        TransactionReqLoaderId(3),
        TransactionsReqLoaderId(4),
        PaymentProductsReqLoaderId(5),
        UpdatePaymentCardLoaderId(6),
        LookupPaymentCardLoaderId(7);

        protected final Integer loaderId;

        RequestLoaderId(Integer loaderId) {
            this.loaderId = loaderId;
        }

        public Integer getIntegerValue() {
            return this.loaderId;
        }

        public static RequestLoaderId fromIntegerValue(Integer value) {

            if (value == null) return null;

            if (value.equals(GenerateTokenReqLoaderId.getIntegerValue())) {
                return GenerateTokenReqLoaderId;
            }

            if (value.equals(OrderReqLoaderId.getIntegerValue())) {
                return OrderReqLoaderId;
            }

            if (value.equals(PaymentPageReqLoaderId.getIntegerValue())) {
                return PaymentPageReqLoaderId;
            }

            if (value.equals(TransactionReqLoaderId.getIntegerValue())) {
                return TransactionReqLoaderId;
            }

            if (value.equals(TransactionsReqLoaderId.getIntegerValue())) {
                return TransactionsReqLoaderId;
            }

            if (value.equals(PaymentProductsReqLoaderId.getIntegerValue())) {
                return PaymentProductsReqLoaderId;
            }

            if (value.equals(UpdatePaymentCardLoaderId.getIntegerValue())) {
                return UpdatePaymentCardLoaderId;
            }

            if (value.equals(LookupPaymentCardLoaderId.getIntegerValue())) {
                return LookupPaymentCardLoaderId;
            }
            return null;
        }
    }



    protected Context getContext() {
        return contextWeakReference.get();
    }

    private IReqHandler getReqHandler() {
        return reqHandler;
    }

    private void setReqHandler(IReqHandler reqHandler) {
        this.reqHandler = reqHandler;
    }

    public AbstractOperation getOperation() {
        return operation;
    }

    public void setOperation(AbstractOperation operation) {
        this.operation = operation;
    }
}
