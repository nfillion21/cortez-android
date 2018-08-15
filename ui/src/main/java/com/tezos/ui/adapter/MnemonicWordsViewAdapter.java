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
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tezos.ui.R;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by nfillion on 6/27/18.
 */

public class MnemonicWordsViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    // The user view type.
    private static final int WORD_ITEM_VIEW_TYPE = 0;

    // The list of message items.
    private OnItemClickListener mOnItemClickListener;
    private LayoutInflater mLayoutInflater;

    private Activity mActivity;

    private List<String> mWords;
    private List<Integer> mOrder;

    public interface OnItemClickListener
    {
        void onClick(View view, int position);
    }

    public MnemonicWordsViewAdapter(Activity activity)
    {
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());
        mActivity = activity;
    }

    /**
     * The {@link WordItemViewHolder} class.
     * Provides a reference to each view in the message item view.
     */
    static class WordItemViewHolder extends RecyclerView.ViewHolder
    {
        final TextView wordNumberItem;
        final TextView wordItem;

        WordItemViewHolder(View view)
        {
            super(view);
            wordNumberItem = view.findViewById(R.id.word_item_info);
            wordItem = view.findViewById(R.id.word_item);
        }
    }

    @Override
    public int getItemCount()
    {
        // always 24 words.
        return mWords.size();
    }

    public List<String> getWords()
    {
        return mWords;
    }

    public boolean isFull()
    {
        boolean isFull = false;

        List<String> words = getWords();
        if (words != null && !words.isEmpty())
        {
            isFull = !words.contains(null);
        }

        return isFull;
    }

    public void updateWords(List<String> words, List<Integer> order)
    {
        if (order != null)
        {
            mOrder = order;
        }

        if (mWords == null)
        {
            mWords = new ArrayList<>(words.size());
        }
        mWords.clear();
        mWords.addAll(words);
        notifyDataSetChanged();
    }

    public void updateWord(String word, int position)
    {
        //if we got a mOrder position,
        // we need a tool to find the real position after that.
        int cardPosition = position;

        if (mOrder != null)
        {
            cardPosition = mOrder.indexOf(position);
        }

        mWords.set(cardPosition, word);
        notifyDataSetChanged();
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
        return WORD_ITEM_VIEW_TYPE;
    }

    /**
     * Creates a new view for a message item view or a banner ad view
     * based on the viewType. This method is invoked by the layout manager.
     */
    @NonNull
    @Override
    public WordItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new WordItemViewHolder(mLayoutInflater
                .inflate(R.layout.item_word, parent, false));
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
            case WORD_ITEM_VIEW_TYPE:
            {
                WordItemViewHolder wordItemHolder = (WordItemViewHolder) holder;

                String wordItem = getItem(position);
                wordItemHolder.wordItem.setText(wordItem);


                int wordNumberInt;
                if (mOrder != null)
                {
                    wordNumberInt = mOrder.get(position);
                }
                else
                {
                    wordNumberInt = position;
                }

                int finalWordNumberInt = wordNumberInt;
                holder.itemView.setOnClickListener(v ->
                        mOnItemClickListener.onClick(v, finalWordNumberInt)
                );

                String wordNumber = String.format(mActivity.getString(R.string.word_info), ++wordNumberInt);
                wordItemHolder.wordNumberItem.setText(wordNumber);

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
