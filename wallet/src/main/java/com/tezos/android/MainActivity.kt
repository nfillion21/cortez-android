package com.tezos.android

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.tezos.android.activities.AboutActivity
import com.tezos.android.activities.SettingsActivity
import com.tezos.android.adapters.OperationRecyclerViewAdapter
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.core.utils.AddressesDatabase
import com.tezos.core.utils.ApiLevelHelper
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.activity.*
import com.tezos.ui.interfaces.IPasscodeHandler
import com.tezos.ui.utils.ScreenUtils
import com.tezos.ui.utils.VolleySingleton
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, IPasscodeHandler
{

    private val OPERATIONS_ARRAYLIST_KEY = "operationsList"
    private val GET_OPERATIONS_LOADING_KEY = "getOperationsLoading"

    private val LOAD_OPERATIONS_TAG = "downloadHistory"

    private val pkHashKey = "pkhash_key"
    private var mPublicKeyHash: String? = null

    private var mRestoreWalletButton: Button? = null
    private var mCreateWalletButton: Button? = null
    private var mTezosLogo: ImageView? = null

    private var mProgressBar: ProgressBar? = null

    private var animating = false

    private var mSwipeRefreshLayout:SwipeRefreshLayout? = null

    private var mRecyclerView:RecyclerView? = null
    private var mRecyclerViewItems:ArrayList<Operation>? = null

    private var mGetHistoryLoading:Boolean = false

    private var mEmptyLoadingTextView:TextView? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // first get the theme
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        // initialize the buttons

        mRestoreWalletButton = findViewById(R.id.restoreWalletButton)
        mRestoreWalletButton!!.setOnClickListener {
            RestoreWalletActivity.start(this, tezosTheme)
        }

        mCreateWalletButton = findViewById(R.id.createWalletButton)
        mCreateWalletButton!!.setOnClickListener {
            CreateWalletActivity.start(this, tezosTheme)
        }

        mTezosLogo = findViewById(R.id.ic_logo)

        initActionBar(tezosTheme)

        mEmptyLoadingTextView = findViewById(R.id.empty_loading_textview)

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            startGetRequestLoadOperations()
        }

        if (savedInstanceState != null)
        {
            mPublicKeyHash = savedInstanceState.getString(pkHashKey, null)

            var messagesBundle = savedInstanceState.getParcelableArrayList<Bundle>(OPERATIONS_ARRAYLIST_KEY)
            mRecyclerViewItems = bundlesToItems(messagesBundle)

            mGetHistoryLoading = savedInstanceState.getBoolean(GET_OPERATIONS_LOADING_KEY)


            if (mGetHistoryLoading)
            {
                // it does back to loading while we got elements on the list
                // put the elements before loading.
                // looks ok

                refreshRecyclerViewAndText()
                startInitialLoading()
            }
            else
            {
                onOperationsLoadComplete()
            }
        }
        else
        {
            mRecyclerViewItems = ArrayList()

            startInitialLoading()
        }

        var recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager as RecyclerView.LayoutManager?

        val adapter = OperationRecyclerViewAdapter(mRecyclerViewItems)

        recyclerView.adapter = adapter

        mRecyclerView = recyclerView
    }

    private fun onOperationsLoadComplete()
    {
        mGetHistoryLoading = false

        mProgressBar?.visibility = View.GONE

        mSwipeRefreshLayout?.isEnabled = true
        mSwipeRefreshLayout?.isRefreshing = false

        refreshRecyclerViewAndText()
    }

    private fun refreshRecyclerViewAndText()
    {
        if (mRecyclerViewItems?.isEmpty()!!)
        {
            mRecyclerView?.visibility = View.GONE

            mEmptyLoadingTextView?.visibility = View.VISIBLE
            mEmptyLoadingTextView?.setText(R.string.empty_list_operations)
        }
        else
        {
            mRecyclerView?.visibility = View.VISIBLE
            mEmptyLoadingTextView?.visibility = View.GONE
            mEmptyLoadingTextView?.text = null
        }
    }

    private fun startInitialLoading()
    {
        mSwipeRefreshLayout?.isEnabled = false

        startGetRequestLoadOperations()
    }

    override fun onResume()
    {
        super.onResume()
        launchPasscode()

        mRecyclerView?.adapter?.notifyDataSetChanged()

        //handleVisibility()
    }

    private fun handleVisibility()
    {
        if (!animating)
        {
            val isPrivateKeyEnabled = AddressesDatabase.getInstance().isPrivateKeyOn(this)
            setMenuItemEnabled(isPrivateKeyEnabled)

            if (isPrivateKeyEnabled)
            {
                mTezosLogo!!.visibility = View.GONE
                mCreateWalletButton!!.visibility = View.GONE
                mRestoreWalletButton!!.visibility = View.GONE
            }
            else
            {
                mTezosLogo!!.alpha = 1.0f
                mTezosLogo!!.visibility = View.VISIBLE
                mCreateWalletButton!!.alpha = 1.0f
                mCreateWalletButton!!.visibility = View.VISIBLE
                mRestoreWalletButton!!.alpha = 1.0f
                mRestoreWalletButton!!.visibility = View.VISIBLE
            }
        }

        animating = false
    }

    private fun animateLogo()
    {
        animating = true

        val animatorCreateButton = ObjectAnimator.ofFloat(mCreateWalletButton, View.ALPHA, 0.0f)
        animatorCreateButton.duration = 1000
        animatorCreateButton.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mCreateWalletButton!!.visibility = View.GONE
            }
        })
        animatorCreateButton.start()

        val animatorRestoreButton = ObjectAnimator.ofFloat(mRestoreWalletButton, View.ALPHA, 0.0f)
        animatorRestoreButton.duration = 1000
        animatorRestoreButton.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mTezosLogo!!.visibility = View.GONE
            }
        })
        animatorRestoreButton.start()

        val animator = ObjectAnimator.ofFloat(mTezosLogo, View.ALPHA, 0.0f)
        animator.duration = 1000
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mTezosLogo!!.visibility = View.GONE
                animating = false
            }
        })
        animator.start()
    }

    override fun launchPasscode()
    {
        ScreenUtils.launchPasscode(this)
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
                    if (data != null && data.hasExtra(CryptoUtils.WALLET_BUNDLE_KEY))
                    {
                        val walletBundle = data.getBundleExtra(CryptoUtils.WALLET_BUNDLE_KEY)
                        mPublicKeyHash = walletBundle.getString(CryptoUtils.PUBLIC_KEY_HASH_KEY)

                        val snackbar = Snackbar.make(findViewById<Button>(R.id.coordinator), R.string.wallet_successfully_created, Snackbar.LENGTH_LONG)
                        val snackBarView = snackbar.getView()
                        snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                                R.color.tz_green)))
                        snackbar.show()

                        AddressesDatabase.getInstance().setPrivateKeyOn(this, true)
                        setMenuItemEnabled(true)
                        animateLogo()
                    }
                    else
                    {
                        //Log.v("ProjectDetails", "data is null")
                    }
                }
            }

            RestoreWalletActivity.RESTORE_WALLET_REQUEST_CODE ->
            {
                if (resultCode == R.id.restore_wallet_succeed)
                {
                    if (data != null && data.hasExtra(CryptoUtils.WALLET_BUNDLE_KEY))
                    {
                        val walletBundle = data.getBundleExtra(CryptoUtils.WALLET_BUNDLE_KEY)
                        mPublicKeyHash = walletBundle.getString(CryptoUtils.PUBLIC_KEY_HASH_KEY)

                        // TODO offset it
                        val snackbar = Snackbar.make(findViewById(R.id.coordinator), R.string.wallet_successfully_restored, Snackbar.LENGTH_LONG)
                        val snackBarView = snackbar.getView()
                        snackBarView.setBackgroundColor((ContextCompat.getColor(this,
                                android.R.color.holo_green_light)))
                        snackbar.show()

                        AddressesDatabase.getInstance().setPrivateKeyOn(this, true)
                        setMenuItemEnabled(true)

                        animateLogo()
                    }
                    else
                    {
                        //Log.v("ProjectDetails", "data is null")
                    }
                }
            }

            SettingsActivity.SETTINGS_REQUEST_CODE ->
            {
                if (resultCode == R.id.logout_succeed)
                {
                    //nothing special to do.
                }
            }

            else ->
            {
                //handleVisibility()
            }
        }
    }

    private fun setMenuItemEnabled(enabled:Boolean)
    {
        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView

        // get menu from navigationView
        val menu = navigationView.menu

        val transferMenuItem = menu.findItem(R.id.nav_transfer)
        transferMenuItem.isEnabled = enabled

        val publicKeyMenuItem = menu.findItem(R.id.nav_publickey)
        publicKeyMenuItem.isEnabled = enabled
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

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val titleBar = findViewById<TextView>(R.id.barTitle)
        titleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))

        mProgressBar = findViewById(R.id.nav_progress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mProgressBar?.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, theme.textColorPrimaryId))
        }
        else
        {
            //mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.colorTextToolbar), PorterDuff.Mode.SRC_IN);
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean
    {
        // Handle navigation view item clicks here.
        val tezosTheme = CustomTheme(
                com.tezos.ui.R.color.theme_tezos_primary,
                com.tezos.ui.R.color.theme_tezos_primary_dark,
                com.tezos.ui.R.color.theme_tezos_text)

        when (item.itemId)
        {
            R.id.nav_transfer ->
            {
                PaymentScreenActivity.start(this, tezosTheme)
            }
            R.id.nav_publickey ->
            {
                PublicKeyHashActivity.start(this, mPublicKeyHash, tezosTheme)
            }
            R.id.nav_addresses ->
            {
                //AddAddressActivity.start(this, tezosTheme)
                PaymentAccountsActivity.start(this, tezosTheme, PaymentAccountsActivity.FromScreen.FromHome, PaymentAccountsActivity.Selection.SelectionAddresses)
            }
            R.id.nav_settings ->
            {
                SettingsActivity.start(this, tezosTheme)
            }
            R.id.nav_info ->
            {
                val starter = Intent(this, AboutActivity::class.java)
                starter.putExtra(CustomTheme.TAG, tezosTheme.toBundle())
                ActivityCompat.startActivityForResult(this, starter, -1, null)
            }
        }

        //drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    // volley


    private fun startGetRequestLoadOperations()
    {
        cancelRequest(true)

        mGetHistoryLoading = true

        mEmptyLoadingTextView?.setText(R.string.loading_list_operations)
        mProgressBar?.visibility = View.VISIBLE


        val url = String.format(getString(R.string.history_url), "tz1dBEF7fUmrNZogkrGdTRFhHdx4PQz4ZuAA")

        val jsObjRequest = JsonArrayRequest(Request.Method.GET, url, null, object : Response.Listener<JSONArray>
        {
            override fun onResponse(answer: JSONArray)
            {
                addOperationItemsFromJSON(answer)

                onOperationsLoadComplete()
            }

        }, object : Response.ErrorListener {

            override fun onErrorResponse(error: VolleyError)
            {
                mGetHistoryLoading = false

                onOperationsLoadComplete()

                //TODO handle network connection snackbar
                //showSnackbarError(true);
            }
        })

        jsObjRequest.tag = LOAD_OPERATIONS_TAG

        VolleySingleton.getInstance(this.applicationContext).addToRequestQueue(jsObjRequest)
    }


    private fun addOperationItemsFromJSON(answer:JSONArray) {

        val response = DataExtractor.getJSONArrayFromField(answer,0)

        mRecyclerViewItems?.clear()

        for (i in 0..(response.length() - 1))
        {
            val item = response.getJSONObject(i)
            val operation = Operation.fromJSONObject(item)

            mRecyclerViewItems!!.add(operation)
        }

        mRecyclerView!!.adapter!!.notifyDataSetChanged()

        //TODO parse it

        /*
    JSONArray messages = DataExtractor.getJSONArrayFromField(object, "messages");

    if (messages != null && messages.length() > 0)
    {
        // clear before adding new elements
        mRecyclerViewItems.clear();

        for (int i = 0; i < messages.length(); i++)
        {
            JSONObject row = DataExtractor.getJSONObjectFromField(messages, i);
            if (row != null)
            {
                String sender = DataExtractor.getStringFromField(row, "sender");
                String body = DataExtractor.getStringFromField(row, "message");

                Integer dateInteger = DataExtractor.getIntegerFromField(row, "date");
                long date = -1;
                if (dateInteger != null)
                {
                    date = dateInteger;
                }

                if (!TextUtils.isEmpty(sender) && !TextUtils.isEmpty(body))
                {

                    int type;
                    if (sender.equalsIgnoreCase("user"))
                    {
                        type = 0;
                    }
                    else
                    {
                        type = 1;
                    }

                    SupportMessageItem messageItem = new SupportMessageItem(type, sender, body, date*1000);
                    mRecyclerViewItems.add(messageItem);
                }

                // sort messages by date, oldest last.
                Collections.sort(mRecyclerViewItems, new Comparator<SupportMessageItem>()
                {
                    @Override
                    public int compare(SupportMessageItem o1, SupportMessageItem o2)
                    {
                        return (int) (o2.getDate() - o1.getDate());
                    }
                });
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }
        */
}

    private fun bundlesToItems( bundles:ArrayList<Bundle>): ArrayList<Operation>?
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
        return null
    }

    private fun itemsToBundles(items:ArrayList<Operation>?):ArrayList<Bundle>?
    {
        if (items != null)
        {
            val bundles = ArrayList<Bundle>(items.size)
            if (!items.isEmpty())
            {
                items.forEach {
                    bundles.add(it.toBundle())
                }
            }
            return bundles
        }
        return null
    }

    override fun onSaveInstanceState(outState: Bundle?)
    {
        super.onSaveInstanceState(outState)

        outState?.putString(pkHashKey, mPublicKeyHash)

        val bundles = itemsToBundles(mRecyclerViewItems)
        outState?.putParcelableArrayList(OPERATIONS_ARRAYLIST_KEY, bundles)

        outState?.putBoolean(GET_OPERATIONS_LOADING_KEY, mGetHistoryLoading)
    }

    private fun cancelRequest(getOperations: Boolean)
    {
        val requestQueue = VolleySingleton.getInstance(this.applicationContext).requestQueue
        if (requestQueue != null)
        {
            if (getOperations)
            {
                requestQueue.cancelAll(LOAD_OPERATIONS_TAG)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelRequest(true)
    }
}
