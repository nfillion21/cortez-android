package com.tezos.core.client;

import android.content.Context;

import com.tezos.core.client.interfaces.callbacks.OrderRequestCallback;
import com.tezos.core.requests.order.OrderRequest;

/**
 * Created by nfillion on 18/02/16.
 */
public class GatewayClient extends AbstractClient  {

    public static final String SIGNATURE_TAG = "signature_tag";

    public GatewayClient(Context context) {
        super(context);
    }

    public void requestNewOrder(OrderRequest orderRequest, String signature, OrderRequestCallback orderRequestCallback) {
        super.createRequest(orderRequest, orderRequestCallback);
    }
}
