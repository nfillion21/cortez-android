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

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.tezos.core.models.CustomTheme
import com.tezos.ui.R
import net.glxn.qrgen.android.QRCode

class SharingAddressFragment : Fragment()
{
    private var mLinearLayout: LinearLayout? = null
    private var mShareButton: Button? = null
    private var mPublicKeyHash: String? = null

    private var mPkhTextview: TextView? = null

    companion object
    {
        const val PKH_KEY = "pkh_key"

        @JvmStatic
        fun newInstance(publicKeyHash:String, theme: CustomTheme) =
                SharingAddressFragment().apply {
                    arguments = Bundle().apply {
                        putBundle(CustomTheme.TAG, theme.toBundle())
                        putString(PKH_KEY, publicKeyHash)
                    }
                }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        var theme:CustomTheme? = null
        arguments?.let {
            val themeBundle = it.getBundle(CustomTheme.TAG)
            theme = CustomTheme.fromBundle(themeBundle)

            mPublicKeyHash = it.getString(PKH_KEY)
        }

        val dm = resources.displayMetrics
        val width = dm.widthPixels / 2

        val myBitmap = QRCode.from(mPublicKeyHash).withSize(width, width).bitmap()
        val myImage = view.findViewById<ImageView>(R.id.qr_code)
        myImage.setImageBitmap(myBitmap)

        mLinearLayout = view.findViewById(R.id.pkh_info_layout)
        mLinearLayout?.setOnTouchListener { _, _ ->
            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText(getString(R.string.copied_pkh), mPublicKeyHash)
            clipboard!!.primaryClip = clip

            Toast.makeText(activity, getString(R.string.copied_your_pkh), Toast.LENGTH_SHORT).show()
            false
        }

        mShareButton = view.findViewById(R.id.shareButton)
        mShareButton?.setOnClickListener { view ->

            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mPublicKeyHash)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)))
        }

        mPkhTextview = view.findViewById(R.id.pkh_textview)
        mPkhTextview?.text = mPublicKeyHash
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        //return super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_sharing_address, container, false)
    }
}
