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

package com.tezos.ui.encryption

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * This class wraps [KeyStore] class apis with some additional possibilities.
 */
class KeyStoreWrapper {

    private val keyStore: KeyStore = createAndroidKeyStore()

    /**
     * @return symmetric key from Android Key Store or null if any key with given pkh exists
     */
    fun getAndroidKeyStoreSymmetricKey(alias: String): SecretKey? = keyStore.getKey(alias, null) as SecretKey?

    /**
     * @return asymmetric keypair from Android Key Store or null if any key with given pkh exists
     */

    fun getAndroidKeyStoreAsymmetricKeyPair(alias: String): KeyPair?
    {
        val privateKey = keyStore.getKey(alias, null) as PrivateKey?
        val publicKey = keyStore.getCertificate(alias)?.publicKey

        return if (privateKey != null && publicKey != null)
        {
            KeyPair(publicKey, privateKey)
        }
        else
        {
            null
        }
    }

    //fun getAndroidKeyStoreAsymmetricPublicKey(alias: String):PublicKey? = keyStore.getCertificate(alias)?.publicKey

    /**
     * Remove key with given pkh from Android Key Store
     */
    fun removeAndroidKeyStoreKey(alias: String) = keyStore.deleteEntry(alias)

    fun containsAlias(alias: String) = keyStore.containsAlias(alias)

    /**
     * Creates symmetric [KeyProperties.KEY_ALGORITHM_AES] key with default [KeyProperties.BLOCK_MODE_CBC] and
     * [KeyProperties.ENCRYPTION_PADDING_PKCS7] and saves it to Android Key Store.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun createAndroidKeyStoreSymmetricKey(
            alias: String,
            userAuthenticationRequired: Boolean = false,
            invalidatedByBiometricEnrollment: Boolean = true,
            userAuthenticationValidityDurationSeconds: Int = -1,
            userAuthenticationValidWhileOnBody: Boolean = true): SecretKey {

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                // Require the user to authenticate with a fingerprint to authorize every use of the key
                .setUserAuthenticationRequired(userAuthenticationRequired)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds)
        // Not working on api 23, try higher ?
        //.setRandomizedEncryptionRequired(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
            builder.setUserAuthenticationValidWhileOnBody(userAuthenticationValidWhileOnBody)
        }
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun createAndroidKeyStoreAsymmetricKey(alias: String): KeyPair
    {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        initGeneratorWithKeyGenParameterSpec(generator, alias)

        return generator.generateKeyPair()
    }

    private fun initGeneratorWithKeyGenParameterSpec(generator: KeyPairGenerator, alias: String)
    {
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_NONE)
        generator.initialize(builder.build())
    }

    private fun createAndroidKeyStore(): KeyStore
    {
        return KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }
}

