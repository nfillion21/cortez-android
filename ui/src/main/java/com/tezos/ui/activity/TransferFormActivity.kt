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
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.android.volley.VolleyError
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.fragment.TransferFormFragment
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.activity_payment_form.*

/**
 * Created by nfillion on 29/02/16.
 */
class TransferFormActivity : BaseSecureActivity(), TransferFormFragment.OnTransferListener
{
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    companion object
    {
        var TRANSFER_REQUEST_CODE = 0x2100 // arbitrary int

        const val DST_ADDRESS_KEY = "dst_address_key"

        private fun getStartIntent(context: Context, srcPkh: String, address: Address?, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, TransferFormActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)
            starter.putExtra(Address.TAG, srcPkh)

            if (address != null)
            {
                starter.putExtra(DST_ADDRESS_KEY, address.toBundle())
            }

            return starter
        }

        fun start(activity: Activity, srcPkh: String, address: Address?, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, srcPkh, address, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, TRANSFER_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_form)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        val srcAddressBundle = intent.getStringExtra(Address.TAG)

        val dstAddressBundle = intent.getBundleExtra(DST_ADDRESS_KEY)

        initToolbar(theme)

        if (savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.form_fragment_container, TransferFormFragment.newInstance(srcAddressBundle, dstAddressBundle, themeBundle)).commit()
        }
    }

    private fun initToolbar(theme: CustomTheme)
    {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryId))
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        val window = window
        window.statusBarColor = ContextCompat.getColor(this,
                theme.colorPrimaryDarkId)
        try
        {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        }
        catch (e: Exception)
        {
            Log.getStackTraceString(e)
        }

        val mCloseButton = findViewById<ImageButton>(R.id.close_button)
        mCloseButton.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId))
        mCloseButton.setOnClickListener {
            //requests stop in onDestroy.
            finish()
        }

        nav_progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId), PorterDuff.Mode.SRC_IN)

        val mTitleBar = findViewById<TextView>(R.id.barTitle)
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
    }

    /*
    private fun showSnackBar(error:VolleyError?)
    {
        var error: String? = if (error != null)
        {
            error.toString()
        }
        else
        {
            getString(R.string.generic_error)
        }

        val snackbar = Snackbar.make(findViewById(R.id.content), error.toString(), Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                android.R.color.holo_red_light)))
        snackbar.show()
    }
    */

    private fun showSnackBar(res:String, color:Int, textColor:Int?)
    {
        val snackbar = Snackbar.make(findViewById(R.id.content), res, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(color)
        if (textColor != null)
        {
            snackbar.setActionTextColor(textColor)
        }
        snackbar.show()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        val fragment = supportFragmentManager.findFragmentById(R.id.form_fragment_container)
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onTransferSucceed()
    {
        //TODO transfer data to display?
        setResult(R.id.transfer_succeed, null)
        finish()
    }

    override fun onTransferFailed(error: VolleyError?)
    {
        var error: String = error?.toString() ?: getString(R.string.generic_error)

        showSnackBar(error, ContextCompat.getColor(this,
                android.R.color.holo_red_light), null)
    }

    override fun noMnemonicsAvailable()
    {
        showSnackBar("no mnemonics available", ContextCompat.getColor(this, R.color.tz_accent), Color.YELLOW)
    }

    override fun isFingerprintAllowed():Boolean
    {
        return storage.isFingerprintAllowed()
    }

    override fun hasEnrolledFingerprints():Boolean
    {
        return systemServices.hasEnrolledFingerprints()
    }

    override fun saveFingerprintAllowed(useInFuture:Boolean)
    {
        storage.saveFingerprintAllowed(useInFuture)
    }

    override fun onTransferLoading(loading:Boolean)
    {
        if (loading)
        {
            nav_progress.visibility = View.VISIBLE
        }
        else
        {
            nav_progress.visibility = View.INVISIBLE
        }
    }
}