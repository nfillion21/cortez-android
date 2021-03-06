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

package com.tezos.ui.fragment

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.R
import com.tezos.ui.activity.CreateWalletActivity
import com.tezos.ui.activity.OperationsActivity
import com.tezos.ui.activity.RestoreWalletActivity
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.VolleySingleton
import kotlinx.android.synthetic.main.fragment_home.*
import org.json.JSONArray
import java.text.DateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

open class HomeFragment : Fragment()
{
    private val OPERATIONS_ARRAYLIST_KEY = "operations_list"
    private val BALANCE_FLOAT_KEY = "balance_float_item"

    private val GET_OPERATIONS_LOADING_KEY = "get_operations_loading"
    private val GET_BALANCE_LOADING_KEY = "get_balance_loading"

    private val LOAD_OPERATIONS_TAG = "load_operations"
    private val LOAD_BALANCE_TAG = "load_balance"

    private val WALLET_AVAILABLE_KEY = "wallet_available_key"

    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mRecyclerViewItems:ArrayList<Operation>? = null
    private var mBalanceItem:Double? = null

    private var mGetHistoryLoading:Boolean = false
    private var mGetBalanceLoading:Boolean = false

    private var mEmptyLoadingBalanceTextview: TextView? = null

    private var mEmptyLoadingOperationsTextView:TextView? = null

    private var mCoordinatorLayout: CoordinatorLayout? = null

    private var mBalanceTextView: TextView? = null
    private var mOperationAmountTextView: TextView? = null
    private var mOperationFeeTextView: TextView? = null
    private var mOperationDateTextView: TextView? = null

    private var mNavProgressBalance: ProgressBar? = null
    private var mNavProgressOperations: ProgressBar? = null

    private var mRestoreWalletButton: Button? = null
    private var mCreateWalletButton: Button? = null

    private var mBalanceLayout: LinearLayout? = null
    private var mCreateWalletLayout: LinearLayout? = null

    private var mLastOperationLayout: RelativeLayout? = null

    private var mWalletEnabled:Boolean = false

    private var listener: HomeListener? = null

    private var mDateFormat:DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    companion object
    {
        const val PKH_KEY = "PKH_KEY"
        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                HomeFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    interface HomeListener
    {
        fun showSnackBar(res:String, color:Int, textColor:Int)
    }

    fun pkh():String?
    {
        var pkh:String? = null

        val hasMnemonics = Storage(activity!!).hasMnemonics()
        if (hasMnemonics)
        {
            pkh = arguments!!.getString(PKH_KEY)
            if (pkh == null)
            {
                val seed = Storage(activity!!).getMnemonics()
                pkh = seed.pkh
            }
        }

        return pkh
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is HomeListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException("$context must implement HomeListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        var pkh:String? = pkh()
        var tzTheme:CustomTheme? = null

        arguments?.let {

            val themeBundle = it.getBundle(CustomTheme.TAG)
            tzTheme = CustomTheme.fromBundle(themeBundle)
        }

        mLastOperationLayout = view.findViewById(R.id.last_operation_layout)

        mLastOperationLayout?.setOnClickListener {

            if (mRecyclerViewItems != null && mRecyclerViewItems?.isEmpty() != true)
            {
                //Toast.makeText(activity, getString(R.string.copied_your_pkh), Toast.LENGTH_SHORT).show()

                val mTezosTheme = CustomTheme(
                        R.color.theme_boo_primary,
                        R.color.theme_boo_primary_dark,
                        R.color.theme_boo_text)

                val bundles = itemsToBundles(mRecyclerViewItems)
                OperationsActivity.start(activity!!, bundles!!, mTezosTheme)
            }
        }

        mCreateWalletButton = view.findViewById(R.id.createWalletButton)
        mRestoreWalletButton = view.findViewById(R.id.restoreWalletButton)

        mCreateWalletLayout = view.findViewById(R.id.create_wallet_layout)
        mBalanceLayout = view.findViewById(R.id.balance_layout)

        mNavProgressBalance = view.findViewById(R.id.nav_progress_balance)
        mNavProgressOperations = view.findViewById(R.id.nav_progress_operations)

        mBalanceTextView = view.findViewById(R.id.balance_textview)

        mOperationAmountTextView = view.findViewById(R.id.operation_amount_textview)
        mOperationFeeTextView = view.findViewById(R.id.operation_fee_textview)
        mOperationDateTextView = view.findViewById(R.id.operation_date_textview)

        mCoordinatorLayout = view.findViewById(R.id.coordinator)
        mEmptyLoadingOperationsTextView = view.findViewById(R.id.empty_loading_operations_textview)
        mEmptyLoadingBalanceTextview = view.findViewById(R.id.empty_loading_balance_textview)

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            startGetRequestLoadBalance()
        }

        mRestoreWalletButton = view.findViewById(R.id.restoreWalletButton)
        mRestoreWalletButton!!.setOnClickListener {
            RestoreWalletActivity.start(activity!!, tzTheme!!)
        }

        mCreateWalletButton = view.findViewById(R.id.createWalletButton)
        mCreateWalletButton!!.setOnClickListener {
            CreateWalletActivity.start(activity!!, tzTheme!!)
        }

        if (pkh == null)
        {
            cancelRequest(true, true)
            mGetBalanceLoading = false
            mGetHistoryLoading = false

            mBalanceLayout?.visibility = View.GONE
            mCreateWalletLayout?.visibility = View.VISIBLE

            mSwipeRefreshLayout?.isEnabled = false
        }
        else
        {
            mBalanceLayout?.visibility = View.VISIBLE
            mCreateWalletLayout?.visibility = View.GONE

            mSwipeRefreshLayout?.isEnabled = true
        }

        if (savedInstanceState != null)
        {
            var messagesBundle = savedInstanceState.getParcelableArrayList<Bundle>(OPERATIONS_ARRAYLIST_KEY)
            mRecyclerViewItems = bundlesToItems(messagesBundle)

            mGetBalanceLoading = savedInstanceState.getBoolean(GET_OPERATIONS_LOADING_KEY)
            mGetHistoryLoading = savedInstanceState.getBoolean(GET_BALANCE_LOADING_KEY)

            mBalanceItem = savedInstanceState.getDouble(BALANCE_FLOAT_KEY, -1.0)

            mWalletEnabled = savedInstanceState.getBoolean(WALLET_AVAILABLE_KEY, false)

            if (mGetBalanceLoading)
            {
                refreshTextBalance(false)

                mWalletEnabled = true
                startInitialLoadingBalance()
            }
            else
            {
                onBalanceLoadComplete(false)

                if (mGetHistoryLoading)
                {
                    refreshRecyclerViewAndTextHistory()

                    //TODO check if there is a key before launching request

                    startInitialLoadingHistory()
                }
                else
                {
                    onOperationsLoadHistoryComplete()
                }
            }
        }
        else
        {
            mRecyclerViewItems = ArrayList()
            //there's no need to initialize mBalanceItem

            //TODO we will start loading only if we got a pkh
            if (pkh != null)
            {
                mWalletEnabled = true
                startInitialLoadingBalance()
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        //avoid this call in OperationsFragment
        //this call is necessary to reload when

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            if (!mWalletEnabled)
            {
                mWalletEnabled = true

                // put the good layers
                mBalanceLayout?.visibility = View.VISIBLE
                mCreateWalletLayout?.visibility = View.GONE

                startInitialLoadingBalance()
            }
        }
        else
        {
            //cancelRequest(true, true)

            mBalanceItem = -1.0
            refreshTextBalance(false)

            mSwipeRefreshLayout?.isEnabled = false
            mSwipeRefreshLayout?.isRefreshing = false

            if (mWalletEnabled)
            {
                mWalletEnabled = false
                // put the good layers
                mBalanceLayout?.visibility = View.GONE
                mCreateWalletLayout?.visibility = View.VISIBLE

                val args = arguments
                args?.putBundle(Address.TAG, null)

                if (mRecyclerViewItems != null)
                {
                    mRecyclerViewItems?.clear()
                }

                if (mBalanceItem != null)
                {
                    mBalanceItem = -1.0
                }
            }
        }
    }

    private fun onOperationsLoadHistoryComplete()
    {
        mGetHistoryLoading = false

        mNavProgressOperations?.visibility = View.GONE

        //TODO cancel the swipe refresh if you're not in the right layout
        mSwipeRefreshLayout?.isEnabled = true
        mSwipeRefreshLayout?.isRefreshing = false

        refreshRecyclerViewAndTextHistory()
    }

    private fun onBalanceLoadComplete(animating:Boolean)
    {
        mGetBalanceLoading = false
        mNavProgressBalance?.visibility = View.GONE

        refreshTextBalance(animating)
    }

    private fun startInitialLoadingBalance()
    {
        mSwipeRefreshLayout?.isEnabled = false

        startGetRequestLoadBalance()
    }

    private fun startInitialLoadingHistory()
    {
        mSwipeRefreshLayout?.isEnabled = false

        startGetRequestLoadOperations()
    }

    private fun refreshRecyclerViewAndTextHistory()
    {
        if (mRecyclerViewItems != null && mRecyclerViewItems?.isEmpty() == false)
        {
            mLastOperationLayout?.visibility = View.VISIBLE

            val lastOperation = mRecyclerViewItems!![0]

            mOperationAmountTextView?.text = (lastOperation.amount/1000000).toString()
            mOperationFeeTextView?.text = (lastOperation.fee/1000000).toString()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                mOperationDateTextView?.text = mDateFormat.format(Date.from(Instant.parse(lastOperation.timestamp)))
            }

            mEmptyLoadingOperationsTextView?.visibility = View.GONE
            mEmptyLoadingOperationsTextView?.text = null

            //TODO handle the click or not click.
        }
        else
        {
            mLastOperationLayout?.visibility = View.GONE

            mEmptyLoadingOperationsTextView?.visibility = View.VISIBLE
            mEmptyLoadingOperationsTextView?.text = "-"
        }
    }

    private fun refreshTextBalance(animating:Boolean)
    {
        if (mBalanceItem != -1.0 && mBalanceItem != null)
        {
            mBalanceTextView?.visibility = View.VISIBLE
            if (!animating)
            {
                mBalanceTextView?.text = mBalanceItem.toString()
            }

            mEmptyLoadingBalanceTextview?.visibility = View.GONE
            mEmptyLoadingBalanceTextview?.text = null
        }
        else
        {
            mBalanceTextView?.visibility = View.GONE
            mEmptyLoadingBalanceTextview?.visibility = View.VISIBLE
            mEmptyLoadingBalanceTextview?.text = "-"
        }
    }

    // volley
    private fun startGetRequestLoadBalance()
    {
        cancelRequest(true, true)

        mGetHistoryLoading = true

        mEmptyLoadingOperationsTextView?.setText(R.string.loading_list_operations)
        mEmptyLoadingBalanceTextview?.setText(R.string.loading_balance)

        mNavProgressBalance?.visibility = View.VISIBLE

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.balance_url), pkh)

            // Request a string response from the provided URL.
            val stringRequest = StringRequest(Request.Method.GET, url,
                    Response.Listener<String> { response ->

                        if (swipeRefreshLayout != null)
                        {
                            val balance = response.replace("[^0-9]".toRegex(), "")
                            mBalanceItem = balance?.toDouble()/1000000
                            if (mBalanceItem != null)
                            {
                                animateBalance(mBalanceItem)
                            }

                            onBalanceLoadComplete(true)
                            startGetRequestLoadOperations()
                        }
                    },
                    Response.ErrorListener {
                        if (swipeRefreshLayout != null)
                        {
                            onBalanceLoadComplete(false)
                            onOperationsLoadHistoryComplete()
                            showSnackbarError(it)
                        }
                    })

            stringRequest.tag = LOAD_BALANCE_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(stringRequest)
        }
    }

    private fun animateBalance(balance: Double?)
    {
        val objectAnimator = ObjectAnimator.ofFloat(mBalanceTextView, View.ALPHA, 0f)
        objectAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mBalanceTextView?.text = balance.toString()
            }
        })

        val objectAnimator2 = ObjectAnimator.ofFloat(mBalanceTextView, View.ALPHA, 1f)

        //mBalanceTextView?.text = balance.toString()

        val animatorSet = AnimatorSet()
        animatorSet.play(objectAnimator).before(objectAnimator2)
        animatorSet.start()
    }

    // volley
    private fun startGetRequestLoadOperations()
    {
        cancelRequest(operations = true, balance = true)

        mGetHistoryLoading = true

        mEmptyLoadingOperationsTextView?.setText(R.string.loading_list_operations)

        mNavProgressOperations?.visibility = View.VISIBLE

        val pkh = pkh()

        if (pkh != null)
        {
            val url = String.format(getString(R.string.history_url), pkh)

            val jsObjRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            { answer ->

                if (activity != null)
                {
                    addOperationItemsFromJSON(answer)
                    onOperationsLoadHistoryComplete()
                }

            }, Response.ErrorListener
            { volleyError ->
                if (activity != null)
                {
                    onOperationsLoadHistoryComplete()
                    showSnackbarError(volleyError)
                }
            })

            jsObjRequest.tag = LOAD_OPERATIONS_TAG

            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private fun showSnackbarError(error:VolleyError?)
    {
        var err: String? = getString(R.string.generic_error)

        listener?.showSnackBar(err!!, ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))

        mEmptyLoadingOperationsTextView?.text = getString(R.string.generic_error)
        mEmptyLoadingBalanceTextview?.text = getString(R.string.generic_error)
    }

    private fun addOperationItemsFromJSON(answer:JSONArray)
    {
        val response = DataExtractor.getJSONArrayFromField(answer,0)

        if (response.length() > 0)
        {
            var sortedList = arrayListOf<Operation>()

            for (i in 0..(response.length() - 1))
            {
                val item = response.getJSONObject(i)
                val operation = Operation.fromJSONObject(item)

                sortedList.add(operation)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                sortedList.sortWith(object: Comparator<Operation>
                {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun compare(o1: Operation, o2: Operation): Int = when {

                        Date.from(Instant.parse(o1.timestamp)) > Date.from(Instant.parse(o2.timestamp)) -> -1
                        Date.from(Instant.parse(o1.timestamp)) == Date.from(Instant.parse(o2.timestamp)) -> 0
                        else -> 1
                    }
                })
            }

            //TODO take the 10 last operations in a better way
            if (mRecyclerViewItems == null)
            {
                mRecyclerViewItems = ArrayList()
            }
            mRecyclerViewItems?.clear()
            mRecyclerViewItems?.addAll(sortedList.subList(0, minOf(10, sortedList.size)))

            //TODO what if the list is empty
            val lastOperation = sortedList[0]

            //TODO put that
            mOperationAmountTextView?.text = (lastOperation!!.amount/1000000).toString()
            mOperationFeeTextView?.text = (lastOperation!!.fee/1000000).toString()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                mOperationDateTextView?.text = mDateFormat.format(Date.from(Instant.parse(lastOperation.timestamp)))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        val bundles = itemsToBundles(mRecyclerViewItems)
        outState.putParcelableArrayList(OPERATIONS_ARRAYLIST_KEY, bundles)

        mBalanceItem?.let {
            outState.putDouble(BALANCE_FLOAT_KEY, it)
        }

        outState.putBoolean(GET_OPERATIONS_LOADING_KEY, mGetHistoryLoading)
        outState.putBoolean(GET_BALANCE_LOADING_KEY, mGetBalanceLoading)

        outState.putBoolean(WALLET_AVAILABLE_KEY, mWalletEnabled)
    }

    private fun bundlesToItems( bundles:ArrayList<Bundle>?): ArrayList<Operation>?
    {
        if (bundles != null)
        {
            var items = ArrayList<Operation>(bundles.size)
            if (bundles.isNotEmpty())
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

    private fun cancelRequest(operations: Boolean, balance:Boolean)
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
        if (requestQueue != null)
        {
            if (operations)
            {
                requestQueue.cancelAll(LOAD_OPERATIONS_TAG)
            }
            if (balance)
            {
                requestQueue.cancelAll(LOAD_BALANCE_TAG)
            }
        }
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequest(true, true)
    }
}