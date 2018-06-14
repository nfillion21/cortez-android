package com.tezos.android.requests.order;

import android.os.Bundle;

import com.tezos.android.mapper.interfaces.BundleMapper;
import com.tezos.android.serialization.AbstractSerializationMapper;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by nfillion on 03/02/16.
 */
public class PaymentPageRequest extends OrderRelatedRequest {

    public static final String TAG = "Payment_page_request";
    public static final int REQUEST_ORDER = 0x2300;

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
        protected PaymentPageRequest mappedObject() {

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