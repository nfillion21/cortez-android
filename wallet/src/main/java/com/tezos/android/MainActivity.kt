package com.tezos.android

import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.core.utils.SeedManager
import com.tezos.core.utils.TezosUtils
import com.tezos.ui.activity.CreateWalletActivity
import com.tezos.ui.activity.PaymentScreenActivity
import com.tezos.ui.activity.RestoreWalletActivity
import com.tezos.ui.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener
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

        val theme3 = CustomTheme(
                com.tezos.ui.R.color.theme_yellow_primary,
                com.tezos.ui.R.color.theme_yellow_primary_dark,
                com.tezos.ui.R.color.theme_yellow_text)

        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        initActionBar(tezosTheme)

        //Toolbar toolbar = (Toolbar) demoActivity.findViewById(R.id.toolbar);
        //toolbar.setBackgroundColor(ContextCompat.getColor(demoActivity, customTheme.getColorPrimaryId()));
        //toolbar.setTitleTextColor(ContextCompat.getColor(demoActivity, customTheme.getTextColorPrimaryId()));

        //PaymentScreenActivity.start(this)

        val restoreWalletButton = findViewById<Button>(R.id.restoreWalletButton)
        restoreWalletButton.setOnClickListener {
            RestoreWalletActivity.start(this, tezosTheme)
        }

        val createWalletButton = findViewById<Button>(R.id.createWalletButton)
        createWalletButton.setOnClickListener {
            CreateWalletActivity.start(this, tezosTheme)
        }
    }

    private fun initActionBar(theme:CustomTheme) {

        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {

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

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun validatePayButton(button:Button, theme:CustomTheme) {

        button.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))

        button.background = Utils.makeSelector(this, theme)

        val drawables = button.compoundDrawables
        //val wrapDrawable = DrawableCompat.wrap(drawables[0])
        //DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.textColorPrimaryId))
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_transfer -> {

                val tezosTheme = CustomTheme(
                        com.tezos.ui.R.color.theme_tezos_primary,
                        com.tezos.ui.R.color.theme_tezos_primary_dark,
                        com.tezos.ui.R.color.theme_tezos_text)
                PaymentScreenActivity.start(this, tezosTheme)
            }
            R.id.nav_publickey -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
