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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.fragment.ExportKeysDialogFragment
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.activity_key_management.*


class KeyManagementActivity : BaseSecureActivity()
{
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    companion object
    {
        private val KEY_MANAGEMENT_TAG = "KeyManagementTag"

        var KEY_MANAGEMENT_CODE = 0x3300 // arbitrary int

        fun start(activity: Activity, theme: CustomTheme)
        {
            var starter = getStartIntent(activity, theme)
            ActivityCompat.startActivityForResult(activity, starter, KEY_MANAGEMENT_CODE, null)
        }

        private fun getStartIntent(context: Context, theme:CustomTheme): Intent
        {
            val starter = Intent(context, KeyManagementActivity::class.java)
            starter.putExtra(CustomTheme.TAG, theme.toBundle())

            return starter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_management)

        val tezosTheme = CustomTheme(
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark,
                R.color.colorTitleText)

        initActionBar(tezosTheme)

        exit_button_layout.setOnClickListener {

            val dialogClickListener = { dialog:DialogInterface, which:Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        dialog.dismiss()

                        onLogOutClicked()
                    }

                    DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                }
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.alert_exit_account)
                    .setMessage(R.string.alert_exit_acccount_body)
                    .setNegativeButton(android.R.string.cancel, dialogClickListener)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setCancelable(false)
                    .show()
        }

        export_24_words_button.setOnClickListener {


            onExportWordsClick()
        }

        remove_24_words_button.setOnClickListener {

            val dialogClickListener = { dialog:DialogInterface, which:Int ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        dialog.dismiss()

                        onMasterKeyRemovedSeed()
                    }

                    DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                }
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.alert_remove_master_key)
                    .setMessage(R.string.alert_remove_master_key_body)
                    .setNegativeButton(android.R.string.cancel, dialogClickListener)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setCancelable(false)
                    .show()
        }

        restore_24_words_button.setOnClickListener {
            val theme = CustomTheme(
                    R.color.theme_tezos_primary,
                    R.color.theme_tezos_primary_dark,
                    R.color.theme_tezos_text)
            RestoreWalletActivity.start(this, theme)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode)
        {
            RestoreWalletActivity.RESTORE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.restore_wallet_succeed)
                {
                    if (data != null && data.hasExtra(RestoreWalletActivity.SEED_DATA_KEY))
                    {
                        setResult(R.id.restore_wallet_succeed, intent)
                        finish()
                        //showSnackBar(getString(R.string.wallet_successfully_restored), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        val hasMnemonics = Storage(this).hasMnemonics()
        validateExitButton(hasMnemonics)

        if (hasMnemonics)
        {
            val hasMasterkey = Storage(this).getMnemonics().mnemonics.isNotEmpty()
            with_mnemonics_layout.visibility = if (hasMasterkey) View.VISIBLE else View.GONE
            without_mnemonics_layout.visibility = if (hasMasterkey) View.GONE else View.VISIBLE

            mnemonics_info_textview.visibility = View.GONE
            export_24_words_button.visibility = if (hasMasterkey) View.VISIBLE else View.GONE
        }
        else
        {
            with_mnemonics_layout.visibility = View.GONE
            without_mnemonics_layout.visibility = View.GONE

            mnemonics_info_textview.visibility = View.VISIBLE
            export_24_words_button.visibility = View.GONE
        }
    }

    private fun onLogOutClicked()
    {
        val encryptionServices = EncryptionServices()
        encryptionServices.removeMasterKey()
        encryptionServices.removeFingerprintKey()
        encryptionServices.removeConfirmCredentialsKey()
        encryptionServices.removeSpendingKey()

        Storage(baseContext).clear()

        setResult(R.id.logout_succeed, null)
        finish()
    }

    private fun onExportWordsClick()
    {
        val dialog = AuthenticationDialog()
        if (isFingerprintAllowed() && hasEnrolledFingerprints())
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices().prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = {
                validateKeyAuthentication(it)
            }
            if (dialog.cryptoObjectToAuthenticateWith == null)
            {
                dialog.stage = AuthenticationDialog.Stage.NEW_FINGERPRINT_ENROLLED
            }
            else
            {
                dialog.stage = AuthenticationDialog.Stage.FINGERPRINT
            }
        }
        else
        {
            dialog.stage = AuthenticationDialog.Stage.PASSWORD
        }
        dialog.authenticationSuccessListener = {
            displayWords()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(supportFragmentManager, "Authentication")
    }

    private fun validatePassword(inputtedPassword: String): Boolean
    {
        val storage = Storage(this)
        return EncryptionServices().decrypt(storage.getPassword()) == inputtedPassword
    }

    private fun displayWords()
    {
        val theme = CustomTheme(
                R.color.colorPrimaryDark,
                R.color.colorPrimaryDark,
                R.color.colorTitleText)

        val mnemonicsData = Storage(this).getMnemonics()
        val mnemonics = EncryptionServices().decrypt(mnemonicsData.mnemonics)

        val exportKeysDialogFragment = ExportKeysDialogFragment.newInstance(mnemonics = mnemonics,theme = theme)
        exportKeysDialogFragment.show(supportFragmentManager, ExportKeysDialogFragment.TAG)
    }

    private fun onMasterKeyRemovedSeed()
    {
        //Storage(baseContext).hasSeed()
        //Storage(baseContext).removeSeed()
        //val encryptionServices = EncryptionServices()
        //encryptionServices.removeMasterKey()

        Storage(baseContext).removeSeed()

        setResult(R.id.master_key_removed, null)
        finish()
    }

    private fun validateExitButton(validate: Boolean)
    {
        if (validate)
        {
            val theme = CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText)

            exit_button.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
            exit_button_layout.isEnabled = true
            exit_button_layout.background = (makeSelector(theme))

            val drawables = exit_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.textColorPrimaryId))

        } else {

            exit_button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            exit_button_layout.isEnabled = false
            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            exit_button_layout.background = makeSelector(greyTheme)

            val drawables = exit_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables[0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(this, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(this, theme.colorPrimaryId)))
        return res
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

        val mCloseButton = findViewById<ImageButton>(R.id.close_button)
        mCloseButton.setColorFilter(ContextCompat.getColor(this, R.color.theme_tezos_text))
        mCloseButton.setOnClickListener {
            finish()
        }
    }


    fun isFingerprintAllowed():Boolean
    {
        return storage.isFingerprintAllowed()
    }

    fun hasEnrolledFingerprints():Boolean
    {
        return systemServices.hasEnrolledFingerprints()
    }

    fun saveFingerprintAllowed(useInFuture:Boolean)
    {
        storage.saveFingerprintAllowed(useInFuture)
    }

    private fun onFingerprintInvalidation(useInFuture: Boolean)
    {
        saveFingerprintAllowed(useInFuture)
        if (useInFuture)
        {
            EncryptionServices().createFingerprintKey()
        }
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices().validateFingerprintAuthentication(cryptoObject))
        {
            displayWords()
        }
        else
        {
            onExportWordsClick()
        }
    }

    private fun sendMail()
    {
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:cortez@tezcore.com")

        try {
            startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            //TODO: Handle case where no email app is available
        }
    }
}
