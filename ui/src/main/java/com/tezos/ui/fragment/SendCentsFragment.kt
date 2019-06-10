package com.tezos.ui.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatDialogFragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.models.CustomTheme

import com.tezos.ui.R
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.encryption.KeyStoreWrapper
import com.tezos.ui.fragment.ScriptFragment.Companion.CONTRACT_PUBLIC_KEY
import com.tezos.ui.utils.Storage
import com.tezos.ui.utils.ecKeyFormat
import kotlinx.android.synthetic.main.dialog_sent_cents.*
import java.security.interfaces.ECPublicKey

class SendCentsFragment : AppCompatDialogFragment()
{
    private var listener: OnSendCentsInteractionListener? = null

    private var mTransferFees:Long = -1

    interface OnSendCentsInteractionListener
    {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object
    {
        const val TAG = "send_cents_fragment"

        private const val TRANSFER_FEE_KEY = "transfer_fee_key"

        @JvmStatic
        fun newInstance(contractPkh:String, theme: CustomTheme) =
                SendCentsFragment().apply {
                    arguments = Bundle().apply {

                        val bundleTheme = theme.toBundle()
                        putBundle(CustomTheme.TAG, bundleTheme)
                        putString(CONTRACT_PUBLIC_KEY, contractPkh)
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, 0)
        arguments?.let {
            val bundleTheme = it.getBundle(CustomTheme.TAG)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        //dialog.setTitle(getString(R.string.delegate_and_contract_title))

        validateSendCentsButton(isTransferFeeValid())

        from_radio_group.setOnCheckedChangeListener { _, i ->

            when (i)
            {
                R.id.from_contract_button ->
                {
                    val i = i
                    val ki = i
                }

                R.id.from_tz1_button ->
                {
                    val seed = Storage(context!!).getMnemonics()
                    if (seed.mnemonics.isEmpty())
                    {
                        showSnackBar(getString(R.string.no_mnemonics_refill_tz3), ContextCompat.getColor(activity!!, R.color.tz_accent), Color.RED)
                        from_contract_button.isChecked = true
                    }
                }
            }
        }

        val tz3 = retrieveTz3()
        tz3_address_textview.text = tz3

        arguments?.let {
            from_contract_button.text = it.getString(CONTRACT_PUBLIC_KEY)
        }

        val seed = Storage(context!!).getMnemonics()
        from_tz1_button.text = seed.pkh
        if (seed.mnemonics.isEmpty())
        {
            //showSnackBar(getString(R.string.no_mnemonics_refill_tz3), ContextCompat.getColor(activity!!, R.color.tz_accent), Color.RED)
            from_tz1_button.isEnabled = false
        }

        if (savedInstanceState != null)
        {
            mTransferFees = savedInstanceState.getLong(TRANSFER_FEE_KEY, -1)
        }
        else
        {
            startInitTransferLoading()
        }
    }

    override fun onResume()
    {
        super.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_sent_cents, container, false)
    }

    private fun startInitTransferLoading()
    {
        // we need to inform the UI we are going to call transfer
        transferLoading(true)

        /*
        putFeesToNegative()
        putPayButtonToNull()

        // validatePay cannot be valid if there is no fees
        validateRemoveDelegateButton(false)

        startPostRequestLoadInitRemoveDelegate()
        */
    }

    private fun transferLoading(loading:Boolean)
    {
        // handle the visibility of bottom buttons

        if (loading)
        {
            fee_progress?.visibility = View.VISIBLE
        }
        else
        {
            fee_progress?.visibility = View.GONE
        }
    }

    private fun putFeesToNegative()
    {
        fee_edittext?.setText("")

        mClickCalculate = false
        fee_edittext?.isEnabled = false
        fee_edittext?.hint = getString(R.string.neutral)

        mDelegateFees = -1

        mDelegatePayload = null
    }
    private fun isTransferFeeValid():Boolean
    {
        val isFeeValid = false

        if (fee_edittext?.text != null && !TextUtils.isEmpty(fee_edittext?.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val fee = fee_edittext.text.toString().toDouble()

                if (fee >= 0.000001f)
                {
                    val longTransferFee = fee*1000000
                    mTransferFees = longTransferFee.toLong()
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mTransferFees = -1
                return false
            }
        }

        return isFeeValid
    }

    private fun validateSendCentsButton(validate: Boolean)
    {
        val bundleTheme = arguments!!.getBundle(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(bundleTheme)

        if (validate)
        {
            send_cents_from_KT1_button.setTextColor(ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
            send_cents_from_KT1_button_layout.isEnabled = true
            send_cents_from_KT1_button_layout.background = makeSelector(theme)

            val drawables = send_cents_from_KT1_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, theme.textColorPrimaryId))
        }
        else
        {
            send_cents_from_KT1_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
            send_cents_from_KT1_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            send_cents_from_KT1_button_layout.background = makeSelector(greyTheme)

            val drawables = send_cents_from_KT1_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
        }
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    private fun retrieveTz3():String?
    {
        var keyPair = KeyStoreWrapper().getAndroidKeyStoreAsymmetricKeyPair(EncryptionServices.SPENDING_KEY)
        if (keyPair != null)
        {
            val ecKey = keyPair!!.public as ECPublicKey
            return CryptoUtils.generatePkhTz3(ecKeyFormat(ecKey))
        }

        return null
    }

    fun showSnackBar(res:String, color:Int, textColor:Int)
    {
        val snackbar = Snackbar.make(rootView, res, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(color)
        snackbar.setActionTextColor(textColor)
        snackbar.show()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putLong(TRANSFER_FEE_KEY, mTransferFees)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnSendCentsInteractionListener)
        {
            listener = context
        }
        else
        {
            //throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach()
    {
        super.onDetach()
        listener = null
    }
}
