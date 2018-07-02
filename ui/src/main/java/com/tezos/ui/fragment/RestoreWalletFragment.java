package com.tezos.ui.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tezos.core.models.CustomTheme;
import com.tezos.core.utils.TezosUtils;
import com.tezos.ui.R;
import com.tezos.ui.adapter.MnemonicWordsViewAdapter;
import com.tezos.ui.widget.OffsetDecoration;

import java.util.ArrayList;
import java.util.List;

import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.Validation.InvalidChecksumException;
import io.github.novacrypto.bip39.Validation.InvalidWordCountException;
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException;
import io.github.novacrypto.bip39.Validation.WordNotFoundException;
import io.github.novacrypto.bip39.wordlists.English;

public class RestoreWalletFragment extends Fragment implements MnemonicWordsViewAdapter.OnItemClickListener
{
    private static final String WORDS_KEY = "words_key";

    private OnWordSelectedListener mCallback;

    private MnemonicWordsViewAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private Button mValidateMnemonicsButton;
    private FrameLayout mValidateMnemonicsButtonLayout;

    public interface OnWordSelectedListener
    {
        void onWordCardNumberClicked(int position);
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
            // handle click
        });

        mRecyclerView = view.findViewById(R.id.words);
        setUpWordGrid(mRecyclerView);

        if (savedInstanceState != null)
        {
            ArrayList<String> words = savedInstanceState.getStringArrayList(WORDS_KEY);
            if (words != null)
            {
                mAdapter.updateWords(words);
            }

            if (isMnemonicsValid(words))
            {
                validateMnemonicsButton(true);
            }
        }
        else
        {
            List<String> words = new ArrayList<>(24);
            for (int i = 0; i < 24; i++)
            {
                words.add(null);
            }
            mAdapter.updateWords(words);
            validateMnemonicsButton(false);
        }
    }

    protected void validateMnemonicsButton(boolean validate) {

        if (validate) {

            final Bundle customThemeBundle = getArguments().getBundle(CustomTheme.TAG);
            CustomTheme theme = CustomTheme.fromBundle(customThemeBundle);

            mValidateMnemonicsButton.setTextColor(ContextCompat.getColor(getActivity(), theme.getTextColorPrimaryId()));
            mValidateMnemonicsButtonLayout.setEnabled(true);
            mValidateMnemonicsButtonLayout.setBackground(makeSelector(theme));

            Drawable[] drawables = mValidateMnemonicsButton.getCompoundDrawables();
            Drawable wrapDrawable = DrawableCompat.wrap(drawables[0]);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getActivity(), theme.getTextColorPrimaryId()));

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
                .getDimensionPixelSize(R.dimen.spacing_nano);
        wordsView.addItemDecoration(new OffsetDecoration(spacing));

        mAdapter = new MnemonicWordsViewAdapter(getActivity());

        mAdapter.setOnItemClickListener(this);

        wordsView.setAdapter(mAdapter);
    }

    public void updateCard(String word, int position)
    {
        mAdapter.updateWord(word, position);

        if (mCallback != null)
        {
            validateMnemonicsButton(isMnemonicsValid(mAdapter.getWords()));
        }
    }

    private boolean isMnemonicsValid(List<String> words)
    {
        boolean isValid;

        if (words == null || words.contains(null))
        {
            isValid = false;
        }
        else
        {
            String separatedWords = TextUtils.join(" ", words);

            boolean isCatched = false;

            try
            {
                MnemonicValidator.ofWordList(English.INSTANCE).validate(separatedWords);
            }
            catch (InvalidChecksumException e)
            {
                e.printStackTrace();
                isCatched = true;
            }
            catch (InvalidWordCountException e)
            {
                e.printStackTrace();
                isCatched = true;
            }
            catch (WordNotFoundException e)
            {
                e.printStackTrace();
                isCatched = true;
            }
            catch (UnexpectedWhiteSpaceException e)
            {
                e.printStackTrace();
                isCatched = true;
            }
            finally
            {
                isValid = !isCatched;
            }
        }

        return isValid;
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
