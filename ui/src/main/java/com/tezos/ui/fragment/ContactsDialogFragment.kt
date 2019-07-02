package com.tezos.ui.fragment

import android.app.Dialog
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
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
 * Created by nfillion on 27/08/18.
 */

class ContactsDialogFragment : DialogFragment(), LoaderManager.LoaderCallbacks<Cursor>
{
    private val QUERY_KEY = "query"
    private val TAG = "DialogFragment"
    private val LOADER_ID = 0

    companion object
    {
        @JvmStatic
        fun newInstance(): ContactsDialogFragment
        {
            val fragment = ContactsDialogFragment()
            return fragment
        }
    }
    private var mCallback: OnNameSelectedListener? = null
    private var mSearchNameEditText: TextInputEditText? = null

    private var mCursorAdapter: CursorAdapter? = null

    private var mList: ListView? = null

    interface OnNameSelectedListener
    {
        fun onNameSelected(word: String)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onAttach(context: Context?)
    {
        super.onAttach(context)

        try
        {
            mCallback = context as OnNameSelectedListener?
        }
        catch (e: ClassCastException)
        {
            throw ClassCastException(context!!.toString() + " must implement OnNameSelectedListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_contacts, null)

        mSearchNameEditText = dialogView.findViewById(R.id.search_name_edittext)
        mSearchNameEditText!!.hint = getString(R.string.contact_name)

        mSearchNameEditText!!.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable)
            {
                val query = editable.toString()

                var queryBundle: Bundle? = null
                if (!query.isEmpty())
                {
                    queryBundle = Bundle()
                    queryBundle.putString("query", query)
                }
                activity!!.supportLoaderManager.restartLoader(LOADER_ID, queryBundle, this@ContactsDialogFragment)
            }
        })

        mCursorAdapter = SimpleCursorAdapter(activity!!,
                R.layout.item_search_word, null,
                arrayOf(ContactsContract.CommonDataKinds.Contactables.DISPLAY_NAME),
                intArrayOf(R.id.word_item), 0)

        mList = dialogView.findViewById(R.id.list)
        mList!!.adapter = mCursorAdapter

        mList!!.setOnItemClickListener { _, _, i, _ ->
            val cursor = mCursorAdapter!!.cursor
            cursor.moveToPosition(i)
            val item = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.DISPLAY_NAME))

            mCallback?.onNameSelected(item)

            //getActivity().getSupportLoaderManager().restartLoader(LOADER_ID, null, SearchWordDialogFragment.this);
            dialog.dismiss()
        }

        if (savedInstanceState == null) {
            activity!!.supportLoaderManager.initLoader(LOADER_ID, null, this@ContactsDialogFragment)
        }

        builder.setView(dialogView)

        return builder.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>
    {
        // BEGIN_INCLUDE(uri_with_query)
        val query = args?.getString(QUERY_KEY)
        val uri = Uri.withAppendedPath(
                ContactsContract.CommonDataKinds.Contactables.CONTENT_FILTER_URI, query)

        return CursorLoader(
                activity!!, // Context
                uri, // selection - Which rows to return (condition rows must match)
                arrayOf("_id", ContactsContract.CommonDataKinds.Contactables.DISPLAY_NAME), // projection - the list of columns to return.  Null means "all"
                null, // selection
                null, // selection args - can be provided separately and subbed into selection.
                null)// URI representing the table/resource to be queried
    }

    override fun onLoaderReset(loader: Loader<Cursor>)
    {
        mCursorAdapter!!.swapCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor)
    {
        if (cursor != null)
        {
            //TODO verify later
            mCursorAdapter?.swapCursor(cursor)
        }

        /*

        /*
        if (cursor.getCount() == 0) {
            return;
        }
        */

        // Pulling the relevant value from the cursor requires knowing the column index to pull
        // it from.
        // BEGIN_INCLUDE(get_columns)
        int phoneColumnIndex = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
        int emailColumnIndex = cursor.getColumnIndex(CommonDataKinds.Email.ADDRESS);
        int nameColumnIndex = cursor.getColumnIndex(CommonDataKinds.Contactables.DISPLAY_NAME);
        int lookupColumnIndex = cursor.getColumnIndex(CommonDataKinds.Contactables.LOOKUP_KEY);
        int typeColumnIndex = cursor.getColumnIndex(CommonDataKinds.Contactables.MIMETYPE);
        // END_INCLUDE(get_columns)

        cursor.moveToFirst();
        // Lookup key is the easiest way to verify a row of cursor is for the same
        // contact as the previous row.
        String lookupKey = "";
        do {
            // BEGIN_INCLUDE(lookup_key)
            String currentLookupKey = cursor.getString(lookupColumnIndex);
            if (!lookupKey.equals(currentLookupKey)) {
                String displayName = cursor.getString(nameColumnIndex);
                //tv.append(displayName + "\n");
                lookupKey = currentLookupKey;
            }
            // END_INCLUDE(lookup_key)

            // BEGIN_INCLUDE(retrieve_data)
            // The cursor type can be determined using the mime type column.

            /*
            String mimeType = cursor.getString(typeColumnIndex);
            if (mimeType.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                tv.append("\tPhone Number: " + cursor.getString(phoneColumnIndex) + "\n");
            } else if (mimeType.equals(CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                tv.append("\tEmail Address: " + cursor.getString(emailColumnIndex) + "\n");
            }
            */
            // END_INCLUDE(retrieve_data)

            // Look at DDMS to see all the columns returned by a query to Contactables.
            // Behold, the firehose!
            /*
            for(String column : cursor.getColumnNames()) {
                Log.d(TAG, column + column + ": " +
                        cursor.getString(cursor.getColumnIndex(column)) + "\n");
            }
            */
        } while (cursor.moveToNext())
        */
    }

    override fun onDetach()
    {
        super.onDetach()
        mCallback = null
    }

    override fun onDestroy()
    {
        super.onDestroy()
        //TODO check if destroying loader is necessary
        activity!!.supportLoaderManager.destroyLoader(LOADER_ID)
    }
}