package com.tezos.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.tezos.ui.R;
import com.tezos.ui.adapter.SearchWordsViewAdapter;
import com.tezos.ui.widget.OffsetDecoration;

/**
 * Created by nfillion on 3/9/18.
 */

public class SearchWordDialogFragment extends DialogFragment implements SearchWordsViewAdapter.OnItemClickListener
{
    private OnSearchWordSelectedListener mCallback;

    private RecyclerView mRecyclerView;
    private SearchWordsViewAdapter mAdapter;

    public interface OnSearchWordSelectedListener
    {
        void onSearchWordClicked(String word);
    }

    public static SearchWordDialogFragment newInstance()
    {
        return new SearchWordDialogFragment();
        //Put the theme here
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try
        {
            mCallback = (OnSearchWordSelectedListener) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString()
                    + " must implement OnSearchWordSelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (savedInstanceState == null)
        {
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_search_word, null);
        mRecyclerView = dialogView.findViewById(R.id.words_recyclerview);
        final int spacing = getContext().getResources()
                .getDimensionPixelSize(R.dimen.spacing_nano);
        mRecyclerView.addItemDecoration(new OffsetDecoration(spacing));

        setUpSearchWordGrid();
        builder.setView(dialogView);

        return builder.create();
    }

    private void setUpSearchWordGrid()
    {
        //mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        mAdapter = new SearchWordsViewAdapter(this.getActivity());
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
    }


    @Override
    public void onClick(View view, String word)
    {
        getDialog().dismiss();
        if (mCallback != null)
        {
            mCallback.onSearchWordClicked(word);
        }
    }
}
