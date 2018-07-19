package com.tezos.ui.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.activity.CreateWalletActivity
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class VerifyCreationWalletFragment : Fragment()
{
    private var listener: OnVerifyWalletCreationListener? = null

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

        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
            theme = CustomTheme.fromBundle(themeBundle)

            val words = it.getString(CreateWalletActivity.MNEMONICS_STR).split(" ")

            //todo add 6 words from 0 to 23, no duplicate
            var sixNumbers = HashSet<Int>(6)
            while (sixNumbers.size < 6)
            {
                val randomInt = (0 until words.size).random()
                sixNumbers.add(randomInt)
            }

            var sixWords:HashMap<Int, String> = HashMap()
            for (item:Int in sixNumbers)
            {
                sixWords[item] = words[item]
            }
        }
    }

    private fun ClosedRange<Int>.random() =
            Random().nextInt((endInclusive + 1) - start) +  start

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_create_wallet, container, false)
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

    private fun makeSelector(theme: CustomTheme?): StateListDrawable {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme!!.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    interface OnVerifyWalletCreationListener
    {
        fun onVerifyWalletCreationValidated()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)
    }
}
