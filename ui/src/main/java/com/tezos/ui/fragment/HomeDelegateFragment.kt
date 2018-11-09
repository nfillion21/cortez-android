package com.tezos.ui.fragment

import android.os.Bundle
import com.tezos.core.models.CustomTheme

class HomeDelegateFragment: HomeFragment()
{
    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme, pkh: String?) =
                HomeFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(PKH_TAG, pkh)
                    }
                }
    }

    override fun isHome():Boolean
    {
        return false
    }
}