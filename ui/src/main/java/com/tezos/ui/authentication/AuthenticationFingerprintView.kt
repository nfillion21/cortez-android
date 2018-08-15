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

import android.content.res.Resources
import android.widget.ImageView
import android.widget.TextView
import com.tezos.ui.extentions.getColorCompat
import com.tezos.ui.R

class AuthenticationFingerprintView(private val icon: ImageView, private val errorTextView: TextView) {

    private val resources: Resources = icon.resources

    fun showSuccessView() {
        icon.setImageResource(R.drawable.ic_fingerprint_success)
        errorTextView.setTextColor(resources.getColorCompat(R.color.success_color))
        errorTextView.text = resources.getString(R.string.authentication_fingerprint_success)
    }

    fun showErrorView(errorId: Int) = showErrorView(resources.getString(errorId))

    fun showErrorView(error: String) {
        icon.setImageResource(R.drawable.ic_fingerprint_error)
        errorTextView.text = error
        errorTextView.setTextColor(resources.getColorCompat(R.color.warning_color))
    }

    fun hideErrorView() {
        errorTextView.setTextColor(resources.getColorCompat(R.color.hint_color))
        errorTextView.text = resources.getString(R.string.authentication_fingerprint_hint)
        icon.setImageResource(R.drawable.ic_fp_40px)
    }
}