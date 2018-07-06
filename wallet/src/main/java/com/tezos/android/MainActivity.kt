package com.tezos.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.activity.*
import com.tezos.ui.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener
{
    private val PK_HASH_KEY = "pkhash_key"
    var mPublicKeyHash: String? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
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
        */

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

        if (savedInstanceState != null)
        {
            mPublicKeyHash = savedInstanceState.getString(PK_HASH_KEY, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode)
        {
            CreateWalletActivity.CREATE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.create_wallet_succeed)
                {
                    if (data != null && data.hasExtra(CryptoUtils.WALLET_BUNDLE_KEY))
                    {
                        val walletBundle = data.getBundleExtra(CryptoUtils.WALLET_BUNDLE_KEY)
                        mPublicKeyHash = walletBundle.getString(CryptoUtils.PUBLIC_KEY_HASH_KEY)

                        // TODO offset it
                        val snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.wallet_successfully_created, Snackbar.LENGTH_LONG)
                        val snackBarView = snackbar.getView()
                        snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                                android.R.color.holo_green_light)))
                        snackbar.show()
                    }
                    else
                    {
                        //Log.v("ProjectDetails", "data is null")
                    }
                }
            }

            RestoreWalletActivity.RESTORE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.restore_wallet_succeed)
                {
                    if (data != null && data.hasExtra(CryptoUtils.WALLET_BUNDLE_KEY))
                    {
                        val walletBundle = data.getBundleExtra(CryptoUtils.WALLET_BUNDLE_KEY)
                        mPublicKeyHash = walletBundle.getString(CryptoUtils.PUBLIC_KEY_HASH_KEY)

                        // TODO offset it
                        val snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.wallet_successfully_restored, Snackbar.LENGTH_LONG)
                        val snackBarView = snackbar.getView()
                        snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                                android.R.color.holo_green_light)))
                        snackbar.show()
                    }
                    else
                    {
                        //Log.v("ProjectDetails", "data is null")
                    }
                }
            }
        }
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean
    {
        // Handle navigation view item clicks here.
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        when (item.itemId)
        {
            R.id.nav_transfer ->
            {
                PaymentScreenActivity.start(this, tezosTheme)
            }
            R.id.nav_publickey ->
            {
                PublicKeyHashActivity.start(this, mPublicKeyHash, tezosTheme)
            }
            R.id.nav_addresses ->
            {
                AddAddressActivity.start(this, tezosTheme)
            }
            R.id.nav_info ->
            {

            }
        }

        //drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?)
    {
        super.onSaveInstanceState(outState)

        outState?.putString(PK_HASH_KEY, mPublicKeyHash)
    }
}
