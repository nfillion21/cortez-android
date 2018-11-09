package com.tezos.ui.fragment

import android.os.Bundle
import com.tezos.core.models.CustomTheme

class HomeDelegateFragment: HomeFragment()
{
    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme, pkh: String?) =
                HomeDelegateFragment().apply {
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

    override fun pkh():String?
    {
        var pkh:String? = null
        arguments?.let {

            pkh = it.getString(PKH_TAG)
        }

        if (pkh == null)
        {
            //TODO let's hope the pkh won't disappear
            /*
            val mnemonicsData = Storage(activity!!).getMnemonics()

            val args = arguments
            args?.putString(PKH_TAG, mnemonicsData.pkh)

            pkh = mnemonicsData.pkh
            */
        }

        return pkh
    }
}