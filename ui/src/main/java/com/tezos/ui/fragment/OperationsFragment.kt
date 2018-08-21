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
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.R
import com.tezos.ui.adapter.OperationRecyclerViewAdapter
import com.tezos.ui.utils.VolleySingleton
import org.json.JSONArray
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

class OperationsFragment : Fragment(), OperationRecyclerViewAdapter.OnItemClickListener
{
    private val OPERATIONS_ARRAYLIST_KEY = "operations_list"
    private val BALANCE_FLOAT_KEY = "balance_float_item"

    private val GET_OPERATIONS_LOADING_KEY = "get_operations_loading"
    private val GET_BALANCE_LOADING_KEY = "get_balance_loading"

    private val LOAD_OPERATIONS_TAG = "load_operations"
    private val LOAD_BALANCE_TAG = "load_balance"

    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mRecyclerView: RecyclerView? = null
    private var mRecyclerViewItems:ArrayList<Operation>? = null
    private var mBalanceItem:Double? = null

    private var mGetHistoryLoading:Boolean = false
    private var mGetBalanceLoading:Boolean = false

    private var mEmptyLoadingTextView: TextView? = null
    private var mEmptyLoadingBalanceTextview: TextView? = null

    private var mCoordinatorLayout: CoordinatorLayout? = null

    private var mBalanceTextView: TextView? = null

    private var mNavProgressBalance: ProgressBar? = null
    private var mNavProgressOperations: ProgressBar? = null

    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme, address: Address) =
                OperationsFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putBundle(Address.TAG, address.toBundle())
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        mNavProgressBalance = view.findViewById(R.id.nav_progress_balance)
        mNavProgressOperations = view.findViewById(R.id.nav_progress_operations)

        mBalanceTextView = view.findViewById(R.id.balance_textview)

        mCoordinatorLayout = view.findViewById(R.id.coordinator)
        mEmptyLoadingTextView = view.findViewById(R.id.empty_loading_textview)
        mEmptyLoadingBalanceTextview = view.findViewById(R.id.empty_loading_balance_textview)

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            startGetRequestLoadBalance()
        }

        if (savedInstanceState != null)
        {
            var messagesBundle = savedInstanceState.getParcelableArrayList<Bundle>(OPERATIONS_ARRAYLIST_KEY)
            mRecyclerViewItems = bundlesToItems(messagesBundle)

            mGetBalanceLoading = savedInstanceState.getBoolean(GET_OPERATIONS_LOADING_KEY)
            mGetHistoryLoading = savedInstanceState.getBoolean(GET_BALANCE_LOADING_KEY)

            mBalanceItem = savedInstanceState.getDouble(BALANCE_FLOAT_KEY, -1.0)

            if (mGetBalanceLoading)
            {
                refreshTextBalance(false)
                startInitialLoadingBalance()
            }
            else
            {
                onBalanceLoadComplete(false)

                if (mGetHistoryLoading)
                {
                    refreshRecyclerViewAndTextHistory()
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

            //TODO we will start loading
            startInitialLoadingBalance()
        }

        var recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager

        val adapter = OperationRecyclerViewAdapter(mRecyclerViewItems)

        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter

        mRecyclerView = recyclerView
    }

    private fun onOperationsLoadHistoryComplete()
    {
        mGetHistoryLoading = false

        mNavProgressOperations?.visibility = View.GONE

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

        mEmptyLoadingTextView?.setText(R.string.loading_list_operations)
        mEmptyLoadingBalanceTextview?.setText(R.string.loading_balance)

        mNavProgressBalance?.visibility = View.VISIBLE

        var pkh:Address? = null
        arguments?.let {
            val addressBundle = it.getBundle(Address.TAG)
            pkh = Address.fromBundle(addressBundle)
        }
        val url = String.format(getString(R.string.balance_url), pkh?.pubKeyHash)

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    val balance = response.replace("[^0-9]".toRegex(), "")
                    mBalanceItem = balance.toDouble()/1000000
                    animateBalance(mBalanceItem)

                    onBalanceLoadComplete(true)
                    startGetRequestLoadOperations()
                },
                Response.ErrorListener {
                    onBalanceLoadComplete(false)
                    onOperationsLoadHistoryComplete()
                    showSnackbarError(true)
                })

        stringRequest.tag = LOAD_BALANCE_TAG
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(stringRequest)
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
        cancelRequest(true, true)

        mGetHistoryLoading = true

        mEmptyLoadingTextView?.setText(R.string.loading_list_operations)

        mNavProgressOperations?.visibility = View.VISIBLE

        var pkh:Address? = null
        arguments?.let {
            val addressBundle = it.getBundle(Address.TAG)
            pkh = Address.fromBundle(addressBundle)
        }
        val url = String.format(getString(R.string.history_url), pkh?.pubKeyHash)

        val jsObjRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
        { answer ->

            addOperationItemsFromJSON(answer)

            onOperationsLoadHistoryComplete()

        }, Response.ErrorListener
        {
            onOperationsLoadHistoryComplete()

            showSnackbarError(true)
        })

        jsObjRequest.tag = LOAD_OPERATIONS_TAG

        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_operations, container, false)
    }

    private fun showSnackbarError(network :Boolean)
    {
        var error:Int = if (network)
        {
            R.string.network_error
        }
        else
        {
            R.string.generic_error
        }

        mEmptyLoadingTextView?.setText(error)
        mEmptyLoadingBalanceTextview?.setText(error)

        val snackbar = Snackbar.make(mCoordinatorLayout!!, error, Snackbar.LENGTH_LONG)
        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor((ContextCompat.getColor(activity!!,
                android.R.color.holo_red_light)))
        snackbar.show()
    }

    private fun addOperationItemsFromJSON(answer:JSONArray)
    {
        val response = DataExtractor.getJSONArrayFromField(answer,0)

        mRecyclerViewItems?.clear()

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

        mRecyclerViewItems?.addAll(sortedList)
        mRecyclerView?.adapter?.notifyDataSetChanged()
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

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequest(true, true)
    }

    override fun onOperationSelected(view: View?, operation: Operation?)
    {
        val operationDetailsFragment = OperationDetailsDialogFragment.newInstance(operation)
        operationDetailsFragment.show(activity?.supportFragmentManager, OperationDetailsDialogFragment.TAG)
    }
}