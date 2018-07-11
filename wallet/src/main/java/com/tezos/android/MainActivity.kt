package com.tezos.android

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.support.annotation.Nullable
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.tezos.android.activities.AboutActivity
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.android.activities.SettingsActivity
import com.tezos.core.utils.AddressesDatabase
import com.tezos.ui.activity.*
import com.tezos.ui.interfaces.IPasscodeHandler
import com.tezos.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, IPasscodeHandler
{
    private val pkHashKey = "pkhash_key"
    private var mPublicKeyHash: String? = null

    private var mRestoreWalletButton: Button? = null
    private var mCreateWalletButton: Button? = null
    private var mTezosLogo: ImageView? = null

    private var animating = false

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

        mRestoreWalletButton = findViewById(R.id.restoreWalletButton)
        mRestoreWalletButton!!.setOnClickListener {
            RestoreWalletActivity.start(this, tezosTheme)
        }

        mCreateWalletButton = findViewById(R.id.createWalletButton)
        mCreateWalletButton!!.setOnClickListener {
            CreateWalletActivity.start(this, tezosTheme)
        }

        mTezosLogo = findViewById(R.id.ic_logo)

        if (savedInstanceState != null)
        {
            mPublicKeyHash = savedInstanceState.getString(pkHashKey, null)
        }
    }

    override fun onResume()
    {
        super.onResume()
        launchPasscode()

        if (!animating)
        {
            val isPrivateKeyEnabled = AddressesDatabase.getInstance().isPrivateKeyOn(this)
            setMenuItemEnabled(isPrivateKeyEnabled)

            if (isPrivateKeyEnabled)
            {
                mTezosLogo!!.visibility = View.GONE
                mCreateWalletButton!!.visibility = View.GONE
                mRestoreWalletButton!!.visibility = View.GONE
            }
            else
            {
                mTezosLogo!!.visibility = View.VISIBLE
                mCreateWalletButton!!.visibility = View.VISIBLE
                mRestoreWalletButton!!.visibility = View.VISIBLE
            }
        }

        animating = false
    }

    private fun animateLogo()
    {
        animating = true

        val animatorCreateButton = ObjectAnimator.ofFloat(mCreateWalletButton, View.ALPHA, 0.0f)
        animatorCreateButton.duration = 1000
        animatorCreateButton.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mCreateWalletButton!!.visibility = View.GONE
            }
        })
        animatorCreateButton.start()

        val animatorRestoreButton = ObjectAnimator.ofFloat(mRestoreWalletButton, View.ALPHA, 0.0f)
        animatorRestoreButton.duration = 1000
        animatorRestoreButton.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mTezosLogo!!.visibility = View.GONE
            }
        })
        animatorRestoreButton.start()

        val animator = ObjectAnimator.ofFloat(mTezosLogo, View.ALPHA, 0.0f)
        animator.duration = 1000
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mTezosLogo!!.visibility = View.GONE
                animating = false
            }
        })
        animator.start()
    }

    override fun launchPasscode()
    {
        ScreenUtils.launchPasscode(this)
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

                        val snackbar = Snackbar.make(findViewById<Button>(R.id.coordinator), R.string.wallet_successfully_created, Snackbar.LENGTH_LONG)
                        val snackBarView = snackbar.getView()
                        snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                                R.color.tz_green)))
                        snackbar.show()

                        AddressesDatabase.getInstance().setPrivateKeyOn(this, true)
                        setMenuItemEnabled(true)
                        animateLogo()
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
                        val snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.wallet_successfully_restored, Snackbar.LENGTH_LONG)
                        val snackBarView = snackbar.getView()
                        snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                                android.R.color.holo_green_light)))
                        snackbar.show()

                        AddressesDatabase.getInstance().setPrivateKeyOn(this, true)
                        setMenuItemEnabled(true)

                        animateLogo()
                    }
                    else
                    {
                        //Log.v("ProjectDetails", "data is null")
                    }
                }
            }
        }
    }

    private fun setMenuItemEnabled(enabled:Boolean)
    {
        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView

        // get menu from navigationView
        val menu = navigationView.menu

        val transferMenuItem = menu.findItem(R.id.nav_transfer)
        transferMenuItem.isEnabled = enabled

        val publicKeyMenuItem = menu.findItem(R.id.nav_publickey)
        publicKeyMenuItem.isEnabled = enabled
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
                //AddAddressActivity.start(this, tezosTheme)
                PaymentAccountsActivity.start(this, tezosTheme, PaymentAccountsActivity.FromScreen.FromHome, PaymentAccountsActivity.Selection.SelectionAddresses)
            }
            R.id.nav_settings ->
            {
                /*
                val starter = Intent(this, AboutActivity::class.java)
                starter.putExtra(CustomTheme.TAG, tezosTheme.toBundle())
                ActivityCompat.startActivityForResult(this, starter, -1, null)
                */
                SettingsActivity.start(this, tezosTheme)
            }
            R.id.nav_info ->
            {
                val starter = Intent(this, AboutActivity::class.java)
                starter.putExtra(CustomTheme.TAG, tezosTheme.toBundle())
                ActivityCompat.startActivityForResult(this, starter, -1, null)
            }
        }

        //drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle?)
    {
        super.onSaveInstanceState(outState)

        outState?.putString(pkHashKey, mPublicKeyHash)
    }
}
