/*
(*****************************************************************************)
(*                                                                           *)
(* Open Source License                                                       *)
(* Copyright (c) 2018 Nomadic Development, Inc. <contact@tezcore.com>        *)
(*                                                                           *)
(* Permission is hereby granted, free of charge, to any person obtaining a   *)
(* copy of this software and associated documentation files (the "Software"),*)
(* to deal in the Software without restriction, including without limitation *)
(* the rights to use, copy, modify, merge, publish, distribute, sublicense,  *)
(* and/or sell copies of the Software, and to permit persons to whom the     *)
(* Software is furnished to do so, subject to the following conditions:      *)
(*                                                                           *)
(* The above copyright notice and this permission notice shall be included   *)
(* in all copies or substantial portions of the Software.                    *)
(*                                                                           *)
(* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR*)
(* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  *)
(* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL   *)
(* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER*)
(* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING   *)
(* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER       *)
(* DEALINGS IN THE SOFTWARE.                                                 *)
(*                                                                           *)
(*****************************************************************************)
*/

package com.tezcore.cortez

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.ProgressBar
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

    private var mProgressBar: ProgressBar? = null

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(toolbar)
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        // first get the theme
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)
        fab.setOnClickListener { view ->

            val isPasswordSaved = Storage(this).isPasswordSaved()
            if (isPasswordSaved)
            {
                val seed = Storage(baseContext).getMnemonics()
                val seedBundle = Storage.toBundle(seed)
                TransferFormActivity.start(this, seedBundle, tezosTheme)
            }
            else
            {
                //TODO this snackbar should be invisible
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
            }
        }

        initActionBar(tezosTheme)

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

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm)
    {
        override fun getItem(position: Int): Fragment
        {
            when (position)
            {
                0 -> {
                    val tezosTheme = CustomTheme(
                            com.tezos.ui.R.color.theme_tezos_primary,
                            com.tezos.ui.R.color.theme_tezos_primary_dark,
                            com.tezos.ui.R.color.theme_tezos_text)
                    val isPasswordSaved = Storage(this@HomeActivity).isPasswordSaved()
                    //setMenuItemEnabled(isPasswordSaved)

                    if (isPasswordSaved)
                    {
                        val mnemonicsData = Storage(baseContext).getMnemonics()

                        var address = Address()
                        address.description = "main address"
                        address.pubKeyHash = mnemonicsData.pkh

                        val operationsFragment = OperationsFragment.newInstance(tezosTheme, address)
                        return operationsFragment
                    }
                    else
                    {
                        //switchToHome()

                        val homeFragment = HomeFragment.newInstance(tezosTheme)
                        return homeFragment

                    }

                }
                1 ->
                {
                    //
                }
                2 ->
                {
                    //
                }
                else ->
                {
                    //
                }
            }

            val tezosTheme = CustomTheme(
                    com.tezos.ui.R.color.theme_tezos_primary,
                    com.tezos.ui.R.color.theme_tezos_primary_dark,
                    com.tezos.ui.R.color.theme_tezos_text)

            val homeFragment = HomeFragment.newInstance(tezosTheme)
            return homeFragment
        }

        override fun getCount(): Int
        {
            // Show 3 total pages.
            return 3
        }
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
                    if (data != null && data.hasExtra(CreateWalletActivity.SEED_DATA_KEY))
                    {
                        val seedDataKey = data.getBundleExtra(CreateWalletActivity.SEED_DATA_KEY)
                        val realSeed = Storage.fromBundle(seedDataKey)

                        showSnackBar(R.string.wallet_successfully_created)

                        //setMenuItemEnabled(true)

                        switchToOperations(realSeed)
                    }
                }
            }

            RestoreWalletActivity.RESTORE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.restore_wallet_succeed)
                {
                    if (data != null && data.hasExtra(RestoreWalletActivity.SEED_DATA_KEY))
                    {
                        val seedDataKey = data.getBundleExtra(RestoreWalletActivity.SEED_DATA_KEY)
                        val realSeed = Storage.fromBundle(seedDataKey)

                        showSnackBar(R.string.wallet_successfully_restored)

                        //setMenuItemEnabled(true)

                        switchToOperations(realSeed)
                    }
                }
            }

            TransferFormActivity.TRANSFER_REQUEST_CODE ->
            {
                if (resultCode == R.id.transfer_succeed)
                {
                    showSnackBar(R.string.transfer_succeed)
                    //TODO I need to refresh balance.
                }
            }

            SettingsActivity.SETTINGS_REQUEST_CODE ->
            {
                if (resultCode == R.id.logout_succeed)
                {
                    switchToHome()
                    //setMenuItemEnabled(false)
                }
            }

            else ->
            {
            }
        }
    }

    private fun showSnackBar(resText:Int)
    {
        val snackbar = Snackbar.make(findViewById(R.id.coordinator), resText, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                android.R.color.holo_green_light)))
        snackbar.show()
    }

    /*
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
    */

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

        /*
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
        */
    }

    override fun onBackPressed()
    {
        /*
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
        {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        else
        {
        }
        */
        AlertDialog.Builder(this)
                .setTitle(R.string.exit)
                .setMessage(R.string.exit_info)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes) {
                    _,
                    _ ->

                    super.onBackPressed()

                }
                .show()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings)
        {
            val tezosTheme = CustomTheme(
                    com.tezos.ui.R.color.theme_tezos_primary,
                    com.tezos.ui.R.color.theme_tezos_primary_dark,
                    com.tezos.ui.R.color.theme_tezos_text)
            SettingsActivity.start(this, tezosTheme)
            return true
        }
        else if (id == R.id.action_about)
        {
            val tezosTheme = CustomTheme(
                    com.tezos.ui.R.color.theme_tezos_primary,
                    com.tezos.ui.R.color.theme_tezos_primary_dark,
                    com.tezos.ui.R.color.theme_tezos_text)
            AboutActivity.start(this, tezosTheme)
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
