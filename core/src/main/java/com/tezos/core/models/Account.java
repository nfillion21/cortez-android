package com.tezos.core.models;

public class Account
{
    protected String title;
    protected String pubKeyHash;

    public Account() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPubKeyHash() {
        return pubKeyHash;
    }

    public void setPubKeyHash(String pubKeyHash) {
        this.pubKeyHash = pubKeyHash;
    }
}
