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

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AppCompatDialogFragment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.tezos.ui.R
import com.tezos.ui.extentions.openSecuritySettings
import kotlinx.android.synthetic.main.dialog_fingerprint_backup.*
import kotlinx.android.synthetic.main.dialog_pwd_container.*
import kotlinx.android.synthetic.main.dialog_pwd_content.*

class PasswordDialog : AppCompatDialogFragment()
{
    private var listener: OnPasswordDialogListener? = null

    interface OnPasswordDialogListener
    {
        fun isFingerprintHardwareAvailable():Boolean
        fun hasEnrolledFingerprints():Boolean
        fun passwordVerified(mnemonics: String, password: String, fingerprint: Boolean)
    }

    companion object
    {
        @JvmStatic
        fun newInstance(mnemonics: String) =
                PasswordDialog().apply {
                    arguments = Bundle().apply {
                        putString(MNEMONICS_KEY, mnemonics)
                    }
                }

        private const val MNEMONICS_KEY = "mnemonics_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        //retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AuthenticationDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_pwd_container, container, false)
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnPasswordDialogListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException(context.toString() + " must implement HomeListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        dialog.setTitle(getString(R.string.sign_up_create_master_password))

        cancelButtonPasswordView.setOnClickListener { dismiss() }
        secondButtonPasswordView.setOnClickListener {
            verifyPassword()
        }

        if (listener!!.isFingerprintHardwareAvailable())
        {
            useFingerprintInFutureCheck.visibility = View.VISIBLE
        }

        useFingerprintInFutureCheck.isChecked = listener!!.hasEnrolledFingerprints()

        useFingerprintInFutureCheck.setOnCheckedChangeListener { _, checked -> onAllowFingerprint(checked) }

        enterPassword.addTextChangedListener(GenericTextWatcher(enterPassword))
        confirmPassword.addTextChangedListener(GenericTextWatcher(confirmPassword))

        validateOkButton(isInputDataValid())
    }

    /**
     * Checks whether the current entered password is correct, and dismisses the the dialog and
     * let's the activity know about the result.
     */
    private fun verifyPassword() {
        val password = enterPassword.text.toString()
        val fingerprint = useFingerprintInFutureCheck.isChecked
        dismiss()

        val listener = listener

        arguments?.let {
            val mnemonics = it.getString(MNEMONICS_KEY)
            listener?.passwordVerified(mnemonics!!, password, fingerprint)
        }
    }

    private fun onAllowFingerprint(checked: Boolean)
    {
        if (checked && !listener!!.hasEnrolledFingerprints())
        {
            useFingerprintInFutureCheck.isChecked = false
            Snackbar.make(rootView, R.string.sign_up_snack_message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_up_snack_action) {
                        activity?.openSecuritySettings()
                    }
                    .setActionTextColor(Color.YELLOW)
                    .show()
        }
    }

    private inner class GenericTextWatcher internal constructor(private val v: View) : TextWatcher
    {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable)
        {
            val i = v.id
            if (i != R.id.enterPassword && i != R.id.confirmPassword)
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }

            validateOkButton(isInputDataValid())
        }
    }

    fun isInputDataValid(): Boolean
    {
        if (!TextUtils.isEmpty(enterPassword.text) && !TextUtils.isEmpty(confirmPassword.text))
        {
            if (enterPassword.text.toString() == confirmPassword.text.toString()
                    && enterPassword.text.length >= 6 )
            {
                return true
            }
        }
        return false
    }

    private fun validateOkButton(validate: Boolean)
    {
        secondButtonPasswordView.isEnabled = validate
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
