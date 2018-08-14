package com.tezos.core.crypto;

import org.libsodium.jni.Sodium;

/**
 * Created by Milfont on 09/03/2018.
 */

public class CryptoGenericHash extends Sodium
{
    public static byte[] cryptoGenericHash(byte[] input, int outputLength) throws Exception
    {
        byte[] genericHash = new byte[outputLength];

        int rc = Sodium.crypto_generichash(genericHash, genericHash.length, input, input.length, input, 0);

        if (rc != 0)
        {
            throw new Exception("cryptoGenericHash libsodium crypto_generichash failed, returned " + rc + ", expected 0");
        }

        return genericHash;
    }
}
