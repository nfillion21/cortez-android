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

package com.tezcore.cortez.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.ImageButton
import com.tezos.ui.fragment.SettingsFragment
import com.tezos.android.R
import com.tezos.core.models.CustomTheme
import com.tezos.ui.activity.BaseSecureActivity
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.extentions.openSecuritySettings
import com.tezos.ui.utils.Storage

/**
 * Created by nfillion on 3/6/18.
 */

class SettingsActivity : BaseSecureActivity(), SettingsFragment.OnFingerprintOptionSelectedListener, SettingsFragment.OnSystemInformationsCallback
{
    private var deviceSecurityAlert: AlertDialog? = null

    companion object
    {
        private const val TAG_SETTINGS = "SettingsTag"

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
                    .setAction(R.string.sign_up_snack_action) { openSecuritySettings() }
                    .setActionTextColor(Color.YELLOW)
                    .show()
        }
        else
        {
            Storage(baseContext).saveFingerprintAllowed(isOptionChecked)
            if (!isOptionChecked)
            {
                EncryptionServices().removeFingerprintKey()
            }
        }
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