package com.medilife.ai.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medilife.ai.ui.MediViewModel
import com.medilife.ai.ui.components.AnimatedRing
import com.medilife.ai.ui.components.DemoBadge
import com.medilife.ai.ui.components.GlassCard
import com.medilife.ai.ui.components.HeartbeatIcon
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette

@Composable
fun OnboardingScreen(vm: MediViewModel) {
    var step by remember { mutableIntStateOf(0) }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { step = 2 }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(step, transitionSpec = { (fadeIn(tween(350)) + slideInHorizontally { it / 3 }) togetherWith fadeOut(tween(200)) }, label = "onb", modifier = Modifier.weight(1f)) { st ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (st) {
                    0 -> {
                        AnimatedRing(1f, size = 170.dp, stroke = 10.dp) { HeartbeatIcon(size = 46.dp) }
                        Spacer(Modifier.height(28.dp))
                        Text("MediLife AI", style = MaterialTheme.typography.displaySmall)
                        Text("Your premium personal health companion", style = MaterialTheme.typography.titleMedium, color = Palette.Cyan)
                        Spacer(Modifier.height(16.dp))
                        Text("Medical history, medicine reminders, safety checks, lab-report insights and Medi AI — all on your phone.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = LocalMedi.current.subtle)
                    }
                    1 -> {
                        Text("Private by design", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(20.dp))
                        Bullet(Icons.Rounded.CloudOff, "Everything stays on this device. MediLife has no internet permission, ads or analytics.")
                        Bullet(Icons.Rounded.Shield, "Sensitive fields are encrypted. You can add a biometric lock, export, back up or delete everything at any time.")
                        Bullet(Icons.Rounded.Notifications, "Notifications are used only for the medicine and hydration reminders you set. You can allow them now or later.")
                        Spacer(Modifier.height(10.dp))
                        Text("MediLife gives general health information. It does not diagnose or prescribe and does not replace your doctor.", style = MaterialTheme.typography.bodySmall, color = Palette.Amber, textAlign = TextAlign.Center)
                    }
                    else -> {
                        Text("How would you like to start?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        GlassCard(Modifier.fillMaxWidth(), onClick = { vm.completeOnboarding(false) }, accent = Palette.tealGradient) {
                            Text("Start with my own data", style = MaterialTheme.typography.titleMedium)
                            Text("An empty, private record that you fill in.", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                        }
                        Spacer(Modifier.height(12.dp))
                        GlassCard(Modifier.fillMaxWidth(), onClick = { vm.completeOnboarding(true) }, accent = Palette.warmGradient) {
                            Row { Text("Explore with demo data  ", style = MaterialTheme.typography.titleMedium); DemoBadge() }
                            Text("A realistic sample patient, clearly labelled DEMO DATA, kept separate from real records. Remove it in one tap whenever you're ready.", style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle)
                        }
                    }
                }
            }
        }
        when (step) {
            0 -> Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Get started") }
            1 -> Column {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS) else step = 2
                }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Allow reminders") }
                TextButton(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
            }
            else -> OutlinedButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

@Composable
private fun Bullet(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, Palette.primaryGradient, 40.dp)
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
