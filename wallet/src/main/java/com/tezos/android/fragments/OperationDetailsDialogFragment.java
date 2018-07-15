package com.tezos.android.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tezos.android.R;
import com.tezos.core.models.Operation;

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
            mCallback = (OnSearchWordSelectedListener) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString()
                    + " must implement OnSearchWordSelectedListener");
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

        mList = dialogView.findViewById(R.id.list);

        List<String> list = Arrays.asList(
                getString(R.string.operation_details_amount),
                getString(R.string.operation_details_fee),
                getString(R.string.operation_details_hash),
                getString(R.string.operation_details_dst)
        );

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
                        viewHolder.titleTextView = rowView.findViewById(R.id.text1);
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
                {
                    holder.valueTextView.setText(mOperation.getAmount().toString());
                }
                break;

                case 1:
                {
                    holder.valueTextView.setText(mOperation.getFee().toString());
                }
                break;

                case 2:
                {
                    holder.valueTextView.setText(mOperation.getBlockHash());
                }
                break;

                case 3:
                {
                    holder.valueTextView.setText(mOperation.getDestination());
                }
                break;

                default:
                {
                    //no-op
                }
                break;
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
