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
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.fragment.OperationsFragment

class AddressDetailsActivity : BaseSecureActivity()
{
    private var mToolbarBackButton: ImageButton? = null

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

    fun start(activity: Activity, theme: CustomTheme)
    {
        var starter = getStartIntent(activity, theme)
        ActivityCompat.startActivityForResult(activity, starter, -1, null)
    }

    private fun getStartIntent(context: Context, theme:CustomTheme): Intent
    {
        val starter = Intent(context, AddressDetailsActivity::class.java)
        starter.putExtra(CustomTheme.TAG, theme.toBundle())

        return starter
    }

    private fun switchToOperations(tezosTheme:CustomTheme, address: Address)
    {
        val operationsFragment = OperationsFragment.newInstance(tezosTheme, address)
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_fragments_container, operationsFragment)
                .commit()
    }
}