package com.tezos.ui.fragment

import android.os.Bundle
import android.view.View
import com.tezos.core.models.CustomTheme
import kotlinx.android.synthetic.main.fragment_home.*

class HomeDelegateFragment: HomeFragment()
{
    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme, pkh: String?) =
                HomeDelegateFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(PKH_KEY, pkh)
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null)
        {
            ongoing_latest_title_layout.visibility = View.GONE
            ongoing_latest_item_layout.visibility = View.GONE
        }
    }

    override fun startInitialLoadingMultisigOngoingOperations()
    {
        //no-op
    }
}