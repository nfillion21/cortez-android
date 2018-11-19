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

import android.content.Intent.getIntent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.utils.Storage

class DelegateFragment : Fragment()
{
    private var mLinearLayout: LinearLayout? = null
    private var mPkhTextview: TextView? = null

    private var mPkhEmptyLayout: LinearLayout? = null

    private var mAddDelegateButton: Button? = null
    private var mAddDelegateButtonLayout: FrameLayout? = null

    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                DelegateFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        mPkhEmptyLayout = view.findViewById(R.id.pkh_empty_layout)

        mLinearLayout = view.findViewById(R.id.pkh_info_layout)

        mPkhTextview = view.findViewById(R.id.pkh_textview)

        mAddDelegateButton = view.findViewById(R.id.add_delegate_button)
        mAddDelegateButtonLayout = view.findViewById(R.id.add_delegate_button_layout)

        mAddDelegateButtonLayout?.setOnClickListener { v ->
        }

        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
            validateAddButton(isInputDataValid(), CustomTheme.fromBundle(themeBundle))
        }
    }

    override fun onResume()
    {
        super.onResume()

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {

        }
        else
        {
            mPkhEmptyLayout?.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_delegate, container, false)
    }

    private fun validateAddButton(validate: Boolean, theme: CustomTheme)
    {
        if (validate)
        {
            mAddDelegateButton?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            mAddDelegateButtonLayout?.isEnabled = true
            mAddDelegateButtonLayout?.background = makeSelector(theme)

            val drawables = mAddDelegateButton?.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
        }
        else
        {
            mAddDelegateButton?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            mAddDelegateButtonLayout?.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            mAddDelegateButtonLayout?.background = makeSelector(greyTheme)

            val drawables = mAddDelegateButton?.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
        }
    }

    private fun isInputDataValid(): Boolean
    {
        return true
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }
}
