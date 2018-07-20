package com.tezos.ui.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.activity.CreateWalletActivity
import com.tezos.ui.adapter.MnemonicWordsViewAdapter
import com.tezos.ui.widget.OffsetDecoration
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

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
            throw RuntimeException(context.toString() + " must implement OnVerifyWalletCreationListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        var theme:CustomTheme? = null

        mValidateWalletButton = view.findViewById(R.id.validate_mnemonics_button)
        mValidateWalletButtonLayout = view.findViewById(R.id.validate_mnemonics_button_layout)
        mValidateWalletButtonLayout?.setOnClickListener(
                { v ->


                })

        mRecyclerView = view.findViewById(R.id.words)
        setUpWordGrid(mRecyclerView)


        if (savedInstanceState != null)
        {
            val words = savedInstanceState.getStringArrayList(SIX_WORDS_KEY)
            if (words != null)
            {
                mAdapter?.updateWords(words, intFromVerifyWords(mVerifyWords))
            }

            mVerifyWords = savedInstanceState.getParcelableArrayList(SIX_WORDS_NUMBER_KEY)

            //TODO need to valid mnemonics differently
            validateMnemonicsButton(isInputValid())
        }
        else
        {
            //TODO put that in onSavedInstance
            arguments?.let {
                val themeBundle = it.getBundle(CustomTheme.TAG)
                theme = CustomTheme.fromBundle(themeBundle)

                val words = it.getString(CreateWalletActivity.MNEMONICS_STR).split(" ")

                var sixNumbers = HashSet<Int>(6)
                while (sixNumbers.size < 6)
                {
                    val randomInt = (0 until words.size).random()
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

            //TODO need to valid mnemonics differently
            validateMnemonicsButton(false)
        }
    }

    private fun isInputValid():Boolean
    {

        return false
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

    private fun ClosedRange<Int>.random() =
            Random().nextInt((endInclusive + 1) - start) +  start

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_verify_creation_wallet, container, false)
    }

    /*
    private fun showSnackbarError(network :Boolean)
    {
        var error:Int = if (network)
        {
            R.string.network_error
        }
        else
        {
            R.string.generic_error
        }

        val snackbar = Snackbar.make(mCoordinatorLayout!!, error, Snackbar.LENGTH_LONG)
        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor((ContextCompat.getColor(activity!!,
                android.R.color.holo_red_light)))
        snackbar.show()
    }
    */

    fun validateMnemonicsButton(validate: Boolean) {

        if (validate) {

            val customThemeBundle = arguments!!.getBundle(CustomTheme.TAG)
            val theme = CustomTheme.fromBundle(customThemeBundle)

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

        val wordToVerifyBundle = mVerifyWords[position]
        val wordToVerify = wordToVerifyBundle.getString(WORD_STRING_KEY)

        val bool = word == wordToVerify

        validateMnemonicsButton(isInputValid())
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
