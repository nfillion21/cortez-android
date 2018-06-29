package com.tezos.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.tezos.core.database.EnglishWordsContentProvider;
import com.tezos.core.database.EnglishWordsDatabaseConstants;
import com.tezos.ui.R;

/**
 * Created by nfillion on 3/9/18.
 */

public class SearchWordDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks
{
    private TextInputEditText mSearchWordEditText;

    private CursorAdapter mCursorAdapter;

    private ListView mList;

    private static final int LOADER_ID = 42;

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
            //mCallback = (OnSearchWordSelectedListener) context;
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

        mCursorAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.item_search_word, null,
                new String[] {EnglishWordsDatabaseConstants.COL_WORD},
                new int[] { R.id.word_item });

        mList = dialogView.findViewById(R.id.list);
        mList.setAdapter(mCursorAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });

        getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        builder.setView(dialogView);

        return builder.create();
    }

    @NonNull
    @Override
    public Loader onCreateLoader(int id, @Nullable Bundle bundle) {
        if (id != LOADER_ID) {
            return null;
        }
        return new CursorLoader(getActivity(),
                EnglishWordsContentProvider.CONTENT_URI,
                new String[] { EnglishWordsDatabaseConstants.COL_ID, EnglishWordsDatabaseConstants.COL_WORD }, null, null,
                null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader loader, Object o)
    {
        if (o != null && o instanceof Cursor)
        {
            Cursor cursor = (Cursor)o;
            if (cursor.moveToFirst()) // data?
            {

                int count = cursor.getCount();
                String word = cursor.getString(cursor.getColumnIndex("word"));
                String word2 = cursor.getString(cursor.getColumnIndex("word"));
                //System.out.println(cursor.getString(cursor.getColumnIndex("word")));
            }
            mCursorAdapter.swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader)
    {
        mCursorAdapter.swapCursor(null);
    }
}
