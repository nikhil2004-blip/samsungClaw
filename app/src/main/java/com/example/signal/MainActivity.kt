package com.example.signal

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.signal.data.repository.OnboardingRepository
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
    lateinit var onboardingRepository: OnboardingRepository

    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants[Manifest.permission.WRITE_CALENDAR] == true
            android.util.Log.d("MainActivity", "Calendar permission granted: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WorkManagerHelper.scheduleOverdueScan(applicationContext)

        val hasRead  = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasWrite = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasRead || !hasWrite) {
            calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        }

        setContent {
            val isDark by onboardingRepository.isDarkTheme.collectAsState(initial = true)
            val onboardingDone by produceState<Boolean?>(
                initialValue  = null,
                key1          = onboardingRepository
            ) {
                onboardingRepository.onboardingCompleted.collect { value = it }
            }

            SignalTheme(darkTheme = isDark) {
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
    val cs = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = cs.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text  = "SIGNAL",
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "Preparing your workspace…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant.copy(alpha = alpha)
                )
            }

            CircularProgressIndicator(
                color     = cs.primary,
                modifier  = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
