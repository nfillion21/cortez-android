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
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.google.firebase.database.*
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.models.Operation
import com.tezos.core.utils.DataExtractor
import com.tezos.core.utils.MultisigBinaries
import com.tezos.ui.R
import com.tezos.ui.activity.CreateWalletActivity
import com.tezos.ui.activity.OngoingMultisigActivity
import com.tezos.ui.activity.OperationsActivity
import com.tezos.ui.activity.RestoreWalletActivity
import com.tezos.ui.database.MultisigOperation
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.VolleySingleton
import kotlinx.android.synthetic.main.fragment_home.*
import org.json.JSONArray
import java.io.Serializable
import java.text.DateFormat
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class HomeFragment : Fragment()
{
    private val OPERATIONS_ARRAYLIST_KEY = "operations_list"
    private val ONGOING_OPERATIONS_ARRAYLIST_KEY = "ongoing_operations_list"
    private val BALANCE_FLOAT_KEY = "balance_float_item"

    private val GET_OPERATIONS_LOADING_KEY = "get_operations_loading"
    private val GET_BALANCE_LOADING_KEY = "get_balance_loading"
    private val GET_MULTISIG_ON_GOING_LOADING_KEY = "get_multisig_on_going_loading"

    private val LOAD_OPERATIONS_TAG = "load_operations"
    private val LOAD_BALANCE_TAG = "load_balance"
    private val LOAD_MULTISIG_ONGOING_TAG = "load_multisig_ongoing"

    private val WALLET_AVAILABLE_KEY = "wallet_available_key"

    private var mRecyclerViewItems:ArrayList<Operation>? = null
    private var mBalanceItem:Double? = null

    private var mOngoingMultisigItems:ArrayList<OngoingMultisigOperation>? = null

    private var mGetHistoryLoading:Boolean = false
    private var mGetBalanceLoading:Boolean = false
    private var mGetMultisigOnGoing:Boolean = false

    private var mWalletEnabled:Boolean = false

    private var listener: HomeListener? = null

    private lateinit var notaryOperationsDatabase: DatabaseReference

    private var mDateFormat:DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)


    data class OngoingMultisigOperation(
            val contractAddress: String,
            val submissionDate:String,
            val hexaOperation: String) : Serializable

    internal class OngoinMultisigSerialization internal constructor(private val ongoingMultisigOperation: OngoingMultisigOperation)
    {
        internal fun getSerializedBundle():Bundle
        {
            val ongoingOperationBundle = Bundle()

            ongoingOperationBundle.putString("contractAddress", ongoingMultisigOperation.contractAddress)
            ongoingOperationBundle.putString("submissionDate", ongoingMultisigOperation.submissionDate)
            ongoingOperationBundle.putString("hexaOperation", ongoingMultisigOperation.hexaOperation)

            return ongoingOperationBundle
        }
    }

    internal class OngoingMultisigMapper internal constructor(private val bundle: Bundle)
    {
        internal fun mappedObjectFromBundle(): OngoingMultisigOperation
        {
            val contractAddress = this.bundle.getString("contractAddress", null)
            val submissionDate = this.bundle.getString("submissionDate", null)
            val hexaOperation = this.bundle.getString("hexaOperation", null)

            return OngoingMultisigOperation(contractAddress, submissionDate, hexaOperation)
        }
    }

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

        fun toBundle(mnemonicsData: OngoingMultisigOperation): Bundle {
            val serializer = OngoinMultisigSerialization(mnemonicsData)
            return serializer.getSerializedBundle()
        }

        fun fromBundle(bundle: Bundle): OngoingMultisigOperation {
            val mapper = OngoingMultisigMapper(bundle)
            return mapper.mappedObjectFromBundle()
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

    fun pk():String?
    {
        val hasMnemonics = Storage(activity!!).hasMnemonics()
        if (hasMnemonics)
        {
            val seed = Storage(activity!!).getMnemonics()
            return seed.pk
        }

        return null
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

        last_operation_layout.setOnClickListener {

            if (!mRecyclerViewItems.isNullOrEmpty())
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

        ongoing_operation_layout?.setOnClickListener {

            if (!mOngoingMultisigItems.isNullOrEmpty())
            {
                //Toast.makeText(activity, getString(R.string.copied_your_pkh), Toast.LENGTH_SHORT).show()

                val mTezosTheme = CustomTheme(
                        R.color.theme_boo_primary,
                        R.color.theme_boo_primary_dark,
                        R.color.theme_boo_text)

                val bundles = ongoingItemsToBundles(mOngoingMultisigItems)
                OngoingMultisigActivity.start(activity!!, bundles!!, mTezosTheme)
            }
        }

        swipe_refresh_layout.setOnRefreshListener {
            startGetRequestLoadBalance()
        }

        restoreWalletButton.setOnClickListener {
            RestoreWalletActivity.start(activity!!, tzTheme!!)
        }

        createWalletButton.setOnClickListener {
            CreateWalletActivity.start(activity!!, tzTheme!!)
        }

        // Initialize Database
        notaryOperationsDatabase = FirebaseDatabase.getInstance().reference
                .child("signatory-operations").child(pk()!!)

        if (pkh == null)
        {
            cancelRequest(operations = true, balance = true, multisigOnGoing = true)
            mGetBalanceLoading = false
            mGetHistoryLoading = false
            mGetMultisigOnGoing = false

            balance_layout?.visibility = View.GONE
            create_wallet_layout.visibility = View.VISIBLE

            swipe_refresh_layout.isEnabled = false
        }
        else
        {
            balance_layout?.visibility = View.VISIBLE
            create_wallet_layout.visibility = View.GONE

            swipe_refresh_layout.isEnabled = true
        }

        if (savedInstanceState != null)
        {
            var messagesBundle = savedInstanceState.getParcelableArrayList<Bundle>(OPERATIONS_ARRAYLIST_KEY)
            mRecyclerViewItems = bundlesToItems(messagesBundle)

            var ongoingItemsBundle = savedInstanceState.getParcelableArrayList<Bundle>(ONGOING_OPERATIONS_ARRAYLIST_KEY)
            mOngoingMultisigItems = bundlesToOngoingItems(ongoingItemsBundle)

            mGetBalanceLoading = savedInstanceState.getBoolean(GET_OPERATIONS_LOADING_KEY)
            mGetHistoryLoading = savedInstanceState.getBoolean(GET_BALANCE_LOADING_KEY)
            mGetMultisigOnGoing = savedInstanceState.getBoolean(GET_MULTISIG_ON_GOING_LOADING_KEY)

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
                    startInitialLoadingHistory()
                }
                else
                {
                    onOperationsLoadHistoryComplete()

                    if (mGetMultisigOnGoing)
                    {
                        refreshOngoingOperationsLayouts()
                        startInitialLoadingMultisigOngoingOperations()
                    }
                    else
                    {
                        onMultisigOnGoinLoadComplete()
                    }
                }
            }
        }
        else
        {
            mRecyclerViewItems = ArrayList()
            //there's no need to initialize mBalanceItem

            mOngoingMultisigItems = ArrayList()

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
                balance_layout?.visibility = View.VISIBLE
                create_wallet_layout.visibility = View.GONE

                startInitialLoadingBalance()
            }
        }
        else
        {
            //cancelRequest(true, true)

            mBalanceItem = -1.0
            refreshTextBalance(false)

            swipe_refresh_layout.isEnabled = false
            swipe_refresh_layout.isRefreshing = false

            if (mWalletEnabled)
            {
                mWalletEnabled = false
                // put the good layers
                balance_layout?.visibility = View.GONE
                create_wallet_layout.visibility = View.VISIBLE

                val args = arguments
                args?.putBundle(Address.TAG, null)

                if (mRecyclerViewItems != null)
                {
                    mRecyclerViewItems?.clear()
                }

                if (mOngoingMultisigItems != null)
                {
                    mOngoingMultisigItems?.clear()
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

        nav_progress_operations.visibility = View.GONE

        //TODO cancel the swipe refresh if you're not in the right layout
        swipe_refresh_layout.isEnabled = true
        swipe_refresh_layout.isRefreshing = false

        refreshRecyclerViewAndTextHistory()
    }

    private fun onMultisigOnGoinLoadComplete()
    {
        mGetMultisigOnGoing = false

        nav_progress_ongoing_operations.visibility = View.GONE

        swipe_refresh_layout.isEnabled = true
        swipe_refresh_layout.isRefreshing = false

        refreshOngoingOperationsLayouts()
    }

    private fun onBalanceLoadComplete(animating:Boolean)
    {
        mGetBalanceLoading = false
        nav_progress_balance.visibility = View.GONE

        refreshTextBalance(animating)
    }

    private fun startInitialLoadingBalance()
    {
        swipe_refresh_layout?.isEnabled = false

        startGetRequestLoadBalance()
    }

    private fun startInitialLoadingHistory()
    {
        swipe_refresh_layout.isEnabled = false

        startGetRequestLoadOperations()
    }

    protected open fun startInitialLoadingMultisigOngoingOperations()
    {
        swipe_refresh_layout.isEnabled = false

        startGetRequestLoadMultisigOnGoingOperations()
    }

    private fun refreshRecyclerViewAndTextHistory()
    {
        if (mRecyclerViewItems != null && mRecyclerViewItems?.isEmpty() == false)
        {
            last_operation_layout.visibility = View.VISIBLE

            val lastOperation = mRecyclerViewItems!![0]

            operation_amount_textview.text = (lastOperation.amount/1000000).toString()
            operation_fee_textview.text = (lastOperation.fee/1000000).toString()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                operation_date_textview.text = mDateFormat.format(Date.from(Instant.parse(lastOperation.timestamp)))
            }

            empty_loading_operations_textview.visibility = View.GONE
            empty_loading_operations_textview.text = null

            //TODO handle the click or not click.
        }
        else
        {
            last_operation_layout.visibility = View.GONE

            empty_loading_operations_textview.visibility = View.VISIBLE
            empty_loading_operations_textview.text = "-"
        }
    }

    private fun refreshOngoingOperationsLayouts()
    {
        if (!mOngoingMultisigItems.isNullOrEmpty())
        {
            ongoing_operation_layout.visibility = View.VISIBLE

            val lastOngoingOperation = mOngoingMultisigItems!![0]

            ongoing_contract_textview.text = lastOngoingOperation.contractAddress
            ongoing_submission_date_textview.text = lastOngoingOperation.submissionDate

            val binaryReader = MultisigBinaries(lastOngoingOperation.hexaOperation)
            binaryReader.getType()
            ongoing_type_textview.text = binaryReader.getOperationTypeString()

            empty_loading_ongoing_operations_textview.visibility = View.GONE
            empty_loading_ongoing_operations_textview.text = null
        }
        else
        {
            ongoing_operation_layout.visibility = View.GONE

            empty_loading_ongoing_operations_textview.visibility = View.VISIBLE
            empty_loading_ongoing_operations_textview.text = "-"
        }
    }

    private fun refreshTextBalance(animating:Boolean)
    {
        if (mBalanceItem != -1.0 && mBalanceItem != null)
        {
            balance_textview.visibility = View.VISIBLE
            if (!animating)
            {
                balance_textview.text = mBalanceItem.toString()
            }

            empty_loading_balance_textview.visibility = View.GONE
            empty_loading_balance_textview.text = null
        }
        else
        {
            balance_textview.visibility = View.GONE
            empty_loading_balance_textview.visibility = View.VISIBLE
            empty_loading_balance_textview.text = getString(R.string.neutral)
        }
    }

    // volley
    private fun startGetRequestLoadBalance()
    {
        cancelRequest(operations = true, balance = true, multisigOnGoing = true)

        mGetHistoryLoading = true

        empty_loading_ongoing_operations_textview.setText(R.string.loading_list_multisig_on_going_operations)
        empty_loading_operations_textview.setText(R.string.loading_list_operations)
        empty_loading_balance_textview.setText(R.string.loading_balance)

        nav_progress_balance.visibility = View.VISIBLE

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.balance_url), pkh)

            // Request a string response from the provided URL.
            val stringRequest = StringRequest(Request.Method.GET, url,
                    Response.Listener<String> { response ->

                        if (swipe_refresh_layout != null)
                        {
                            val balance = response.replace("[^0-9]".toRegex(), "")
                            mBalanceItem = balance?.toDouble()/1000000
                            if (mBalanceItem != null)
                            {
                                animateBalance(mBalanceItem)
                            }

                            onBalanceLoadComplete(true)
                            startInitialLoadingHistory()
                        }
                    },
                    Response.ErrorListener {
                        if (swipe_refresh_layout != null)
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
        val objectAnimator = ObjectAnimator.ofFloat(balance_textview, View.ALPHA, 0f)
        objectAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                balance_textview.text = balance.toString()
            }
        })

        val objectAnimator2 = ObjectAnimator.ofFloat(balance_textview, View.ALPHA, 1f)

        //mBalanceTextView?.text = balance.toString()

        val animatorSet = AnimatorSet()
        animatorSet.play(objectAnimator).before(objectAnimator2)
        animatorSet.start()
    }

    // volley
    private fun startGetRequestLoadOperations()
    {
        cancelRequest(operations = true, balance = true, multisigOnGoing = true)

        mGetHistoryLoading = true

        empty_loading_operations_textview.setText(R.string.loading_list_operations)

        nav_progress_operations.visibility = View.VISIBLE

        val pkh = pkh()

        if (pkh != null)
        {
            val url = String.format(getString(R.string.history_url), pkh)

            val jsObjRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            { answer ->

                if (swipe_refresh_layout != null)
                {
                    addOperationItemsFromJSON(answer)
                    onOperationsLoadHistoryComplete()

                    startInitialLoadingMultisigOngoingOperations()
                }

            }, Response.ErrorListener
            { volleyError ->

                if (swipe_refresh_layout != null)
                {
                    onOperationsLoadHistoryComplete()
                    showSnackbarError(volleyError)
                }
            })

            jsObjRequest.tag = LOAD_OPERATIONS_TAG

            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
        }
    }


    // volley
    private fun startGetRequestLoadMultisigOnGoingOperations()
    {
        cancelRequest(operations = true, balance = true, multisigOnGoing = true)

        mGetMultisigOnGoing = true

        empty_loading_ongoing_operations_textview.setText(R.string.loading_list_multisig_on_going_operations)

        nav_progress_ongoing_operations.visibility = View.VISIBLE

        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val operation = dataSnapshot.value

                for (i in dataSnapshot.children)
                {
                    val k = i.value as HashMap<String, Any>
                    //val k2 = i
                    //val k3 = i

                    val element = MultisigOperation.fromMap(k)
                    val element2 = MultisigOperation.fromMap(k)
                }
                // ...
                val k = dataSnapshot
                val k2 = dataSnapshot.children

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                //Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
                val k = databaseError
                val k2 = databaseError
            }
        }
        //notaryOperationsDatabase.addValueEventListener(postListener)
        notaryOperationsDatabase.addListenerForSingleValueEvent(postListener)



        /*
        val pkh = pkh()

        if (pkh != null)
        {
            val url = String.format(getString(R.string.history_url), pkh)

            val jsObjRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            { answer ->

                if (swipe_refresh_layout != null)
                {
                    addMultisigOngoingOperationsFromJSON(answer)
                    onMultisigOnGoinLoadComplete()
                }

            }, Response.ErrorListener
            { volleyError ->
                if (swipe_refresh_layout != null)
                {
                    onMultisigOnGoinLoadComplete()
                    showSnackbarError(volleyError)
                }
            })

            jsObjRequest.tag = LOAD_MULTISIG_ONGOING_TAG

            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsObjRequest)
        }
        */
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private fun showSnackbarError(error:VolleyError?)
    {
        var err: String? = error?.toString() ?: getString(R.string.generic_error)

        listener?.showSnackBar(err!!, ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))

        empty_loading_operations_textview.text = getString(R.string.generic_error)
        empty_loading_balance_textview.text = getString(R.string.generic_error)
    }

    private fun addOperationItemsFromJSON(answer:JSONArray)
    {
        val response = DataExtractor.getJSONArrayFromField(answer,0)

        if (response.length() > 0)
        {
            var sortedList = arrayListOf<Operation>()

            for (i in 0 until response.length())
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
            operation_amount_textview.text = (lastOperation!!.amount/1000000).toString()
            operation_fee_textview.text = (lastOperation!!.fee/1000000).toString()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                operation_date_textview.text = mDateFormat.format(Date.from(Instant.parse(lastOperation.timestamp)))
            }
        }
    }

    private fun addMultisigOngoingOperationsFromJSON(answer:JSONArray)
    {
        val response = DataExtractor.getJSONArrayFromField(answer,0)

        if (response.length() > 0)
        {
            var sortedList = arrayListOf<OngoingMultisigOperation>()

            //for (i in 0 until response.length())
            for (i in 0 until 4)
            {
                //val item = response.getJSONObject(i)
                //val operation = Operation.fromJSONObject(item)

                if (i == 0)
                {
                    val ongoingOperation = OngoingMultisigOperation(
                            contractAddress = "KT1Gen5CXA9Uh5TQSGKtGYAptsZEbpCz7kKX",
                            submissionDate = "24/01/1987",
                            hexaOperation = "05070707070a000000049caecab90a0000001601588317ff8c2df3024d180109239ce16c80e6f6d10007070007050805080707000302000000720a00000021007bce946147500e3945702697be1e69814e3b210a55d77a6a3f3c144b27ba941e0a0000002100af72f76635c9d2929ef294ca8a0f7aaeb3ef687f0f57c361947f759f466262c40a0000002100bb05f79bdb4d4917b786d9a41a156a8fb37d5949be2e7edd85abb4e8fc1fde3e"
                    )

                    sortedList.add(ongoingOperation)
                }
                else if (i == 1)
                {
                    val ongoingOperation = OngoingMultisigOperation(
                            contractAddress = "KT1Gen5CXA9Uh5TQSGKtGYAptsZEbpCz7kKX",
                            submissionDate = "25/01/1987",
                            hexaOperation = "05070707070a000000049caecab90a0000001601588317ff8c2df3024d180109239ce16c80e6f6d10007070007050805080707000302000000720a00000021007bce946147500e3945702697be1e69814e3b210a55d77a6a3f3c144b27ba941e0a0000002100af72f76635c9d2929ef294ca8a0f7aaeb3ef687f0f57c361947f759f466262c40a0000002100bb05f79bdb4d4917b786d9a41a156a8fb37d5949be2e7edd85abb4e8fc1fde3e"
                    )

                    sortedList.add(ongoingOperation)
                }
                else if (i == 2)
                {
                    // remove delegate
                    val ongoingOperation = OngoingMultisigOperation(
                            contractAddress = "KT1Gen5CXA9Uh5TQSGKtGYAptsZEbpCz7kKX",
                            submissionDate = "26/01/1987",
                            hexaOperation = "05070707070a000000049caecab90a0000001601588317ff8c2df3024d180109239ce16c80e6f6d1000707000c050805050306"
                    )

                    sortedList.add(ongoingOperation)
                }
                else if (i == 3)
                {
                    // set delegate
                    val ongoingOperation = OngoingMultisigOperation(
                            contractAddress = "KT1Gen5CXA9Uh5TQSGKtGYAptsZEbpCz7kKX",
                            submissionDate = "27/01/1987",
                            hexaOperation = "05070707070a000000049caecab90a0000001601588317ff8c2df3024d180109239ce16c80e6f6d100070700090508050505090a000000150018eaa9e67d24188b82b35d34d99afa6b0f780970"
                    )

                    sortedList.add(ongoingOperation)
                }
            }

            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                sortedList.sortWith(Comparator<Operation> { o1, o2 ->
                    when {

                        Date.from(Instant.parse(o1.timestamp)) > Date.from(Instant.parse(o2.timestamp)) -> -1
                        Date.from(Instant.parse(o1.timestamp)) == Date.from(Instant.parse(o2.timestamp)) -> 0
                        else -> 1
                    }
                })
            }
            */


            if (mOngoingMultisigItems == null)
            {
                mOngoingMultisigItems = ArrayList()
            }
            mOngoingMultisigItems?.clear()

            //mOngoingMultisigItems?.addAll(sortedList.subList(0, minOf(10, sortedList.size)))
            mOngoingMultisigItems?.addAll(sortedList)


            val lastOperation = sortedList[0]

            //TODO put that
            ongoing_contract_textview.text = lastOperation?.contractAddress

            ongoing_submission_date_textview.text = lastOperation?.submissionDate

            val binaryReader = MultisigBinaries(lastOperation.hexaOperation)
            binaryReader.getType()

            ongoing_type_textview.text = binaryReader.getOperationTypeString()

            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                operation_date_textview.text = mDateFormat.format(Date.from(Instant.parse(lastOperation.timestamp)))
            }
            */
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        val bundles = itemsToBundles(mRecyclerViewItems)
        outState.putParcelableArrayList(OPERATIONS_ARRAYLIST_KEY, bundles)

        val ongoingBundles = ongoingItemsToBundles(mOngoingMultisigItems)
        outState.putParcelableArrayList(ONGOING_OPERATIONS_ARRAYLIST_KEY, ongoingBundles)

        mBalanceItem?.let {
            outState.putDouble(BALANCE_FLOAT_KEY, it)
        }

        outState.putBoolean(GET_OPERATIONS_LOADING_KEY, mGetHistoryLoading)
        outState.putBoolean(GET_BALANCE_LOADING_KEY, mGetBalanceLoading)
        outState.putBoolean(GET_MULTISIG_ON_GOING_LOADING_KEY, mGetMultisigOnGoing)

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

    private fun bundlesToOngoingItems( bundles:ArrayList<Bundle>?): ArrayList<OngoingMultisigOperation>?
    {
        if (bundles != null)
        {
            var items = ArrayList<OngoingMultisigOperation>(bundles.size)
            if (bundles.isNotEmpty())
            {
                bundles.forEach {
                    val op = fromBundle(it)
                    items.add(op)
                }
            }
            return items
        }

        return ArrayList()
    }

    private fun ongoingItemsToBundles(items:ArrayList<OngoingMultisigOperation>?):ArrayList<Bundle>?
    {
        if (items != null)
        {
            val bundles = ArrayList<Bundle>(items.size)
            if (items.isNotEmpty())
            {
                items.forEach {
                    bundles.add(toBundle(it))
                }
            }
            return bundles
        }
        return null
    }


    private fun itemsToBundles(items:ArrayList<Operation>?):ArrayList<Bundle>?
    {
        if (items != null)
        {
            val bundles = ArrayList<Bundle>(items.size)
            if (items.isNotEmpty())
            {
                items.forEach {
                    bundles.add(it.toBundle())
                }
            }
            return bundles
        }
        return null
    }


    private fun cancelRequest(operations: Boolean, balance:Boolean, multisigOnGoing:Boolean)
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

            if (multisigOnGoing)
            {
                requestQueue.cancelAll(LOAD_MULTISIG_ONGOING_TAG)
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
        cancelRequest(operations = true, balance = true, multisigOnGoing = true)
    }
}