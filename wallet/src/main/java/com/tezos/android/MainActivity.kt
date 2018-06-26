package com.tezos.android

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tezos.core.utils.SeedManager
import com.tezos.core.utils.TezosUtils
import com.tezos.ui.activity.PaymentScreenActivity

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mnemonics = "orchard roof outside sustain depth robot inherit across coil hospital gravity guilt feel napkin hire tank yard mandate theme learn hollow gravity permit undo"
        SeedManager.getInstance().save(this, TezosUtils.generateNovaSeed(mnemonics))

        PaymentScreenActivity.start(this)
    }
}
