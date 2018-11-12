package com.tezos.ui.fragment

import android.os.Bundle
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme

class OperationsFragment: HomeFragment()
{
    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme, address: Address?) =
                OperationsFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putBundle(Address.TAG, address?.toBundle())
                    }
                }
    }
}