package com.tezos.android.serialization.interfaces.order;

import android.os.Bundle;

import com.tezos.android.requests.order.OrderRelatedRequest;
import com.tezos.android.serialization.interfaces.AbstractSerialization;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nfillion on 04/02/16.
 */
public abstract class OrderRelatedRequestSerialization extends AbstractSerialization {

    public OrderRelatedRequestSerialization(OrderRelatedRequest orderRelatedRequest) {
        super(orderRelatedRequest);
    }

    @Override
    public Map<String, String> getSerializedRequest()
    {
        OrderRelatedRequest orderRelatedRequest = (OrderRelatedRequest)this.getModel();

        Map<String, String> retMap = new HashMap<>();

        retMap.put("orderid", orderRelatedRequest.getOrderId());
        Float amount = orderRelatedRequest.getAmount();
        if (amount != null) {
            retMap.put("amount", String.valueOf(orderRelatedRequest.getAmount()));
        }
        while (retMap.values().remove(null));

        return retMap;
    }

    @Override
    public Bundle getSerializedBundle()
    {
        super.getSerializedBundle();

        OrderRelatedRequest orderRelatedRequest = (OrderRelatedRequest)this.getModel();

        this.putStringForKey("orderid", orderRelatedRequest.getOrderId());
        this.putFloatForKey("amount",orderRelatedRequest.getAmount());
        return this.getBundle();
    }
}
