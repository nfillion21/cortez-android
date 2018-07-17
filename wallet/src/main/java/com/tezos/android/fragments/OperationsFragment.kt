package com.tezos.android.fragments

import android.os.Bundle
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
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.tezos.android.R
import com.tezos.android.adapters.OperationRecyclerViewAdapter
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.utils.VolleySingleton
import org.json.JSONArray

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

    private var mCoordinatorLayout: CoordinatorLayout? = null

    private var mBalanceTextView: TextView? = null

    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                OperationsFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBalanceTextView = view.findViewById(R.id.balance_textview)

        mCoordinatorLayout = view.findViewById(R.id.coordinator)
        mEmptyLoadingTextView = view.findViewById(R.id.empty_loading_textview)

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            //startGetRequestLoadOperations()
            startGetRequestLoadBalance()
        }

        if (savedInstanceState != null)
        {
            var messagesBundle = savedInstanceState.getParcelableArrayList<Bundle>(OPERATIONS_ARRAYLIST_KEY)
            mRecyclerViewItems = bundlesToItems(messagesBundle)

            mGetBalanceLoading = savedInstanceState.getBoolean(GET_OPERATIONS_LOADING_KEY)
            mGetHistoryLoading = savedInstanceState.getBoolean(GET_BALANCE_LOADING_KEY)

            mBalanceItem = savedInstanceState.getDouble(BALANCE_FLOAT_KEY)

            //TODO this needs to be well handled

            if (mGetHistoryLoading)
            {
                // it does back to loading while we got elements on the list
                // put the elements before loading.
                // looks ok

                refreshRecyclerViewAndTextHistory()
                startInitialLoadingHistory()
            }
            else
            {
                onOperationsLoadHistoryComplete()
            }

            if (mGetBalanceLoading)
            {
                refreshRecyclerViewAndTextHistory()
                startInitialLoadingHistory()
            }
            else
            {
                onOperationsLoadHistoryComplete()
            }
        }
        else
        {
            mRecyclerViewItems = ArrayList()
            //there's no need to initialize mBalanceItem

            //TODO we will start loading
            startInitialLoadingHistory()
        }

        var recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager

        val adapter = OperationRecyclerViewAdapter(mRecyclerViewItems)

        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter

        mRecyclerView = recyclerView
    }

    override fun onResume()
    {
        super.onResume()

        mRecyclerView?.adapter?.notifyDataSetChanged()
        mBalanceTextView?.text = mBalanceItem.toString()
    }

    private fun onOperationsLoadHistoryComplete()
    {
        mGetHistoryLoading = false

        //TODO progressBar from activity
        //mProgressBar?.visibility = View.GONE

        //TODO see how we handle this swipe
        mSwipeRefreshLayout?.isEnabled = true
        mSwipeRefreshLayout?.isRefreshing = false

        refreshRecyclerViewAndTextHistory()
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

    // volley
    private fun startGetRequestLoadBalance()
    {
        cancelRequest(true)

        mGetHistoryLoading = true

        mEmptyLoadingTextView?.setText(R.string.loading_list_operations)

        //TODO progressBar from activity
        //mProgressBar?.visibility = View.VISIBLE

        val url = String.format(getString(R.string.balance_url), "tz1VyfL1U3x8GwKwrwBy3odwQfZX5CdXwcvK")

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    // Display the first 500 characters of the response string.
                    val balance = response.replace("[^0-9]".toRegex(), "")
                    mBalanceItem = balance.toDouble()/1000000
                    mBalanceTextView?.text = mBalanceItem.toString()

                    //TODO handle that for the loadBalanceComplete
                    onOperationsLoadHistoryComplete()
                },
                Response.ErrorListener {
                    mGetBalanceLoading = false

                    //TODO handle that for the loadBalanceComplete
                    onOperationsLoadHistoryComplete()
                    showSnackbarError(true)
                })

        stringRequest.tag = LOAD_BALANCE_TAG
        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(stringRequest)
    }

    // volley
    private fun startGetRequestLoadOperations()
    {
        cancelRequest(true)

        mGetHistoryLoading = true

        mEmptyLoadingTextView?.setText(R.string.loading_list_operations)

        //TODO progressBar from activity
        //mProgressBar?.visibility = View.VISIBLE


        val url = String.format(getString(R.string.history_url), "tz1dBEF7fUmrNZogkrGdTRFhHdx4PQz4ZuAA")

        val jsObjRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray> { answer ->
            addOperationItemsFromJSON(answer)

            onOperationsLoadHistoryComplete()
        }, Response.ErrorListener {
            mGetHistoryLoading = false

            onOperationsLoadHistoryComplete()

            showSnackbarError(true)
        })

        jsObjRequest.tag = LOAD_OPERATIONS_TAG

        VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_operations, container, false);
    }

    private fun showSnackbarError(network :Boolean)
    {
        mEmptyLoadingTextView?.setText(R.string.network_error)

        var error:Int = if (network)
        {
            R.string.network_error
        }
        else
        {
            R.string.generic_error
        }

        val snackbar = Snackbar.make(mCoordinatorLayout!!, error, Snackbar.LENGTH_LONG)
        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor((ContextCompat.getColor(activity!!,
                android.R.color.holo_red_light)))
        snackbar.show()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val bundles = itemsToBundles(mRecyclerViewItems)
        outState.putParcelableArrayList(OPERATIONS_ARRAYLIST_KEY, bundles)

        mBalanceItem?.let {
            outState.putDouble(BALANCE_FLOAT_KEY, it)
        }

        outState.putBoolean(GET_OPERATIONS_LOADING_KEY, mGetHistoryLoading)
        outState.putBoolean(GET_BALANCE_LOADING_KEY, mGetBalanceLoading)
    }

    private fun cancelRequest(getOperations: Boolean)
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
        if (requestQueue != null)
        {
            if (getOperations)
            {
                requestQueue.cancelAll(LOAD_OPERATIONS_TAG)
                requestQueue.cancelAll(LOAD_BALANCE_TAG)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelRequest(true)
    }

    override fun onOperationSelected(view: View?, operation: Operation?)
    {
        val operationDetailsFragment = OperationDetailsDialogFragment.newInstance(operation)
        operationDetailsFragment.show(activity?.supportFragmentManager, OperationDetailsDialogFragment.TAG)
    }
}