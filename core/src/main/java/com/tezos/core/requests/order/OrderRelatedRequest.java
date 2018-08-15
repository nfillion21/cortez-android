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
        protected OrderRelatedRequest mappedObjectFromJSON() {

            return null;
            //actually this won't come from JSON
        }
    }
}
