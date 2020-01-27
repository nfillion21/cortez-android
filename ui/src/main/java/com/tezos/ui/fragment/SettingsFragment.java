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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.tezos.ui.R;
import com.tezos.ui.authentication.EncryptionServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nfillion on 3/6/18.
 */

public class SettingsFragment extends Fragment implements AdapterView.OnItemClickListener
{
    private OnFingerprintOptionSelectedListener mFingerprintOptionCallback;
    private OnSystemInformationsCallback mSystemInformationsCallback;

    // The user view type.
    private static final int CONFIRM_CREDENTIALS_ITEM_VIEW_TYPE = 0;

    public interface OnFingerprintOptionSelectedListener
    {
        void onFingerprintOptionClicked(boolean isOptionChecked);
    }

    public interface OnSystemInformationsCallback
    {
        boolean isFingerprintHardwareAvailable();
        boolean isDeviceSecure();
        boolean isFingerprintAllowed();
    }

    public static SettingsFragment newInstance()
    {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        try
        {
            mFingerprintOptionCallback = (OnFingerprintOptionSelectedListener) context;
            mSystemInformationsCallback = (OnSystemInformationsCallback) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString()
                    + " must implement OnFingerprintOptionSelectedListener");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        ListView mList = view.findViewById(R.id.list);
        mList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mList.setItemsCanFocus(false);
        mList.setOnItemClickListener(this);

        ArrayList<String> settingsList = new ArrayList<>();

        if (mSystemInformationsCallback.isDeviceSecure())
        {
            settingsList.add(getString(R.string.ask_for_credentials));

            // device needs to be secure to create fingerprint keys
            if (mSystemInformationsCallback.isFingerprintHardwareAvailable())
            {
                settingsList.add(getString(R.string.use_fingerprint));
            }
        }

        ArrayAdapter adapter = new SettingsArrayAdapter(getActivity(), settingsList);
        mList.setAdapter(adapter);

        mList.setItemChecked(0, new EncryptionServices().containsConfirmCredentialsKey());

        mList.setItemChecked(1, mSystemInformationsCallback.isFingerprintAllowed());
    }

    private class SettingsArrayAdapter extends ArrayAdapter<String>
    {
        private final Context context;
        private final List<String> mList;

        SettingsArrayAdapter(Context context, List<String> list)
        {
            super(context, R.layout.item_confirm_credentials_settings, list);
            this.context = context;
            this.mList = list;
        }

        private class ViewHolder
        {
            TextView textView;
        }

        @Override
        public int getCount()
        {
            return mList.size();
        }

        @Nullable
        @Override
        public String getItem(int position)
        {
            return mList.get(position);
        }

        @Override
        public int getItemViewType(int position)
        {
            return CONFIRM_CREDENTIALS_ITEM_VIEW_TYPE;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent)
        {
            View rowView = convertView;
            int type = getItemViewType(position);

            if (rowView == null)
            {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();

                ViewHolder viewHolder = new ViewHolder();
                switch (type)
                {
                    case CONFIRM_CREDENTIALS_ITEM_VIEW_TYPE:
                    {
                        rowView = inflater.inflate(R.layout.item_confirm_credentials_settings, parent, false);
                        viewHolder.textView = rowView.findViewById(R.id.text1);
                    }
                    break;
                }

                try {
                    rowView.setTag(viewHolder);
                } catch (Exception e) {
                    Log.getStackTraceString(e);}
            }

            ViewHolder holder = (ViewHolder) rowView.getTag();
            holder.textView.setText(mList.get(position));

            return rowView;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        CheckedTextView checkedTextView = (CheckedTextView)view;

        switch (position)
        {
            case 0:
            {
                EncryptionServices encryptionServices = new EncryptionServices();
                if (checkedTextView.isChecked())
                {
                    encryptionServices.createConfirmCredentialsKey();
                }
                else
                {
                    encryptionServices.removeConfirmCredentialsKey();
                }
            }
            break;

            case 1:
            {
                if (mFingerprintOptionCallback != null)
                {
                    mFingerprintOptionCallback.onFingerprintOptionClicked(checkedTextView.isChecked());
                }
            }
            break;
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        mSystemInformationsCallback = null;
        mFingerprintOptionCallback = null;
    }
}
