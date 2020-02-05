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

package com.tezos.ui.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.tezos.ui.R
import kotlinx.android.synthetic.main.dialog_add_signatory_container.*
import kotlinx.android.synthetic.main.dialog_add_signatory_content.*

class AddSignatoryDialogFragment : AppCompatDialogFragment()
{
    private var listener: OnSignatorySelectorListener? = null

    interface OnSignatorySelectorListener
    {
        fun onPublicKeyClicked(publicKey:String)
    }

    companion object
    {
        @JvmStatic
        fun newInstance() =
                AddSignatoryDialogFragment().apply {
                    arguments = Bundle().apply {}
                }
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
        return inflater.inflate(R.layout.dialog_add_signatory_container, container, false)
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnSignatorySelectorListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException("$context must implement onSignatorySelectorListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        dialog.setTitle(getString(R.string.add_signatory_title))

        cancelButtonView.setOnClickListener { dismiss() }
        OkButtonView.setOnClickListener {
            dismiss()
            if (isInputDataValid())
            {
                listener?.onPublicKeyClicked(enterPublicKeyEditText.text.toString())
            }
        }

        enterPublicKeyEditText.setOnEditorActionListener { v, i, _ ->

            when (i)
            {
                EditorInfo.IME_ACTION_DONE -> {

                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)

                    true }
                else -> false
            }
        }

        enterPublicKeyEditText.addTextChangedListener(GenericTextWatcher(enterPublicKeyEditText))

        validateOkButton(isInputDataValid())
    }

    private inner class GenericTextWatcher internal constructor(private val v: View) : TextWatcher
    {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable)
        {
            val i = v.id
            if (i != R.id.enterPublicKeyEditText)
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }

            validateOkButton(isInputDataValid())
        }
    }

    fun isInputDataValid(): Boolean
    {
        if (!TextUtils.isEmpty(enterPublicKeyEditText.text))
        {
            if (
                    (enterPublicKeyEditText.text.toString().startsWith("edpk", ignoreCase = true)
                    && enterPublicKeyEditText.text.length == 54)
                    ||
                    (enterPublicKeyEditText.text.toString().startsWith("p2pk", ignoreCase = true)
                            && enterPublicKeyEditText.text.length == 55)
                    ||
                    (enterPublicKeyEditText.text.toString().startsWith("sppk", ignoreCase = true)
                            && enterPublicKeyEditText.text.length == 55)
            )
            {
                return true
            }
        }
        return false
    }

    private fun validateOkButton(validate: Boolean)
    {
        OkButtonView.isEnabled = validate
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
