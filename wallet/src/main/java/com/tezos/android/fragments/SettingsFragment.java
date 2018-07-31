
package com.tezos.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tezos.android.R;
import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.AddressesDatabase;
import com.tezos.ui.activity.PasscodeActivity;
import com.tezos.ui.utils.Storage;

import java.util.Arrays;
import java.util.List;

/**
 * Created by nfillion on 3/6/18.
 */

public class SettingsFragment extends Fragment implements AdapterView.OnItemClickListener
{
    private OnRowSelectedListener mCallback;
    private OnLogOutClickedListener mLogOutCallback;

    // The user view type.
    private static final int PASSCODE_ITEM_VIEW_TYPE = 0;

    // The team support view type.
    private static final int ICON_ITEM_VIEW_TYPE = 1;

    private Button mExitButton;
    private FrameLayout mExitButtonLayout;

    private ListView mList;

    public interface OnRowSelectedListener
    {
        void onItemClicked();
    }

    public interface OnLogOutClickedListener
    {
        void onLogOutClicked();
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
            mLogOutCallback = (OnLogOutClickedListener) context;
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

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mList = view.findViewById(R.id.list);
        mList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mList.setItemsCanFocus(false);
        mList.setOnItemClickListener(this);

        List<String> list = Arrays.asList(
                getString(R.string.use_passcode)
        );

        ArrayAdapter adapter = new SettingsArrayAdapter(getActivity(), list);
        mList.setAdapter(adapter);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String codeGuess = sharedPref.getString(PasscodeActivity.PASSCODE_KEY, null);

        mList.setItemChecked(0, codeGuess != null);

        mExitButton = view.findViewById(R.id.exit_button);
        mExitButtonLayout = view.findViewById(R.id.exit_button_layout);

        mExitButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) ->
                {
                    switch (which)
                    {
                        case DialogInterface.BUTTON_POSITIVE:
                        {
                            dialog.dismiss();
                            //TODO don't remove addresses
                            //AddressesDatabase.getInstance().logOut(getActivity());

                            mLogOutCallback.onLogOutClicked();
                        }
                        break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            dialog.dismiss();
                            break;
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.alert_exit_account)
                        .setMessage(R.string.alert_exit_acccount_body)
                        .setNegativeButton(android.R.string.cancel, dialogClickListener)
                        .setPositiveButton(android.R.string.yes, dialogClickListener)
                        .setCancelable(false)
                        .show();
            }
        });

        //boolean isPrivateKeyOn = AddressesDatabase.getInstance().isPrivateKeyOn(getActivity());
        boolean isPasswordSaved = new Storage(getActivity()).isPasswordSaved();
        validateExitButton(isPasswordSaved);
    }

    protected void validateExitButton(boolean validate) {

        if (validate) {

            CustomTheme theme = new CustomTheme(R.color.tz_error, R.color.tz_accent, R.color.tz_light);

            mExitButton.setTextColor(ContextCompat.getColor(getActivity(), theme.getTextColorPrimaryId()));
            mExitButtonLayout.setEnabled(true);
            mExitButtonLayout.setBackground(makeSelector(theme));

            Drawable[] drawables = mExitButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getActivity(), theme.getTextColorPrimaryId()));

        } else {

            mExitButton.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
            mExitButtonLayout.setEnabled(false);
            CustomTheme greyTheme = new CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey);
            mExitButtonLayout.setBackground(makeSelector(greyTheme));

            Drawable[] drawables = mExitButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getActivity(), android.R.color.white));
        }
    }

    private StateListDrawable makeSelector(CustomTheme theme) {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(getActivity(), theme.getColorPrimaryDarkId())));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(getActivity(), theme.getColorPrimaryId())));
        return res;
    }

    public void notifyChanged()
    {
        mList.setItemChecked(0, false);
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
