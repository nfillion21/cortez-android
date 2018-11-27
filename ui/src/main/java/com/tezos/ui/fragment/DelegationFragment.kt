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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.tezcore.ui.activity.DelegateActivity
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.fragment_delegation.*

class DelegationFragment : Fragment()
{
    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                DelegationFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
        }
    }

    override fun onResume()
    {
        super.onResume()

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            delegates_empty_layout.visibility = View.GONE
            delegates_layout.visibility = View.VISIBLE
        }
        else
        {
            delegates_empty_layout.visibility = View.VISIBLE
            delegates_layout.visibility = View.GONE
        }

        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
            address_button.setOnClickListener { DelegateActivity.start(activity!!,"KT1WckkuUK46AiSZBhDVAiS6GCZWwSZC37EG", CustomTheme.fromBundle(themeBundle)) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_delegation, container, false)
    }
}
