package com.tezcore.cortez.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.ImageButton
import com.tezcore.cortez.fragments.SettingsFragment
import com.tezos.android.R
import com.tezos.core.models.CustomTheme
import com.tezos.ui.activity.BaseSecureActivity
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.extentions.openSecuritySettings
import com.tezos.ui.utils.Storage

/**
 * Created by nfillion on 3/6/18.
 */

class SettingsActivity : BaseSecureActivity(), SettingsFragment.OnFingerprintOptionSelectedListener, SettingsFragment.OnLogOutClickedListener, SettingsFragment.OnSystemInformationsCallback
{

    companion object
    {
        private val TAG_SETTINGS = "SettingsTag"

        var SETTINGS_REQUEST_CODE = 0x2500 // arbitrary int

        private fun getStartIntent(context: Context, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, SettingsActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, SETTINGS_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null)
        {
            val settingsFragment = SettingsFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.settings_container, settingsFragment, TAG_SETTINGS)
                    .commit()
        }

        initActionBar()
    }

    override fun onStart() {
        super.onStart()
        if (!systemServices.isDeviceSecure()) {
            deviceSecurityAlert = systemServices.showDeviceSecurityAlert()
        }
    }

    override fun onStop() {
        super.onStop()
        deviceSecurityAlert?.dismiss()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        /*
        if (requestCode == PasscodeActivity.ASK_NEW_CODE_RESULT)
        {
            if (resultCode == R.id.passcode_succeed)
            {
                // success
                String code = data.getStringExtra(PasscodeActivity.BUNDLE_CODE);

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(PasscodeActivity.PASSCODE_KEY, code);
                editor.apply();
            }
            //else if (resultCode == R.id.passcode_failed) {// should not happen actually}
            else
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.settings_container);
                if (fragment != null)
                {
                    SettingsFragment settingsFragment = (SettingsFragment)fragment;
                    settingsFragment.notifyChanged();
                }
                // user just canceled
                // uncheck the password.
            }
        }
        */
    }

    private fun initActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        } catch (e: Exception) {
            Log.getStackTraceString(e)
        }

        supportActionBar!!.setDisplayShowTitleEnabled(false)

        val mCloseButton = findViewById<ImageButton>(R.id.close_button)
        mCloseButton.setColorFilter(ContextCompat.getColor(this, R.color.theme_tezos_text))
        mCloseButton.setOnClickListener {
            //requests stop in onDestroy.
            finish()
        }
    }

    override fun onFingerprintOptionClicked(isOptionChecked:Boolean)
    {
        if (!systemServices.hasEnrolledFingerprints())
        {
            //item.isChecked = false
            // TODO put the checkbox to false

            Snackbar.make(findViewById(android.R.id.content), R.string.sign_up_snack_message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_up_snack_action, { openSecuritySettings() })
                    .show()
        }
        else
        {
            Storage(baseContext).saveFingerprintAllowed(isOptionChecked)
            if (!isOptionChecked)
            {
                EncryptionServices(this).removeFingerprintKey()
            }
        }
    }

    override fun onLogOutClicked()
    {
        val encryptionServices = EncryptionServices(applicationContext)
        encryptionServices.removeMasterKey()
        encryptionServices.removeFingerprintKey()
        encryptionServices.removeConfirmCredentialsKey()

        Storage(baseContext).clear()

        setResult(R.id.logout_succeed, null)
        finish()
    }

    override fun isFingerprintHardwareAvailable(): Boolean
    {
        return (systemServices.isFingerprintHardwareAvailable())
    }

    override fun isFingerprintAllowed(): Boolean
    {
        return (Storage(applicationContext).isFingerprintAllowed())
    }

    override fun isDeviceSecure(): Boolean
    {
        return systemServices.isDeviceSecure()
    }
}