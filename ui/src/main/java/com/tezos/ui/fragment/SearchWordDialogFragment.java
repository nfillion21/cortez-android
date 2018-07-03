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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;

import com.tezos.core.database.EnglishWordsContentProvider;
import com.tezos.core.database.EnglishWordsDatabaseConstants;
import com.tezos.ui.R;

/**
 * Created by nfillion on 3/9/18.
 */

public class SearchWordDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks
{

    public static final String CARD_POSITION_KEY = "card_position";

    private OnSearchWordSelectedListener mCallback;
    private TextInputEditText mSearchWordEditText;

    private CursorAdapter mCursorAdapter;

    private ListView mList;

    private static final int LOADER_ID = 42;

    public interface OnSearchWordSelectedListener
    {
        void onSearchWordClicked(String word, int position);
    }

    public static SearchWordDialogFragment newInstance(int cardPosition)
    {
        SearchWordDialogFragment fragment = new SearchWordDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(CARD_POSITION_KEY, cardPosition);

        fragment.setArguments(bundle);
        return fragment;
        //Put the theme here

        //TODO put here the number
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_search_word, null);

        mSearchWordEditText = dialogView.findViewById(R.id.search_word_edittext);

        int position = getArguments().getInt(CARD_POSITION_KEY);
        int cardPos = position + 1;
        if (position != -1)
        {
            mSearchWordEditText.setHint("Word #" + cardPos);
        }

        mSearchWordEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable)
            {

                String query = editable.toString();

                Bundle queryBundle = null;
                if (!query.isEmpty())
                {
                    queryBundle = new Bundle();
                    queryBundle.putString("query", query);
                }
                getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, queryBundle, SearchWordDialogFragment.this);
            }
        });

        mCursorAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.item_search_word, null,
                new String[] {EnglishWordsDatabaseConstants.COL_WORD},
                new int[] { R.id.word_item });

        mList = dialogView.findViewById(R.id.list);
        mList.setAdapter(mCursorAdapter);

        mList.setOnItemClickListener((adapterView, view, i, l) ->
        {
            Cursor cursor = mCursorAdapter.getCursor();
            cursor.moveToPosition(i);
            String item = cursor.getString(cursor.getColumnIndex(EnglishWordsDatabaseConstants.COL_WORD));

            mCallback.onSearchWordClicked(item, position);

            //getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, SearchWordDialogFragment.this);
            getDialog().dismiss();
        });

        if (savedInstanceState == null)
        {
            getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        }

        builder.setView(dialogView);

        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @NonNull
    @Override
    public Loader onCreateLoader(int id, @Nullable Bundle bundle)
    {
        if (id == LOADER_ID)
        {
            String query = null;
            if (bundle != null)
            {
                query = bundle.getString("query");
            }

            // it filters everything when I put null as a parameter
            return new CursorLoader(getActivity(),
                    EnglishWordsContentProvider.CONTENT_URI,
                    new String[] { EnglishWordsDatabaseConstants.COL_ID, EnglishWordsDatabaseConstants.COL_WORD }, null, new String[]{query},
                    null);
        }

        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader loader, Object o)
    {
        if (o != null && o instanceof Cursor)
        {
            Cursor cursor = (Cursor)o;
            mCursorAdapter.swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader)
    {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        getActivity().getSupportLoaderManager().destroyLoader(LOADER_ID);
    }
}
