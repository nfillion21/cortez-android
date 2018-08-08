package com.tezcore.cortez.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.tezos.android.R
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.activity.BaseSecureActivity
import com.tezos.ui.interfaces.IPasscodeHandler
import com.tezos.ui.utils.ScreenUtils


class AboutActivity : BaseSecureActivity(), IPasscodeHandler
{
    private var mMailButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        initActionBar(tezosTheme)

        mMailButton = findViewById(R.id.mailButton)
        mMailButton?.setOnClickListener { _ ->

            sendMail()
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

        val mCloseButton = findViewById<ImageButton>(R.id.close_button)
        mCloseButton.setColorFilter(ContextCompat.getColor(this, R.color.theme_tezos_text))
        mCloseButton.setOnClickListener {
            finish()
        }
    }

    private fun sendMail()
    {
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:cortez@tezcore.com")

        try {
            startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            //TODO: Handle case where no email app is available
        }
    }
}
