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
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatDialogFragment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tezos.ui.R
import kotlinx.android.synthetic.main.dialog_pwd_container.*
import kotlinx.android.synthetic.main.dialog_pwd_content.*

class ContractSelectorFragment : AppCompatDialogFragment()
{
    private var listener: OnContractSelectorListener? = null

    interface OnContractSelectorListener
    {
        fun onContractClicked(withScript:Boolean)
    }

    companion object
    {
        @JvmStatic
        fun newInstance() =
                ContractSelectorFragment().apply {
                    arguments = Bundle().apply {}
                }

        private const val MNEMONICS_KEY = "mnemonics_key"
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        isCancelable = true

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        //retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AuthenticationDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_contract_selector_container, container, false)
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnContractSelectorListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException("$context must implement onContractSelectorListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        dialog.setTitle(getString(R.string.sign_up_create_master_password))

        cancelButtonPasswordView.setOnClickListener {
            listener?.onContractClicked(false)
            dismiss() }
        secondButtonPasswordView.setOnClickListener {
            listener?.onContractClicked(true)
            dismiss()
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

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
