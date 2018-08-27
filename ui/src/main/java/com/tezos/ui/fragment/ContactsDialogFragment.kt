package com.tezos.ui.fragment

import android.app.Dialog
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.CursorAdapter
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ListView
import com.tezos.core.database.EnglishWordsDatabaseConstants
import com.tezos.ui.R

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

/**
 * Created by nfillion on 3/9/18.
 */

class ContactsDialogFragment : DialogFragment(), LoaderManager.LoaderCallbacks<Cursor>
{
    companion object
    {
        val CARD_POSITION_KEY = "card_position"
        val TAG = "search_word_dialog_fragment"

        private val LOADER_ID = 42

        fun newInstance(cardPosition: Int): SearchWordDialogFragment {
            val fragment = SearchWordDialogFragment()

            val bundle = Bundle()
            bundle.putInt(CARD_POSITION_KEY, cardPosition)

            fragment.arguments = bundle
            return fragment
            //Put the theme here

            //TODO put here the number
        }
    }
    private var mCallback: OnWordSelectedListener? = null
    private var mSearchWordEditText: TextInputEditText? = null

    private var mCursorAdapter: CursorAdapter? = null

    private var mList: ListView? = null

    interface OnWordSelectedListener {
        fun onWordClicked(word: String, position: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            mCallback = context as OnWordSelectedListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(context!!.toString() + " must implement OnWordSelectedListener")
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_search_word, null)

        mSearchWordEditText = dialogView.findViewById(R.id.search_word_edittext)

        val position = arguments!!.getInt(CARD_POSITION_KEY)
        val cardPos = position + 1
        if (position != -1) {
            mSearchWordEditText!!.hint = "Word #$cardPos"
        }

        mSearchWordEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {

                val query = editable.toString()

                var queryBundle: Bundle? = null
                if (!query.isEmpty()) {
                    queryBundle = Bundle()
                    queryBundle.putString("query", query)
                }
                activity!!.supportLoaderManager.restartLoader<Any>(LOADER_ID, queryBundle, this@SearchWordDialogFragment)
            }
        })

        mCursorAdapter = SimpleCursorAdapter(activity!!,
                R.layout.item_search_word, null,
                arrayOf(EnglishWordsDatabaseConstants.COL_WORD),
                intArrayOf(R.id.word_item))

        mList = dialogView.findViewById(R.id.list)
        mList!!.adapter = mCursorAdapter

        mList!!.setOnItemClickListener { adapterView, view, i, l ->
            val cursor = mCursorAdapter!!.cursor
            cursor.moveToPosition(i)
            val item = cursor.getString(cursor.getColumnIndex(EnglishWordsDatabaseConstants.COL_WORD))

            mCallback!!.onWordClicked(item, position)

            //getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, SearchWordDialogFragment.this);
            dialog.dismiss()
        }

        if (savedInstanceState == null) {
            activity!!.supportLoaderManager.initLoader<Any>(LOADER_ID, null, this)
        }

        builder.setView(dialogView)

        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<*> {
        if (id == LOADER_ID) {
            var query: String? = null
            if (bundle != null) {
                query = bundle.getString("query")
            }

            // the
            val packageName = activity!!.applicationContext.packageName
            val packageProvider = "$packageName.provider"
            val contentUri = Uri.parse("content://" + packageProvider
                    + "/" + EnglishWordsDatabaseConstants.TABLE_WORD)

            // it filters everything when I put null as a parameter
            return CursorLoader(activity!!,
                    contentUri,
                    arrayOf(EnglishWordsDatabaseConstants.COL_ID, EnglishWordsDatabaseConstants.COL_WORD), null, arrayOf<String>(query), null)
        }

        return null
    }

    override fun onLoaderReset(loader: Loader<Cursor>?)
    {
        mCursorAdapter!!.swapCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?)
    {
        if (data != null)
        {
            mCursorAdapter?.swapCursor(data)
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        activity!!.supportLoaderManager.destroyLoader(LOADER_ID)
    }
}