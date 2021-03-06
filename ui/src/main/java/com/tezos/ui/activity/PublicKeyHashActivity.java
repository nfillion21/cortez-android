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

package com.tezos.ui.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;

import net.glxn.qrgen.android.QRCode;

public class PublicKeyHashActivity extends BaseSecureActivity
{
    public static final String PKH_KEY = "pkh_key";

    private String mPublicKeyHash;

    public static Intent getStartIntent(Context context, String publicKeyHash, Bundle themeBundle)
    {
        Intent starter = new Intent(context, PublicKeyHashActivity.class);
        starter.putExtra(CustomTheme.TAG, themeBundle);
        starter.putExtra(PKH_KEY, publicKeyHash);

        return starter;
    }

    public static void start(Activity activity, String publicKeyHash, CustomTheme theme)
    {
        Intent starter = getStartIntent(activity, publicKeyHash, theme.toBundle());
        ActivityCompat.startActivityForResult(activity, starter, -1, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_public_key);

        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);
        initToolbar(theme);

        if (savedInstanceState == null) {}

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels / 2;

        mPublicKeyHash = getIntent().getStringExtra(PKH_KEY);

        Bitmap myBitmap = QRCode.from(mPublicKeyHash).withSize(width, width).bitmap();
        ImageView myImage = findViewById(R.id.qr_code);
        myImage.setImageBitmap(myBitmap);

        LinearLayout mLinearLayout = findViewById(R.id.pkh_info_layout);
        mLinearLayout.setOnTouchListener((view, motionEvent) ->
        {
            view.performClick();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.copied_pkh), mPublicKeyHash);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(PublicKeyHashActivity.this, getString(R.string.copied_your_pkh), Toast.LENGTH_SHORT).show();
            return false;
        });


        Button mShareButton = findViewById(R.id.shareButton);
        mShareButton.setOnClickListener(view -> {

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, mPublicKeyHash);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)));
        });

        String pkhash = getIntent().getStringExtra(PKH_KEY);
        TextView mPkhTextview = findViewById(R.id.pkh_textview);
        mPkhTextview.setText(pkhash);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    private void initToolbar(CustomTheme theme)
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.getColorPrimaryId()));
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this,
                theme.getColorPrimaryDarkId()));
        try
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        catch (Exception e)
        {
            Log.getStackTraceString(e);
        }

        ImageButton mCloseButton = findViewById(R.id.close_button);
        mCloseButton.setColorFilter((ContextCompat.getColor(this, theme.getTextColorPrimaryId())));
        mCloseButton.setOnClickListener(v -> {
            //requests stop in onDestroy.
            finish();
        });

        TextView mTitleBar = findViewById(R.id.barTitle);
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));
    }
}

