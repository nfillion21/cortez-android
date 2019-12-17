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

package com.tezos.core.serialization;

import android.os.Bundle;

import com.tezos.core.models.AbstractModel;
import com.tezos.core.models.Account;
import com.tezos.core.models.Address;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.models.Operation;
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
    }

    private void initSerializing(AbstractModel model)
    {
        if (model instanceof CustomTheme)
        {
            CustomTheme customTheme = (CustomTheme) model;
            this.setSerialization(new CustomThemeSerialization(customTheme));
        }
        else if (model instanceof Account)
        {
            Account account = (Account) model;
            this.setSerialization(new Account.AccountSerialization(account));
        }
        else if (model instanceof Address)
        {
            Address address = (Address) model;
            this.setSerialization(new Address.AddressSerialization(address));
        }
        else if (model instanceof Operation)
        {
            Operation operation = (Operation) model;
            this.setSerialization(new Operation.OperationSerialization(operation));
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
