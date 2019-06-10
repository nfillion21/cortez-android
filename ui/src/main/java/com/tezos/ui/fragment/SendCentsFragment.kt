package com.tezos.ui.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tezos.core.models.CustomTheme

import com.tezos.ui.R
import kotlinx.android.synthetic.main.dialog_sent_cents.*

class SendCentsFragment : AppCompatDialogFragment()
{
    private var listener: OnSendCentsInteractionListener? = null

    interface OnSendCentsInteractionListener
    {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object
    {
        const val TAG = "send_cents_fragment"

        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                SendCentsFragment().apply {
                    arguments = Bundle().apply {

                        val bundleTheme = theme.toBundle()
                        putBundle(CustomTheme.TAG, bundleTheme)
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, 0)
        arguments?.let {
            val bundleTheme = it.getBundle(CustomTheme.TAG)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        dialog.setTitle(getString(R.string.delegate_and_contract_title))

        /*
        default_contract_button.setOnClickListener {
            listener?.onContractClicked(false)
            dismiss()
        }
        daily_spending_limit_contract_button.setOnClickListener {
            listener?.onContractClicked(true)
            dismiss()
        }
        */
    }

    override fun onResume()
    {
        super.onResume()

        validateSendCentsButton(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_sent_cents, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnSendCentsInteractionListener)
        {
            listener = context
        }
        else
        {
            //throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    private fun validateSendCentsButton(validate: Boolean)
    {
        val bundleTheme = arguments!!.getBundle(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(bundleTheme)

        if (validate)
        {
            send_cents_button.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            send_cents_button_layout.isEnabled = true
            send_cents_button_layout.background = makeSelector(theme)

            val drawables = send_cents_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
        }
        else
        {
            send_cents_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            send_cents_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            send_cents_button_layout.background = makeSelector(greyTheme)

            val drawables = send_cents_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
        }
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
