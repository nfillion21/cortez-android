package com.tezos.core.models;

public class Account
{
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
}
