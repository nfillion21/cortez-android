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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.tezos.core.crypto.CryptoUtils;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.adapter.MnemonicWordsViewAdapter;
import com.tezos.ui.widget.OffsetDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestoreWalletFragment extends Fragment implements MnemonicWordsViewAdapter.OnItemClickListener
{
    private static final String WORDS_KEY = "words_key";
    private static final int MNEMONICS_WORDS_NUMBER = 24;

    private OnWordSelectedListener mCallback;

    private MnemonicWordsViewAdapter mAdapter;

    private Button mValidateMnemonicsButton;
    private FrameLayout mValidateMnemonicsButtonLayout;

    public interface OnWordSelectedListener
    {
        void onWordCardNumberClicked(int position);
        void mnemonicsVerified(String mnemonics);
        boolean wordsFilled(String mnemonics);
        void showSnackBar(String res, int color, int textColor);
    }

    public static RestoreWalletFragment newInstance(Bundle customTheme)
    {
        RestoreWalletFragment fragment = new RestoreWalletFragment();

        Bundle bundle = new Bundle();
        bundle.putBundle(CustomTheme.TAG, customTheme);

        fragment.setArguments(bundle);
        return fragment;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_restore_wallet, container, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mValidateMnemonicsButton = view.findViewById(R.id.validate_mnemonics_button);
        mValidateMnemonicsButtonLayout = view.findViewById(R.id.validate_mnemonics_button_layout);
        mValidateMnemonicsButtonLayout.setOnClickListener(v ->
        {
            //TODO verify if it does always work
            String mnemonics = mnemonicsListToString(mAdapter.getWords());
            if (mnemonics != null)
            {
                mCallback.mnemonicsVerified(mnemonics);
            }
            else
            {
                //TODO an error occurred
            }
        });

        RecyclerView mRecyclerView = view.findViewById(R.id.words);
        setUpWordGrid(mRecyclerView);

        if (savedInstanceState != null)
        {
            ArrayList<String> words = savedInstanceState.getStringArrayList(WORDS_KEY);
            if (words != null)
            {
                mAdapter.updateWords(words, null);
            }

            boolean isValid = CryptoUtils.validateMnemonics(mAdapter.getWords());
            if (isValid)
            {
                String mnemonics = mnemonicsListToString(mAdapter.getWords());
                boolean isRightWords = mCallback.wordsFilled(mnemonics);
                if (isRightWords)
                {
                    validateMnemonicsButton(CryptoUtils.validateMnemonics(words));
                }
                else
                {
                    validateMnemonicsButton(false);
                }
            }
            else
            {
                validateMnemonicsButton(false);
            }
        }
        else
        {
            List<String> words = new ArrayList<>(MNEMONICS_WORDS_NUMBER);
            for (int i = 0; i < MNEMONICS_WORDS_NUMBER; i++)
            {
                words.add(null);
            }
            mAdapter.updateWords(words, null);
            validateMnemonicsButton(false);
        }
    }

    private String mnemonicsListToString(List<String> words)
    {
        String listString = null;

        if (words != null && words.size() == MNEMONICS_WORDS_NUMBER && !words.contains(null))
        {
            listString = TextUtils.join(" ", words);
        }

        return listString;
    }

    protected void validateMnemonicsButton(boolean validate) {

        if (validate) {

            //final Bundle customThemeBundle = getArguments().getBundle(CustomTheme.TAG);
            //CustomTheme theme = CustomTheme.fromBundle(customThemeBundle);
            CustomTheme theme = new CustomTheme(R.color.colorAccentSecondaryDark, R.color.colorAccentSecondary, R.color.colorStandardText);

            mValidateMnemonicsButton.setTextColor(ContextCompat.getColor(getActivity(), theme.getTextColorPrimaryId()));
            mValidateMnemonicsButtonLayout.setEnabled(true);
            mValidateMnemonicsButtonLayout.setBackground(makeSelector(theme));

            Drawable[] drawables = mValidateMnemonicsButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getContext(),R.color.tz_dark));

        } else {

            mValidateMnemonicsButton.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
            mValidateMnemonicsButtonLayout.setEnabled(false);
            CustomTheme greyTheme = new CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey);
            mValidateMnemonicsButtonLayout.setBackground(makeSelector(greyTheme));

            Drawable[] drawables = mValidateMnemonicsButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getActivity(), android.R.color.white));
        }
    }

    private StateListDrawable makeSelector(CustomTheme theme)
    {
        StateListDrawable res = new StateListDrawable();
        res.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(ContextCompat.getColor(getActivity(), theme.getColorPrimaryDarkId())));
        res.addState(new int[]{}, new ColorDrawable(ContextCompat.getColor(getActivity(), theme.getColorPrimaryId())));
        return res;
    }

    private void setUpWordGrid(final RecyclerView wordsView)
    {
        final int spacing = getContext().getResources()
                .getDimensionPixelSize(R.dimen.spacing_micro);
        wordsView.addItemDecoration(new OffsetDecoration(spacing));

        mAdapter = new MnemonicWordsViewAdapter(getActivity());

        mAdapter.setOnItemClickListener(this);

        wordsView.setAdapter(mAdapter);
    }

    public void updateCard(String word, int position)
    {
        mAdapter.updateWord(word, position);

        //konami code

        if (position == 23 && word.equalsIgnoreCase("zebra"))
        {
            List<String> zebras = Arrays.asList(
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra",
                    "zebra" );

            mAdapter.updateWords(zebras, null);
        }
        /*
        else if (position == 0 && word.equalsIgnoreCase("link"))
        {
            List<String> link = Arrays.asList(
                    "link",
                    "warm",
                    "visual",
                    "pony",
                    "bike",
                    "person",
                    "truck",
                    "pupil",
                    "moral",
                    "gift",
                    "shoulder",
                    "eye",
                    "kit",
                    "human",
                    "jacket",
                    "rich",
                    "sand",
                    "cupboard",
                    "position",
                    "friend",
                    "fox",
                    "calm",
                    "bring",
                    "kick" );

            mAdapter.updateWords(link, null);
        }
        else if (position == 0 && word.equalsIgnoreCase("blue"))
        {
            List<String> link = Arrays.asList(
                    "blue",
                    "junk",
                    "trap",
                    "expect",
                    "mammal",
                    "such",
                    "vacant",
                    "quarter",
                    "siege",
                    "carbon",
                    "extra",
                    "eight",
                    "notice",
                    "short",
                    "cheap",
                    "expose",
                    "soccer",
                    "clean",
                    "lawn",
                    "envelope",
                    "goose",
                    "major",
                    "orange",
                    "capable" );

            mAdapter.updateWords(link, null);
        }
        else if (position == 0 && word.equalsIgnoreCase("green"))
        {
            List<String> link = Arrays.asList(
                    "green",
                    "kind",
                    "inquiry",
                    "alarm",
                    "razor",
                    "zone",
                    "benefit",
                    "again",
                    "ski",
                    "erase",
                    "another",
                    "wide",
                    "liberty",
                    "multiply",
                    "pen",
                    "risk",
                    "love",
                    "corn",
                    "monster",
                    "honey",
                    "level",
                    "poem",
                    "position",
                    "spell" );

            mAdapter.updateWords(link, null);
        }

        else if (position == 0 && word.equalsIgnoreCase("bronze"))
        {
            List<String> link = Arrays.asList(
                    "bronze",
                    "avoid",
                    "sound",
                    "tongue",
                    "claw",
                    "reward",
                    "choice",
                    "pottery",
                    "jump",
                    "dream",
                    "ripple",
                    "extra",
                    "adapt",
                    "wisdom",
                    "infant",
                    "urban",
                    "tenant",
                    "squirrel",
                    "crop",
                    "midnight",
                    "craft",
                    "cool",
                    "recall",
                    "public" );

            mAdapter.updateWords(link, null);
        }
         //*/

        boolean isValid = CryptoUtils.validateMnemonics(mAdapter.getWords());
        if (isValid)
        {
            //TODO verify if it does always work
            String mnemonics = mnemonicsListToString(mAdapter.getWords());
            boolean isRightWords = mCallback.wordsFilled(mnemonics);
            if (!isRightWords)
            {
                mCallback.showSnackBar(getString(R.string.not_mnemonics_expected), ContextCompat.getColor(getActivity(), android.R.color.holo_red_light), ContextCompat.getColor(getActivity(), R.color.tz_light));
            }

            validateMnemonicsButton(isRightWords);
        }
        else
        {
            validateMnemonicsButton(false);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onClick(View view, int position)
    {
        if (mCallback != null)
        {
            mCallback.onWordCardNumberClicked(position);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        List<String> words = mAdapter.getWords();
        if (words != null)
        {
            outState.putStringArrayList(WORDS_KEY, (ArrayList<String>) mAdapter.getWords());
        }
    }
}
