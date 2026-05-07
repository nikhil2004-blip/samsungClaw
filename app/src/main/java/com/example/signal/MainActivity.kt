package com.example.signal

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.signal.data.repository.OnboardingRepositoryInterface
import com.example.signal.ui.navigation.MainNavigation
import com.example.signal.ui.onboarding.OnboardingScreen
import com.example.signal.ui.theme.SignalTheme
import com.example.signal.worker.WorkManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var onboardingRepository: OnboardingRepositoryInterface

    // ── Calendar permission request ────────────────────────────────────────────
    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants[Manifest.permission.WRITE_CALENDAR] == true
            android.util.Log.d("MainActivity", "Calendar permission granted: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kick off daily overdue scan
        WorkManagerHelper.scheduleOverdueScan(applicationContext)

        // Request calendar permissions only if not already granted
        val hasRead = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasWrite = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasRead || !hasWrite) {
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
        }

        setContent {
            SignalTheme {
                val onboardingDone by produceState<Boolean?>(initialValue = null, key1 = onboardingRepository) {
                    onboardingRepository.onboardingCompleted.collect { value = it }
                }

                when (onboardingDone) {
                    null  -> LaunchSplash()
                    true  -> MainNavigation()
                    false -> OnboardingScreen(onComplete = {
                        lifecycleScope.launch {
                            onboardingRepository.setOnboardingCompleted(true)
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun LaunchSplash() {
    val gradient = Brush.verticalGradient(colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF6C63FF))
            Text(
                text = "SIGNAL",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preparing your workspace...",
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}