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
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.ui.R
import com.tezos.ui.adapter.OperationRecyclerViewAdapter
import com.tezos.ui.fragment.OperationDetailsDialogFragment

class OperationsActivity : BaseSecureActivity(), OperationRecyclerViewAdapter.OnItemClickListener
{
    private var mRecyclerView: RecyclerView? = null

    companion object
    {
        const val OPERATIONS_KEY = "operations_key"

        private fun getStartIntent(context: Context, list: ArrayList<Bundle>, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, OperationsActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)
            starter.putParcelableArrayListExtra(OPERATIONS_KEY, list)

            return starter
        }

        fun start(activity: Activity, list: ArrayList<Bundle>, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, list, theme.toBundle())
            ActivityCompat.startActivity(activity, starter, null)
        }
    }

    private fun bundlesToItems( bundles:ArrayList<Bundle>?): ArrayList<Operation>?
    {
        if (bundles != null)
        {
            var items = ArrayList<Operation>(bundles.size)
            if (!bundles.isEmpty())
            {
                bundles.forEach {
                    val op = Operation.fromBundle(it)
                    items.add(op)
                }
            }
            return items
        }

        return ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_operations)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)
        initToolbar(theme)

        var recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val elements = intent.getParcelableArrayListExtra<Bundle>(OPERATIONS_KEY)
        val adapter = OperationRecyclerViewAdapter(bundlesToItems(elements))

        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter

        mRecyclerView = recyclerView

        if (savedInstanceState == null)
        {
            /*
            val restoreWalletFragment = OperationsFragment.newInstance(themeBundle)
            supportFragmentManager.beginTransaction()
                    .add(R.id.restorewallet_container, restoreWalletFragment)
                    .commit()
            */
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

    override fun onOperationSelected(view: View?, operation: Operation?)
    {
        val operationDetailsFragment = OperationDetailsDialogFragment.newInstance(operation)
        operationDetailsFragment.show(supportFragmentManager, OperationDetailsDialogFragment.TAG)
    }
}
