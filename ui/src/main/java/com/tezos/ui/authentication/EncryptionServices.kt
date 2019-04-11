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

package com.tezos.ui.authentication

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import com.tezos.ui.encryption.CipherWrapper
import com.tezos.ui.encryption.KeyStoreWrapper
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyStore
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

class EncryptionServices(context: Context) {

    /**
     * The place to keep all constants.
     */
    companion object
    {
        const val DEFAULT_KEY_STORE_NAME = "default_keystore"

        const val MASTER_KEY = "MASTER_KEY"
        const val FINGERPRINT_KEY = "FINGERPRINT_KEY"
        const val CONFIRM_CREDENTIALS_KEY = "CONFIRM_CREDENTIALS_KEY"

        const val SPENDING_KEY = "SPENDING_KEY"

        val KEY_VALIDATION_DATA = byteArrayOf(0, 1, 0, 1)
        const val CONFIRM_CREDENTIALS_VALIDATION_DELAY = 5 // Seconds
    }

    private val keyStoreWrapper = KeyStoreWrapper(context, DEFAULT_KEY_STORE_NAME)

    /*
     * Encryption Stage
     */

    /**
     * Create and save cryptography key, to protect Secrets with.
     */
    fun createMasterKey() {
        createAndroidSymmetricKey()
    }

    /**
     * Remove master cryptography key. May be used for re sign up functionality.
     */
    fun removeMasterKey() {
        keyStoreWrapper.removeAndroidKeyStoreKey(MASTER_KEY)
    }

    /**
     * Encrypt user password and Secrets with created master key.
     */
    fun encrypt(data: String): String {
        return encryptWithAndroidSymmetricKey(data)
    }

    /**
     * Decrypt user password and Secrets with created master key.
     */
    fun decrypt(data: String): String {
        return decryptWithAndroidSymmetricKey(data)
    }

    private fun createAndroidSymmetricKey() {
        keyStoreWrapper.createAndroidKeyStoreSymmetricKey(MASTER_KEY)
    }

    private fun encryptWithAndroidSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).encrypt(data, masterKey, true)
    }

    private fun decryptWithAndroidSymmetricKey(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
        return CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).decrypt(data, masterKey, true)
    }

    /**
     * Create and save cryptography key, to protect Secrets with.
     */
    fun createSpendingKey()
    {
        createAndroidAsymmetricKey()
    }

    /**
     * Remove master cryptography key. May be used for re sign up functionality.
     */
    fun removeSpendingKey()
    {
        keyStoreWrapper.removeAndroidKeyStoreKey(SPENDING_KEY)
    }

    private fun createAndroidAsymmetricKey()
    {
        keyStoreWrapper.createAndroidKeyStoreAsymmetricKey(SPENDING_KEY)
    }

    fun sign(data: ByteArray): ByteArray? {
        return signWithAndroidAsymmetricKey(data)
    }

    private fun signWithAndroidAsymmetricKey(data: ByteArray): ByteArray?
    {
        val keyPair = keyStoreWrapper.getAndroidKeyStoreAsymmetricKeyPair(SPENDING_KEY)
        if (keyPair != null)
        {
            return Signature.getInstance("NONEwithECDSA").run {
                initSign(keyPair.private)
                update(data)
                sign()
            }
        }
        return null
    }

    /*
     * Fingerprint Stage
     */

    /**
     * Create and save cryptography key, that will be used for fingerprint authentication.
     */
    fun createFingerprintKey()
    {
        keyStoreWrapper.createAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY,
                userAuthenticationRequired = true,
                invalidatedByBiometricEnrollment = true,
                userAuthenticationValidWhileOnBody = false)
    }

    /**
     * Remove fingerprint authentication cryptographic key.
     */
    fun removeFingerprintKey()
    {
        keyStoreWrapper.removeAndroidKeyStoreKey(FINGERPRINT_KEY)
    }

    /**
     * @return initialized crypto object or null if fingerprint key was invalidated or not created yet.
     */
    fun prepareFingerprintCryptoObject(): FingerprintManager.CryptoObject?
    {
        return if (SystemServices.hasMarshmallow())
        {
            try
            {
                val symmetricKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(FINGERPRINT_KEY)
                val cipher = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC).cipher
                cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
                FingerprintManager.CryptoObject(cipher)
            }
            catch (e: Throwable)
            {
                // VerifyError will be thrown on API lower then 23 if we will use unedited
                // class reference directly in catch block
                if (e is KeyPermanentlyInvalidatedException || e is IllegalBlockSizeException)
                {
                    return null
                }
                else if (e is InvalidKeyException)
                {
                    // Fingerprint key was not generated
                    return null
                }
                throw e
            }
        }
        else null
    }

    /**
     * @return true if cryptoObject was initialized successfully and key was not invalidated during authentication.
     */
    @TargetApi(23)
    fun validateFingerprintAuthentication(cryptoObject: FingerprintManager.CryptoObject): Boolean
    {
        try
        {
            cryptoObject.cipher.doFinal(KEY_VALIDATION_DATA)
            return true
        }
        catch (e: Throwable)
        {
            // VerifyError is will be thrown on API lower then 23 if we will use unedited
            // class reference directly in catch block
            if (e is KeyPermanentlyInvalidatedException || e is IllegalBlockSizeException)
            {
                return false
            }
            throw e
        }
    }

    /*
     * Confirm Credential Stage
     */

    /**
     * Create and save cryptography key, that will be used for confirm credentials authentication.
     */
    fun createConfirmCredentialsKey()
    {
        keyStoreWrapper.createAndroidKeyStoreSymmetricKey(
                CONFIRM_CREDENTIALS_KEY,
                userAuthenticationRequired = true,
                userAuthenticationValidityDurationSeconds = CONFIRM_CREDENTIALS_VALIDATION_DELAY)
    }

    /**
     * Remove confirm credentials authentication cryptographic key.
     */
    fun removeConfirmCredentialsKey()
    {
        keyStoreWrapper.removeAndroidKeyStoreKey(CONFIRM_CREDENTIALS_KEY)
    }

    fun containsConfirmCredentialsKey(): Boolean
    {
        return keyStoreWrapper.containsAlias(CONFIRM_CREDENTIALS_KEY)
    }

    /**
     * @return true if confirm credential authentication is not required.
     */
    fun validateConfirmCredentialsAuthentication(): Boolean
    {
        val symmetricKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(CONFIRM_CREDENTIALS_KEY)
        val cipherWrapper = CipherWrapper(CipherWrapper.TRANSFORMATION_SYMMETRIC)

        try
        {
            return if (symmetricKey != null)
            {
                cipherWrapper.encrypt(KEY_VALIDATION_DATA.toString(), symmetricKey)
                true
            }
            else false
        }
        catch (e: Throwable)
        {
            // VerifyError is will be thrown on API lower then 23 if we will use unedited
            // class reference directly in catch block
            if (e is UserNotAuthenticatedException || e is KeyPermanentlyInvalidatedException) {
                // User is not authenticated or the lock screen has been disabled or reset
                return false
            } else if (e is InvalidKeyException) {
                // Confirm Credentials key was not generated
                return false
            }
            throw e
        }
    }
}