package com.tezos.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.tezos.core.database.EnglishWordsProvider;
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

    private TextInputEditText mSearchWordEditText;

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

        mSearchWordEditText = dialogView.findViewById(R.id.search_word_edittext);
        mSearchWordEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // database change.
                Log.i("database change", "database change");
            }
        });

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
        showResults("b");
        //getDialog().dismiss();
        if (mCallback != null)
        {
            mCallback.onSearchWordClicked(word);
        }
    }

    private void showResults(String query) {

        Cursor cursor = getActivity().managedQuery(EnglishWordsProvider.CONTENT_URI, null, null,
                new String[] {query}, null);

        if (cursor == null) {
            // There are no results
            //mTextView.setText(getString(R.string.no_results, new Object[] {query}));
        } else {
            /*
            // Display the number of results
            int count = cursor.getCount();
            String countString = getResources().getQuantityString(R.plurals.search_results,
                    count, new Object[] {count, query});
            //mTextView.setText(countString);

            // Specify the columns we want to display in the result
            String[] from = new String[] {
                    EnglishWordsDatabase.COL_WORD
            };

            // Specify the corresponding layout elements where we want the columns to go
            int[] to = new int[] { R.id.word,
                    R.id.definition };

            // Create a simple cursor adapter for the definitions and apply them to the ListView
            SimpleCursorAdapter words = new SimpleCursorAdapter(this,
                    R.layout.result, cursor, from, to);
            mListView.setAdapter(words);

            // Define the on-click listener for the list items
            mListView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Build the Intent used to open WordActivity with a specific word Uri
                    Intent wordIntent = new Intent(getApplicationContext(), WordActivity.class);
                    Uri data = Uri.withAppendedPath(DictionaryProvider.CONTENT_URI,
                            String.valueOf(id));
                    wordIntent.setData(data);
                    startActivity(wordIntent);
                }
            });
        */
        }
    }
}
