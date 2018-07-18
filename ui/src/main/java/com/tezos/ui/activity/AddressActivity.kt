package com.tezos.ui.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R

class AddressActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address)

        // first get the theme
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        //initActionBar(tezosTheme)

        if (savedInstanceState == null)
        {
            switchToOperations()
        }
    }

    private fun switchToOperations()
    {
        val tezosTheme = CustomTheme(
                R.color.theme_tezos_primary,
                R.color.theme_tezos_primary_dark,
                R.color.theme_tezos_text)

        /*
        val operationsFragment = OperationsFragment.newInstance(tezosTheme)
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragments_container, operationsFragment)
                .commit()
        */
    }
}