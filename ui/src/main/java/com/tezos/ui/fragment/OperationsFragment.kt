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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.ui.R
import com.tezos.ui.adapter.OperationRecyclerViewAdapter

class OperationsFragment: Fragment(), OperationRecyclerViewAdapter.OnItemClickListener
{
    private var mRecyclerView: RecyclerView? = null

    companion object
    {
        const val PKH_KEY = "pkh_key"

        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                SharingAddressFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        var theme:CustomTheme? = null
        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
            theme = CustomTheme.fromBundle(themeBundle)
        }
    }

    override fun onResume()
    {
        super.onResume()

        mRecyclerView?.adapter?.notifyDataSetChanged()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.activity_operations, container, false)
    }

    override fun onOperationSelected(view: View?, operation: Operation?)
    {
        val operationDetailsFragment = OperationDetailsDialogFragment.newInstance(operation)
        operationDetailsFragment.show(activity?.supportFragmentManager, OperationDetailsDialogFragment.TAG)
    }
}
