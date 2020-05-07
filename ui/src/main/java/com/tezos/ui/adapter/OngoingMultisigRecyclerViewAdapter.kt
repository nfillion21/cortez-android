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
/*
 * Created by nfillion on 7/12/18.
 */
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tezos.core.utils.MultisigBinaries
import com.tezos.ui.R
import com.tezos.ui.fragment.HomeFragment.OngoingMultisigOperation

/**
 * The [OngoingMultisigRecyclerViewAdapter] class.
 *
 * The adapter provides access to the items in the [OperationItemViewHolder]
 */
class OngoingMultisigRecyclerViewAdapter constructor(ongoingOperationItems: List<OngoingMultisigOperation>, ongoingOperationForNotaryItems: List<OngoingMultisigOperation>) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    // The list of message items.
    private val mOngoingOperationItems: List<OngoingMultisigOperation> = ongoingOperationItems
    private val mOngoingOperationForNotaryItems: List<OngoingMultisigOperation> = ongoingOperationForNotaryItems

    private var mOnItemClickListener: OnItemClickListener? = null

    companion object
    {
        // The operation view type.
        private const val HEADER_OPERATION_ITEM_VIEW_TYPE: Int = 0
        private const val OPERATION_ITEM_VIEW_TYPE: Int = 1

        private const val HEADER_NOTARY_ITEM_VIEW_TYPE: Int = 2
        private const val NOTARY_OPERATION_ITEM_VIEW_TYPE: Int = 3
    }

    open interface OnItemClickListener
    {
        open fun onOperationSelected(view: View?, operation: OngoingMultisigOperation?, isFromNotary:Boolean): Unit
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?): Unit
    {
        mOnItemClickListener = onItemClickListener
    }

    override fun getItemCount(): Int
    {
        var count = 0

        if (mOngoingOperationItems.isNotEmpty())
        {
            count += mOngoingOperationItems.size + 1
        }

        if (mOngoingOperationForNotaryItems.isNotEmpty())
        {
            count += mOngoingOperationForNotaryItems.size + 1
        }

        return count
    }

    /**
     * Determines the view type for the given position.
     */
    override fun getItemViewType(position: Int): Int
    {

        when (position)
        {
            0 ->
            {
                return if (mOngoingOperationItems.isNotEmpty())
                    HEADER_OPERATION_ITEM_VIEW_TYPE else
                    HEADER_NOTARY_ITEM_VIEW_TYPE
            }
        }

        if (mOngoingOperationItems.isNotEmpty())
        {
            return if (position <= mOngoingOperationItems.size)
            {
                OPERATION_ITEM_VIEW_TYPE
            }
            else
            {
                if (position == mOngoingOperationItems.size + 1)
                {
                    HEADER_NOTARY_ITEM_VIEW_TYPE
                }
                else
                {
                    NOTARY_OPERATION_ITEM_VIEW_TYPE
                }
            }
        }
        else
        {
            return NOTARY_OPERATION_ITEM_VIEW_TYPE
        }

        return -1
    }

    /**
     * Creates a new view for a message item view or a banner ad view
     * based on the viewType. This method is invoked by the layout manager.
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        return when (viewType)
        {
            OPERATION_ITEM_VIEW_TYPE -> OperationItemViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_container_ongoing_multisig, viewGroup, false))
            NOTARY_OPERATION_ITEM_VIEW_TYPE -> ForNotaryOperationItemViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_container_ongoing_multisig, viewGroup, false))

            HEADER_OPERATION_ITEM_VIEW_TYPE -> OperationsHeaderViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_header, viewGroup, false))
            HEADER_NOTARY_ITEM_VIEW_TYPE -> OperationsForNotaryHeaderViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_header, viewGroup, false))

            else -> OperationsForNotaryHeaderViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_header, viewGroup, false))
        }
    }

    private inner class OperationsForNotaryHeaderViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        internal var headerTextView: TextView = itemView.findViewById(R.id.signatories_contracts_textview)
        internal fun bind()
        {
            headerTextView.text = itemView.context.getString(R.string.ongoing_contract_for_notary)
        }
    }

    private inner class OperationsHeaderViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        internal var headerTextView: TextView = itemView.findViewById(R.id.signatories_contracts_textview)
        internal fun bind()
        {
            headerTextView.text = itemView.context.getString(R.string.ongoing_contract_operations)
        }
    }

    internal inner class OperationItemViewHolder constructor(view: View) : RecyclerView.ViewHolder(view)
    {
        private val submissionDateItem: TextView = view.findViewById(R.id.submission_item_date)
        private val contractAddressItem: TextView = view.findViewById(R.id.contract_address_item)
        private val operationTypeItem: TextView = view.findViewById(R.id.operation_type_item)

        internal fun bind(position: Int)
        {
            val operationItem: OngoingMultisigOperation = mOngoingOperationItems[position - 1]

            contractAddressItem.text = operationItem.contractAddress
            submissionDateItem.text = operationItem.submissionDate

            val binaryReader = MultisigBinaries(operationItem.hexaOperation)
            binaryReader.getType()
            operationTypeItem.text = binaryReader.getOperationTypeString()

            itemView.setOnClickListener { view: View? -> mOnItemClickListener?.onOperationSelected(view, operationItem, isFromNotary = false) }
        }
    }


    internal inner class ForNotaryOperationItemViewHolder constructor(view: View) : RecyclerView.ViewHolder(view)
    {
        private val submissionDateItem: TextView = view.findViewById(R.id.submission_item_date)
        private val contractAddressItem: TextView = view.findViewById(R.id.contract_address_item)
        private val operationTypeItem: TextView = view.findViewById(R.id.operation_type_item)

        internal fun bind(position: Int)
        {
            var count = 0
            if (mOngoingOperationItems.isNotEmpty())
                count += mOngoingOperationItems.size + 1

            val operationItem: OngoingMultisigOperation = mOngoingOperationForNotaryItems[position - count - 1]

            contractAddressItem.text = operationItem.contractAddress
            submissionDateItem.text = operationItem.submissionDate

            val binaryReader = MultisigBinaries(operationItem.hexaOperation)
            binaryReader.getType()
            operationTypeItem.text = binaryReader.getOperationTypeString()

            itemView.setOnClickListener {
                view: View? ->
                run {
                    mOnItemClickListener?.onOperationSelected(view, operationItem, isFromNotary = true)
                }
            }
        }
    }

    /**
     * Replaces the content in the views that make up the message item view and the
     * banner ad view. This method is invoked by the layout manager.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit
    {
        when (holder)
        {
            is OperationItemViewHolder -> holder.bind(position)
            is ForNotaryOperationItemViewHolder -> holder.bind(position)
            is OperationsForNotaryHeaderViewHolder -> holder.bind()
            is OperationsHeaderViewHolder -> holder.bind()
        }
    }
}