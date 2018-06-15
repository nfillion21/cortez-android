package com.tezos.core.requests.order;

import android.os.Bundle;

import com.tezos.core.mapper.AbstractMapper;
import com.tezos.core.mapper.interfaces.BundleMapper;
import com.tezos.core.requests.AbstractRequest;

/**
 * Created by nfillion on 02/02/16.
 */

public class OrderRelatedRequest extends AbstractRequest
{
    protected String orderId;
    protected Float amount;

    public OrderRelatedRequest()
    {
    }

    public OrderRelatedRequest(OrderRelatedRequest orderRelatedRequest)
    {
        this.setOrderId(orderRelatedRequest.getOrderId());
        this.setAmount(orderRelatedRequest.getAmount());
    }

    public String getOrderId()
    {
        return orderId;
    }

    public void setOrderId(String orderId)
    {
        this.orderId = orderId;
    }
    public Float getAmount()
    {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    protected static class OrderRelatedRequestMapper extends AbstractMapper
    {
        public OrderRelatedRequestMapper(Bundle object)
        {
            super(object);
        }

        @Override
        protected boolean isValid()
        {
            if (this.getBehaviour() instanceof BundleMapper)
            {
                return true;
            }

            return false;
        }

        @Override
        protected OrderRelatedRequest mappedObjectFromBundle()
        {
            OrderRelatedRequest orderRelatedRequest = new OrderRelatedRequest();

            orderRelatedRequest.setOrderId(this.getStringForKey("orderid"));
            orderRelatedRequest.setAmount(this.getFloatForKey("amount"));
            return orderRelatedRequest;
        }

        @Override
        protected Object mappedObjectFromUri() {
            return null;
        }

        @Override
        protected OrderRelatedRequest mappedObject() {

            return null;
            //actually this won't come from JSON
        }
    }
}
