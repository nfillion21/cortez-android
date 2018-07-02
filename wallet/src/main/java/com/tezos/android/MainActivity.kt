package com.tezos.android

import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Window
import android.widget.Button
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
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

        val theme2 = CustomTheme(
                com.tezos.ui.R.color.theme_blue_primary,
                com.tezos.ui.R.color.theme_blue_primary_dark,
                com.tezos.ui.R.color.theme_blue_text)

        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {

            val window = getWindow()
            window.statusBarColor = ContextCompat.getColor(this, theme2.colorPrimaryDarkId)
        }

        //Toolbar toolbar = (Toolbar) demoActivity.findViewById(R.id.toolbar);
        //toolbar.setBackgroundColor(ContextCompat.getColor(demoActivity, customTheme.getColorPrimaryId()));
        //toolbar.setTitleTextColor(ContextCompat.getColor(demoActivity, customTheme.getTextColorPrimaryId()));

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
            PaymentScreenActivity.start(this, theme)
        }
    }
}
