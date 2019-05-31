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
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.authentication.PasswordDialog
import com.tezos.ui.authentication.SystemServices
import com.tezos.ui.fragment.RestoreWalletFragment
import com.tezos.ui.fragment.SearchWordDialogFragment
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.activity_restore_wallet.*

class RestoreWalletActivity : BaseSecureActivity(), RestoreWalletFragment.OnWordSelectedListener, SearchWordDialogFragment.OnWordSelectedListener, PasswordDialog.OnPasswordDialogListener {

    companion object {
        var RESTORE_WALLET_REQUEST_CODE = 0x2700 // arbitrary int
        const val SEED_DATA_KEY = "seed_data_key"

        private fun getStartIntent(context: Context, themeBundle: Bundle): Intent {
            val starter = Intent(context, RestoreWalletActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme) {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, RESTORE_WALLET_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restore_wallet)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)
        initToolbar(theme)

        if (savedInstanceState == null) {
            val restoreWalletFragment = RestoreWalletFragment.newInstance(themeBundle)
            supportFragmentManager.beginTransaction()
                    .add(R.id.restorewallet_container, restoreWalletFragment)
                    .commit()
        }
    }

    private fun initToolbar(theme: CustomTheme) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryId))
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
        mCloseButton.setOnClickListener { _ ->
            //requests stop in onDestroy.
            finish()
        }

        val mTitleBar = findViewById<TextView>(R.id.barTitle)
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
    }

    override fun onWordCardNumberClicked(position: Int) {
        val searchWordDialogFragment = SearchWordDialogFragment.newInstance(position)
        searchWordDialogFragment.show(supportFragmentManager, "searchWordDialog")
    }

    override fun mnemonicsVerified(mnemonics: String)
    {
        val dialog = PasswordDialog.newInstance(mnemonics)
        dialog.show(supportFragmentManager, "Password")
    }

    private fun createSeedData(mnemonics: String): Storage.MnemonicsData {
        val encryptedSecret = EncryptionServices().encrypt(mnemonics)

        val pkh = CryptoUtils.generatePkh(mnemonics, "")
        return Storage.MnemonicsData(pkh, encryptedSecret)
    }

    private fun createKeys(isFingerprintAllowed: Boolean) {
        val encryptionService = EncryptionServices()
        encryptionService.createMasterKey()
        encryptionService.createSpendingKey()

        if (SystemServices.hasMarshmallow()) {
            if (isFingerprintAllowed && systemServices.hasEnrolledFingerprints()) {
                encryptionService.createFingerprintKey()
            }
        }
    }

    override fun onWordClicked(word: String, position: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.restorewallet_container)
        if (fragment != null && fragment is RestoreWalletFragment) {
            val restoreWalletFragment = fragment as RestoreWalletFragment?
            restoreWalletFragment!!.updateCard(word, position)
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
            setResult(R.id.restore_wallet_succeed, intent)
            finish()
        }
    }

    override fun wordsFilled(words: String?):Boolean
    {
        with(Storage(this))
        {
            val hasMnemonics = hasMnemonics()
            if (hasMnemonics)
            {
                val pkh = getMnemonics().pkh
                if (pkh != CryptoUtils.generatePkh(words, ""))
                {
                    //showSnackBar(getString(R.string.no_mnemonics_contracts), ContextCompat.getColor(this, R.color.tz_accent), Color.YELLOW)

                    val snackbar = Snackbar.make(restorewallet_container, getString(R.string.no_mnemonics_contracts), Snackbar.LENGTH_LONG)
                    snackbar.view.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.tz_accent))
                    snackbar.setActionTextColor(Color.YELLOW)
                    snackbar.show()
                    return false
                }
            }
        }

        return true
    }
}
