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

package com.tezos.ui.adapter;

/*
 * Created by nfillion on 7/12/18.
 */

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tezos.core.models.Operation;
import com.tezos.ui.R;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * The {@link OperationRecyclerViewAdapter} class.
 * <p>The adapter provides access to the items in the {@link OperationItemViewHolder}
 */
public class OperationRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    // The operation view type.
    private static final int OPERATION_ITEM_VIEW_TYPE = 0;

    // The list of message items.
    private final List<Operation> mRecyclerViewItems;

    private final DateFormat mDateFormat;

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener
    {
        void onOperationSelected(View view, Operation operation);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener)
    {
        mOnItemClickListener = onItemClickListener;
    }

    /**
     * For this example app, the recyclerViewItems list contains only
     * {@link Operation} type.
     */
    public OperationRecyclerViewAdapter(List<Operation> recyclerViewItems)
    {
        this.mRecyclerViewItems = recyclerViewItems;
        this.mDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    }

    /**
     * The {@link OperationItemViewHolder} class.
     * Provides a reference to each view in the message item view.
     */
    class OperationItemViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView itemAmount;
        private final TextView itemFee;
        private final TextView itemDate;

        OperationItemViewHolder(View view)
        {
            super(view);

            itemAmount = view.findViewById(R.id.operation_item_amount);
            itemFee = view.findViewById(R.id.operation_item_fee);
            itemDate = view.findViewById(R.id.operation_item_date);
        }
    }

    @Override
    public int getItemCount()
    {
        return mRecyclerViewItems.size();
    }

    /**
     * Determines the view type for the given position.
     */
    @Override
    public int getItemViewType(int position)
    {
        return OPERATION_ITEM_VIEW_TYPE;
    }

    /**
     * Creates a new view for a message item view or a banner ad view
     * based on the viewType. This method is invoked by the layout manager.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType)
    {
        switch (viewType)
        {
            case OPERATION_ITEM_VIEW_TYPE:
            {
                View messageItemLayoutView = LayoutInflater.from(viewGroup.getContext()).inflate(
                        R.layout.item_container_operation, viewGroup, false);
                return new OperationItemViewHolder(messageItemLayoutView);
            }

            default:
                return null;
        }
    }

    /**
     * Replaces the content in the views that make up the message item view and the
     * banner ad view. This method is invoked by the layout manager.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        int viewType = getItemViewType(position);
        switch (viewType)
        {
            case OPERATION_ITEM_VIEW_TYPE:
            {
                OperationItemViewHolder operationItemHolder = (OperationItemViewHolder) holder;
                Operation operationItem = mRecyclerViewItems.get(position);

                operationItemHolder.itemAmount.setText(operationItem.getAmount()/1000000 + "ꜩ");

                operationItemHolder.itemFee.setText(operationItem.getFee()/1000000 + "ꜩ");

                Date date = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    date = Date.from(Instant.parse(operationItem.getTimestamp()));
                    operationItemHolder.itemDate.setText(mDateFormat.format(date));
                }
                else {

                    operationItemHolder.itemDate.setText(operationItem.getTimestamp());
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        mOnItemClickListener.onOperationSelected(view, operationItem);
                    }
                });
            }
            break;

            default:
                // no-op
        }
    }
}

