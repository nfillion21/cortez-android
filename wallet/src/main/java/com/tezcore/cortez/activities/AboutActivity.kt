package com.tezcore.cortez.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.TextView
import com.tezos.android.R
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.interfaces.IPasscodeHandler
import com.tezos.ui.utils.ScreenUtils

class AboutActivity : AppCompatActivity(), IPasscodeHandler
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        initActionBar(tezosTheme)

        if (savedInstanceState != null)
        {
            //mPublicKeyHash = savedInstanceState.getString(PK_HASH_KEY, null)
        }
    }

    override fun onResume()
    {
        super.onResume()
        launchPasscode()
    }

    override fun launchPasscode()
    {
        ScreenUtils.launchPasscode(this)
    }

    fun start(activity: Activity, theme: CustomTheme)
    {
        var starter = getStartIntent(activity, theme)
        ActivityCompat.startActivityForResult(activity, starter, -1, null)
    }

    private fun getStartIntent(context: Context, theme:CustomTheme): Intent
    {
        val starter = Intent(context, AboutActivity::class.java)
        starter.putExtra(CustomTheme.TAG, theme.toBundle())

        return starter
    }

    private fun initActionBar(theme:CustomTheme)
    {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP))
        {
            val window = window
            window.statusBarColor = ContextCompat.getColor(this, theme.colorPrimaryDarkId)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        //toolbar.title = getString(R.string.app_name)
        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryId))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))

        setSupportActionBar(toolbar)

        try {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        } catch (e:Exception) {
        }
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val titleBar = findViewById<TextView>(R.id.barTitle)
        titleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
    }
}
