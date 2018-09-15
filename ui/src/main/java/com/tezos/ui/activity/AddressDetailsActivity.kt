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
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.fragment.HomeFragment

class AddressDetailsActivity : BaseSecureActivity(), HomeFragment.HomeListener
{

    private var mToolbarBackButton: ImageButton? = null

    companion object
    {
        private val ADDRESS_DETAILS_TAG = "AdressDetailsTag"

        var ADDRESS_DETAILS_TAG_CODE = 0x3200 // arbitrary int

        fun start(activity: Activity, theme: CustomTheme, address: Address)
        {
            var starter = getStartIntent(activity, theme, address)
            ActivityCompat.startActivityForResult(activity, starter, -1, null)
        }

        private fun getStartIntent(context: Context, theme:CustomTheme, pkh:Address): Intent
        {
            val starter = Intent(context, AddressDetailsActivity::class.java)
            starter.putExtra(CustomTheme.TAG, theme.toBundle())
            starter.putExtra(Address.TAG, pkh.toBundle())

            return starter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_details)

        val tezosThemeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val tezosTheme = CustomTheme.fromBundle(tezosThemeBundle)

        initBackButton(tezosTheme)

        val addressBundle = intent.getBundleExtra(Address.TAG)
        val address = Address.fromBundle(addressBundle)

        initToolbar(tezosTheme, address)

        if (savedInstanceState == null)
        {
            switchToOperations(tezosTheme, address)
        }
    }

    private fun initToolbar(theme: CustomTheme, address:Address)
    {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryId))

        val window = window
        window.statusBarColor = ContextCompat.getColor(this,
                theme.colorPrimaryDarkId)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val mTitleBar = findViewById<TextView>(R.id.barTitle)
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))

        mTitleBar.text = address.description
    }

    private fun initBackButton(theme:CustomTheme)
    {
        mToolbarBackButton = findViewById<ImageButton>(R.id.back)
        mToolbarBackButton?.setColorFilter(ContextCompat.getColor(this,
                theme.textColorPrimaryId))
        mToolbarBackButton?.setOnClickListener(View.OnClickListener {
            finish()
        })
    }

    private fun switchToOperations(tezosTheme:CustomTheme, address: Address)
    {
        val operationsFragment = HomeFragment.newInstance(tezosTheme, address)
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragments_container, operationsFragment)
                .commit()
    }

    override fun showSnackBar(resText:String, color:Int)
    {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), resText, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                color)))
        snackbar.show()
    }
}