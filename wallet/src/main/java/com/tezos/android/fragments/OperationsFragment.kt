package com.tezos.android.fragments

import android.content.Context
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
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.tezos.android.R
import com.tezos.android.adapters.OperationRecyclerViewAdapter
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.utils.VolleySingleton
import org.json.JSONArray

class OperationsFragment : Fragment(), OperationRecyclerViewAdapter.OnItemClickListener
{

    private val OPERATIONS_ARRAYLIST_KEY = "operationsList"
    private val GET_OPERATIONS_LOADING_KEY = "getOperationsLoading"

    private val LOAD_OPERATIONS_TAG = "downloadHistory"

    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mRecyclerView: RecyclerView? = null
    private var mRecyclerViewItems:ArrayList<Operation>? = null

    private var mGetHistoryLoading:Boolean = false

    private var mEmptyLoadingTextView: TextView? = null

    private var mCoordinatorLayout: CoordinatorLayout? = null

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

        mCoordinatorLayout = view.findViewById<CoordinatorLayout>(R.id.coordinator)
        mEmptyLoadingTextView = view.findViewById(R.id.empty_loading_textview)

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            startGetRequestLoadOperations()
        }

        if (savedInstanceState != null)
        {
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

        var recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager as RecyclerView.LayoutManager?

        val adapter = OperationRecyclerViewAdapter(mRecyclerViewItems)

        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter

        mRecyclerView = recyclerView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onAttach(context: Context?)
    {
        super.onAttach(context)
    }

    override fun onResume()
    {
        super.onResume()

        mRecyclerView?.adapter?.notifyDataSetChanged()
    }

    private fun onOperationsLoadComplete()
    {
        mGetHistoryLoading = false

        //TODO progressBar from activity
        //mProgressBar?.visibility = View.GONE

        mSwipeRefreshLayout?.isEnabled = true
        mSwipeRefreshLayout?.isRefreshing = false

        refreshRecyclerViewAndText()
    }

    private fun startInitialLoading()
    {
        mSwipeRefreshLayout?.isEnabled = false

        startGetRequestLoadOperations()
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

    // volley
    private fun startGetRequestLoadOperations()
    {
        cancelRequest(true)

        mGetHistoryLoading = true

        mEmptyLoadingTextView?.setText(R.string.loading_list_operations)

        //TODO progressBar from activity
        //mProgressBar?.visibility = View.VISIBLE


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

                showSnackbarError(true)
            }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val bundles = itemsToBundles(mRecyclerViewItems)
        outState.putParcelableArrayList(OPERATIONS_ARRAYLIST_KEY, bundles)

        outState.putBoolean(GET_OPERATIONS_LOADING_KEY, mGetHistoryLoading)
    }

    private fun cancelRequest(getOperations: Boolean)
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
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

    override fun onOperationSelected(view: View?, operation: Operation?)
    {
        //call the new fragment
        showSnackbarError(true)
    }
}