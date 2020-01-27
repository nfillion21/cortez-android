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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import com.tezos.ui.utils.Storage
import net.glxn.qrgen.android.QRCode

class SharingAddressFragment : Fragment()
{
    private var mLinearLayout: LinearLayout? = null
    private var mPkhTextview: TextView? = null

    private var mPkhLayout: LinearLayout? = null
    private var mPkhEmptyLayout: LinearLayout? = null

    companion object
    {
        const val TAG = "sharedAddressTag"

        const val PKH_KEY = "PKH_KEY"

        @JvmStatic
        fun newInstance(theme: CustomTheme, sharedAddress: String?) =
                SharingAddressFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(PKH_KEY, sharedAddress)
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

        mPkhLayout = view.findViewById(R.id.pkh_layout)
        mPkhEmptyLayout = view.findViewById(R.id.pkh_empty_layout)

        mLinearLayout = view.findViewById(R.id.pkh_info_layout)

        mPkhTextview = view.findViewById(R.id.pkh_textview)
    }

    override fun onResume()
    {
        super.onResume()

        //val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        val pkh = pkh()

        if (pkh != null)
        {
            mPkhLayout?.visibility = View.VISIBLE
            mPkhEmptyLayout?.visibility = View.GONE

            val dm = resources.displayMetrics

            val width:Int
            width = if (dm.widthPixels < dm.heightPixels)
            {
                dm.widthPixels / 2
            }
            else
            {
                dm.heightPixels / 2
            }

            val myBitmap = QRCode.from(pkh).withSize(width, width).bitmap()
            val myImage = view?.findViewById<ImageView>(R.id.qr_code)
            myImage?.setImageBitmap(myBitmap)

            mLinearLayout?.setOnClickListener {

                val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val clip = ClipData.newPlainText(getString(R.string.copied_pkh), pkh)
                clipboard!!.primaryClip = clip

                Toast.makeText(activity, getString(R.string.copied_your_pkh), Toast.LENGTH_SHORT).show()
            }

            mPkhTextview?.text = pkh
        }
        else
        {
            mPkhLayout?.visibility = View.GONE
            mPkhEmptyLayout?.visibility = View.VISIBLE
        }
    }

    fun pkh():String?
    {
        var pkh:String? = null

        val isPasswordSaved = Storage(activity!!).isPasswordSaved()
        if (isPasswordSaved)
        {
            pkh = arguments!!.getString(PKH_KEY)
            if (pkh == null)
            {
                val seed = Storage(activity!!).getMnemonics()
                pkh = seed.pkh
            }
        }

        return pkh
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_sharing_address, container, false)
    }
}
