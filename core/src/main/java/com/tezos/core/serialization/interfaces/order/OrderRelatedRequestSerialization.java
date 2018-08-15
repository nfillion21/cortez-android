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

package com.tezos.core.serialization.interfaces.order;

import android.os.Bundle;

import com.tezos.core.requests.order.OrderRelatedRequest;
import com.tezos.core.serialization.interfaces.AbstractSerialization;

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
