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

package com.tezos.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentTransaction
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import com.tezos.core.crypto.CryptoUtils

import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.authentication.PasswordDialog
import com.tezos.ui.authentication.SystemServices
import com.tezos.ui.fragment.CreateWalletFragment
import com.tezos.ui.fragment.SearchWordDialogFragment
import com.tezos.ui.fragment.VerifyCreationWalletFragment
import com.tezos.ui.utils.Storage

class CreateWalletActivity : BaseSecureActivity(), CreateWalletFragment.OnCreateWalletListener, VerifyCreationWalletFragment.OnVerifyWalletCreationListener, SearchWordDialogFragment.OnWordSelectedListener, PasswordDialog.OnPasswordDialogListener {

    private var mTitleBar: TextView? = null

    companion object {
        var CREATE_WALLET_REQUEST_CODE = 0x2600 // arbitrary int
        const val SEED_DATA_KEY = "seed_data_key"

        var MNEMONICS_STR = "mnemonics_str"

        private fun getStartIntent(context: Context, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, CreateWalletActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, CREATE_WALLET_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_wallet)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)
        initToolbar(theme)

        if (savedInstanceState == null) {
            val createWalletFragment = CreateWalletFragment.newInstance(theme)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.create_wallet_container, createWalletFragment)
                    .commit()

            //TextView mTitleBar = findViewById(R.id.barTitle);
            //TitleBar.setText(R.string.create_wallet_title_1);
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun updateTitle() {
        // Update your UI here.
        val fragment = supportFragmentManager.findFragmentById(R.id.create_wallet_container)
        if (fragment != null) {
            var titleScreen: String? = null

            if (fragment is VerifyCreationWalletFragment) {
                titleScreen = getString(R.string.create_wallet_title_2)
            } else if (fragment is CreateWalletFragment) {
                titleScreen = getString(R.string.create_wallet_title_1)
            }
            if (mTitleBar != null) {
                mTitleBar!!.text = titleScreen
            }
        }
    }

    private fun initToolbar(theme: CustomTheme) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryDarkId))
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        val window = window
        window.statusBarColor = ContextCompat.getColor(this,
                theme.colorPrimaryDarkId)
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        } catch (e: Exception) {
            Log.getStackTraceString(e)
        }

        val mCloseButton = findViewById<ImageButton>(R.id.close_button)
        mCloseButton.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId))
        mCloseButton.setOnClickListener {
            //requests stop in onDestroy.
            finish()
        }

        mTitleBar = findViewById(R.id.barTitle)
        mTitleBar!!.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
    }

    override fun onCreateWalletValidated(mnemonics: String) {
        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        val verifyCreationWalletFragment = VerifyCreationWalletFragment.newInstance(theme, mnemonics)
        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.create_wallet_container, verifyCreationWalletFragment)
                .addToBackStack(null)
                .commit()

        supportFragmentManager.addOnBackStackChangedListener { updateTitle() }
    }

    override fun onVerifyWalletCardNumberClicked(position: Int) {
        val searchWordDialogFragment = SearchWordDialogFragment.newInstance(position)
        searchWordDialogFragment.show(supportFragmentManager, SearchWordDialogFragment.TAG)
    }

    override fun onWordClicked(word: String, position: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.create_wallet_container)
        if (fragment != null && fragment is VerifyCreationWalletFragment) {
            val verifyCreationWalletFragment = fragment as VerifyCreationWalletFragment?
            verifyCreationWalletFragment!!.updateCard(word, position)
        }
    }

    override fun mnemonicsVerified(mnemonics: String) {

        val dialog = PasswordDialog.newInstance(mnemonics)
        dialog.show(supportFragmentManager, "Password")
    }

    private fun createSeedData(mnemonics: String): Storage.MnemonicsData {
        val encryptedSecret = EncryptionServices().encrypt(mnemonics)
        //logi("Original mnemonics is: $mnemonics")
        //logi("Saved mnemonics is: $encryptedSecret")
        val pkh = CryptoUtils.generatePkh(mnemonics, "")
        val pk = CryptoUtils.generatePk(mnemonics, "")
        return Storage.MnemonicsData(pkh, pk, encryptedSecret)
    }

    private fun createKeys(isFingerprintAllowed: Boolean) {
        val encryptionService = EncryptionServices()
        encryptionService.createMasterKey()

        if (SystemServices.hasMarshmallow()) {
            if (isFingerprintAllowed && systemServices.hasEnrolledFingerprints()) {
                encryptionService.createFingerprintKey()
            }
        }
    }

    override fun isFingerprintHardwareAvailable(): Boolean {
        return systemServices.isFingerprintHardwareAvailable()
    }

    override fun hasEnrolledFingerprints(): Boolean {
        return systemServices.hasEnrolledFingerprints()
    }

    override fun passwordVerified(mnemonics: String, password: String, fingerprint: Boolean)
    {
        createKeys(fingerprint)
        with(Storage(this)) {
            val encryptedPassword = EncryptionServices().encrypt(password)

            savePassword(encryptedPassword)
            saveFingerprintAllowed(fingerprint)

            val seedData = createSeedData(mnemonics)
            saveSeed(seedData)

            intent.putExtra(SEED_DATA_KEY, Storage.toBundle(seedData))
            setResult(R.id.create_wallet_succeed, intent)
            finish()
        }
    }
}
