package com.tezos.core.models;

import android.os.Bundle;

import com.tezos.core.mapper.AbstractMapper;
import com.tezos.core.mapper.interfaces.MapMapper;
import com.tezos.core.serialization.AbstractSerializationMapper;
import com.tezos.core.serialization.interfaces.AbstractSerialization;

import java.util.Map;

public class Account extends AbstractModel
{
    public static final String TAG = "Account";

    protected String description;
    protected String pubKeyHash;
    protected String privateKeyHash;

    public Account() {}

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

    protected static class AccountMapper extends AbstractMapper
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

        public Account mappedObject()
        {
            return null;
        }

        @Override
        public Account mappedObjectFromBundle()
        {
            Account object = new Account();

            object.setDescription(this.getStringForKey("description"));
            object.setPubKeyHash(this.getStringForKey("pubKeyHash"));
            object.setPrivateKeyHash(this.getStringForKey("privateKeyHash"));

            return object;
        }
    }

    public static class AccountSerialization extends AbstractSerialization
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
            this.putStringForKey("description", account.getDescription());
            this.putStringForKey("pubKeyHash", account.getPubKeyHash());
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