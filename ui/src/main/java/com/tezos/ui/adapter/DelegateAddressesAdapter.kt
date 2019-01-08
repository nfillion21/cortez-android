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

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.tezos.core.models.Address
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import java.util.*

/**
 * Created by nfillion on 29/02/16.
 */

class DelegateAddressesAdapter(private val mContext: Context, private val mCustomTheme: CustomTheme) : RecyclerView.Adapter<DelegateAddressesAdapter.ViewHolder>()
{
    private val mResources: Resources
    private val mLayoutInflater: LayoutInflater
    private val mAddresses: MutableList<Address>

    private var mOnItemClickListener: OnItemClickListener? = null
    private var mOnItemLongClickListener: OnItemLongClickListener? = null

    interface OnItemClickListener
    {
        fun onClick(view: View, paymentProduct: Address)
    }

    interface OnItemLongClickListener
    {
        fun onLongClick(view: View, address: Address)
    }

    init
    {
        mResources = mContext.resources
        mLayoutInflater = LayoutInflater.from(mContext)

        mAddresses = ArrayList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        return ViewHolder(mLayoutInflater
                .inflate(R.layout.item_payment_account, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val address = mAddresses[holder.adapterPosition]

        holder.pubKeyHash.text = address.pubKeyHash

        holder.title.text = address.description
        holder.title.setTextColor(getColor(mCustomTheme.textColorPrimaryId))
        holder.title.setBackgroundColor(getColor(mCustomTheme.colorPrimaryId))

        holder.itemView.setBackgroundColor(getColor(android.R.color.background_light))

        holder.itemView.setOnClickListener { v: View -> mOnItemClickListener!!.onClick(v, getItem(holder.adapterPosition)) }

        holder.itemView.setOnLongClickListener { view ->
            mOnItemLongClickListener!!.onLongClick(view, getItem(holder.adapterPosition))
            false
        }
    }

    override fun getItemCount(): Int
    {
        return mAddresses.size
    }

    fun getItem(position: Int): Address
    {
        return mAddresses[position]
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener)
    {
        mOnItemClickListener = onItemClickListener
    }

    fun updateAddresses(addresses: List<Address>?)
    {
        mAddresses.clear()
        if (addresses != null && !addresses.isEmpty())
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

    class ViewHolder(container: View) : RecyclerView.ViewHolder(container)
    {
        val title: TextView = container.findViewById(R.id.payment_account_title)
        val pubKeyHash: TextView = container.findViewById(R.id.src_payment_account_pub_key_hash)
    }
}