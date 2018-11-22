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

package com.tezos.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.android.volley.VolleyError
import com.tezos.core.models.CustomTheme
import com.tezos.core.utils.Utils
import com.tezos.ui.R
import com.tezos.ui.utils.Storage
import kotlinx.android.synthetic.main.activity_add_delegate.*

/**
 * Created by nfillion on 20/11/18.
 */
class AddDelegateActivity : BaseSecureActivity()
{
    private val storage: Storage by lazy(LazyThreadSafetyMode.NONE) { Storage(applicationContext) }

    private var mAmountCache:Double = -1.0
    private var mSpinnerPosition:Int = 0

    companion object
    {
        var ADD_DELEGATE_REQUEST_CODE = 0x3100 // arbitrary int

        private val TRANSFER_AMOUNT_KEY = "transfer_amount_key"
        private val TRANSFER_SPINNER_POS_KEY = "transfer_spinner_pos_key"

        private fun getStartIntent(context: Context, themeBundle: Bundle): Intent
        {
            val starter = Intent(context, AddDelegateActivity::class.java)
            starter.putExtra(CustomTheme.TAG, themeBundle)

            return starter
        }

        fun start(activity: Activity, theme: CustomTheme)
        {
            val starter = getStartIntent(activity, theme.toBundle())
            ActivityCompat.startActivityForResult(activity, starter, TransferFormActivity.TRANSFER_REQUEST_CODE, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_delegate)

        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        validateAddButton(isInputDataValid())

        initToolbar(theme)

        amount_transfer.addTextChangedListener(GenericTextWatcher(amount_transfer))

        val focusChangeListener = this.focusChangeListener()
        amount_transfer.onFocusChangeListener = focusChangeListener

        tezos_address.addTextChangedListener(GenericTextWatcher(tezos_address))

        tezos_address.onFocusChangeListener = focusChangeListener

        val adapter = ArrayAdapter.createFromResource(this,
                R.array.array_fee, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fee_spinner.adapter = adapter
        fee_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long)
            {
                putAmountInRed(false)
                mSpinnerPosition = i
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }

        if (savedInstanceState != null)
        {
            mAmountCache = savedInstanceState.getDouble(TRANSFER_AMOUNT_KEY, -1.0)
            mSpinnerPosition = savedInstanceState.getInt(TRANSFER_SPINNER_POS_KEY, 0)
        }

        fee_spinner.setSelection(mSpinnerPosition)

        putEverythingInRed()
    }

    private fun focusChangeListener(): View.OnFocusChangeListener
    {
        return View.OnFocusChangeListener { v, hasFocus ->
            val i = v.id

            if (i == R.id.amount_transfer)
            {
                putAmountInRed(!hasFocus)
            }
            else if (i == R.id.tezos_address)
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

    private fun initToolbar(theme: CustomTheme)
    {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.colorPrimaryId))
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        val window = window
        window.statusBarColor = ContextCompat.getColor(this,
                theme.colorPrimaryDarkId)
        try
        {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
        }
        catch (e: Exception)
        {
            Log.getStackTraceString(e)
        }

        val mCloseButton = findViewById<ImageButton>(R.id.close_button)
        mCloseButton.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId))
        mCloseButton.setOnClickListener { _ ->
            //requests stop in onDestroy.
            finish()
        }

        //nav_progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(this, theme.textColorPrimaryId), PorterDuff.Mode.SRC_IN)

        val mTitleBar = findViewById<TextView>(R.id.barTitle)
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
    }

    private fun showSnackBar(error:VolleyError?)
    {
        var error: String? = if (error != null)
        {
            error.toString()
        }
        else
        {
            getString(R.string.generic_error)
        }

        val snackbar = Snackbar.make(findViewById(R.id.content), error.toString(), Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor((ContextCompat.getColor(this,
                android.R.color.holo_red_light)))
        snackbar.show()
    }

    private fun validateAddButton(validate: Boolean)
    {
        val themeBundle = intent.getBundleExtra(CustomTheme.TAG)
        val theme = CustomTheme.fromBundle(themeBundle)

        if (validate)
        {
            add_delegate_button.setTextColor(ContextCompat.getColor(this, theme.textColorPrimaryId))
            add_delegate_button_layout.isEnabled = true
            add_delegate_button_layout.background = makeSelector(theme)

            val drawables = add_delegate_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, theme.textColorPrimaryId))
        }
        else
        {
            add_delegate_button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            add_delegate_button_layout.isEnabled = false

            val greyTheme = CustomTheme(R.color.dark_grey, R.color.dark_grey, R.color.dark_grey)
            add_delegate_button_layout.background = makeSelector(greyTheme)

            val drawables = add_delegate_button.compoundDrawables
            val wrapDrawable = DrawableCompat.wrap(drawables!![0])
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun makeSelector(theme: CustomTheme): StateListDrawable
    {
        val res = StateListDrawable()
        res.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(ContextCompat.getColor(this, theme.colorPrimaryDarkId)))
        res.addState(intArrayOf(), ColorDrawable(ContextCompat.getColor(this, theme.colorPrimaryId)))
        return res
    }

    private inner class GenericTextWatcher internal constructor(private val v: View) : TextWatcher
    {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable)
        {
            val i = v.id

            if (i == R.id.amount_transfer)
            {
                putAmountInRed(false)
                mSpinnerPosition = i
            }

            else if (i == R.id.tezos_address)
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

    fun isInputDataValid(): Boolean
    {
        return isTransferAmountValid() && isTzAddressValid()
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


    private fun isTransferAmountValid():Boolean
    {
        val isAmountValid = false

        if (amount_transfer != null && !TextUtils.isEmpty(amount_transfer.text))
        {
            try
            {
                //val amount = java.lang.Double.parseDouble()
                val amount = amount_transfer.text!!.toString().toDouble()

                if (amount >= 0.000001f)
                {
                    mAmountCache = amount
                    return true
                }
            }
            catch (e: NumberFormatException)
            {
                mAmountCache = -1.0
                return false
            }
        }

        return isAmountValid
    }

    private fun putEverythingInRed()
    {
        this.putAmountInRed(true)
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

        tezos_address.setTextColor(ContextCompat.getColor(this, color))
    }

// put everything in RED

    private fun putAmountInRed(red: Boolean)
    {
        val color: Int

        val amountValid = isTransferAmountValid()

        if (red && !amountValid)
        {
            color = R.color.tz_error
            add_delegate_button.text = getString(R.string.delegate_format, "")
        }
        else
        {
            color = R.color.tz_accent

            if (amountValid)
            {
                val amount = amount_transfer.text.toString()
                this.setTextPayButton(amount)
            }
            else
            {
                add_delegate_button.text = getString(R.string.delegate_format, "")
            }
        }

        amount_transfer.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun setTextPayButton(amount: String)
    {
        var amount = amount
//var amountDouble: Double = java.lang.Double.parseDouble(amount)
        var amountDouble: Double = amount.toDouble()

        val selectedItemThreeDS = fee_spinner.selectedItemId

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

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        val fragment = supportFragmentManager.findFragmentById(R.id.form_fragment_container)
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle?)
    {
        super.onSaveInstanceState(outState)

        outState?.putDouble(TRANSFER_AMOUNT_KEY, mAmountCache)
        outState?.putInt(TRANSFER_SPINNER_POS_KEY, mSpinnerPosition)
    }
}
