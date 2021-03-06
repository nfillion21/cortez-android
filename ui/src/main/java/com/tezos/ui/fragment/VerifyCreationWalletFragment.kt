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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.activity.CreateWalletActivity
import com.tezos.ui.adapter.MnemonicWordsViewAdapter
import com.tezos.ui.widget.OffsetDecoration
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import com.tezos.core.crypto.CryptoUtils



class VerifyCreationWalletFragment : Fragment(), MnemonicWordsViewAdapter.OnItemClickListener {

    private val SIX_WORDS_KEY = "words_key"
    private val SIX_WORDS_NUMBER_KEY = "words_number_key"
    private val MNEMONICS_WORDS_NUMBER = 6

    private val WORD_INTEGER_KEY = "word_integer_key"
    private val WORD_STRING_KEY = "word_string_key"

    private var mAdapter: MnemonicWordsViewAdapter? = null
    private var mRecyclerView: RecyclerView? = null

    private var listener: OnVerifyWalletCreationListener? = null

    private var mValidateWalletButton: Button? = null
    private var mValidateWalletButtonLayout: FrameLayout? = null

    private var mVerifyWords: ArrayList<Bundle> = ArrayList(MNEMONICS_WORDS_NUMBER)

    private var mCoordinatorLayout: CoordinatorLayout? = null

    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme, mnemonics:String) =
                VerifyCreationWalletFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(CreateWalletActivity.MNEMONICS_STR, mnemonics)
                    }
                }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnVerifyWalletCreationListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnVerifyWalletCreationListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        mCoordinatorLayout = view.findViewById(R.id.coordinator)

        mValidateWalletButton = view.findViewById(R.id.validate_mnemonics_button)
        mValidateWalletButtonLayout = view.findViewById(R.id.validate_mnemonics_button_layout)
        mValidateWalletButtonLayout?.setOnClickListener { _ ->

            arguments?.let {

                val words = it.getString(CreateWalletActivity.MNEMONICS_STR)
                //val words = "link warm visual pony bike person truck pupil moral gift shoulder eye kit human jacket rich sand cupboard position friend fox calm bring kick"
                listener?.mnemonicsVerified(words!!)
            }
        }

        mRecyclerView = view.findViewById(R.id.words)
        setUpWordGrid(mRecyclerView)

        listener?.updateTitle()

        if (savedInstanceState != null)
        {
            mVerifyWords = savedInstanceState.getParcelableArrayList(SIX_WORDS_NUMBER_KEY)

            val words = savedInstanceState.getStringArrayList(SIX_WORDS_KEY)
            if (words != null)
            {
                mAdapter?.updateWords(words, intFromVerifyWords(mVerifyWords))
            }

            //TODO need to valid mnemonics differently
            validateMnemonicsButton(isInputValid())
        }
        else
        {
            //TODO put that in onSavedInstance
            arguments?.let {
                val words = it.getString(CreateWalletActivity.MNEMONICS_STR).split(" ")

                var sixNumbers = HashSet<Int>(MNEMONICS_WORDS_NUMBER)
                while (sixNumbers.size < MNEMONICS_WORDS_NUMBER)
                {
                    val randomInt = (words.indices).random()
                    sixNumbers.add(randomInt)
                }

                mVerifyWords = ArrayList(MNEMONICS_WORDS_NUMBER)
                for (item:Int in sixNumbers)
                {
                    val bundleWord = Bundle()
                    bundleWord.putInt(WORD_INTEGER_KEY, item)
                    bundleWord.putString(WORD_STRING_KEY, words[item])
                    mVerifyWords.add(bundleWord)
                }
            }

            val words = ArrayList<String?>(MNEMONICS_WORDS_NUMBER)
            for (i in 0 until MNEMONICS_WORDS_NUMBER) {
                words.add(null)
            }
            mAdapter?.updateWords(words, intFromVerifyWords(mVerifyWords))

            validateMnemonicsButton(false)
        }
    }

    private fun isInputValid():Boolean
    {
        val wordsFromVerifyWords = wordsFromVerifyWords(mVerifyWords)
        val wordsFromAdapter = mAdapter?.words

        return wordsFromAdapter == wordsFromVerifyWords
    }

    private fun intFromVerifyWords(verifyWords:ArrayList<Bundle>):List<Int>
    {
        var list = ArrayList<Int>(MNEMONICS_WORDS_NUMBER)

        for (item:Bundle in verifyWords)
        {
            list.add(item.getInt(WORD_INTEGER_KEY))
        }
        return list
    }

    private fun wordsFromVerifyWords(verifyWords:ArrayList<Bundle>):List<String>
    {
        var list = ArrayList<String>(MNEMONICS_WORDS_NUMBER)

        for (item:Bundle in verifyWords)
        {
            list.add(item.getString(WORD_STRING_KEY)!!)
        }
        return list
    }

    private fun ClosedRange<Int>.random() =
            Random().nextInt((endInclusive + 1) - start) +  start

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_verify_creation_wallet, container, false)
    }

    private fun showSnackbar(valid :Boolean)
    {
        val text:Int = if (valid) { R.string.six_words_match }
        else { R.string.six_words_do_not_match }

        val color:Int = if (valid) { ContextCompat.getColor(activity!!, R.color.tz_green) }
        else { ContextCompat.getColor(activity!!, R.color.tz_red) }

        val snackbar = Snackbar.make(mCoordinatorLayout!!, text, Snackbar.LENGTH_LONG)
        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor((color))
        snackbar.show()
    }

    private fun validateMnemonicsButton(validate: Boolean) {

        if (validate) {

            //val customThemeBundle = arguments!!.getBundle(CustomTheme.TAG)
            //val theme = CustomTheme.fromBundle(customThemeBundle)

            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

            mValidateWalletButton?.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            mValidateWalletButtonLayout?.isEnabled = true
            mValidateWalletButtonLayout?.background = makeSelector(theme)

            val drawables = mValidateWalletButton?.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))

        } else {

            mValidateWalletButton?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            mValidateWalletButtonLayout?.isEnabled = false
            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            mValidateWalletButtonLayout?.background = makeSelector(greyTheme)

            val drawables = mValidateWalletButton?.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
        }
    }

    private fun setUpWordGrid(wordsView: RecyclerView?)
    {
        val spacing = context!!.resources.getDimensionPixelSize(R.dimen.spacing_micro)
        wordsView?.addItemDecoration(OffsetDecoration(spacing))

        mAdapter = MnemonicWordsViewAdapter(activity!!)
        mAdapter?.setOnItemClickListener(this)

        wordsView?.adapter = mAdapter
    }

    override fun onClick(view: View?, position: Int)
    {
        listener?.onVerifyWalletCardNumberClicked(position)
    }

    fun updateCard(word: String, position: Int)
    {
        mAdapter?.updateWord(word, position)

        val isInputValid = isInputValid()

        if (mAdapter?.isFull!!)
        {
            validateMnemonicsButton(isInputValid)
            showSnackbar(isInputValid)
        }
    }

    private fun makeSelector(theme: CustomTheme?): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme!!.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    interface OnVerifyWalletCreationListener
    {
        fun onVerifyWalletCardNumberClicked(position: Int)
        fun updateTitle()
        fun mnemonicsVerified(mnemonics: String)
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        val words = mAdapter?.words
        if (words != null) {
            outState.putStringArrayList(SIX_WORDS_KEY, mAdapter?.words as ArrayList<String>)
        }

        outState.putParcelableArrayList(SIX_WORDS_NUMBER_KEY, mVerifyWords)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
