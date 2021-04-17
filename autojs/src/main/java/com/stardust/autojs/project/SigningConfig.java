package com.stardust.autojs.project;

import com.google.gson.annotations.SerializedName;

public class SigningConfig {

    @SerializedName("alias")
    private String mAlias;

    @SerializedName("keystore")
    private String mKeyStore;

    public String getAlias() {
        return mAlias;
    }

    public void setAlias(String mAlias) {
        this.mAlias = mAlias;
    }

    public String getKeyStore() {
        return mKeyStore;
    }

    public void setKeyStore(String mKeyStore) {
        this.mKeyStore = mKeyStore;
    }

}
