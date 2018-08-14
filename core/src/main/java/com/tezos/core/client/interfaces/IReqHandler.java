package com.tezos.core.client.interfaces;

import android.content.Context;
import android.os.Bundle;

import com.tezos.core.network.HttpResult;
import com.tezos.core.operations.AbstractOperation;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by nfillion on 22/02/16.
 */
public interface IReqHandler {

    String getReqQueryString();
    String getReqSignatureString();
    AbstractOperation getReqOperation(Context context, Bundle bundle);
    void handleCallback(HttpResult result);
    int getLoaderId();
    void onError(Exception exception);
    void onSuccess(JSONObject jsonObject);
    void onSuccess(JSONArray jsonArray);

}
