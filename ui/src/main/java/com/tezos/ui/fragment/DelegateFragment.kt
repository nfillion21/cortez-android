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

package com.tezos.ui.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.authentication.AuthenticationDialog
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.delegate_form_card_info.*
import kotlinx.android.synthetic.main.fragment_delegate.*

class DelegateFragment : Fragment()
{
    private var mSpinnerPosition:Int = 0
    private var mTheme:CustomTheme? = null

    private var listener: OnDelegateListener? = null

    companion object
    {
        private val DELEGATE_SPINNER_POS_KEY = "delegate_spinner_pos_key"

        @JvmStatic
        fun newInstance(theme: CustomTheme) =
                DelegateFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        add_delegate_button_layout?.setOnClickListener { v ->
        }

        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
            mTheme = CustomTheme.fromBundle(themeBundle)
        }

        validateAddButton(isInputDataValid())

        validateAddButton(isInputDataValid())

        add_delegate_button_layout.setOnClickListener {
            onDelegateClick()
        }

        val focusChangeListener = this.focusChangeListener()

        tezos_address.addTextChangedListener(GenericTextWatcher(tezos_address))

        tezos_address.onFocusChangeListener = focusChangeListener

        val adapter = ArrayAdapter.createFromResource(activity,
                R.array.array_fee, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        if (savedInstanceState != null)
        {
            mSpinnerPosition = savedInstanceState.getInt(DELEGATE_SPINNER_POS_KEY, 0)
        }
    }

    override fun onResume()
    {
        super.onResume()

        putEverythingInRed()
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        if (context is OnDelegateListener)
        {
            listener = context
        }
        else
        {
            throw RuntimeException(context.toString() + " must implement OnDelegateListener")
        }
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id

            if (i == R.id.tezos_address)
            {
                putTzAddressInRed(!hasFocus)
            }
            else
            {
                throw UnsupportedOperationException(
                        "onFocusChange has not been implemented for " + resources.getResourceName(v.id))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_delegate, container, false)
    }

    private fun validateAddButton(validate: Boolean)
    {
        if (mTheme != null)
        {
            if (validate)
            {
                add_delegate_button.setTextColor(ContextCompat.getColor(activity!!, mTheme!!.textColorPrimaryId))
                add_delegate_button_layout.isEnabled = true
                add_delegate_button_layout.background = makeSelector(mTheme!!)

                val drawables = add_delegate_button.compoundDrawables
                val wrapDrawable = DrawableCompat.wrap(drawables!![0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, mTheme!!.textColorPrimaryId))
            }
            else
            {
                add_delegate_button.setTextColor(ContextCompat.getColor(activity!!, android.R.color.white))
                add_delegate_button_layout.isEnabled = false

                val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
                add_delegate_button_layout.background = makeSelector(greyTheme)

                val drawables = add_delegate_button.compoundDrawables
                val wrapDrawable = DrawableCompat.wrap(drawables!![0])
                DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity!!, android.R.color.white))
            }
        }
    }

    private fun isInputDataValid(): Boolean
    {
        return isTzAddressValid()
    }

    private fun isTzAddressValid(): Boolean
    {
        val isTzAddressValid = false

        return if (!TextUtils.isEmpty(tezos_address.text))
        {
            Utils.isTzAddressValid(tezos_address.text!!.toString())
        }

        else isTzAddressValid
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(activity!!, theme.colorPrimaryId)))
        return res
    }

    private fun putEverythingInRed()
    {
        this.putTzAddressInRed(true)
    }

    private fun putTzAddressInRed(red: Boolean)
    {
        val color: Int

        val tzAddressValid = isTzAddressValid()

        if (red && !tzAddressValid)
        {
            color = R.color.tz_error
        }
        else
        {
            color = R.color.tz_accent
        }

        tezos_address.setTextColor(ContextCompat.getColor(activity!!, color))
    }

// put everything in RED

    private fun setTextPayButton(amount: String)
    {
        var amount = amount
        var amountDouble: Double = amount.toDouble()

        //val selectedItemThreeDS = fee_spinner.selectedItemId

        /*
        when (selectedItemThreeDS.toInt())
        {
            0 -> {
                amountDouble += 0.01
            }

            1 -> {
                amountDouble += 0.00
            }

            2 -> {
                amountDouble += 0.05
            }

            else -> {
                //no-op
            }
        }
        */

//amount = java.lang.Double.toString(amountDouble)
        amount = amountDouble.toString()

//check the correct amount
        if (amount.contains("."))
        {
            val elements = amount.substring(amount.indexOf("."))

            when
            {
                elements.length > 7 ->
                {
                    amount = String.format("%.6f", amount.toDouble())
                    val d = amount.toDouble()
                    amount = d.toString()
                }

                elements.length <= 3 ->
                {
                    amount = String.format("%.2f", amount.toDouble())
                }
                else ->
                {
                    //                        int length = elements.length() - 1;
                    //                        String format = "%." + length + "f";
                    //                        Float f = Float.parseFloat(amount);
                    //                        amount = String.format(format, f);
                }
            }
        }
        else
        {
            amount = String.format("%.2f", amount.toDouble())
//amount = Double.parseDouble(amount).toString();
        }

        val moneyFormatted2 = "$amount ꜩ"
//String moneyFormatted3 = Double.toString(amountDouble) + " ꜩ";
        add_delegate_button.text = getString(R.string.delegate_format, moneyFormatted2)
    }

    private fun onDelegateClick()
    {
        val dialog = AuthenticationDialog()
        if (listener?.isFingerprintAllowed()!! && listener?.hasEnrolledFingerprints()!!)
        {
            dialog.cryptoObjectToAuthenticateWith = EncryptionServices(activity!!).prepareFingerprintCryptoObject()
            dialog.fingerprintInvalidationListener = { onFingerprintInvalidation(it) }
            dialog.fingerprintAuthenticationSuccessListener = {
                validateKeyAuthentication(it)
            }
            if (dialog.cryptoObjectToAuthenticateWith == null)
            {
                dialog.stage = AuthenticationDialog.Stage.NEW_FINGERPRINT_ENROLLED
            }
            else
            {
                dialog.stage = AuthenticationDialog.Stage.FINGERPRINT
            }
        }
        else
        {
            dialog.stage = AuthenticationDialog.Stage.PASSWORD
        }
        dialog.authenticationSuccessListener = {
            //startInitTransferLoading()
        }
        dialog.passwordVerificationListener =
                {
                    validatePassword(it)
                }
        dialog.show(activity!!.supportFragmentManager, "Authentication")
    }

    interface OnDelegateListener
    {
        fun isFingerprintAllowed():Boolean
        fun hasEnrolledFingerprints():Boolean

        fun saveFingerprintAllowed(useInFuture: Boolean)
    }

    /*
    private fun isFingerprintAllowed():Boolean
    {
        return storage.isFingerprintAllowed()
    }

    private fun hasEnrolledFingerprints():Boolean
    {
        return systemServices.hasEnrolledFingerprints()
    }

    private fun saveFingerprintAllowed(useInFuture:Boolean)
    {
        storage.saveFingerprintAllowed(useInFuture)
    }
    */

    /**
     * Fingerprint was invalidated, decide what to do in this case.
     */
    private fun onFingerprintInvalidation(useInFuture: Boolean)
    {
        listener?.saveFingerprintAllowed(useInFuture)
        if (useInFuture)
        {
            EncryptionServices(activity!!).createFingerprintKey()
        }
    }

    /**
     * Validate password inputted from Authentication Dialog.
     */
    private fun validatePassword(inputtedPassword: String): Boolean
    {
        val storage = Storage(activity!!)
        return EncryptionServices(activity!!).decrypt(storage.getPassword(), inputtedPassword) == inputtedPassword
    }

    private fun validateKeyAuthentication(cryptoObject: FingerprintManager.CryptoObject)
    {
        if (EncryptionServices(activity!!).validateFingerprintAuthentication(cryptoObject))
        {
            //startInitTransferLoading()
        }
        else
        {
            onDelegateClick()
        }
    }


    private inner class GenericTextWatcher internal constructor(private val v: View) : TextWatcher
    {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable)
        {
            val i = v.id

            if (i == R.id.tezos_address)
            {
                putTzAddressInRed(false)
            }
            else
            {
                throw UnsupportedOperationException(
                        "OnClick has not been implemented for " + resources.getResourceName(v.id))
            }

            validateAddButton(isInputDataValid())
        }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        outState.putInt(DELEGATE_SPINNER_POS_KEY, mSpinnerPosition)
    }
}
