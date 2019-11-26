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

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.DataExtractor
import com.tezos.ui.R
import com.tezos.ui.adapter.DelegateAddressesAdapter
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.VolleySingleton
import com.tezos.ui.widget.OffsetDecoration
import kotlinx.android.synthetic.main.fragment_delegation.*
import org.json.JSONArray


/**
 * Created by nfillion on 03/12/18.
 */

class ContractsFragment : Fragment(), DelegateAddressesAdapter.OnItemClickListener
{
    private var mCallback: OnDelegateAddressSelectedListener? = null

    private var mAdapter: DelegateAddressesAdapter? = null

    private var mGetDelegatedAddressesLoading:Boolean = false

    private var mWalletEnabled:Boolean = false

    private var mRecyclerViewAddresses:ArrayList<String>? = null

    companion object
    {
        private const val PUBLIC_KEY = "publicKeyTag"
        private const val LOAD_DELEGATED_ADDRESSES_TAG = "load_delegated_addresses_tag"

        private const val GET_DELEGATED_ADDRESSES_LOADING_KEY = "get_delegated_addresses_loading"

        private const val WALLET_AVAILABLE_KEY = "wallet_available_key"

        private const val DELEGATED_ADDRESSES_ARRAYLIST_KEY = "delegated_addresses_arraylist_key"

        fun newInstance(customTheme: CustomTheme, pkh: String?): ContractsFragment
        {
            val fragment = ContractsFragment()

            val bundle = Bundle()
            bundle.putBundle(CustomTheme.TAG, customTheme.toBundle())

            if (pkh != null)
            {
                bundle.putString(PUBLIC_KEY, pkh)
            }

            fragment.arguments = bundle
            return fragment
        }
    }

    interface OnDelegateAddressSelectedListener
    {
        fun onDelegateAddressClicked(address: String, pos: Int)
        fun showSnackBar(res:String, color:Int, textColor:Int)
    }

    override fun onAttach(context: Context?)
    {
        super.onAttach(context)

        try
        {
            mCallback = context as OnDelegateAddressSelectedListener?
        }
        catch (e: ClassCastException)
        {
            throw ClassCastException(context!!.toString() + " must implement onDelegateAddressSelectedListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_delegation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh_layout.setOnRefreshListener {
            startGetRequestLoadDelegatedAddresses()
        }

        setUpAccountGrid()

        var pkh:String? = pkh()

        if (pkh == null)
        {
            cancelRequest()
            mGetDelegatedAddressesLoading = false


            //no_delegates_text_layout.visibility = View.GONE
            //swipe_refresh_layout.isEnabled = false
        }
        else
        {
            //no_delegates_text_layout.visibility = View.VISIBLE
            //swipe_refresh_layout.isEnabled = true
        }

        addresses_recyclerview_layout.visibility = View.GONE
        empty_nested_scrollview.visibility = View.GONE

        if (savedInstanceState != null)
        {
            mGetDelegatedAddressesLoading = savedInstanceState.getBoolean(GET_DELEGATED_ADDRESSES_LOADING_KEY)

            mWalletEnabled = savedInstanceState.getBoolean(WALLET_AVAILABLE_KEY, false)

            mRecyclerViewAddresses = savedInstanceState.getStringArrayList(DELEGATED_ADDRESSES_ARRAYLIST_KEY)

            if (mGetDelegatedAddressesLoading)
            {
                mWalletEnabled = true
                refreshTextUnderDelegation(false)

                startInitialLoadingDelegatedAddresses()
            }
            else
            {
                onDelegatedAddressesComplete(false)
            }
        }
        else
        {
            //mAddressList = ArrayList()
            //TODO we will start loading only if we got a pkh
            if (pkh != null)
            {
                mWalletEnabled = true
                startInitialLoadingDelegatedAddresses()
            }
        }
    }

    private fun setUpAccountGrid()
    {
        val spacing = context!!.resources
                .getDimensionPixelSize(R.dimen.spacing_nano)
        addresses_recyclerview.addItemDecoration(OffsetDecoration(spacing))

        val args = arguments
        val customThemeBundle = args?.getBundle(CustomTheme.TAG)
        val customTheme = CustomTheme.fromBundle(customThemeBundle)

        mAdapter = DelegateAddressesAdapter(activity!!, customTheme)
        mAdapter!!.setOnItemClickListener(this)

        addresses_recyclerview.adapter = mAdapter
    }

    private fun refreshTextUnderDelegation(animating:Boolean)
    {
        //this method handles the data and loading texts

        if (mRecyclerViewAddresses != null && mRecyclerViewAddresses!!.size >= 0)
        {
            if (mRecyclerViewAddresses!!.size == 0)
            {
                empty_nested_scrollview?.visibility = View.VISIBLE
                no_delegates_layout?.visibility = View.VISIBLE

                addresses_recyclerview_layout?.visibility = View.GONE
                cannot_delegate_layout?.visibility = View.GONE
            }
            else
            {
                // show recyclerview layout
                addresses_recyclerview_layout?.visibility = View.VISIBLE

                // hide nested scrollview
                empty_nested_scrollview?.visibility = View.GONE
                no_delegates_layout?.visibility = View.GONE

                mAdapter?.updateAddresses(mRecyclerViewAddresses)
            }

            if (!animating)
            {
                //no_delegates_text_layout.text = mBalanceItem.toString()

                //reloadList()
            }

            empty_loading_textview?.visibility = View.GONE
            empty_loading_textview?.text = null
        }
        else
        {
            // mAddressList is null then just show "-"

            //no_delegates_text_layout.visibility = View.GONE

            empty_loading_textview?.visibility = View.VISIBLE
            empty_loading_textview?.text = "-"
        }
    }

    private fun reloadList()
    {
        if (mRecyclerViewAddresses != null)
        {
            mAdapter!!.updateAddresses(mRecyclerViewAddresses)
            //mRecyclerViewAddresses!!.clear()
        }
        else
        {
            mRecyclerViewAddresses = ArrayList()

            addresses_recyclerview_layout?.visibility = View.GONE
            empty_nested_scrollview?.visibility = View.VISIBLE

            no_delegates_layout?.visibility = View.VISIBLE
            cannot_delegate_layout?.visibility = View.GONE
        }
    }

    override fun onResume()
    {
        super.onResume()

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            if (!mWalletEnabled)
            {
                mWalletEnabled = true

                startInitialLoadingDelegatedAddresses()
            }
        }
        else
        {
            mRecyclerViewAddresses = null
            refreshTextUnderDelegation(false)

            cancelRequest()

            mWalletEnabled = false

            addresses_recyclerview_layout.visibility = View.GONE
            empty_nested_scrollview.visibility = View.VISIBLE

            cannot_delegate_layout.visibility = View.VISIBLE
            no_delegates_layout.visibility = View.GONE

            swipe_refresh_layout.isEnabled = false
            swipe_refresh_layout.isRefreshing = false
        }
    }

    //requests
    private fun startInitialLoadingDelegatedAddresses()
    {
        swipe_refresh_layout.isEnabled = false

        startGetRequestLoadDelegatedAddresses()
    }

    private fun onDelegatedAddressesComplete(animating:Boolean)
    {
        mGetDelegatedAddressesLoading = false
        nav_progress?.visibility = View.GONE

        swipe_refresh_layout?.isEnabled = true
        swipe_refresh_layout?.isRefreshing = false

        refreshTextUnderDelegation(animating)
    }

    // volley
    private fun startGetRequestLoadDelegatedAddresses()
    {
        cancelRequest()

        mGetDelegatedAddressesLoading = true

        empty_loading_textview.setText(R.string.loading_contracts)

        nav_progress.visibility = View.VISIBLE

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.contracts_url), pkh)

            // Request a string response from the provided URL.
            val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null, Response.Listener<JSONArray>
            {
                if (activity != null)
                {
                    addContractAddressesFromJSON(it, pkh)

                    reloadList()
                    onDelegatedAddressesComplete(true)
                }
            },
                    Response.ErrorListener {

                        if (activity != null)
                        {
                            onDelegatedAddressesComplete(false)

                            showSnackbarError(it)
                        }
                    })

            jsonArrayRequest.tag = LOAD_DELEGATED_ADDRESSES_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(jsonArrayRequest)
        }
    }

    private fun addContractAddressesFromJSON(answer: JSONArray, pkh: String?)
    {
        val contracts = DataExtractor.getJSONArrayFromField(answer,0)

        if (contracts != null && contracts.length() > 0)
        {
            var contractsList = arrayListOf<String>()

            for (i in 0..(contracts.length() - 1))
            {
                val item = contracts.getString(i)
                if (item != pkh)
                {
                    contractsList.add(item)
                }
            }

            if (mRecyclerViewAddresses == null)
            {
                mRecyclerViewAddresses = ArrayList()
            }
            else
            {
                mRecyclerViewAddresses?.clear()
            }
            mRecyclerViewAddresses?.addAll(contractsList)
        }
    }

    private fun showSnackbarError(error: VolleyError?)
    {
        var err: String? = error?.toString() ?: getString(R.string.generic_error)

        mCallback?.showSnackBar(err!!, ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))

        empty_loading_textview?.text = getString(R.string.generic_error)
    }

    private fun cancelRequest()
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
        requestQueue?.cancelAll(LOAD_DELEGATED_ADDRESSES_TAG)
    }

    fun pkh():String?
    {
        var pkh:String? = null

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            val seed = Storage(activity!!).getMnemonics()
            pkh = seed.pkh
        }

        return pkh
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(GET_DELEGATED_ADDRESSES_LOADING_KEY, mGetDelegatedAddressesLoading)
        outState.putBoolean(WALLET_AVAILABLE_KEY, mWalletEnabled)
        outState.putStringArrayList(DELEGATED_ADDRESSES_ARRAYLIST_KEY, mRecyclerViewAddresses)
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequest()
    }

    override fun onDetach()
    {
        super.onDetach()
        cancelRequest()
        mCallback = null
    }

    override fun onClick(view: View, address: String, pos:Int)
    {
        if (mCallback != null)
        {
            mCallback!!.onDelegateAddressClicked(address, pos)
        }
    }
}