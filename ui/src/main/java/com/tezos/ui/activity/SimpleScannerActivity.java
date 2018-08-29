package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.Result;
import com.tezos.core.utils.Utils;
import com.tezos.ui.R;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class SimpleScannerActivity extends Activity implements ZXingScannerView.ResultHandler
{
    private ZXingScannerView mScannerView;

    public static String EXTRA_SCAN_RESULT = "extra_scan_result_key";

    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult)
    {
        Intent intent = getIntent();

        if (Utils.isTzAddressValid(rawResult.getText()))
        {
            intent.putExtra(EXTRA_SCAN_RESULT, rawResult.getText());
            setResult(R.id.scan_succeed, intent);
        }
        else
        {
            setResult(R.id.scan_failed, intent);
        }

        finish();

        // Do something with the result here
        //Log.v("", rawResult.getText()); // Prints scan results
        //Log.v("", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        // If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
    }
}
