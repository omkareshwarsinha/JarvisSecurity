package com.example.jarvis.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.data.model.RemediationAction
import com.example.jarvis.domain.PerformanceOptimizer
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.jarvis.ui.components.JarvisTopBar
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisGreenSecure
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisWarningAmber

@Composable
fun PerformanceScreen(viewModel: JarvisMainViewModel) {
    val memoryStatus by viewModel.memoryStatus.collectAsStateWithLifecycle()
    val isBoosting by viewModel.isBoosting.collectAsStateWithLifecycle()
    val lastBoostResult by viewModel.lastBoostResult.collectAsStateWithLifecycle()
    val gamingTips = viewModel.gamingTips

    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(isBoosting) {
        if (isBoosting) {
            rotationAnim.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotationAnim.snapTo(0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        JarvisTopBar(
            title = "Hardware & Performance",
            subtitle = "RAM, Storage & Gaming Optimization",
            onBackClick = { viewModel.navigateBack() },
            onSettingsClick = { viewModel.navigateTo("settings") }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GAMING & RAM ACCELERATOR",
                    color = JarvisCyanPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Real-time Memory Flush & Thread Prioritization",
                    color = JarvisTextMuted,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = { viewModel.refreshMemoryStatus() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh RAM", tint = JarvisCyanPrimary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Center Futuristic Circular RAM HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE60D1527))
                .border(1.5.dp, JarvisCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background track
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0x3300E5FF),
                        strokeWidth = 10.dp
                    )
                    // Used RAM progress
                    CircularProgressIndicator(
                        progress = { memoryStatus.usedPercentage / 100f },
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotationAnim.value),
                        color = if (memoryStatus.usedPercentage > 85) JarvisWarningAmber else JarvisCyanPrimary,
                        strokeWidth = 10.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${memoryStatus.usedPercentage}%",
                            color = JarvisTextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "RAM USED",
                            color = JarvisCyanPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total RAM", color = JarvisTextMuted, fontSize = 11.sp)
                        Text("${memoryStatus.totalMemMb} MB", color = JarvisTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Free RAM", color = JarvisTextMuted, fontSize = 11.sp)
                        Text("${memoryStatus.availableMemMb} MB", color = JarvisGreenSecure, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Used RAM", color = JarvisTextMuted, fontSize = 11.sp)
                        Text("${memoryStatus.usedMemMb} MB", color = JarvisCyanPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action: 1-Tap Gaming Boost Button
                Button(
                    onClick = { viewModel.triggerGameBoost() },
                    enabled = !isBoosting,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("boost_gaming_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBoosting) "OPTIMIZING & FLUSHING RAM..." else "OPTIMIZE RAM FOR GAMING",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Boost Feedback Card (if applied)
        if (lastBoostResult != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC002B36))
                    .border(1.dp, JarvisGreenSecure, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = JarvisGreenSecure,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GAME BOOST APPLIED",
                            color = JarvisGreenSecure,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "+${lastBoostResult!!.freedMemMb} MB RAM reclaimed across ${lastBoostResult!!.killedProcessesCount} idle background processes. Background CPU jitter minimized.",
                            color = JarvisTextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "GAMING HARDENING & TWEAKS",
            color = JarvisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Gaming Tuning Cards
        for (tip in gamingTips) {
            GamingTipCard(
                tip = tip,
                onAction = {
                    when (tip.category) {
                        "INTERRUPTIONS" -> viewModel.launchRemediation(RemediationAction.OPEN_NOTIFICATION_SETTINGS)
                        "LATENCY" -> viewModel.navigateTo("firewall")
                        "CPU_GPU" -> viewModel.launchRemediation(RemediationAction.OPEN_BATTERY_OPTIMIZATION_SETTINGS)
                        else -> viewModel.triggerGameBoost()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}
}

@Composable
fun GamingTipCard(
    tip: PerformanceOptimizer.GamingTuningTip,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x990A101E))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tip.description,
                    color = JarvisTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(tip.actionLabel, fontSize = 10.sp, color = JarvisCyanPrimary)
            }
        }
    }
}
