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

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tezos.ui.R;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by nfillion on 6/28/18.
 */

public class SearchWordsViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    // The user view type.
    private static final int SEARCH_WORD_ITEM_VIEW_TYPE = 0;

    // The list of message items.
    private OnItemClickListener mOnItemClickListener;
    private LayoutInflater mLayoutInflater;

    private List<String> mWords;

    public interface OnItemClickListener
    {
        void onClick(View view, String word);
    }

    public SearchWordsViewAdapter(Activity activity)
    {
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());

        mWords = new ArrayList<>();
        for (int i = 0; i < 24; i++)
        {
            mWords.add("hello");
        }
    }

    /**
     * The {@link WordItemViewHolder} class.
     * Provides a reference to each view in the message item view.
     */
    static class WordItemViewHolder extends RecyclerView.ViewHolder
    {
        final TextView wordItem;

        WordItemViewHolder(View view)
        {
            super(view);
            wordItem = view.findViewById(R.id.word_item);
        }
    }

    @Override
    public int getItemCount()
    {
        // always 24 words.
        return mWords.size();
    }

    public String getItem(int position)
    {
        return mWords.get(position);
    }

    /**
     * Determines the view type for the given position.
     */
    @Override
    public int getItemViewType(int position)
    {
        return SEARCH_WORD_ITEM_VIEW_TYPE;
    }

    /**
     * Creates a new view for a message item view or a banner ad view
     * based on the viewType. This method is invoked by the layout manager.
     */
    @NonNull
    @Override
    public WordItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new WordItemViewHolder(mLayoutInflater
                .inflate(R.layout.item_search_word, parent, false));
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
            case SEARCH_WORD_ITEM_VIEW_TYPE:
            {
                WordItemViewHolder wordItemHolder = (WordItemViewHolder) holder;

                String wordItem = getItem(position);
                wordItemHolder.wordItem.setText(wordItem);

                holder.itemView.setOnClickListener(v ->
                        mOnItemClickListener.onClick(v, getItem(holder.getAdapterPosition()))
                );
            }
            break;

            default:
                // no-op
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener)
    {
        mOnItemClickListener = onItemClickListener;
    }
}
