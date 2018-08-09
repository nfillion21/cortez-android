import android.app.Activity
import android.content.Intent
import com.tezos.ui.utils.Storage

/*
fun Activity.startSecretActivity(requestCode: Int, mode: Int = SecretActivity.MODE_CREATE, password: String? = null, secretData: Storage.SecretData? = null) {
    val intent = Intent(this, SecretActivity::class.java)
    intent.putExtra("mode", mode)
    password?.let { intent.putExtra("password", password) }
    secretData?.let { intent.putExtra("mnemonics", secretData) }
    startActivityForResult(intent, requestCode)
}
*/

//fun Activity.startHomeActivity(finishCallingActivity: Boolean = true) = startActivity(HomeActivity::class.java, finishCallingActivity)

//fun Activity.startSignUpActivity(finishCallingActivity: Boolean = true) = startActivity(SignUpActivity::class.java, finishCallingActivity)

private fun Activity.startActivity(cls: Class<*>, finishCallingActivity: Boolean = true) {
    val intent = Intent(this, cls)
    startActivity(intent)
    finishCallingActivity.let { finish() }
}