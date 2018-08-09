package com.tezos.ui.activity

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.tezos.ui.authentication.EncryptionServices
import com.tezos.ui.authentication.SystemServices
import com.tezos.ui.utils.ArchLifecycleApp

open class BaseSecureActivity : AppCompatActivity() {

    protected val systemServices by lazy(LazyThreadSafetyMode.NONE) { SystemServices(this) }
    private var isAuthenticating = false

    private var deviceSecurityAlert: AlertDialog? = null

    override fun onStart() {
        super.onStart()
        if (!systemServices.isDeviceSecure()) {
            deviceSecurityAlert = systemServices.showDeviceSecurityAlert()
        }
    }

    override fun onStop() {
        super.onStop()
        deviceSecurityAlert?.dismiss()
    }

    override fun onResume() {
        super.onResume()

        val archLifecycleApp = application as ArchLifecycleApp
        val isStarted = archLifecycleApp.isStarted

        val encryptionServices = EncryptionServices(applicationContext)
        if (isStarted) {
            if (!isAuthenticating && encryptionServices.containsConfirmCredentialsKey() && !encryptionServices.validateConfirmCredentialsAuthentication()) {
                isAuthenticating = true
                systemServices.showAuthenticationScreen(this, SystemServices.AUTHENTICATION_SCREEN_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SystemServices.AUTHENTICATION_SCREEN_CODE) {
            isAuthenticating = false
            if (resultCode != Activity.RESULT_OK) {
                finish()
            }
        }
    }
}