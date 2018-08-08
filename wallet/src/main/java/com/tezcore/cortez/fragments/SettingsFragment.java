
package com.tezcore.cortez.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.tezos.ui.authentication.EncryptionServices;
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
    private static final int CONFIRM_CREDENTIALS_ITEM_VIEW_TYPE = 0;

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
                getString(R.string.ask_for_credentials),
                getString(R.string.use_fingerprint)
        );

        ArrayAdapter adapter = new SettingsArrayAdapter(getActivity(), list);
        mList.setAdapter(adapter);

        mList.setItemChecked(0, new EncryptionServices(getActivity()).containsConfirmCredentialsKey());

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
        switch (position)
        {
            case 0:
            {
                CheckedTextView checkedTextView = (CheckedTextView)view;

                EncryptionServices encryptionServices = new EncryptionServices(getActivity());
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
                /*
                if (!systemServices.hasEnrolledFingerprints()) {
                    item.isChecked = false
                    Snackbar.make(rootView, R.string.sign_up_snack_message, Snackbar.LENGTH_LONG)
                            .setAction(R.string.sign_up_snack_action, { openSecuritySettings() })
                            .show()
                } else {
                    // Set new checkbox state
                    item.isChecked = !item.isChecked

                    new Storage(baseContext).saveFingerprintAllowed(item.isChecked)
                    if (!item.isChecked) {
                        EncryptionServices(this).removeFingerprintKey()
                    }
                }
                */


                if (mCallback != null)
                {
                    mCallback.onItemClicked();
                }
            }
            break;
        }
    }
}
