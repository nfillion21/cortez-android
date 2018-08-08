package com.tezos.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.tezos.ui.authentication.SystemServices
import com.tezos.ui.fragment.RestoreWalletFragment
import com.tezos.ui.fragment.SearchWordDialogFragment
import com.tezos.ui.utils.ScreenUtils
import com.tezos.ui.utils.Storage

class RestoreWalletActivity : AppCompatActivity(), RestoreWalletFragment.OnWordSelectedListener, SearchWordDialogFragment.OnWordSelectedListener {

    val systemServices by lazy(LazyThreadSafetyMode.NONE) { SystemServices(this) }

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

    override fun onResume() {
        super.onResume()
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

    override fun mnemonicsVerified(mnemonics: String) {
        /*
        //TODO put the seed in Secrets
        createKeys("hello", true)
        with(Storage(this)) {
        val encryptedPassword = EncryptionServices(applicationContext).encrypt("123", "123")

        savePassword(encryptedPassword)
        saveFingerprintAllowed(true)
    }
        byte[] seed = CryptoUtils.generateSeed(mnemonics, "");
        //Bundle keyBundle = CryptoUtils.generateKeys(mnemonics);
        //intent.putExtra(CryptoUtils.WALLET_BUNDLE_KEY, keyBundle);
        setResult(R.id.restore_wallet_succeed, null);
        finish();
        */
        val password = "123"

        //TODO asks the user to put his password.
        // the password hello is not used in Marshmallow
        createKeys("hello", true)
        with(Storage(this)) {
            val encryptedPassword = EncryptionServices(applicationContext).encrypt("123", "123")

            savePassword(encryptedPassword)
            saveFingerprintAllowed(true)

            val seedData = createSeedData(mnemonics, password)
            saveSeed(seedData)

            //Bundle keyBundle = CryptoUtils.generateKeys(mnemonics);
            //intent.putExtra(CryptoUtils.WALLET_BUNDLE_KEY, keyBundle);

            intent.putExtra(SEED_DATA_KEY, Storage.toBundle(seedData))
            setResult(R.id.restore_wallet_succeed, intent)
            finish()
        }
    }

    private fun createSeedData(mnemonics: String, password: String): Storage.SeedData {
        val encryptedSecret = EncryptionServices(applicationContext).encrypt(mnemonics, password)

        val pkh = CryptoUtils.generatePkh(mnemonics, "")
        return Storage.SeedData(pkh, encryptedSecret)
    }

    private fun createKeys(password: String, isFingerprintAllowed: Boolean) {
        val encryptionService = EncryptionServices(applicationContext)
        encryptionService.createMasterKey(password)

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

    companion object {
        var RESTORE_WALLET_REQUEST_CODE = 0x2400 // arbitrary int
        const val SEED_DATA_KEY = "seed_data_key"

        fun getStartIntent(context: Context, themeBundle: Bundle): Intent {
            val starter = Intent(context, RestoreWalletActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme) {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, RESTORE_WALLET_REQUEST_CODE, null)
        }
    }
}
