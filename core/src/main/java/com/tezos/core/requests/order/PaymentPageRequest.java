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

import com.tezos.core.mapper.interfaces.BundleMapper;
import com.tezos.core.serialization.AbstractSerializationMapper;

/**
 * Created by nfillion on 03/02/16.
 */
public class PaymentPageRequest extends OrderRelatedRequest {

    public static final String TAG = "Payment_page_request";
    public static final int REQUEST_ORDER = 0x2200;

    public PaymentPageRequest() {}

    public PaymentPageRequest(PaymentPageRequest paymentPageRequest) {
        super(paymentPageRequest);
    }

    public static PaymentPageRequest fromBundle(Bundle bundle) {

        PaymentPageRequestMapper mapper = new PaymentPageRequestMapper(bundle);
        return mapper.mappedObjectFromBundle();
    }

    public Bundle toBundle() {

        PaymentPageRequestSerializationMapper mapper = new PaymentPageRequestSerializationMapper(this);
        return mapper.getSerializedBundle();
    }

    public String getStringParameters() {

        PaymentPageRequestSerializationMapper mapper = new PaymentPageRequestSerializationMapper(this);
        return mapper.getQueryString();
    }

    protected static class PaymentPageRequestSerializationMapper extends AbstractSerializationMapper {

        protected PaymentPageRequestSerializationMapper(PaymentPageRequest request) {
            super(request);
        }

        @Override
        protected String getQueryString() {

            return super.getQueryString();
        }

        @Override
        protected Bundle getSerializedBundle() {

            return super.getSerializedBundle();
        }
    }

    protected static class PaymentPageRequestMapper extends OrderRelatedRequestMapper {
        public PaymentPageRequestMapper(Bundle object) {
            super(object);
        }

        @Override
        protected boolean isValid() {

            if (this.getBehaviour() instanceof BundleMapper) {

                return true;
            }
            return false;
        }

        @Override
        protected PaymentPageRequest mappedObjectFromJSON() {

            //we don't receive it from json
            return null;
        }

        @Override
        protected PaymentPageRequest mappedObjectFromBundle() {

            OrderRelatedRequest orderRelatedRequest = super.mappedObjectFromBundle();
            PaymentPageRequest paymentPageRequest = this.pageRequestFromRelatedRequest(orderRelatedRequest);

            return paymentPageRequest;
        }

        private PaymentPageRequest pageRequestFromRelatedRequest(OrderRelatedRequest orderRelatedRequest) {

            PaymentPageRequest paymentPageRequest = new PaymentPageRequest();

            paymentPageRequest.setOrderId(orderRelatedRequest.getOrderId());
            paymentPageRequest.setAmount(orderRelatedRequest.getAmount());

            return paymentPageRequest;
        }
    }
}