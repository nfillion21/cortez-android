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
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import com.tezos.ui.R

class AuthenticationFingerprint(
        private val systemServices: SystemServices,
        private val view: AuthenticationFingerprintView,
        private val callback: Callback) {

    companion object {
        private const val ERROR_TIMEOUT_MILLIS: Long = 1600
        private const val SUCCESS_DELAY_MILLIS: Long = 1300
    }

    private var mCancellationSignal: CancellationSignal? = null
    private var selfCancelled: Boolean = false
    private var handler: Handler = Handler(Looper.getMainLooper())

    fun isFingerprintAuthAvailable(): Boolean {
        return systemServices.isFingerprintHardwareAvailable() && systemServices.hasEnrolledFingerprints()
    }

    fun startListening(cryptoObject: FingerprintManager.CryptoObject) {
        if (isFingerprintAuthAvailable()) {
            mCancellationSignal = CancellationSignal()
            selfCancelled = false
            systemServices.authenticateFingerprint(cryptoObject, mCancellationSignal!!, 0, fingerprintCallback, null)
        }
    }

    fun stopListening() {
        mCancellationSignal?.let {
            it.cancel()
            selfCancelled = true
            mCancellationSignal = null
        }
    }

    private val fingerprintCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
            if (!selfCancelled) {
                view.showErrorView(errString.toString())
                handler.postDelayed({ callback.onAuthenticationError() }, ERROR_TIMEOUT_MILLIS)
            }
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) {
            view.showErrorView(helpString.toString())
            showErrorAndHideItAfterDelay()
        }

        override fun onAuthenticationFailed() {
            view.showErrorView(R.string.authentication_fingerprint_not_recognized)
            showErrorAndHideItAfterDelay()
        }

        @TargetApi(23)
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
            handler.removeCallbacks(hideErrorRunnable)
            view.showSuccessView()
            handler.postDelayed({ callback.onAuthenticated(result.cryptoObject) }, SUCCESS_DELAY_MILLIS)
        }

        private fun showErrorAndHideItAfterDelay() {
            handler.removeCallbacks(hideErrorRunnable)
            handler.postDelayed(hideErrorRunnable, ERROR_TIMEOUT_MILLIS)
        }

        private val hideErrorRunnable = Runnable { view.hideErrorView() }
    }

    interface Callback {
        fun onAuthenticated(cryptoObject: FingerprintManager.CryptoObject)
        fun onAuthenticationError()
    }
}

