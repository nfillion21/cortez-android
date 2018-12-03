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
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.AddressesDatabase
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.adapter.DelegateAddressesAdapter
import com.tezos.ui.widget.OffsetDecoration
import java.util.*


/**
 * Created by nfillion on 03/12/18.
 */

class DelegationFragmentBis : Fragment(), DelegateAddressesAdapter.OnItemClickListener
{
    private var mCallback: OnDelegateAddressSelectedListener? = null

    private var mAdapter: DelegateAddressesAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private var mNestedScrollview: NestedScrollView? = null

    private var mAddressList: MutableList<Address>? = null

    companion object {
        private val ADDRESSES_ARRAYLIST = "addressList"

        private val PUBLIC_KEY = "publicKeyTag"

        fun newInstance(customTheme: CustomTheme, pkh: String): DelegationFragmentBis
        {
            val fragment = DelegationFragmentBis()

            val bundle = Bundle()
            bundle.putBundle(CustomTheme.TAG, customTheme.toBundle())
            bundle.putString(PUBLIC_KEY, pkh)

            fragment.arguments = bundle
            return fragment
        }
    }

    interface OnDelegateAddressSelectedListener
    {
        fun onDelegateAddressClicked(address: Address)
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

        if (savedInstanceState != null)
        {
            val messagesBundle = savedInstanceState.getParcelableArrayList<Bundle>(ADDRESSES_ARRAYLIST)
            mAddressList = bundlesToItems(messagesBundle)
        }
        else
        {
            mAddressList = ArrayList()
        }

        mNestedScrollview = view!!.findViewById(R.id.empty_nested_scrollview)

        mRecyclerView = view.findViewById(R.id.addresses_recyclerview)
        setUpAccountGrid(mRecyclerView!!)
    }

    private fun setUpAccountGrid(categoriesView: RecyclerView)
    {
        val spacing = context!!.resources
                .getDimensionPixelSize(R.dimen.spacing_nano)
        categoriesView.addItemDecoration(OffsetDecoration(spacing))

        val args = arguments
        val customThemeBundle = args?.getBundle(CustomTheme.TAG)
        val customTheme = CustomTheme.fromBundle(customThemeBundle)

        mAdapter = DelegateAddressesAdapter(activity!!, customTheme)
        mAdapter!!.setOnItemClickListener(this)

        categoriesView.adapter = mAdapter

        reloadList()
    }

    fun reloadList()
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

            mRecyclerView!!.visibility = View.VISIBLE
            mNestedScrollview!!.visibility = View.GONE
        }
        else
        {
            mRecyclerView!!.visibility = View.GONE
            mNestedScrollview!!.visibility = View.VISIBLE
        }
    }

    override fun onResume()
    {
        super.onResume()
        reloadList()
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

    override fun onClick(view: View, address: Address)
    {
        if (mCallback != null)
        {
            mCallback!!.onDelegateAddressClicked(address)
        }
    }
}