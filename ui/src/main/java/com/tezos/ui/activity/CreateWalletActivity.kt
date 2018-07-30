package com.tezos.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView

import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.authentication.SystemServices
import com.tezos.ui.fragment.CreateWalletFragment
import com.tezos.ui.fragment.SearchWordDialogFragment
import com.tezos.ui.fragment.VerifyCreationWalletFragment
import com.tezos.ui.interfaces.IPasscodeHandler
import com.tezos.ui.utils.ScreenUtils
import com.tezos.ui.utils.Storage

class CreateWalletActivity : AppCompatActivity(), IPasscodeHandler, CreateWalletFragment.OnCreateWalletListener, VerifyCreationWalletFragment.OnVerifyWalletCreationListener, SearchWordDialogFragment.OnWordSelectedListener {

    private var mTitleBar: TextView? = null

    val systemServices by lazy(LazyThreadSafetyMode.NONE) { SystemServices(this) }

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

        launchPasscode()
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

    override fun launchPasscode() {
        ScreenUtils.launchPasscode(this)
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
        mCloseButton.setOnClickListener { v ->
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
        //TODO put the seed in secrets

        val seed = CryptoUtils.generateSeed(mnemonics, "")

        //TODO asks the user to put his password.
        // the password hello is not used in Marshmallow
        createKeys("hello", true)
        with(Storage(this)) {
            val encryptedPassword = EncryptionServices(applicationContext).encrypt("123", "123")

            savePassword(encryptedPassword)
            saveFingerprintAllowed(true)
        }

        //TODO put the seed in Secrets
        //Bundle keyBundle = CryptoUtils.generateKeys(mnemonics);
        //intent.putExtra(CryptoUtils.WALLET_BUNDLE_KEY, keyBundle);
        setResult(R.id.restore_wallet_succeed, null)
        finish()

        // then, ask for password:

        //val keyBundle = CryptoUtils.generateKeys(words)

        /*
        val dialog = AuthenticationDialog()
        dialog.stage = AuthenticationDialog.Stage.PASSWORD
        dialog.authenticationSuccessListener = {
            //startSecretActivity(ADD_SECRET_REQUEST_CODE, password = it)
            //
            val seed2 = CryptoUtils.generateSeed(mnemonics, "")
            val seed3 = CryptoUtils.generateSeed(mnemonics, "")

            //EncryptionServices(applicationContext).createConfirmCredentialsKey()

        }
        dialog.passwordVerificationListener = { validatePassword(it) }
        dialog.show(supportFragmentManager, "Authentication")
        */

    }

    private fun createKeys(password: String, isFingerprintAllowed: Boolean) {
        val encryptionService = EncryptionServices(applicationContext)
        encryptionService.createMasterKey(password)

        if (SystemServices.hasMarshmallow()) {
            if (isFingerprintAllowed && systemServices.hasEnrolledFingerprints()) {
                encryptionService.createFingerprintKey()
            }
            encryptionService.createConfirmCredentialsKey()
        }
    }

    private fun validatePassword(inputtedPassword: String): Boolean {
        val storage = Storage(this)
        return EncryptionServices(applicationContext).decrypt(storage.getPassword(), inputtedPassword) == inputtedPassword
    }

    companion object {
        var CREATE_WALLET_REQUEST_CODE = 0x2300 // arbitrary int

        var MNEMONICS_STR = "mnemonics_str"

        fun getStartIntent(context: Context, themeBundle: Bundle): Intent {
            val starter = Intent(context, CreateWalletActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme) {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, CREATE_WALLET_REQUEST_CODE, null)
        }
    }

}
