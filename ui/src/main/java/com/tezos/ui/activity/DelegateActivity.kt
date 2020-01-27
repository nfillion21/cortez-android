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
import android.os.Build
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.widget.Toolbar
import android.view.ViewGroup
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.ui.R
import com.tezos.ui.fragment.*
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.activity_delegate.*

class DelegateActivity : BaseSecureActivity(), HomeFragment.HomeListener, DelegateFragment.OnAddedDelegationListener, ScriptFragment.OnUpdateScriptListener, SendCentsFragment.OnSendCentsInteractionListener
{

    private val mTezosTheme: CustomTheme = CustomTheme(
            R.color.colorPrimaryDark,
            R.color.colorPrimaryDark,
            R.color.colorTitleText)

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    companion object
    {
        private val TAG_DELEGATE = "DelegateTag"
        private const val TAG_PKH = "PkhTag"
        private const val POS_KEY = "PosKey"

        var DELEGATE_REQUEST_CODE = 0x2900 // arbitrary int

        private fun getStartIntent(context: Context, pkh: String, position: Int, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, DelegateActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)
            starter.putExtra(TAG_PKH, pkh)
            starter.putExtra(POS_KEY, position)

            return starter
        }

        fun start(activity: Activity, pkh:String, position: Int, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, pkh, position, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, DELEGATE_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delegate)

        setSupportActionBar(toolbar)
        initActionBar(mTezosTheme)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        container.addOnPageChangeListener(object : ViewPager.OnPageChangeListener
        {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)
            {}

            override fun onPageSelected(position: Int)
            {
                val isPasswordSaved = Storage(this@DelegateActivity).isPasswordSaved()
                when (position)
                {
                    0 ->
                    {
                        if (isPasswordSaved)
                        {
                            fabTransfer.show()
                            fabSharing.hide()
                        }
                        else
                        {
                            fabTransfer.hide()
                            fabSharing.hide()
                        }
                    }

                    1 ->
                    {
                        if (isPasswordSaved)
                        {
                            fabTransfer.hide()
                            fabSharing.show()
                        }
                        else
                        {
                            fabTransfer.hide()
                            fabSharing.hide()
                        }
                    }

                    2 ->
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                    }

                    3 ->
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                    }

                    else ->
                    {
                        //no-op
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int)
            {}
        })

        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        fabTransfer.setOnClickListener {

            val hasMnemonics = Storage(this).hasMnemonics()
            if (hasMnemonics)
            {
                val pkh = intent.getStringExtra(TAG_PKH)
                TransferFormActivity.start(this, pkh, null, mTezosTheme)
            }
            else
            {
                //the fab is invisible
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        //.setAction("Action", null).show()
            }
        }

        fabSharing.setOnClickListener { view ->

            val isPasswordSaved = Storage(this).isPasswordSaved()
            if (isPasswordSaved)
            {
                val pkh = intent.getStringExtra(TAG_PKH)

                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, pkh)
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
            }
            else
            {
                //TODO this snackbar should be invisible
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
            }
        }

        initActionBar(mTezosTheme)
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm)
    {
        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any)
        {
            super.setPrimaryItem(container, position, `object`)
            val isPasswordSaved = Storage(this@DelegateActivity).isPasswordSaved()

            when (position)
            {
                0 ->
                {
                    if (isPasswordSaved)
                    {
                        fabTransfer.show()
                        fabSharing.hide()
                    }
                    else
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                    }
                }

                1 ->
                {
                    if (isPasswordSaved)
                    {
                        fabTransfer.hide()
                        fabSharing.show()
                    }
                    else
                    {
                        fabTransfer.hide()
                        fabSharing.hide()
                    }
                }

                2 ->
                {
                    fabTransfer.hide()
                    fabSharing.hide()
                }

                3 ->
                {
                    fabTransfer.hide()
                    fabSharing.hide()
                }

                else ->
                {
                    //no-op
                }
            }
        }

        override fun getItem(position: Int): Fragment
        {
            when (position)
            {
                0 ->
                {
                    val pkh = intent.getStringExtra(TAG_PKH)
                    return HomeDelegateFragment.newInstance(mTezosTheme, pkh)
                }
                1 ->
                {
                    val pkh = intent.getStringExtra(TAG_PKH)
                    return SharingAddressFragment.newInstance(mTezosTheme, pkh)
                }

                2 ->
                {
                    val pkh = intent.getStringExtra(TAG_PKH)
                    return DelegateFragment.newInstance(mTezosTheme, pkh)
                }

                3 ->
                {
                    val isPasswordSaved = Storage(this@DelegateActivity).isPasswordSaved()

                    return if (isPasswordSaved)
                    {
                        val pkh = intent.getStringExtra(TAG_PKH)
                        ScriptFragment.newInstance(mTezosTheme, pkh)
                    }
                    else
                    {
                        ScriptFragment.newInstance(mTezosTheme, null)
                    }
                }
            }

            return HomeDelegateFragment.newInstance(mTezosTheme, null)
        }

        override fun getCount(): Int
        {
            // Show 4 total pages.
            return 4
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode)
        {
            CreateWalletActivity.CREATE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.create_wallet_succeed)
                {
                    if (data != null && data.hasExtra(CreateWalletActivity.SEED_DATA_KEY))
                    {
                        showSnackBar(getString(R.string.wallet_successfully_created), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }
                }
            }

            RestoreWalletActivity.RESTORE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.restore_wallet_succeed)
                {
                    if (data != null && data.hasExtra(RestoreWalletActivity.SEED_DATA_KEY))
                    {
                        showSnackBar(getString(R.string.wallet_successfully_restored), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    }
                }
            }

            TransferFormActivity.TRANSFER_REQUEST_CODE ->
            {
                if (resultCode == R.id.transfer_succeed)
                {
                    showSnackBar(getString(R.string.transfer_succeed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                    //TODO I need to refresh balance.
                }
            }

            AddAddressActivity.ADD_ADDRESS_REQUEST_CODE ->
            {
                if (resultCode == R.id.add_address_succeed)
                {
                    showSnackBar(getString(R.string.address_successfully_added), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
                }
            }

            else ->
            {
            }
        }
    }

    override fun showSnackBar(text:String, color:Int, textColor:Int)
    {
        val snackbar = Snackbar.make(fabTransfer, text, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(color)
        snackbar.setActionTextColor(textColor)
        snackbar.show()
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

        val position = intent.getIntExtra(POS_KEY, 0)
        toolbar.title = "Contract #$position"

        setSupportActionBar(toolbar)
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

    override fun onTransferSucceed()
    {
        showSnackBar(getString(R.string.transfer_succeed), ContextCompat.getColor(this, android.R.color.holo_green_light), ContextCompat.getColor(this, R.color.tz_light))
    }

    override fun finish(res: Int)
    {
        setResult(res)
        finish()
    }
}
