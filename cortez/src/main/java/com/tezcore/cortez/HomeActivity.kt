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
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.crashlytics.android.Crashlytics
import com.tezcore.cortez.activities.AboutActivity
import com.tezcore.cortez.activities.SettingsActivity
import com.tezos.ui.activity.DelegateActivity
import com.tezos.android.R
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.activity.*
import com.tezos.ui.fragment.ContractSelectorFragment
import com.tezos.ui.fragment.AddressBookFragment
import com.tezos.ui.fragment.ContractsFragment
import com.tezos.ui.fragment.HomeFragment
import com.tezos.ui.fragment.SharingAddressFragment
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.hexToByteArray
import kotlinx.android.synthetic.main.activity_home.*
import java.math.BigInteger
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey


class HomeActivity : BaseSecureActivity(), AddressBookFragment.OnCardSelectedListener, HomeFragment.HomeListener, ContractsFragment.OnDelegateAddressSelectedListener, ContractSelectorFragment.OnContractSelectorListener
{

    /*
    private val mTezosTheme: CustomTheme = CustomTheme(
            com.tezos.ui.R.color.theme_tezos_primary,
            com.tezos.ui.R.color.theme_tezos_primary_dark,
            com.tezos.ui.R.color.theme_tezos_text)
    */

    private val mTezosTheme: CustomTheme = CustomTheme(
            com.tezos.ui.R.color.theme_boo_primary,
            com.tezos.ui.R.color.theme_boo_primary_dark,
            com.tezos.ui.R.color.theme_boo_text)

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        /*
        val date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Date.from(Instant.parse("2019-06-08T10:22:59Z"))
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        */




        setSupportActionBar(toolbar)
        initActionBar(mTezosTheme)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        container.addOnPageChangeListener(object : ViewPager.OnPageChangeListener
        {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)
            {}

            override fun onPageSelected(position: Int)
            {
                val isPasswordSaved = Storage(this@HomeActivity).isPasswordSaved()
                when (position)
                {
                    0 ->
                    {
                        if (isPasswordSaved)
                        {
                            fabTransfer.show()
                        }
                        else
                        {
                            fabTransfer.hide()
                        }

                        fabSharing.hide()
                        fabAddAddress.hide()
                        fabAddDelegate.hide()
                    }

                    1 ->
                    {
                        if (isPasswordSaved)
                        {
                            fabSharing.show()
                        }
                        else
                        {
                            fabSharing.hide()
                        }

                        fabAddAddress.hide()
                        fabTransfer.hide()
                        fabAddDelegate.hide()
                    }

                    2 ->
                    {
                        fabAddAddress.show()
                        fabTransfer.hide()
                        fabSharing.hide()
                        fabAddDelegate.hide()
                    }

                    3 ->
                    {
                        if (isPasswordSaved)
                        {
                            fabAddDelegate.show()
                        }
                        else
                        {
                            fabAddDelegate.hide()
                        }

                        fabAddAddress.hide()
                        fabTransfer.hide()
                        fabSharing.hide()
                    }

                    else ->
                    {
                        //no-op
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int)
            {}
        })

        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        fabTransfer.setOnClickListener {

            val hasMnemonics = Storage(this).hasMnemonics()
            if (hasMnemonics)
            {
                val seed = Storage(baseContext).getMnemonics()

                if (seed.mnemonics.isNotEmpty())
                {
                    TransferFormActivity.start(this, seed.pkh, null, mTezosTheme)
                }
                else
                {
                    showSnackBar(getString(R.string.no_mnemonics_found), ContextCompat.getColor(this, R.color.tz_accent), Color.YELLOW)
                }
            }
            else
            {
                //the fab is invisible
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        //.setAction("Action", null).show()
            }
        }

        fabSharing.setOnClickListener { view ->

            val isPasswordSaved = Storage(this).isPasswordSaved()
            if (isPasswordSaved)
            {
                val seed = Storage(baseContext).getMnemonics()

                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, seed.pkh)
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
            }
            else
            {
                //TODO this snackbar should be invisible
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
            }
        }

        fabAddDelegate.setOnClickListener {

            val hasMnemonics = Storage(this).hasMnemonics()
            if (hasMnemonics)
            {
                val seed = Storage(baseContext).getMnemonics()

                if (seed.mnemonics.isNotEmpty())
                {
                    val dialog = ContractSelectorFragment.newInstance()
                    dialog.show(supportFragmentManager, "ContractSelector")
                }
                else
                {
                    showSnackBar(getString(R.string.no_mnemonics_contracts), ContextCompat.getColor(this, R.color.tz_accent), Color.YELLOW)
                }
            }
        }

        fabAddAddress.setOnClickListener {
            AddAddressActivity.start(this, mTezosTheme)
        }

        initActionBar(mTezosTheme)
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : androidx.fragment.app.FragmentPagerAdapter(fm)
    {

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any)
        {
            super.setPrimaryItem(container, position, `object`)
            val isPasswordSaved = Storage(this@HomeActivity).isPasswordSaved()

            when (position)
            {
                0 ->
                {
                    if (isPasswordSaved)
                    {
                        fabTransfer.show()
                    }
                    else
                    {
                        fabTransfer.hide()
                    }

                    fabSharing.hide()
                    fabAddDelegate.hide()
                    fabAddAddress.hide()
                }

                1 ->
                {
                    if (isPasswordSaved)
                    {
                        fabSharing.show()
                    }
                    else
                    {
                        fabSharing.hide()
                    }

                    fabTransfer.hide()
                    fabAddDelegate.hide()
                    fabAddAddress.hide()
                }

                2 ->
                {
                    fabAddAddress.show()
                    fabTransfer.hide()
                    fabSharing.hide()
                    fabAddDelegate.hide()
                }

                3 ->
                {
                    if (isPasswordSaved)
                    {
                        fabAddDelegate.show()
                    }
                    else
                    {
                        fabAddDelegate.hide()
                    }
                    fabTransfer.hide()
                    fabSharing.hide()
                    fabAddAddress.hide()
                }

                else ->
                {
                    //no-op
                }
            }
        }

        override fun getItem(position: Int): Fragment
        {
            when (position)
            {
                0 ->
                {
                    return HomeFragment.newInstance(mTezosTheme)
                }
                1 ->
                {
                    return SharingAddressFragment.newInstance(mTezosTheme, null)
                }

                2 ->
                {
                    return AddressBookFragment.newInstance(mTezosTheme, null, AddressBookActivity.Selection.SelectionAccountsAndAddresses)
                }

                3 ->
                {
                    val hasMnemonics = Storage(this@HomeActivity).hasMnemonics()

                    return if (hasMnemonics)
                    {
                        val mnemonicsData = Storage(baseContext).getMnemonics()
                        ContractsFragment.newInstance(mTezosTheme, mnemonicsData.pkh)
                    }
                    else
                    {
                        ContractsFragment.newInstance(mTezosTheme, null)
                    }
                }
                else ->
                {
                    //should not happen
                    return Fragment()
                }
            }
        }

        override fun getCount(): Int
        {
            // Show 4 total pages.
            return 4
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
                    if (data != null && data.hasExtra(CreateWalletActivity.SEED_DATA_KEY))
                    {
                        showSnackBar(getString(R.string.wallet_successfully_created), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }
                }
            }

            RestoreWalletActivity.RESTORE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.restore_wallet_succeed)
                {
                    if (data != null && data.hasExtra(RestoreWalletActivity.SEED_DATA_KEY))
                    {
                        showSnackBar(getString(R.string.wallet_successfully_restored), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }
                }
            }

            TransferFormActivity.TRANSFER_REQUEST_CODE ->
            {
                if (resultCode == R.id.transfer_succeed)
                {
                    showSnackBar(getString(R.string.transfer_succeed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    //TODO I need to refresh balance.
                }
            }

            KeyManagementActivity.KEY_MANAGEMENT_CODE ->
            {
                when (resultCode)
                {
                    R.id.logout_succeed ->
                    {
                        showSnackBar(getString(R.string.log_out_succeed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }

                    R.id.master_key_removed ->
                    {
                        showSnackBar(getString(R.string.master_key_removed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }

                    R.id.restore_wallet_succeed ->
                    {
                        showSnackBar(getString(R.string.wallet_successfully_restored), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }
                }
            }

            AddAddressActivity.ADD_ADDRESS_REQUEST_CODE ->
            {
                if (resultCode == R.id.add_address_succeed)
                {
                    showSnackBar(getString(R.string.address_successfully_added), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                }
            }

            AddDelegateActivity.ADD_DELEGATE_REQUEST_CODE ->
            {
                if (resultCode == R.id.add_address_succeed)
                {
                    showSnackBar(getString(R.string.address_successfully_delegated), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                }
            }

            DelegateActivity.DELEGATE_REQUEST_CODE ->
            {
                when (resultCode)
                {
                    R.id.remove_delegate_succeed ->
                    {
                        showSnackBar(getString(R.string.delegation_successfully_deleted), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }

                    R.id.add_delegate_succeed ->
                    {
                        showSnackBar(getString(R.string.delegate_successfully_added), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }

                    R.id.request_remove_delegate ->
                    {
                        showSnackBar(getString(R.string.remove_baker_request_created), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }

                    R.id.update_storage_succeed ->
                    {
                        showSnackBar(getString(R.string.contract_storage_successfully_updated), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }

                    R.id.update_multisig_storage_succeed ->
                    {
                        showSnackBar(getString(R.string.contract_storage_multisig_successfully_updated), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }
                }
            }

            AddLimitsActivity.ADD_DSL_REQUEST_CODE ->
            {
                if (resultCode == R.id.add_dsl_succeed)
                {
                    showSnackBar(getString(R.string.dsl_successfully_added), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                }
            }

            OngoingMultisigActivity.ACCEPT_REQUEST_CODE ->
            {
                if (resultCode == R.id.multisig_operation_confirmed)
                {
                    showSnackBar(getString(R.string.multisig_operation_confirmed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                }
                else if (resultCode == R.id.multisig_ongoing_operation_removed)
                {
                    showSnackBar(getString(R.string.multisig_ongoing_operation_removed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                }
            }

            else ->
            {
            }
        }
    }

    override fun showSnackBar(res:String, color:Int, textColor:Int)
    {
        val snackbar = Snackbar.make(fabTransfer, res, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(color)
        snackbar.setActionTextColor(textColor)
        snackbar.show()
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
        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryDarkId))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))

        setSupportActionBar(toolbar)
    }

    override fun onBackPressed()
    {
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

    /*
    MENU
     */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId)
        {
            R.id.action_settings ->
            {
                SettingsActivity.start(this, mTezosTheme)
                true
            }
            R.id.action_about ->
            {
                AboutActivity.start(this, mTezosTheme)
                true
            }
            R.id.action_key_management ->
            {
                KeyManagementActivity.start(this, mTezosTheme)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /*
    Addresses
     */

    override fun onCardClicked(address: Address?)
    {
        //AddressDetailsActivity.start(this, mTezosTheme, address!!)

        val hasMnemonics = Storage(this).hasMnemonics()
        if (hasMnemonics)
        {
            val seed = Storage(baseContext).getMnemonics()
            //val seedBundle = Storage.toBundle(seed)
            TransferFormActivity.start(this, seed.pkh, address, mTezosTheme)
        }
        else
        {
            //TODO this snackbar should be invisible
            //Snackbar.make(fabTransfer, "Replace with your own action", Snackbar.LENGTH_LONG)
                    //.setAction("Action", null).show()

            showSnackBar(getString(R.string.create_restore_wallet_transfer_info), ContextCompat.getColor(this, R.color.tz_accent), Color.YELLOW)
        }
    }

    override fun onDelegateAddressClicked(address: String, pos: Int)
    {
        DelegateActivity.start(this, address, pos, mTezosTheme)
    }

    override fun onContractClicked(withScript: ContractSelectorFragment.Companion.ContractType)
    {
        when(withScript)
        {
            ContractSelectorFragment.Companion.ContractType.DEFAULT -> AddDelegateActivity.start(this, mTezosTheme)
            ContractSelectorFragment.Companion.ContractType.SPENDING_LIMIT -> AddLimitsActivity.start(this, mTezosTheme)
            ContractSelectorFragment.Companion.ContractType.MULTISIG -> AddMultisigActivity.start(this, mTezosTheme)
        }
    }
}
