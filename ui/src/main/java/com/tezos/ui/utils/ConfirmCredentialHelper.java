package com.tezos.ui.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class ConfirmCredentialHelper
{
    private static AppCompatActivity mContext;
    private static KeyguardManager mKeyguardManager;

    private static final String KEY_NAME = "my_key";
    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    private static final int AUTHENTICATION_DURATION_SECONDS = 5;
    private static final byte[] SECRET_BYTE_ARRAY = new byte[] {1, 2, 3, 4, 5, 6};

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void launchConfirmCredential(AppCompatActivity context)
    {
        mContext = context;
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        ArchLifecycleApp archLifecycleApp = (ArchLifecycleApp) context.getApplication();
        boolean isStarted = archLifecycleApp.isStarted();

        if (isStarted)
        {
            tryEncrypt();
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which
     * only works if the user has just authenticated via device credentials.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean tryEncrypt()
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
            Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // Try encrypting something, it will only work if the user authenticated within
            // the last AUTHENTICATION_DURATION_SECONDS seconds.
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            cipher.doFinal(SECRET_BYTE_ARRAY);

            // If the user has recently authenticated, you will reach here.
            //showAlreadyAuthenticated();
            return true;
        }
        catch (UserNotAuthenticatedException e)
        {
            // User is not authenticated, let's authenticate with device credentials.
            showAuthenticationScreen();
            return false;
        }
        catch (KeyPermanentlyInvalidatedException e)
        {
            // This happens if the lock screen has been disabled or reset after the key was
            // generated after the key was generated.
            Toast.makeText(mContext, "Keys are invalidated after created. Retry the purchase\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        catch (BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with device credentials within the last X seconds.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void createKey()
    {
        // Generate a key to decrypt payment credentials, tokens, etc.
        // This will most likely be a registration step for the user when they are setting up your app.

        try
        {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            // Set the pkh of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    // Require that the user has unlocked in the last 30 seconds
                    .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException
                | CertificateException | IOException e)
        {
            throw new RuntimeException("Failed to create a symmetric key", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void showAuthenticationScreen()
    {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null)
        {
            mContext.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == ConfirmCredentialHelper.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS)
        {
            // Challenge completed, proceed with using cipher
            if (resultCode == Activity.RESULT_OK)
            {
                if (tryEncrypt())
                {
                    //showPurchaseConfirmation();
                }
            } else {
                // The user canceled or didn’t complete the lock screen
                // operation. Go to error/cancellation flow.
                Toast.makeText(mContext, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPurchaseConfirmation() {
        //findViewById(R.id.confirmation_message).setVisibility(View.VISIBLE);
        //findViewById(R.id.purchase_button).setEnabled(false);
    }

    private void showAlreadyAuthenticated()
    {
        /*
        TextView textView = (TextView) findViewById(
                R.id.already_has_valid_device_credential_message);
        textView.setVisibility(View.VISIBLE);
        textView.setText(getResources().getQuantityString(
                R.plurals.already_confirmed_device_credentials_within_last_x_seconds,
                AUTHENTICATION_DURATION_SECONDS, AUTHENTICATION_DURATION_SECONDS));
        findViewById(R.id.purchase_button).setEnabled(false);
        */
    }
}
