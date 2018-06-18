package com.tezos.android

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tezos.ui.activity.PaymentScreenActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PaymentScreenActivity.start(this)
    }
}
