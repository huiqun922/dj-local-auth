package com.djlocalauth

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity;
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import kotlinx.coroutines.*
import java.util.HashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val DEVICE_CREDENTIAL_FALLBACK_CODE = 6


class DjLocalAuthModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  override fun getName(): String {
    return NAME
  }
  companion object {
    const val NAME = "DjLocalAuth"
  }
  private val context: Context
    get() = reactApplicationContext ?: throw Exception()

  private val keyguardManager: KeyguardManager
    get() = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager



  private val biometricManager by lazy { BiometricManager.from(context)}
  private val packageManager by lazy { context.packageManager }
  private var biometricPrompt: BiometricPrompt? = null
  private var promise: Promise? = null
  private var authOptions: ReadableMap? = null
  private var isRetryingWithDeviceCredentials = false
  private var isAuthenticating = false

  override fun onActivityResult(
    activity: Activity?,
    requestCode: Int,
    resultCode: Int,
    intent: Intent?
  ) {
    if (requestCode == DEVICE_CREDENTIAL_FALLBACK_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        promise?.resolve(createResponse())
      } else {
        promise?.resolve(
          createResponse(
            error = "user_cancel",
            warning = "Device Credentials canceled"
          )
        )
      }

      isAuthenticating = false
      isRetryingWithDeviceCredentials = false
      biometricPrompt = null
      promise = null
      authOptions = null
    } else if (activity is FragmentActivity) {
      // If the user uses PIN as an authentication method, the result will be passed to the `onActivityResult`.
      // Unfortunately, react-native doesn't pass this value to the underlying fragment - we won't resolve the promise.
      // So we need to do it manually.
      val fragment = activity.supportFragmentManager.findFragmentByTag("androidx.biometric.BiometricFragment")
      fragment?.onActivityResult(requestCode and 0xffff, resultCode, intent)
    }
  }
  override fun onNewIntent(p0: Intent?) {
    TODO("Not yet implemented")
  }

  private val authenticationCallback: BiometricPrompt.AuthenticationCallback = @RequiresApi(Build.VERSION_CODES.P)
  object : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
      isAuthenticating = false
      isRetryingWithDeviceCredentials = false
      biometricPrompt = null
      promise?.resolve(
        WritableNativeMap().apply {
          putBoolean("success", true)
        }
      )
      promise = null
      authOptions = null
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
      // Make sure to fallback to the Device Credentials if the Biometrics hardware is unavailable.
      if (isBiometricUnavailable(errMsgId) && isDeviceSecure && !isRetryingWithDeviceCredentials) {
        val options = authOptions

        if (options != null) {
          val disableDeviceFallback = options.getBoolean("disableDeviceFallback")

          // Don't run the device credentials fallback if it's disabled.
          if (!disableDeviceFallback) {
            promise?.let {
              isRetryingWithDeviceCredentials = true
              promptDeviceCredentialsFallback(options, it)
              return
            }
          }
        }
      }

      isAuthenticating = false
      isRetryingWithDeviceCredentials = false
      biometricPrompt = null
      promise?.resolve(
        createResponse(
          error = convertErrorCode(errMsgId),
          warning = errString.toString()
        )
      )
      promise = null
      authOptions = null
    }
  }
  @ReactMethod
  fun authenticate(options: ReadableMap, promise: Promise) {
    if (isAuthenticating) {
      this.promise?.resolve(
        createResponse(
          error = "app_cancel"
        )
      )
      this.promise = promise
      return
    }

    val fragmentActivity = currentActivity as? FragmentActivity
    if (fragmentActivity == null) {
      this.promise?.resolve(
        createResponse(
          error = "miss_activity"
        )
      )
      this.promise = promise
      return
    }

    val promptMessage = if(options.hasKey("promptMessage")){options.getString("promptMessage")}else{context.getString(R.string.prompt_message)}
    val cancelLabel = if(options.hasKey("cancelLabel")){options.getString("cancelLabel")}else{context.getString(R.string.cancel_label)}
    val requireConfirmation = options.hasKey("requireConfirmation") && options.getBoolean("requireConfirmation")
    val allowedAuthenticators =
      options.getString("biometricsSecurityLevel")?.let { toNativeBiometricSecurityLevel(it) };
//    val allowedAuthenticators = if (options["disableDeviceFallback"] == true) {
//      toNativeBiometricSecurityLevel(options["biometricsSecurityLevel"])
//    } else {
//      options.biometricsSecurityLevel.toNativeBiometricSecurityLevel() or BiometricManager.Authenticators.DEVICE_CREDENTIAL
//    }

    isAuthenticating = true
    this.promise = promise
    val executor: Executor = Executors.newSingleThreadExecutor()
    biometricPrompt = BiometricPrompt(fragmentActivity, executor, authenticationCallback)
    val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder().apply {
      promptMessage?.let { setTitle(it.toString()) }
      if (allowedAuthenticators != null) {
        setAllowedAuthenticators(allowedAuthenticators)
      }
      cancelLabel?.let { setNegativeButtonText(it.toString()) }
      if (requireConfirmation != null) {
        setConfirmationRequired(requireConfirmation)
      }
    }

    val promptInfo = promptInfoBuilder.build()
    try {
      GlobalScope.launch(Dispatchers.Main) {
        biometricPrompt!!.authenticate(promptInfo)
      }
    } catch (e: NullPointerException) {
      promise.reject(Exception("Canceled authentication due to an internal error", e))
    }
  }

  private fun promptDeviceCredentialsFallback(options: ReadableMap, promise: Promise) {
    val fragmentActivity = currentActivity as FragmentActivity?
    if (fragmentActivity == null) {
      promise.resolve(
        createResponse(
          error = "not_available",
          warning = "getCurrentActivity() returned null"
        )
      )
      return
    }

    val promptMessage = options.getString("promptMessage")
    val requireConfirmation = options.getBoolean("requireConfirmation")

    // BiometricPrompt callbacks are invoked on the main thread so also run this there to avoid
    // having to do locking.
    GlobalScope.launch(Dispatchers.Main) {
      // On Android devices older than 11, we need to use Keyguard to unlock by Device Credentials.
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        val credentialConfirmationIntent = keyguardManager.createConfirmDeviceCredentialIntent(promptMessage, "")
        fragmentActivity.startActivityForResult(credentialConfirmationIntent, DEVICE_CREDENTIAL_FALLBACK_CODE)
        return@launch
      }

      val executor: Executor = Executors.newSingleThreadExecutor()
      val localBiometricPrompt = BiometricPrompt(fragmentActivity, executor, authenticationCallback)

      biometricPrompt = localBiometricPrompt

      val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder().apply {
        if (promptMessage != null) {
          setTitle(promptMessage)
        }
        setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (requireConfirmation != null) {
          setConfirmationRequired(requireConfirmation)
        }
      }

      val promptInfo = promptInfoBuilder.build()
      try {
        GlobalScope.launch(Dispatchers.Main) {
          localBiometricPrompt.authenticate(promptInfo)
        }
      } catch (e: NullPointerException) {
        promise.reject(Exception("Canceled authentication due to an internal error", e))
      }
    }
  }

  private fun toNativeBiometricSecurityLevel(level: String): Int {
    return when (level) {
      "weak" -> BiometricManager.Authenticators.BIOMETRIC_WEAK
      "strong" -> BiometricManager.Authenticators.BIOMETRIC_STRONG
      else -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
  }

  private fun hasSystemFeature(feature: String) = packageManager.hasSystemFeature(feature)

  // NOTE: `KeyguardManager#isKeyguardSecure()` considers SIM locked state,
  // but it will be ignored on falling-back to device credential on biometric authentication.
  // That means, setting level to `SECURITY_LEVEL_SECRET` might be misleading for some users.
  // But there is no equivalent APIs prior to M.
  // `andriodx.biometric.BiometricManager#canAuthenticate(int)` looks like an alternative,
  // but specifying `BiometricManager.Authenticators.DEVICE_CREDENTIAL` alone is not
  // supported prior to API 30.
  // https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)
  private val isDeviceSecure: Boolean
    get() = keyguardManager.isDeviceSecure

  private fun convertErrorCode(code: Int): String {
    return when (code) {
      BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED -> "user_cancel"
      BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> "not_available"
      BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "lockout"
      BiometricPrompt.ERROR_NO_SPACE -> "no_space"
      BiometricPrompt.ERROR_TIMEOUT -> "timeout"
      BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> "unable_to_process"
      else -> "unknown"
    }
  }

  private fun isBiometricUnavailable(code: Int): Boolean {
    return when (code) {
      BiometricPrompt.ERROR_HW_NOT_PRESENT,
      BiometricPrompt.ERROR_HW_UNAVAILABLE,
      BiometricPrompt.ERROR_NO_BIOMETRICS,
      BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
      BiometricPrompt.ERROR_NO_SPACE -> true

      else -> false
    }
  }

  private fun canAuthenticateUsingWeakBiometrics(): Int =
    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

  private fun canAuthenticateUsingStrongBiometrics(): Int =
    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

  private fun createResponse(
    error: String? = null,
    warning: String? = null
  ) =  WritableNativeMap().apply {
    putBoolean("success", error == null)
    error?.let {
      putString("error", it)
    }
    warning?.let {
      putString("warning", it)
    }
  }

}
