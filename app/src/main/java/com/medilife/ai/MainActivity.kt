package com.medilife.ai

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.medilife.ai.ui.MediLifeRoot
import com.medilife.ai.ui.MediViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    /** true while the biometric lock screen should cover the app. */
    val locked = MutableStateFlow(true)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MediLifeApp).container
        val vm = ViewModelProvider(this, MediViewModel.factory(container))[MediViewModel::class.java]
        lifecycleScope.launch {
            if (!container.settings.current().biometricLock) locked.value = false else authenticate()
        }
        setContent { MediLifeRoot(vm, this) }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }

    override fun onRestart() {
        super.onRestart()
        // Re-lock after 60 s in the background (short trips to the file picker/camera don't re-lock).
        lifecycleScope.launch {
            val c = (application as MediLifeApp).container
            if (c.settings.current().biometricLock && System.currentTimeMillis() - stoppedAt > 60_000) {
                locked.value = true
                authenticate()
            }
        }
    }

    fun canUseBiometric(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate() {
        if (!canUseBiometric()) { locked.value = false; return }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { locked.value = false }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock MediLife AI")
                .setSubtitle("Your health data is protected")
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build()
        )
    }
}
