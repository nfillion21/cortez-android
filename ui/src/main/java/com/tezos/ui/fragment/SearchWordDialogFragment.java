package com.tezos.ui.fragment;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.tezos.ui.R;

/**
 * Created by nfillion on 3/9/18.
 */

public class SearchWordDialogFragment extends DialogFragment
{
    private AppCompatRadioButton mRadioButton;
    private AppCompatRadioButton mRadioButtonDiscreet;

    private Button mSubmitButton;
    private FrameLayout mSubmitButtonLayout;

    private Button mCancelButton;
    private FrameLayout mCancelButtonLayout;

    private ChangeIconListener mCallback;

    public interface ChangeIconListener
    {
        void iconChanged();
    }

    public static SearchWordDialogFragment newInstance()
    {
        return new SearchWordDialogFragment();
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

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try
        {
            mCallback = (ChangeIconListener) context;
        }
        catch (ClassCastException e)
        {
            //throw new ClassCastException(context.toString()
                    //+ " must implement WebviewListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (savedInstanceState == null)
        {
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_search_word, null);

        builder.setView(dialogView);

        return builder.create();
    }

    private void validateSubmitButton(boolean validate)
    {
    }

    private void validateCancelButton(boolean validate)
    {
    }

    private StateListDrawable makeSelector(int primaryColor, int primaryColorDark)
    {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(getActivity(), primaryColorDark)));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(getActivity(), primaryColor)));
        return res;
    }

    private String getStandardName()
    {

        PackageManager packageManager = getActivity().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getActivity().getPackageName());
        ComponentName componentName = intent.getComponent();

        String className = componentName.getClassName();
        return className;
    }

    private String getDiscreetName()
    {
        PackageManager packageManager = getActivity().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getActivity().getPackageName());
        ComponentName componentName = intent.getComponent();

        String className = componentName.getClassName();
        return className;
    }

    private boolean changeIcon(boolean discreet)
    {
        return true;
    }
}
