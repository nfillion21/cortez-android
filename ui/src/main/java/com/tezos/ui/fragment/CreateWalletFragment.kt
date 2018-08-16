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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.AppCompatCheckBox
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R

class CreateWalletFragment : Fragment()
{
    private val MNEMONICS_KEY = "mnemonics_key"
    private val BACKUP_CHECKBOX_KEY = "backup_checkbox_key"

    private var mRenewFab: FloatingActionButton? = null
    private var mMnemonicsTextview: TextView? = null
    private var mMnemonicsString: String? = null

    private var mCreateButton: Button? = null
    private var mBackupCheckbox: AppCompatCheckBox? = null
    private var mCreateButtonLayout: FrameLayout? = null

    private var mBackupChecked: Boolean = false

    private var listener: OnCreateWalletListener? = null

    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                CreateWalletFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnCreateWalletListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException(context.toString() + " must implement OnCreateWalletListener")
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

        mMnemonicsTextview = view.findViewById(R.id.mnemonics_textview)
        mMnemonicsTextview?.setTextColor(ContextCompat.getColor(activity!!, theme!!.colorPrimaryDarkId))

        mBackupCheckbox = view.findViewById(R.id.backup_checkbox)

        mCreateButton = view.findViewById(R.id.create_button)
        mCreateButtonLayout = view.findViewById(R.id.create_button_layout)

        mCreateButtonLayout?.visibility = View.VISIBLE

        mCreateButton?.setText(R.string.next_step_button)

        mCreateButtonLayout?.setOnClickListener { _ ->

            if (mMnemonicsString != null)
            {
                listener?.onCreateWalletValidated(mMnemonicsString!!)
            }
        }
        listener?.updateTitle()

        mRenewFab = view.findViewById(R.id.renew)
        mRenewFab?.setOnClickListener { v ->
            val i = v.id
            if (i == R.id.renew)
            {
                mBackupCheckbox?.isEnabled = false

                mRenewFab?.isEnabled = false

                val textViewAnimator = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_X, 0f)
                val textViewAnimator2 = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_Y, 0f)
                val textViewAnimator3 = ObjectAnimator.ofFloat(mRenewFab, View.ALPHA, 0f)

                val animatorSet = AnimatorSet()
                animatorSet.interpolator = FastOutSlowInInterpolator()
                animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
                animatorSet.addListener(object : AnimatorListenerAdapter()
                {
                    override fun onAnimationEnd(animation: Animator)
                    {
                        super.onAnimationEnd(animation)

                        mMnemonicsString = CryptoUtils.generateMnemonics()
                        mMnemonicsTextview?.text = mMnemonicsString
                        // renew the mnemonic
                        showDoneFab()
                    }
                })
                animatorSet.start()
            }
            else
            {
                throw UnsupportedOperationException(
                        "The onClick method has not been implemented for " + resources
                                .getResourceEntryName(v.id))
            }
        }

        if (savedInstanceState == null)
        {
            mMnemonicsString = CryptoUtils.generateMnemonics()
            if (mMnemonicsString != null)
            {
                mMnemonicsTextview?.text = mMnemonicsString
            }

            mBackupChecked = false
        }
        else
        {
            mMnemonicsString = savedInstanceState.getString(MNEMONICS_KEY, null)
            if (mMnemonicsString != null)
            {
                mMnemonicsTextview?.text = mMnemonicsString
            }

            mBackupChecked = savedInstanceState.getBoolean(BACKUP_CHECKBOX_KEY, false)
            mBackupCheckbox?.isChecked = mBackupChecked
        }

        validateCreateButton(isCreateButtonValid(), theme)

        mBackupCheckbox?.setOnCheckedChangeListener { buttonView, isChecked ->
            mBackupChecked = buttonView.isChecked
            validateCreateButton(isCreateButtonValid(), theme)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_create_wallet, container, false)
    }

    private fun isCreateButtonValid(): Boolean
    {
        return mBackupChecked
    }

    private fun showDoneFab()
    {
        mRenewFab?.show()

        mRenewFab?.scaleX = 0f
        mRenewFab?.scaleY = 0f

        val textViewAnimator = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_X, 1f)
        val textViewAnimator2 = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_Y, 1f)
        val textViewAnimator3 = ObjectAnimator.ofFloat(mRenewFab, View.ALPHA, 1f)

        val animatorSet = AnimatorSet()
        //animatorSet.startDelay = 200

        animatorSet.interpolator = FastOutSlowInInterpolator()
        animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
        animatorSet.addListener(object : AnimatorListenerAdapter()
        {
            override fun onAnimationEnd(animation: Animator)
            {
                super.onAnimationEnd(animation)
                mRenewFab?.isEnabled = true

                mBackupCheckbox?.isEnabled = true
            }
        })
        animatorSet.start()
    }

    private fun validateCreateButton(validate: Boolean, theme: CustomTheme?)
    {
        if (validate)
        {
            mCreateButton?.setTextColor(ContextCompat.getColor(activity!!, theme!!.textColorPrimaryId))
            mCreateButtonLayout?.isEnabled = true
            mCreateButtonLayout?.background = makeSelector(theme)

            val drawables = mCreateButton?.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme!!.textColorPrimaryId))

            mRenewFab?.isEnabled = false
            mRenewFab?.hide()

        }
        else
        {
            mCreateButton?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            mCreateButtonLayout?.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            mCreateButtonLayout?.background = makeSelector(greyTheme)

            val drawables = mCreateButton?.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))

            mRenewFab?.show()
            mRenewFab?.isEnabled = true
        }
    }

    private fun makeSelector(theme: CustomTheme?): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme!!.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    interface OnCreateWalletListener
    {
        fun onCreateWalletValidated(mnemonics:String)
        fun updateTitle()
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putString(MNEMONICS_KEY, mMnemonicsString)
        outState.putBoolean(BACKUP_CHECKBOX_KEY, mBackupChecked)
    }
}
