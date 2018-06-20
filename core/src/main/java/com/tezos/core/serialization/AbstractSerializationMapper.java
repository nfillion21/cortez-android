package com.tezos.core.serialization;

import android.os.Bundle;

import com.tezos.core.errors.exceptions.ApiException;
import com.tezos.core.errors.exceptions.HttpException;
import com.tezos.core.models.AbstractModel;
import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.requests.AbstractRequest;
import com.tezos.core.requests.order.PaymentPageRequest;
import com.tezos.core.serialization.interfaces.CustomThemeSerialization;
import com.tezos.core.serialization.interfaces.ISerialization;
import com.tezos.core.serialization.interfaces.order.PaymentPageRequestSerialization;

import java.util.Map;

/**
 * Created by nfillion on 04/02/16.
 */
public abstract class AbstractSerializationMapper {

    ISerialization serialization;

    protected String getQueryString() {

        return this.getSerialization().getQueryString();
    }

    protected Map<String, String> getSerializedObject() {

        return this.getSerialization().getSerializedRequest();
    }

    protected Bundle getSerializedBundle() {

        return this.getSerialization().getSerializedBundle();
    }

    public AbstractSerializationMapper(AbstractRequest request) {

        this.initSerializing(request);
    }

    public AbstractSerializationMapper(AbstractModel model) {

        this.initSerializing(model);
    }

    public AbstractSerializationMapper(Exception exception) {

        this.initSerializing(exception);
    }

    private void initSerializing(Exception exception) {

        if (exception instanceof ApiException) {

            ApiException apiException = (ApiException)exception;
            this.setSerialization(new ApiException.ApiExceptionSerialization(apiException));

        } else if (exception instanceof HttpException) {

            HttpException httpException = (HttpException)exception;
            this.setSerialization(new HttpException.HttpExceptionSerialization(httpException));
        }
    }

    private void initSerializing(AbstractModel model) {

        if (model instanceof CustomTheme)
        {
            CustomTheme customTheme = (CustomTheme) model;
            this.setSerialization(new CustomThemeSerialization(customTheme));

        } else if (model instanceof Account) {

            Account account = (Account) model;
            this.setSerialization(new Account.AccountSerialization(account));
        }
    }

    private void initSerializing(AbstractRequest request) {

        if (request instanceof PaymentPageRequest) {

            PaymentPageRequest paymentPageRequest = (PaymentPageRequest)request;
            this.setSerialization(new PaymentPageRequestSerialization(paymentPageRequest));
        }
    }

    protected ISerialization getSerialization()
    {
        return serialization;
    }

    protected void setSerialization(ISerialization serialization)
    {
        this.serialization = serialization;
    }
}
