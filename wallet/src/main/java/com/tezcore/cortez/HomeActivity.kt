package com.tezcore.cortez

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.tezcore.cortez.activities.AboutActivity
import com.tezcore.cortez.activities.SettingsActivity
import com.tezcore.cortez.fragments.HomeFragment
import com.tezos.android.R
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.activity.*
import com.tezos.ui.fragment.OperationsFragment
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : BaseSecureActivity(), NavigationView.OnNavigationItemSelectedListener, HomeFragment.OnFragmentInteractionListener
{
    override fun onFragmentInteraction() {
        //switchToOperations(realSeed)
    }

    companion object {
        const val ADD_SECRET_REQUEST_CODE = 300
        const val AUTHENTICATION_SCREEN_CODE = 301
    }

    private var mProgressBar: ProgressBar? = null

    private var isAuthenticating = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // first get the theme
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        initActionBar(tezosTheme)

        val isPasswordSaved = Storage(this).isPasswordSaved()
        if (isPasswordSaved)
        {
            val seed = Storage(baseContext).getMnemonics()
            switchToOperations(seed)
        }
        else
        {
            switchToHome()
        }

        setMenuItemEnabled(isPasswordSaved)

        if (savedInstanceState != null)
        {
            //TODO not useful anymore, remote it.
            //mPublicKeyHash = savedInstanceState.getString(pkHashKey, null)
        }
        else
        {
            //switchToHome()
        }
    }

    private fun switchToOperations(realMnemonics: Storage.MnemonicsData)
    {
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        var address = Address()
        address.description = "main address"
        address.pubKeyHash = realMnemonics.pkh

        val operationsFragment = OperationsFragment.newInstance(tezosTheme, address)
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragments_container, operationsFragment)
                .commit()
    }

    private fun switchToHome()
    {
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        val homeFragment = HomeFragment.newInstance(tezosTheme)
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragments_container, homeFragment)
                .commit()
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
                    /*
                    if (data != null && data.hasExtra(CryptoUtils.WALLET_BUNDLE_KEY))
                    {
                        val walletBundle = data.getBundleExtra(CryptoUtils.WALLET_BUNDLE_KEY)
                        mPublicKeyHash = walletBundle.getString(CryptoUtils.PUBLIC_KEY_HASH_KEY)

                        val snackbar = Snackbar.make(findViewById<Button>(R.id.coordinator), R.string.wallet_successfully_created, Snackbar.LENGTH_LONG)
                        val snackBarView = snackbar.getView()
                        snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                                R.color.tz_green)))
                        snackbar.show()

                        setMenuItemEnabled(true)

                        switchToOperations(realSeed)
                    }
                    else
                    {
                        //Log.v("ProjectDetails", "data is null")
                    }
                        */

                    //TODO check data later
                    if (data != null && data.hasExtra(CreateWalletActivity.SEED_DATA_KEY))
                    {
                        val seedDataKey = data.getBundleExtra(CreateWalletActivity.SEED_DATA_KEY)
                        val realSeed = Storage.fromBundle(seedDataKey)

                        // TODO offset it
                        val snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.wallet_successfully_restored, Snackbar.LENGTH_LONG)
                        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                                android.R.color.holo_green_light)))
                        snackbar.show()

                        setMenuItemEnabled(true)

                        switchToOperations(realSeed)
                    }
                    else
                    {

                    }
                }
            }

            RestoreWalletActivity.RESTORE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.restore_wallet_succeed)
                {
                    //val seeds = Storage(baseContext).getMnemonicsList()
                    //val seedOne = seeds[0]

                    // in marshmallow, you don't need any password
                    //val mnemonics = EncryptionServices(applicationContext).decrypt(seedOne.mnemonics, "not useful for marshmallow")
                    //val walletBundle = data.getBundleExtra(CryptoUtils.WALLET_BUNDLE_KEY)
                    //mPublicKeyHash = walletBundle.getString(CryptoUtils.PUBLIC_KEY_HASH_KEY)

                    //TODO check data later
                    if (data != null && data.hasExtra(RestoreWalletActivity.SEED_DATA_KEY))
                    {
                        val seedDataKey = data.getBundleExtra(RestoreWalletActivity.SEED_DATA_KEY)
                        val realSeed = Storage.fromBundle(seedDataKey)

                        // TODO offset it
                        val snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.wallet_successfully_restored, Snackbar.LENGTH_LONG)
                        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                                android.R.color.holo_green_light)))
                        snackbar.show()

                        setMenuItemEnabled(true)

                        switchToOperations(realSeed)
                    }
                    else
                    {

                    }
                }
            }

            SettingsActivity.SETTINGS_REQUEST_CODE ->
            {
                if (resultCode == R.id.logout_succeed)
                {
                    switchToHome()
                    setMenuItemEnabled(false)
                }
            }

            AUTHENTICATION_SCREEN_CODE ->
            {
                isAuthenticating = false
                if (resultCode != Activity.RESULT_OK) {
                    finish()
                }
            }

            else ->
            {
                //handleVisibility()
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

        val settingsMenuItem = menu.findItem(R.id.nav_settings)
        settingsMenuItem.isEnabled = enabled
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

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val titleBar = findViewById<TextView>(R.id.barTitle)
        titleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))

        mProgressBar = findViewById(R.id.nav_progress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mProgressBar?.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, theme.textColorPrimaryId))
        }
        else
        {
            //mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.colorTextToolbar), PorterDuff.Mode.SRC_IN);
        }

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

            AlertDialog.Builder(this)
                    .setTitle(R.string.exit)
                    .setMessage(R.string.exit_info)
                    .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener
                    {
                        dialog,
                        which ->

                        super.onBackPressed()

                    })
                    .setNegativeButton(android.R.string.no, null).show()
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
                val isPasswordSaved = Storage(this).isPasswordSaved()
                if (isPasswordSaved)
                {
                    val seed = Storage(baseContext).getMnemonics()
                    val seedBundle = Storage.toBundle(seed)
                    TransferFormActivity.start(this, seedBundle, tezosTheme)
                }
            }
            R.id.nav_publickey ->
            {
                val isPasswordSaved = Storage(this).isPasswordSaved()
                if (isPasswordSaved)
                {
                    val seed = Storage(baseContext).getMnemonics()
                    PublicKeyHashActivity.start(this, seed.pkh, tezosTheme)
                }
            }
            R.id.nav_addresses ->
            {
                //AddAddressActivity.start(this, tezosTheme)
                PaymentAccountsActivity.start(this, tezosTheme, PaymentAccountsActivity.FromScreen.FromHome, PaymentAccountsActivity.Selection.SelectionAddresses)
            }
            R.id.nav_settings ->
            {
                SettingsActivity.start(this, tezosTheme)
            }
            R.id.nav_info ->
            {
                AboutActivity.start(this, tezosTheme)
            }
        }

        //drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
