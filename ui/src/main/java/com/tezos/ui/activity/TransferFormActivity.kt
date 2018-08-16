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
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.fragment.TransferFormFragment
import com.tezos.ui.utils.Storage

/**
 * Created by nfillion on 29/02/16.
 */
class TransferFormActivity : BaseSecureActivity(), TransferFormFragment.OnTransferListener
{
    companion object
    {
        var TRANSFER_REQUEST_CODE = 0x2100 // arbitrary int

        private fun getStartIntent(context: Context, seedBundle: Bundle, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, TransferFormActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)
            starter.putExtra(Storage.TAG, seedBundle)

            return starter
        }

        fun start(activity: Activity, seedBundle: Bundle, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, seedBundle, theme.toBundle())
            //TODO remove this request code
            ActivityCompat.startActivityForResult(activity, starter, TransferFormActivity.TRANSFER_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_form)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        val seedDataBundle = intent.getBundleExtra(Storage.TAG)
        //Storage.MnemonicsData seedData = Storage.Companion.fromBundle(seedDataBundle);

        initToolbar(theme)

        if (savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.form_fragment_container, TransferFormFragment.newInstance(seedDataBundle, themeBundle)).commit()
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
        mCloseButton.setOnClickListener { _ ->
            //requests stop in onDestroy.
            finish()
        }

        val mTitleBar = findViewById<TextView>(R.id.barTitle)
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        val fragment = supportFragmentManager.findFragmentById(R.id.form_fragment_container)
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onTransferSucceed()
    {

    }

}