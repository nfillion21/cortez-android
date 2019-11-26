/**
 * Copyright 2013 Bruno Oliveira, and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tezos.core.crypto;

import org.libsodium.jni.crypto.Point;
import org.libsodium.jni.encoders.Encoder;
import org.libsodium.jni.keys.PublicKey;

import java.util.Arrays;

import static org.libsodium.jni.NaCl.sodium;
import static org.libsodium.jni.SodiumConstants.PUBLICKEY_BYTES;
import static org.libsodium.jni.SodiumConstants.SECRETKEY_BYTES;
import static org.libsodium.jni.SodiumConstants.SIGNATURE_BYTES;
import static org.libsodium.jni.crypto.Util.zeros;

public class KeyPair
{
    private byte[] publicKey;
    private final byte[] secretKey;

    public KeyPair()
    {
        this.secretKey = zeros(SECRETKEY_BYTES*2);
        this.publicKey = zeros(PUBLICKEY_BYTES);
        sodium().crypto_box_curve25519xsalsa20poly1305_keypair(publicKey, secretKey);
    }

    public KeyPair(byte[] seed)
    {
        //Util.checkLength(seed, SECRETKEY_BYTES);
        this.secretKey = zeros(SECRETKEY_BYTES*2);
        this.publicKey = zeros(PUBLICKEY_BYTES);
        sodium().crypto_sign_seed_keypair(publicKey, secretKey, seed);
        //Util.isValid(sodium().crypto_box_curve25519xsalsa20poly1305_seed_keypair(publicKey, secretKey, seed), "Failed to generate a key pair");
    }

    //    public KeyPair(byte[] secretKey) {
    //        this.secretKey = secretKey;
    //        checkLength(this.secretKey, SECRETKEY_BYTES);
    //    }

    public KeyPair(String secretKey, Encoder encoder)
    {
        this(encoder.decode(secretKey));
    }

    public PublicKey getPublicKey()
    {
        Point point = new Point();
        byte[] key = publicKey != null ? publicKey : point.mult(secretKey).toBytes();
        return new PublicKey(key);
    }

    public PrivateKey getPrivateKey()
    {
        return new PrivateKey(secretKey);
    }

    public static byte[] sign(String sk, byte[] data)
    {
        byte[] payload_hash = new byte[32];
        sodium().crypto_generichash(payload_hash, payload_hash.length, data, data.length, new byte[]{0}, 0);

        byte[] decodeChecked = Base58.decode(sk);
        byte[] decodeCheckedWithoutfirstSk = Arrays.copyOfRange(decodeChecked, 4, 68);

        byte[] signature = new byte[SIGNATURE_BYTES];
        sodium().crypto_sign_detached(signature, new int[]{signature.length}, payload_hash, payload_hash.length, decodeCheckedWithoutfirstSk);

        return signature;
    }

    public static byte[] b2b(byte[] data)
    {
        byte[] payload_hash = new byte[32];
        sodium().crypto_generichash(payload_hash, payload_hash.length, data, data.length, new byte[]{0}, 0);
        return payload_hash;
    }
}
