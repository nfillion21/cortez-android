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
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tezos.ui.R
import kotlinx.android.synthetic.main.dialog_contract_selector_container.*

class ContractSelectorFragment : AppCompatDialogFragment()
{
    private var listener: OnContractSelectorListener? = null

    interface OnContractSelectorListener
    {
        fun onContractClicked(contract:ContractType)
    }

    companion object
    {
        @JvmStatic
        fun newInstance() =
                ContractSelectorFragment().apply {
                    arguments = Bundle().apply {}
                }

        enum class ContractType
        {
            DEFAULT, SPENDING_LIMIT, MULTISIG
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
        dialog.setTitle(getString(R.string.contracts_and_delegation_title))

        default_contract_button.setOnClickListener {
            listener?.onContractClicked(ContractType.DEFAULT)
            dismiss()
        }

        mulsitig_contract_button.setOnClickListener {
            listener?.onContractClicked(ContractType.MULTISIG)
            dismiss()
        }
        daily_spending_limit_contract_button.setOnClickListener {
            listener?.onContractClicked(ContractType.SPENDING_LIMIT)
            dismiss()
        }
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
