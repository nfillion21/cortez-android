
package com.tezos.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ListFragment;
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

import com.tezos.android.R;
import com.tezos.android.activities.PasscodeActivity;

import java.util.Arrays;
import java.util.List;

/**
 * Created by nfillion on 3/6/18.
 */

public class SettingsFragment extends ListFragment implements AdapterView.OnItemClickListener
{
    private OnRowSelectedListener mCallback;

    // The user view type.
    private static final int PASSCODE_ITEM_VIEW_TYPE = 0;

    // The team support view type.
    private static final int ICON_ITEM_VIEW_TYPE = 1;

    public interface OnRowSelectedListener
    {
        void onItemClicked();
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
            mCallback = (OnRowSelectedListener) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString()
                    + " must implement OnRowSelectedListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null)
        {
        }

        ListView listView = getListView();
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setItemsCanFocus(false);
        listView.setOnItemClickListener(this);

        List<String> list = Arrays.asList(
                getString(R.string.use_passcode)
        );

        ArrayAdapter adapter = new SettingsArrayAdapter(getActivity(), list);
        setListAdapter(adapter);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String codeGuess = sharedPref.getString(PasscodeActivity.PASSCODE_KEY, null);

        listView.setItemChecked(0, codeGuess != null);
    }

    public void notifyChanged()
    {
        ListView listView = getListView();
        listView.setItemChecked(0, false);
    }

    private class SettingsArrayAdapter extends ArrayAdapter<String>
    {
        private final Context context;
        private final List<String> mList;

        SettingsArrayAdapter(Context context, List<String> list)
        {
            super(context, android.R.layout.simple_list_item_multiple_choice, list);
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
            if (position == 0)
            {
                return PASSCODE_ITEM_VIEW_TYPE;
            }
            else
            {
                return ICON_ITEM_VIEW_TYPE;
            }
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
                    case PASSCODE_ITEM_VIEW_TYPE:
                    {
                        rowView = inflater.inflate(R.layout.item_passcode_settings, parent, false);
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
        switch (position)
        {
            case 0:
            {
                CheckedTextView checkedTextView = (CheckedTextView)view;

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                if (checkedTextView.isChecked())
                {
                    String passcode = sharedPref.getString(PasscodeActivity.PASSCODE_KEY, null);

                    if (passcode == null)
                    {
                        Intent intent = new Intent(getActivity(), PasscodeActivity.class);
                        intent.putExtra(PasscodeActivity.ASK_NEW_CODE_PARAMETER, true);

                        ActivityCompat.startActivityForResult(getActivity(), intent, PasscodeActivity.ASK_NEW_CODE_RESULT, null);
                    }
                }
                else
                {
                    editor.remove(PasscodeActivity.PASSCODE_KEY);
                }
                editor.apply();
            }
            break;

            case 1:
            {
                if (mCallback != null)
                {
                    mCallback.onItemClicked();
                }
            }
            break;
        }
    }
}
