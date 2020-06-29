package com.tezos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.adapter.MnemonicWordsViewAdapter
import com.tezos.ui.widget.OffsetDecoration
import java.util.*

class ExportKeysDialogFragment : AppCompatDialogFragment()
{
    private var mAdapter: MnemonicWordsViewAdapter? = null

    companion object
    {
        const val TAG = "export_keys_dialog_fragment"

        private const val MNEMONICS_WORDS_NUMBER = 24

        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                ExportKeysDialogFragment().apply {
                    arguments = Bundle().apply {

                        val bundleTheme = theme.toBundle()
                        putBundle(CustomTheme.TAG, bundleTheme)
                    }
                }

    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, 0)
        arguments?.let {
        }
        //isCancelable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        val mRecyclerView: RecyclerView = view.findViewById(R.id.words)
        setUpWordGrid(mRecyclerView)

        if (savedInstanceState != null)
        {
            /*
            val words = savedInstanceState.getStringArrayList(RestoreWalletFragment.WORDS_KEY)
            if (words != null) {
                mAdapter.updateWords(words, null)
            }
            */
        }

        val words: MutableList<String?> = ArrayList(MNEMONICS_WORDS_NUMBER)
        for (i in 0 until MNEMONICS_WORDS_NUMBER)
        {
            words.add(null)
        }
        mAdapter!!.updateWords(words, null)
    }

    private fun setUpWordGrid(wordsView: RecyclerView) {
        val spacing = context!!.resources
                .getDimensionPixelSize(R.dimen.spacing_micro)
        wordsView.addItemDecoration(OffsetDecoration(spacing))
        mAdapter = MnemonicWordsViewAdapter(activity)
        mAdapter!!.setOnItemClickListener(null)
        wordsView.adapter = mAdapter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_export_keys, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy()
    {
        super.onDestroy()
    }
}
