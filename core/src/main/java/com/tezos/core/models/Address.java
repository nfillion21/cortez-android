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

package com.tezos.core.models;

import android.os.Bundle;

import com.tezos.core.mapper.AbstractMapper;
import com.tezos.core.mapper.interfaces.MapMapper;
import com.tezos.core.serialization.AbstractSerializationMapper;
import com.tezos.core.serialization.interfaces.AbstractSerialization;

import java.util.Map;

public class /**/Address extends AbstractModel
{
    public static final String TAG = "Address";

    protected String description;
    protected String pubKeyHash;

    public Address() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPubKeyHash() {
        return pubKeyHash;
    }

    public void setPubKeyHash(String pubKeyHash) {
        this.pubKeyHash = pubKeyHash;
    }

    public Bundle toBundle()
    {
        AddressSerializationMapper mapper = new AddressSerializationMapper(this);
        return mapper.getSerializedBundle();
    }

    public static Address fromBundle(Bundle bundle)
    {
        AddressMapper mapper = new AddressMapper(bundle);
        return mapper.mappedObjectFromBundle();
    }

    public static class AddressSerializationMapper extends AbstractSerializationMapper
    {
        protected AddressSerializationMapper(Address address)
        {
            super(address);
        }

        @Override
        protected String getQueryString()
        {
            return super.getQueryString();
        }

        @Override
        protected Bundle getSerializedBundle()
        {
            return super.getSerializedBundle();
        }
    }

    protected static class AddressMapper extends AbstractMapper
    {
        public AddressMapper(Object rawData)
        {
            super(rawData);
        }

        @Override
        protected boolean isValid()
        {
            if (this.getBehaviour() instanceof MapMapper)
            {
                //if (this.getStringForKey("enrollmentStatus") != null) return true;
            }

            //return false;

            return true;
        }

        public Account mappedObjectFromJSON()
        {
            return null;
        }

        @Override
        public Account mappedObjectFromBundle()
        {
            Account object = new Account();

            object.setDescription(this.getStringForKey("description"));
            object.setPubKeyHash(this.getStringForKey("pubKeyHash"));

            return object;
        }
    }

    public static class AddressSerialization extends AbstractSerialization
    {
        public AddressSerialization(Address address)
        {
            super(address);
        }

        @Override
        public Map<String, String> getSerializedRequest()
        {
            return null;
        }

        @Override
        public Bundle getSerializedBundle()
        {
            super.getSerializedBundle();

            Address address = (Address) this.getModel();
            this.putStringForKey("description", address.getDescription());
            this.putStringForKey("pubKeyHash", address.getPubKeyHash());

            return this.getBundle();
        }

        @Override
        public String getQueryString()
        {
            return null;
        }
    }
}
