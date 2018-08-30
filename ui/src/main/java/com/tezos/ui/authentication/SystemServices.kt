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
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AlertDialog
import com.tezos.ui.BuildConfig
import com.tezos.ui.extentions.openLockScreenSettings
import com.tezos.ui.R

@TargetApi(Build.VERSION_CODES.M)
class SystemServices(private val context: Context) {

    companion object {
        //@JvmStatic
        fun hasMarshmallow() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        const val AUTHENTICATION_SCREEN_CODE = 301
    }

    private val keyguardManager: KeyguardManager

    /**
     * There is a nice [FingerprintManagerCompat] class that makes all dirty work for us, but as always, shit happens.
     * Behind the scenes it is using `Context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)`
     * method, that is returning false on 23 API emulators, when in fact [FingerprintManager] is there and is working fine.
     */
    private var fingerprintManager: FingerprintManager? = null

    init {
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (hasMarshmallow()) {
            fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        }
    }

    fun isDeviceSecure(): Boolean = if (hasMarshmallow()) keyguardManager.isDeviceSecure else keyguardManager.isKeyguardSecure

    fun isFingerprintHardwareAvailable() = fingerprintManager?.isHardwareDetected ?: false

    fun hasEnrolledFingerprints() = fingerprintManager?.hasEnrolledFingerprints() ?: false

    fun authenticateFingerprint(cryptoObject: FingerprintManager.CryptoObject, cancellationSignal: CancellationSignal, flags: Int, callback: FingerprintManager.AuthenticationCallback, handler: Handler?) {
        fingerprintManager?.authenticate(cryptoObject, cancellationSignal, flags, callback, handler)
    }

    fun showAuthenticationScreen(activity: Activity, requestCode: Int, title: String? = null, description: String? = null) {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        if (hasMarshmallow()) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, description)
            if (intent != null) {
                activity.startActivityForResult(intent, requestCode)
            }
        }
    }

    fun showDeviceSecurityAlert(): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle(R.string.lock_title)
                .setMessage(R.string.lock_body)
                .setPositiveButton(R.string.lock_settings) { _, _ -> context.openLockScreenSettings() }
                .setNegativeButton(R.string.lock_exit) { _, _ -> System.exit(0) }
                .setCancelable(BuildConfig.DEBUG)
                .show()
    }
}