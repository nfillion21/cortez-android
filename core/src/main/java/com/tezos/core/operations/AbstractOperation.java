package com.tezos.core.operations;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.tezos.core.network.AbstractHttpClient;
import com.tezos.core.network.HttpResult;

/**
 * Created by nfillion on 09/03/16.
 */

public abstract class AbstractOperation extends AbstractHttpClient<HttpResult> {
    public AbstractOperation(Context context, Bundle bundle) {
        super(context, bundle);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected String getAuthHeader() {

        /*
        String username = ClientConfig.getInstance().getUsername();

        StringBuilder authHeaderStringBuilder = new StringBuilder(username).append(":");

        //sha1( orderId . amount . currency . passPhrase )
        String signature = this.getSignature();

        // include the md1 signature from merchant server
        if (signature != null) {

            authHeaderStringBuilder
                    .append(signature);

        } else {

            String password = ClientConfig.getInstance().getPassword();
            authHeaderStringBuilder.append(password);
        }


        String authHeaderString = authHeaderStringBuilder.toString();

        byte[] b;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            b = authHeaderString.getBytes(StandardCharsets.US_ASCII);
        } else {

            try {
                b = authHeaderString.getBytes("US-ASCII");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        String keySign = "Basic";
        if (signature != null) {
            keySign = "HS";
        }

        return new StringBuilder(keySign)
                .append(" ")
                .append(Base64.encodeToString(b, Base64.NO_WRAP))
                .toString();
        */

        return null;
    }
}
