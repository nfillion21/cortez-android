package com.tezos.android.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView

import com.tezos.android.R
import com.tezos.core.models.CustomTheme
import com.tezos.ui.activity.CreateWalletActivity
import com.tezos.ui.activity.RestoreWalletActivity

private const val ARG_THEME = "theme"
class HomeFragment : Fragment()
{
    private var listener: OnFragmentInteractionListener? = null

    private var mRestoreWalletButton: Button? = null
    private var mCreateWalletButton: Button? = null
    private var mTezosLogo: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        var tezosTheme:CustomTheme? = null
        arguments?.let {
            val themeBundle = it.getBundle(ARG_THEME)
            tezosTheme = CustomTheme.fromBundle(themeBundle)
        }

        mRestoreWalletButton = view.findViewById(R.id.restoreWalletButton)
        mRestoreWalletButton!!.setOnClickListener {
            RestoreWalletActivity.start(activity, tezosTheme)
        }

        mCreateWalletButton = view.findViewById(R.id.createWalletButton)
        mCreateWalletButton!!.setOnClickListener {
            CreateWalletActivity.start(activity, tezosTheme)
        }

        mTezosLogo = view.findViewById(R.id.ic_logo)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

   interface OnFragmentInteractionListener
   {
        // TODO: Update argument type and name
        fun onFragmentInteraction()
    }

    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                HomeFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(ARG_THEME, theme.toBundle())
                    }
                }
    }
}
