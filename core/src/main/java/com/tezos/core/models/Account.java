package com.tezos.core.models;

import android.os.Bundle;

import com.tezos.core.mapper.AbstractMapper;
import com.tezos.core.mapper.interfaces.MapMapper;
import com.tezos.core.serialization.AbstractSerializationMapper;
import com.tezos.core.serialization.interfaces.AbstractSerialization;

import java.util.Map;

public class Account extends Address
{
    public static final String TAG = "Account";

    protected String privateKeyHash;

    public Account() {}

    public String getPrivateKeyHash() {
        return privateKeyHash;
    }

    public void setPrivateKeyHash(String privateKeyHash) {
        this.privateKeyHash = privateKeyHash;
    }

    public Bundle toBundle()
    {
        AccountSerializationMapper mapper = new AccountSerializationMapper(this);
        return mapper.getSerializedBundle();
    }

    public static Account fromBundle(Bundle bundle)
    {
        AccountMapper mapper = new AccountMapper(bundle);
        return mapper.mappedObjectFromBundle();
    }

    public static class AccountSerializationMapper extends AbstractSerializationMapper
    {
        protected AccountSerializationMapper(Account account)
        {
            super(account);
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

    protected static class AccountMapper extends AddressMapper
    {
        public AccountMapper(Object rawData)
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
            Address address = super.mappedObjectFromBundle();

            Account account = this.accountFromAddress(address);
            account.setPrivateKeyHash(this.getStringForKey("privateKeyHash"));

            return account;
        }

        private Account accountFromAddress(Address address)
        {
            Account account = new Account();
            account.setDescription(address.getDescription());
            account.setPubKeyHash(address.getPubKeyHash());

            return account;
        }
    }

    public static class AccountSerialization extends AddressSerialization
    {
        public AccountSerialization(Account account)
        {
            super(account);
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

            Account account = (Account) this.getModel();
            this.putStringForKey("privateKeyHash", account.getPrivateKeyHash());

            return this.getBundle();
        }

        @Override
        public String getQueryString()
        {
            return null;
        }
    }
}