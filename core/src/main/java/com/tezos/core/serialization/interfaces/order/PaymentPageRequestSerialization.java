package com.tezos.core.serialization.interfaces.order;

import android.os.Bundle;

import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.core.utils.Utils;

import java.util.Map;

/**
 * Created by nfillion on 04/02/16.
 */
public class PaymentPageRequestSerialization extends OrderRelatedRequestSerialization
{
    public PaymentPageRequestSerialization(PaymentPageRequest paymentPageRequest)
    {
        super(paymentPageRequest);
    }

    @Override
    public Map<String, String> getSerializedRequest()
    {
        // get the OrderRelatedRequest serialization
        Map<String, String> relatedRequestMap = super.getSerializedRequest();

        while (relatedRequestMap.values().remove(null));

        return relatedRequestMap;
    }

    @Override
    public Bundle getSerializedBundle()
    {
        super.getSerializedBundle();

        PaymentPageRequest paymentPageRequest = (PaymentPageRequest)this.getModel();
        return this.getBundle();
    }

    public String getQueryString()
    {
        return Utils.queryStringFromMap(this.getSerializedRequest());
    }
}
