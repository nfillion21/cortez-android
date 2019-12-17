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
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Path;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tezos.core.models.Operation;
import com.tezos.ui.R;

import java.util.Arrays;
import java.util.List;

public class OperationDetailsDialogFragment extends DialogFragment
{
    private ListView mList;

    public static final String TAG = "OperationDetailsDialogFragment";

    public static OperationDetailsDialogFragment newInstance(Operation operation)
    {
        OperationDetailsDialogFragment fragment = new OperationDetailsDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putBundle(Operation.TAG, operation.toBundle());

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

        /*
        try
        {
            mCallback = (OnWordSelectedListener) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString()
                    + " must implement OnWordSelectedListener");
        }
        */
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_operation_details, null);

        List<String> list = Arrays.asList(
                getString(R.string.operation_details_hash),
                getString(R.string.operation_details_operation_id),
                getString(R.string.operation_details_block_hash),
                getString(R.string.operation_details_timestamp),
                getString(R.string.operation_details_src),
                getString(R.string.operation_details_src_mgr),
                getString(R.string.operation_details_dst),
                getString(R.string.operation_details_dst_mgr),
                getString(R.string.operation_details_amount),
                getString(R.string.operation_details_fee)
        );

        mList = dialogView.findViewById(R.id.list);
        mList.setOnItemClickListener((adapterView, view, i, l) -> {
            String copied = null;

            Bundle operationBundle = getArguments().getBundle(Operation.TAG);
            Operation operation = Operation.fromBundle(operationBundle);

            switch (i)
            {
                case 0: { copied = operation.getHash(); } break;
                case 1: { copied = operation.getOperationId().toString(); } break;
                case 2: { copied = operation.getBlockHash(); } break;
                case 3: { copied = operation.getTimestamp(); } break;
                case 4: { copied = operation.getSource(); } break;
                case 5: { copied = operation.getSourceManager(); } break;
                case 6: { copied = operation.getDestination(); } break;
                case 7: { copied = operation.getDestinationManager(); } break;
                case 8: { copied = operation.getAmount().toString(); } break;
                case 9: { copied = operation.getFee().toString(); } break;
                default:
                    //no-op
                break;
            }

            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(copied, copied);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getActivity(), String.format(getString(R.string.copied), copied), Toast.LENGTH_SHORT).show();
            getDialog().dismiss();
        });

        Bundle args = getArguments();
        Bundle operationBundle = args.getBundle(Operation.TAG);
        ArrayAdapter adapter = new OperationDetailsArrayAdapter(getActivity(), list, Operation.fromBundle(operationBundle));
        mList.setAdapter(adapter);

        builder.setView(dialogView);

        if (savedInstanceState == null)
        {
        }

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
    }

    private class OperationDetailsArrayAdapter extends ArrayAdapter<String>
    {
        private static final int ITEM_VIEW_TYPE = 0;

        private final Context context;
        private final List<String> mList;
        private final Operation mOperation;

        OperationDetailsArrayAdapter(Context context, List<String> list, Operation operation)
        {
            super(context, R.layout.item_operation_details, list);
            this.context = context;
            this.mList = list;
            this.mOperation = operation;
        }

        private class ViewHolder
        {
            TextView titleTextView;
            TextView valueTextView;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
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
            return ITEM_VIEW_TYPE;
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
                switch (type) {
                    case ITEM_VIEW_TYPE:
                    {
                        rowView = inflater.inflate(R.layout.item_operation_details, parent, false);
                        viewHolder.titleTextView = rowView.findViewById(R.id.text);
                        viewHolder.valueTextView = rowView.findViewById(R.id.text2);
                    }
                    break;
                }

                try {
                    rowView.setTag(viewHolder);
                } catch (Exception e) {
                    Log.getStackTraceString(e);}
            }

            ViewHolder holder = (ViewHolder) rowView.getTag();
            holder.titleTextView.setText(mList.get(position));
            switch (position)
            {
                case 0:
                    holder.valueTextView.setText(mOperation.getHash()); break;

                case 1:
                    holder.valueTextView.setText(String.format("%d", mOperation.getOperationId())); break;

                case 2:
                    holder.valueTextView.setText(mOperation.getBlockHash()); break;

                case 3:
                    holder.valueTextView.setText(mOperation.getTimestamp()); break;

                case 4:
                    holder.valueTextView.setText(mOperation.getSource()); break;

                case 5:
                    holder.valueTextView.setText(mOperation.getSourceManager()); break;

                case 6:
                    holder.valueTextView.setText(mOperation.getDestination()); break;

                case 7:
                    holder.valueTextView.setText(mOperation.getDestinationManager()); break;

                case 8:
                    holder.valueTextView.setText(mOperation.getAmount().toString()); break;

                case 9:
                    holder.valueTextView.setText(mOperation.getFee().toString()); break;

                default: break;
            }

            return rowView;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
}
