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

package com.tezos.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import androidx.fragment.app.DialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;

import com.tezos.core.database.EnglishWordsDatabaseConstants;
import com.tezos.ui.R;

/**
 * Created by nfillion on 3/9/18.
 */

public class SearchWordDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks
{

    public static final String CARD_POSITION_KEY = "card_position";
    public static final String TAG = "search_word_dialog_fragment";

    private OnWordSelectedListener mCallback;

    private CursorAdapter mCursorAdapter;

    private ListView mList;

    private static final int LOADER_ID = 42;

    public interface OnWordSelectedListener
    {
        void onWordClicked(String word, int position);
    }

    public static SearchWordDialogFragment newInstance(int cardPosition)
    {
        SearchWordDialogFragment fragment = new SearchWordDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(CARD_POSITION_KEY, cardPosition);

        fragment.setArguments(bundle);
        return fragment;
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
            mCallback = (OnWordSelectedListener) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString()
                    + " must implement OnWordSelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_search_word, null);

        TextInputEditText mSearchWordEditText = dialogView.findViewById(R.id.search_name_edittext);

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
                new int[] { R.id.word_item }, 0);

        mList = dialogView.findViewById(R.id.list);
        mList.setAdapter(mCursorAdapter);

        mList.setOnItemClickListener((adapterView, view, i, l) ->
        {
            Cursor cursor = mCursorAdapter.getCursor();
            cursor.moveToPosition(i);
            String item = cursor.getString(cursor.getColumnIndex(EnglishWordsDatabaseConstants.COL_WORD));

            mCallback.onWordClicked(item, position);

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

            // the
            String packageName = getActivity().getApplicationContext().getPackageName();
            String packageProvider = packageName + ".provider";
            Uri contentUri = Uri.parse("content://" + packageProvider
                    + "/" + EnglishWordsDatabaseConstants.TABLE_WORD);

            // it filters everything when I put null as a parameter
            return new CursorLoader(getActivity(),
                    contentUri,
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
    public void onDetach()
    {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        getActivity().getSupportLoaderManager().destroyLoader(LOADER_ID);
    }
}
