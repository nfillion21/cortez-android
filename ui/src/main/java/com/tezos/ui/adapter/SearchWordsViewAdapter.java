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