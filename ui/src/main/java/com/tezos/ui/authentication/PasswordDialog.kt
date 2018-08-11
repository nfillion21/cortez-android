package com.tezos.ui.authentication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.tezos.ui.R
import com.tezos.ui.extentions.openSecuritySettings
import kotlinx.android.synthetic.main.dialog_fingerprint_backup.*
import kotlinx.android.synthetic.main.dialog_fingerprint_container.*
import kotlinx.android.synthetic.main.dialog_pwd_container.*
import kotlinx.android.synthetic.main.dialog_pwd_content.*

class PasswordDialog : AppCompatDialogFragment()
{
    var passwordVerificationListener: ((password: String) -> Boolean)? = null
    var authenticationSuccessListener: ((password: String) -> Unit)? = null

    private var listener: OnPasswordDialogListener? = null

    interface OnPasswordDialogListener
    {
        fun isFingerprintHardwareAvailable():Boolean
        fun hasEnrolledFingerprints():Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AuthenticationDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.dialog_pwd_container, container, false)
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

        cancelButtonPasswordView.setText(R.string.authentication_cancel)
        secondButtonPasswordView.setText(R.string.authentication_ok)

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

        enterPassword.setOnEditorActionListener {
            _, actionId,
            _ -> onEditorAction(actionId)
        }
    }

    private fun onEditorAction(actionId: Int): Boolean {
        return if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword()
            true
        } else false
    }

    /**
     * Checks whether the current entered password is correct, and dismisses the the dialog and
     * let's the activity know about the result.
     */
    private fun verifyPassword() {
        val password = passwordView.text.toString()
        if (!checkPassword(password)) {
            passwordView.error = getString(R.string.authentication_error_incorrect_password)
            return
        }
        passwordView.setText("")
        authenticationSuccessListener?.invoke(password)
        dismiss()
    }

    /**
     * @return true if `password` is correct, false otherwise
     */
    private fun checkPassword(password: String): Boolean {
        return passwordVerificationListener?.invoke(password) ?: false
    }

    private fun onAllowFingerprint(checked: Boolean)
    {
        if (checked && !listener!!.hasEnrolledFingerprints())
        {
            useFingerprintInFutureCheck.isChecked = false
            Snackbar.make(rootView, R.string.sign_up_snack_message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_up_snack_action, { activity?.openSecuritySettings() })
                    .setActionTextColor(Color.YELLOW)
                    .show()
        }
    }
}
