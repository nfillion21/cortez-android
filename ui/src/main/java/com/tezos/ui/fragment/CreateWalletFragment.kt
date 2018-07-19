package com.tezos.ui.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.AppCompatCheckBox
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R

class CreateWalletFragment : Fragment()
{
    private val MNEMONICS_KEY = "mnemonics_key"
    private val BACKUP_CHECKBOX_KEY = "backup_checkbox_key"

    private var mRenewFab: FloatingActionButton? = null
    private var mMnemonicsTextview: TextView? = null
    private var mMnemonicsString: String? = null

    private var mCreateButton: Button? = null
    private var mBackupCheckbox: AppCompatCheckBox? = null
    private var mCreateButtonLayout: FrameLayout? = null

    private var mBackupChecked: Boolean = false

    companion object
    {
        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                CreateWalletFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        var theme:CustomTheme? = null
        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
            theme = CustomTheme.fromBundle(themeBundle)
        }

        mMnemonicsTextview = view.findViewById(R.id.mnemonics_textview)
        mMnemonicsTextview?.setTextColor(ContextCompat.getColor(activity!!, theme!!.colorPrimaryDarkId))

        mBackupCheckbox = view.findViewById(R.id.backup_checkbox)

        mCreateButton = view.findViewById(R.id.create_button)
        mCreateButtonLayout = view.findViewById(R.id.create_button_layout)

        mCreateButtonLayout?.visibility = View.VISIBLE

        mCreateButton?.setText(R.string.create_wallet)

        mCreateButtonLayout?.setOnClickListener { _ ->

            if (mMnemonicsString != null)
            {
                /*
                val intent = getIntent()

                //TODO verify if it does always work
                val keyBundle = CryptoUtils.generateKeys(mMnemonicsString)
                intent.putExtra(CryptoUtils.WALLET_BUNDLE_KEY, keyBundle)
                setResult(R.id.create_wallet_succeed, intent)
                finish()
                */
            }
        }

        mRenewFab = view.findViewById(R.id.renew)
        mRenewFab?.setOnClickListener { v ->
            val i = v.id
            if (i == R.id.renew)
            {
                mBackupCheckbox?.isEnabled = false

                mRenewFab?.isEnabled = false

                val textViewAnimator = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_X, 0f)
                val textViewAnimator2 = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_Y, 0f)
                val textViewAnimator3 = ObjectAnimator.ofFloat(mRenewFab, View.ALPHA, 0f)

                val animatorSet = AnimatorSet()
                animatorSet.interpolator = FastOutSlowInInterpolator()
                animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
                animatorSet.addListener(object : AnimatorListenerAdapter()
                {
                    override fun onAnimationEnd(animation: Animator)
                    {
                        super.onAnimationEnd(animation)

                        mMnemonicsString = CryptoUtils.generateMnemonics()
                        mMnemonicsTextview?.text = mMnemonicsString
                        // renew the mnemonic
                        showDoneFab()
                    }
                })
                animatorSet.start()
            } else
            {
                throw UnsupportedOperationException(
                        "The onClick method has not been implemented for " + resources
                                .getResourceEntryName(v.id))
            }
        }

        if (savedInstanceState == null) {
            mMnemonicsString = CryptoUtils.generateMnemonics()
            if (mMnemonicsString != null) {
                mMnemonicsTextview?.text = mMnemonicsString
            }

            mBackupChecked = false
        } else {
            mMnemonicsString = savedInstanceState.getString(MNEMONICS_KEY, null)
            if (mMnemonicsString != null) {
                mMnemonicsTextview?.text = mMnemonicsString
            }

            mBackupChecked = savedInstanceState.getBoolean(BACKUP_CHECKBOX_KEY, false)
            mBackupCheckbox?.isChecked = mBackupChecked
        }

        validateCreateButton(isCreateButtonValid(), theme)

        mBackupCheckbox?.setOnCheckedChangeListener { buttonView, isChecked ->
            mBackupChecked = buttonView.isChecked
            validateCreateButton(isCreateButtonValid(), theme)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_create_wallet, container, false)
    }

    /*
    private fun showSnackbarError(network :Boolean)
    {
        var error:Int = if (network)
        {
            R.string.network_error
        }
        else
        {
            R.string.generic_error
        }

        val snackbar = Snackbar.make(mCoordinatorLayout!!, error, Snackbar.LENGTH_LONG)
        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor((ContextCompat.getColor(activity!!,
                android.R.color.holo_red_light)))
        snackbar.show()
    }
    */

    protected fun isCreateButtonValid(): Boolean
    {
        return mBackupChecked
    }

    private fun showDoneFab()
    {
        mRenewFab?.show()

        mRenewFab?.scaleX = 0f
        mRenewFab?.scaleY = 0f

        val textViewAnimator = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_X, 1f)
        val textViewAnimator2 = ObjectAnimator.ofFloat(mRenewFab, View.SCALE_Y, 1f)
        val textViewAnimator3 = ObjectAnimator.ofFloat(mRenewFab, View.ALPHA, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.startDelay = 200

        animatorSet.interpolator = FastOutSlowInInterpolator()
        animatorSet.play(textViewAnimator).with(textViewAnimator2).with(textViewAnimator3)
        animatorSet.addListener(object : AnimatorListenerAdapter()
        {
            override fun onAnimationEnd(animation: Animator)
            {
                super.onAnimationEnd(animation)
                mRenewFab?.isEnabled = true

                mBackupCheckbox?.isEnabled = true
            }
        })
        animatorSet.start()
    }

    fun validateCreateButton(validate: Boolean, theme: CustomTheme?)
    {
        if (validate)
        {
            mCreateButton?.setTextColor(ContextCompat.getColor(activity!!, theme!!.textColorPrimaryId))
            mCreateButtonLayout?.isEnabled = true
            mCreateButtonLayout?.background = makeSelector(theme)

            val drawables = mCreateButton?.getCompoundDrawables()
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme!!.textColorPrimaryId))

            mRenewFab?.isEnabled = false
            mRenewFab?.hide()
        } else {
            mCreateButton?.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            mCreateButtonLayout?.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            mCreateButtonLayout?.background = makeSelector(greyTheme)

            val drawables = mCreateButton?.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))

            mRenewFab?.show()
            mRenewFab?.isEnabled = true
        }
    }

    private fun makeSelector(theme: CustomTheme?): StateListDrawable {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme!!.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putString(MNEMONICS_KEY, mMnemonicsString)
        outState.putBoolean(BACKUP_CHECKBOX_KEY, mBackupChecked)
    }
}
