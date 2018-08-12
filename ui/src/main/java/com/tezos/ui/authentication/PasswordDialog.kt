package com.tezos.ui.authentication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatDialogFragment
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
    var passwordVerificationListener: ((password: String) -> Unit)? = null

    private var listener: OnPasswordDialogListener? = null

    interface OnPasswordDialogListener
    {
        fun isFingerprintHardwareAvailable():Boolean
        fun hasEnrolledFingerprints():Boolean
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
            throw RuntimeException(context.toString() + " must implement OnPasswordDialogListener")
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
        dismiss()
        passwordVerificationListener?.invoke(password)
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
