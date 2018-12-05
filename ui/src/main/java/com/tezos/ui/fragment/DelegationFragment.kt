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
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.AddressesDatabase
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.adapter.DelegateAddressesAdapter
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.VolleySingleton
import com.tezos.ui.widget.OffsetDecoration
import kotlinx.android.synthetic.main.fragment_delegation.*
import java.util.*


/**
 * Created by nfillion on 03/12/18.
 */

class DelegationFragment : Fragment(), DelegateAddressesAdapter.OnItemClickListener
{
    private var mCallback: OnDelegateAddressSelectedListener? = null

    private var mAdapter: DelegateAddressesAdapter? = null

    private var mAddressList: MutableList<Address>? = null

    private var mGetDelegatedAddressesLoading:Boolean = false

    private var mAddresses:Int = 1

    private var mWalletEnabled:Boolean = false

    companion object {

        private const val PUBLIC_KEY = "publicKeyTag"
        private const val LOAD_DELEGATED_ADDRESSES_TAG = "load_delegated_addresses_tag"

        private const val GET_DELEGATED_ADDRESSES_LOADING_KEY = "get_delegated_addresses_loading"

        private const val ADDRESSES_INT_KEY = "addresses_int_item"

        private const val WALLET_AVAILABLE_KEY = "wallet_available_key"

        fun newInstance(customTheme: CustomTheme, pkh: String?): DelegationFragment
        {
            val fragment = DelegationFragment()

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
        fun onDelegateAddressClicked(address: Address)
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

        if (savedInstanceState != null)
        {
            mGetDelegatedAddressesLoading = savedInstanceState.getBoolean(GET_DELEGATED_ADDRESSES_LOADING_KEY)

            mAddresses = savedInstanceState.getInt(ADDRESSES_INT_KEY, -1)

            mWalletEnabled = savedInstanceState.getBoolean(WALLET_AVAILABLE_KEY, false)

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
            mAddressList = ArrayList()
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

        reloadList()
    }

    private fun refreshTextUnderDelegation(animating:Boolean)
    {
        if (mAddresses != -1 && mAddresses != null)
        {
            no_delegates_text_layout.visibility = View.VISIBLE
            if (!animating)
            {
                //no_delegates_text_layout.text = mBalanceItem.toString()
            }

            empty_loading_addresses_textview.visibility = View.GONE
            empty_loading_addresses_textview.text = null
        }
        else
        {
            no_delegates_text_layout.visibility = View.GONE
            empty_loading_addresses_textview.visibility = View.VISIBLE
            empty_loading_addresses_textview.text = "-"
        }
    }

    private fun reloadList()
    {
        if (mAddressList != null)
        {
            mAddressList!!.clear()
        }
        else
        {
            mAddressList = ArrayList()
        }

        val set = AddressesDatabase.getInstance().getAddresses(activity)

        if (set != null && !set.isEmpty())
        {
            for (addressString in set)
            {
                val addressBundle = Utils.fromJSONString(addressString)
                if (addressBundle != null)
                {
                    val address = Address.fromBundle(addressBundle)
                    mAddressList!!.add(address)
                }
            }

            mAdapter!!.updateAddresses(mAddressList)

            addresses_recyclerview_layout.visibility = View.VISIBLE
            empty_nested_scrollview.visibility = View.GONE
        }
        else
        {
            addresses_recyclerview_layout.visibility = View.GONE
            empty_nested_scrollview.visibility = View.VISIBLE

            no_delegates_layout.visibility = View.VISIBLE
            cannot_delegate_layout.visibility = View.GONE
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

                // put the good layers
                //mBalanceLayout?.visibility = View.VISIBLE
                //mCreateWalletLayout?.visibility = View.GONE

                startInitialLoadingDelegatedAddresses()

            }
        }
        else
        {
            mAddresses = -1
            refreshTextUnderDelegation(false)

            cancelRequest()

            if (mWalletEnabled)
            {
                mWalletEnabled = false

                // put the good layers

                // put the available layers:
                // mBalanceLayout visibility

                //val args = arguments
                //args?.putBundle(Address.TAG, null)

                //if (mRecyclerViewItems != null)
                //{
                    //mRecyclerViewItems?.clear()
                //}

                mAddresses = -1
            }

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
        nav_progress_no_delegate.visibility = View.GONE

        swipe_refresh_layout.isEnabled = true
        swipe_refresh_layout.isRefreshing = false

        refreshTextUnderDelegation(animating)
    }

    // volley
    private fun startGetRequestLoadDelegatedAddresses()
    {
        cancelRequest()

        mGetDelegatedAddressesLoading = true

        empty_loading_addresses_textview.setText(R.string.loading_delegated_addresses)

        nav_progress_no_delegate.visibility = View.VISIBLE

        val pkh = pkh()
        if (pkh != null)
        {
            val url = String.format(getString(R.string.balance_url), pkh)

            // Request a string response from the provided URL.
            val stringRequest = StringRequest(Request.Method.GET, url,
                    Response.Listener<String> { response ->

                        setUpAccountGrid()

                        animateBalance()

                        onDelegatedAddressesComplete(true)
                    },
                    Response.ErrorListener {
                        onDelegatedAddressesComplete(false)
                        showSnackbarError(it)
                    })

            stringRequest.tag = LOAD_DELEGATED_ADDRESSES_TAG
            VolleySingleton.getInstance(activity?.applicationContext).addToRequestQueue(stringRequest)
        }
    }

    private fun showSnackbarError(error: VolleyError?)
    {
        var error: String? = if (error != null)
        {
            error.toString()
        }
        else
        {
            getString(R.string.generic_error)
        }

        mCallback?.showSnackBar(error!!, ContextCompat.getColor(context!!, android.R.color.holo_red_light), ContextCompat.getColor(context!!, R.color.tz_light))

        empty_loading_addresses_textview.text = getString(R.string.generic_error)
    }

    private fun animateBalance()
    {
        val objectAnimator = ObjectAnimator.ofFloat(no_delegates_text_layout, View.ALPHA, 0f)
        objectAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                //mBalanceTextView?.text = balance.toString()
            }
        })

        val objectAnimator2 = ObjectAnimator.ofFloat(no_delegates_text_layout, View.ALPHA, 1f)

        //mBalanceTextView?.text = balance.toString()

        val animatorSet = AnimatorSet()
        animatorSet.play(objectAnimator).before(objectAnimator2)
        animatorSet.start()
    }

    private fun itemsToBundles(items: List<Address>?): ArrayList<Bundle>?
    {
        if (items != null)
        {
            val bundles = ArrayList<Bundle>(items.size)
            if (!items.isEmpty())
            {
                for (it in items)
                {
                    bundles.add(it.toBundle())
                }
            }
            return bundles
        }

        return null
    }

    private fun bundlesToItems(bundles: ArrayList<Bundle>?): ArrayList<Address>?
    {
        if (bundles != null)
        {
            val items = ArrayList<Address>(bundles.size)
            if (!bundles.isEmpty())
            {
                for (bundle in bundles)
                {
                    items.add(Address.fromBundle(bundle))
                }
            }
            return items
        }

        return null
    }

    private fun cancelRequest()
    {
        val requestQueue = VolleySingleton.getInstance(activity?.applicationContext).requestQueue
        requestQueue?.cancelAll(LOAD_DELEGATED_ADDRESSES_TAG)
    }

    fun pkh():String?
    {
        var pkh:String? = null
        arguments?.let {

            pkh = it.getString(PUBLIC_KEY)
        }

        return pkh
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putBoolean(GET_DELEGATED_ADDRESSES_LOADING_KEY, mGetDelegatedAddressesLoading)
    }

    override fun onDestroy()
    {
        super.onDestroy()
        cancelRequest()
    }

    override fun onDetach()
    {
        super.onDetach()
        mCallback = null
    }

    override fun onClick(view: View, address: Address)
    {
        if (mCallback != null)
        {
            mCallback!!.onDelegateAddressClicked(address)
        }
    }
}