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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.tezcore.cortez.activities.AboutActivity
import com.tezcore.cortez.activities.SettingsActivity
import com.tezcore.ui.activity.DelegateActivity
import com.tezos.android.R
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.activity.*
import com.tezos.ui.authentication.ContractSelectorFragment
import com.tezos.ui.fragment.*
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.hexToByteArray
import kotlinx.android.synthetic.main.activity_home.*
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec


class HomeActivity : BaseSecureActivity(), AddressBookFragment.OnCardSelectedListener, HomeFragment.HomeListener, ContractsFragment.OnDelegateAddressSelectedListener, ContractSelectorFragment.OnContractSelectorListener
{

    private val mTezosTheme: CustomTheme = CustomTheme(
            com.tezos.ui.R.color.theme_tezos_primary,
            com.tezos.ui.R.color.theme_tezos_primary_dark,
            com.tezos.ui.R.color.theme_tezos_text)

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
                            fabSharing.hide()
                            fabAddDelegate.hide()
                        }
                        else
                        {
                            fabTransfer.hide()
                            fabSharing.hide()
                            fabAddDelegate.hide()
                        }
                    }

                    1 ->
                    {
                        if (isPasswordSaved)
                        {
                            fabTransfer.hide()
                            fabSharing.show()
                            fabAddDelegate.hide()
                        }
                        else
                        {
                            fabTransfer.hide()
                            fabSharing.hide()
                            fabAddDelegate.hide()
                        }
                    }

                    2 ->
                    {
                        if (isPasswordSaved)
                        {
                            fabTransfer.hide()
                            fabSharing.hide()
                            fabAddDelegate.show()
                        }
                        else
                        {
                            fabTransfer.hide()
                            fabSharing.hide()
                            fabAddDelegate.hide()
                        }
                    }

                    3 ->
                    {
                        //TODO hide everything for now
                        fabTransfer.hide()
                        fabSharing.hide()
                        fabAddDelegate.hide()
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

        fabTransfer.setOnClickListener { view ->

            val isPasswordSaved = Storage(this).isPasswordSaved()
            if (isPasswordSaved)
            {
                val seed = Storage(baseContext).getMnemonics()
                //val seedBundle = Storage.toBundle(seed)
                TransferFormActivity.start(this, seed.pkh, null, mTezosTheme)
            }
            else
            {
                //TODO this snackbar should be invisible
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
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
            //AddDelegateActivity.start(this, mTezosTheme)

            val dialog = ContractSelectorFragment.newInstance()
            dialog.show(supportFragmentManager, "ContractSelector")
        }

        initActionBar(mTezosTheme)

        //CryptoUtils.GeneratePublicKey()
    }

    private fun p256()
    {
        var keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                        "key1",
                KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                        //KeyProperties.DIGEST_SHA384,
                        //KeyProperties.DIGEST_SHA512)
                // Only permit the private key to be used if the user authenticated
                // within the last five minutes.
                //.setUserAuthenticationRequired(true)
                //.setUserAuthenticationValidityDurationSeconds(5 * 60)
                .build())
        var keyPair = keyPairGenerator.generateKeyPair()
        var signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)


        // The key pair can also be obtained from the Android Keystore any time as follows:
        var keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        var privateKey = keyStore.getKey("key1", null)
        //var privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey()

        var publicKey = keyStore.getCertificate("key1").publicKey// as ECPublicKey
        //var publicKey2 = keyStore.getCertificate("key1").publicKey

        var ecKey = publicKey as ECPublicKey

        var x = ecKey.w.affineX.toByteArray()
        //byte[] array = bigInteger.toByteArray();
        if (x[0].toInt() == 0)
        {
            val tmp = ByteArray(x.size - 1)
            System.arraycopy(x, 1, tmp, 0, tmp.size)
            x = tmp
        }


        var y = ecKey.w.affineY

        var yEvenOdd = if (y.rem(BigInteger.valueOf(2L)) == BigInteger.ZERO)
        {
            "0x02".hexToByteArray()
        }
        else
        {
            "0x03".hexToByteArray()
        }


        val xLen = x.size

        val yLen = yEvenOdd.size
        val result = ByteArray(yLen + xLen)

        System.arraycopy(yEvenOdd, 0, result, 0, yLen)
        System.arraycopy(x, 0, result, yLen, xLen)


        //var ecPointX = ecKey.w.affineX.toByteArray()

        //val publicKeyLength = (extended.size - 1) / 2 * 8

        val tz3 = CryptoUtils.generatePkhTz3(result)
        val p2pk = CryptoUtils.generateP2Pk(result)
        val tz34 = CryptoUtils.generatePkhTz3(result)


        //val tz3 = CryptoUtils.generatePkh(x)
        //val tz32 = CryptoUtils.generatePkh(result)

        //var privateKey2 = privateKey.toString()
        //var publicKey2 = publicKey.toString()
        //var publicKey3 = publicKey.toString()

    }

    private fun verifySig(data:ByteArray, signature:ByteArray)
    {
        /*
  * Verify a signature previously made by a PrivateKey in our
  * KeyStore. This uses the X.509 certificate attached to our
  * private key in the KeyStore to validate a previously
  * generated signature.
  */
        val ks = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val entry = ks.getEntry("key1", null) as? KeyStore.PrivateKeyEntry
        if (entry != null)
        {
            val valid: Boolean = Signature.getInstance("SHA256withECDSA").run {
                initVerify(entry.certificate)
                update(data)
                verify(signature)
            }

            val validTest = valid
            //TODO test valid
        }
    }

    private fun signData(data:ByteArray):ByteArray
    {
        /*
        * Use a PrivateKey in the KeyStore to create a signature over
        * some data.
        */

        val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val entry: KeyStore.Entry = ks.getEntry("key1", null)
        if (entry is KeyStore.PrivateKeyEntry)
        {
            return Signature.getInstance("SHA256withECDSA").run {
                initSign(entry.privateKey)
                update(data)
                sign()
            }
        }
        return ByteArray(0)
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm)
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
                        fabSharing.hide()
                        fabAddDelegate.hide()
                    }
                    else
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                        fabAddDelegate.hide()
                    }
                }

                1 ->
                {
                    if (isPasswordSaved)
                    {
                        fabTransfer.hide()
                        fabSharing.show()
                        fabAddDelegate.hide()
                    }
                    else
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                        fabAddDelegate.hide()
                    }
                }

                2 ->
                {
                    if (isPasswordSaved)
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                        fabAddDelegate.show()
                    }
                    else
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                        fabAddDelegate.hide()
                    }
                }

                3 ->
                {
                    fabTransfer.hide()
                    fabSharing.hide()
                    fabAddDelegate.hide()
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
                    val isPasswordSaved = Storage(this@HomeActivity).isPasswordSaved()

                    return if (isPasswordSaved)
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
            // Show 5 total pages.
            return 3
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


                        p256()
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

            SettingsActivity.SETTINGS_REQUEST_CODE ->
            {
                if (resultCode == R.id.logout_succeed)
                {
                    showSnackBar(getString(R.string.log_out_succeed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
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
                if (resultCode == R.id.remove_delegate_succeed)
                {
                    showSnackBar(getString(R.string.delegation_successfully_deleted), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                }
                else if (resultCode == R.id.add_delegate_succeed)
                {
                    showSnackBar(getString(R.string.delegate_successfully_added), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
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
        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryId))
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings)
        {
            SettingsActivity.start(this, mTezosTheme)
            return true
        }
        else if (id == R.id.action_about)
        {
            AboutActivity.start(this, mTezosTheme)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /*
    Addresses
     */

    override fun onCardClicked(address: Address?)
    {
        //AddressDetailsActivity.start(this, mTezosTheme, address!!)

        val isPasswordSaved = Storage(this).isPasswordSaved()
        if (isPasswordSaved)
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

    override fun onContractClicked(withScript: Boolean)
    {
        if (withScript)
        {
            AddLimitsActivity.start(this, mTezosTheme)
        }
        else
        {
            AddDelegateActivity.start(this, mTezosTheme)
        }
    }
}
