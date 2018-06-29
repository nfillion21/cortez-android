package com.tezos.android

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.SeedManager
import com.tezos.core.utils.TezosUtils
import com.tezos.ui.activity.CreateWalletActivity
import com.tezos.ui.activity.PaymentScreenActivity
import com.tezos.ui.activity.RestoreWalletActivity

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mnemonics = "orchard roof outside sustain depth robot inherit across coil hospital gravity guilt feel napkin hire tank yard mandate theme learn hollow gravity permit undo"
        SeedManager.getInstance().save(this, TezosUtils.generateNovaSeed(mnemonics))

        val theme = CustomTheme(
                com.tezos.ui.R.color.tz_primary,
                com.tezos.ui.R.color.tz_primary_dark,
                com.tezos.ui.R.color.theme_blue_text)

        //PaymentScreenActivity.start(this)

        val restoreWalletButton = findViewById<Button>(R.id.restoreWalletButton)
        restoreWalletButton.setOnClickListener {
            RestoreWalletActivity.start(this, theme)
        }

        val createWalletButton = findViewById<Button>(R.id.createWalletButton)
        createWalletButton.setOnClickListener {
            CreateWalletActivity.start(this, theme)
        }

        val paymentScreenButton = findViewById<Button>(R.id.paymentScreenButton)
        paymentScreenButton.setOnClickListener {
            PaymentScreenActivity.start(this)
        }
    }
}
