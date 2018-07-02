package com.tezos.ui.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.adapter.MnemonicWordsViewAdapter;
import com.tezos.ui.widget.OffsetDecoration;

import java.util.ArrayList;
import java.util.List;

public class RestoreWalletFragment extends Fragment implements MnemonicWordsViewAdapter.OnItemClickListener
{
    private static final String WORDS_KEY = "words_key";

    private OnWordSelectedListener mCallback;

    private MnemonicWordsViewAdapter mAdapter;
    private RecyclerView mRecyclerView;

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

        mRecyclerView = view.findViewById(R.id.words);
        setUpWordGrid(mRecyclerView);

        if (savedInstanceState != null)
        {
            ArrayList<String> words = savedInstanceState.getStringArrayList(WORDS_KEY);
            if (words != null)
            {
                mAdapter.updateWords(words);
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
        }
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
           // mCallback.onWordCardNumberClicked(++position);
        }
        // and then click on the next word
    }

    @Override
    public void onResume()
    {
        //getActivity().supportStartPostponedEnterTransition();
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
