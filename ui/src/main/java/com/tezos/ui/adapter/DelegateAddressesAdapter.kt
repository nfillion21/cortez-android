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

package com.tezos.ui.adapter

import android.content.Context
import android.content.res.Resources
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import java.util.*

/**
 * Created by nfillion on 29/02/16.
 */

class DelegateAddressesAdapter(private val mContext: Context, private val mCustomTheme: CustomTheme) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    private val mResources: Resources
    private val mLayoutInflater: LayoutInflater
    private val mAddresses: MutableList<String>

    private var mOnItemClickListener: OnItemClickListener? = null
    private val TYPE_ITEM = 0
    private val TYPE_HEADER = 1

    interface OnItemClickListener
    {
        fun onClick(view: View, paymentProduct: String, position: Int)
    }

    init
    {
        mResources = mContext.resources
        mLayoutInflater = LayoutInflater.from(mContext)

        mAddresses = ArrayList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        return when (viewType)
        {
            TYPE_HEADER -> HeaderViewHolder(mLayoutInflater.inflate(R.layout.item_header, parent, false))
            // other view holders...
            else -> ContractViewHolder(mLayoutInflater.inflate(R.layout.item_contract, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    {
        if (position == 1)
        {
            (holder as HeaderViewHolder).bind(position)
        }
        else
        {
            (holder as ContractViewHolder).bind(position)
        }
    }

    private inner class HeaderViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        internal var headerTextView: TextView = itemView.findViewById(R.id.signatories_contracts_textview)
        internal fun bind(position: Int)
        {
            headerTextView.text = mContext.getString(R.string.multisig_contracts_as_signatory)
        }
    }

    private inner class ContractViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        internal var titleTextView: TextView = itemView.findViewById(R.id.info_textview)
        internal var publicKeyHashTextView: TextView = itemView.findViewById(R.id.pub_key_hash_textview)
        internal fun bind(position: Int)
        {
            publicKeyHashTextView.text = mAddresses[position]
            titleTextView.text = String.format(mContext.getString(R.string.contract_address), "#${position + 1}")
            itemView.setBackgroundColor(getColor(android.R.color.background_light))
            itemView.setOnClickListener { v: View -> mOnItemClickListener!!.onClick(v, getItem(position), position+1) }
        }
    }

    override fun getItemCount(): Int
    {
        return mAddresses.size
    }

    override fun getItemViewType(position: Int): Int
    {
        return if (position == 1)
        {
            TYPE_HEADER
        }
        else
        {
            TYPE_ITEM
        }
    }

    private fun getItem(position: Int): String
    {
        return mAddresses[position]
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener)
    {
        mOnItemClickListener = onItemClickListener
    }

    fun updateAddresses(addresses: List<String>?)
    {
        mAddresses.clear()
        if (!addresses.isNullOrEmpty())
        {
            mAddresses.addAll(addresses)
        }
        notifyDataSetChanged()
    }

    /**
     * Convenience method for color loading.
     *
     * @param colorRes The resource id of the color to load.
     * @return The loaded color.
     */
    private fun getColor(@ColorRes colorRes: Int): Int
    {
        return ContextCompat.getColor(mContext, colorRes)
    }
}
