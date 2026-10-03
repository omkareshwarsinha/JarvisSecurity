package com.example.jarvis.ui.phishing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.data.db.JarvisDatabase
import com.example.jarvis.data.db.entities.PhishingHistoryEntity
import com.example.jarvis.data.db.entities.SecurityEventEntity
import com.example.jarvis.domain.PhishingDetector
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisGreenSecure
import com.example.ui.theme.JarvisRedCritical
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.JarvisWarningAmber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhishingGateActivity : ComponentActivity() {

    private val phishingDetector = PhishingDetector()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val incomingUri = intent?.data
        val targetUrl = incomingUri?.toString() ?: ""

        val evaluation = phishingDetector.evaluateUrl(targetUrl)
        val sanitizedUrl = sanitizeUrlForStorage(evaluation.url)

        // Log the event locally
        CoroutineScope(Dispatchers.IO).launch {
            val db = JarvisDatabase.getInstance(applicationContext)
            db.phishingHistoryDao().insert(
                PhishingHistoryEntity(
                    timestamp = System.currentTimeMillis(),
                    url = sanitizedUrl,
                    domain = evaluation.domain,
                    isPhishing = evaluation.isPhishing,
                    threatReason = evaluation.threatReason,
                    brandImpersonated = evaluation.brandImpersonated
                )
            )

            if (evaluation.isPhishing) {
                db.securityEventDao().insertEvent(
                    SecurityEventEntity(
                        timestamp = System.currentTimeMillis(),
                        title = "Phishing Link Blocked",
                        category = "PHISHING_SHIELD",
                        severity = "CRITICAL",
                        description = "Intercepted deceptive link: ${evaluation.domain}. Reason: ${evaluation.threatReason}",
                        source = "MobiArmour Phishing Guard"
                    )
                )
            }
        }

        setContent {
            JarvisTheme {
                PhishingGateScreen(
                    evaluation = evaluation,
                    onProceed = { proceedToBrowser(evaluation.url) },
                    onAbort = { finish() }
                )
            }
        }
    }

    private fun sanitizeUrlForStorage(rawUrl: String): String {
        return try {
            val uri = Uri.parse(rawUrl)
            if (uri.queryParameterNames.isEmpty()) return rawUrl
            val sensitiveKeys = setOf("token", "auth", "key", "password", "session", "code", "email", "secret", "sig", "jwt", "reset", "access_token")
            val builder = uri.buildUpon().clearQuery()
            for (param in uri.queryParameterNames) {
                val value = if (sensitiveKeys.any { param.contains(it, ignoreCase = true) }) "[REDACTED]" else uri.getQueryParameter(param)
                builder.appendQueryParameter(param, value)
            }
            builder.build().toString()
        } catch (e: Exception) {
            rawUrl
        }
    }

    private fun proceedToBrowser(url: String) {
        try {
            // Find a browser activity other than ourselves to prevent looping
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pm = packageManager
            val activities = pm.queryIntentActivities(browserIntent, 0)
            val nonSelfActivity = activities.firstOrNull { it.activityInfo.packageName != packageName }

            if (nonSelfActivity != null) {
                browserIntent.setClassName(nonSelfActivity.activityInfo.packageName, nonSelfActivity.activityInfo.name)
                startActivity(browserIntent)
            } else {
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback
        }
        finish()
    }
}

@Composable
fun PhishingGateScreen(
    evaluation: PhishingDetector.PhishingEvaluation,
    onProceed: () -> Unit,
    onAbort: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(if (!evaluation.isPhishing && evaluation.riskScore < 20) 3 else 0) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
            if (countdown == 0) {
                onProceed()
            }
        }
    }

    val isDanger = evaluation.isPhishing || evaluation.riskScore >= 45
    val statusColor = if (isDanger) JarvisRedCritical else if (evaluation.riskScore >= 20) JarvisWarningAmber else JarvisGreenSecure

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE60D1527))
                .border(1.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-Tech Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(2.dp, statusColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDanger) Icons.Default.Dangerous else Icons.Default.CheckCircle,
                    contentDescription = "Shield Status",
                    tint = statusColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isDanger) "PHISHING THREAT INTERCEPTED" else "LINK VERIFIED CLEAN",
                color = statusColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = evaluation.domain,
                color = JarvisTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reason Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x80080D18))
                    .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "MobiArmour Threat Model Assessment:",
                        color = JarvisCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = evaluation.threatReason,
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (evaluation.indicators.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        evaluation.indicators.forEach { indicator ->
                            Text(
                                text = "• $indicator",
                                color = JarvisTextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            if (isDanger) {
                Button(
                    onClick = onAbort,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisRedCritical),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("phishing_abort_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abort to Safety (Recommended)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onProceed,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("phishing_proceed_button")
                ) {
                    Text("Proceed Anyway (Unsafe)", color = JarvisTextMuted, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onProceed,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisGreenSecure),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("phishing_open_button")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (countdown > 0) "Opening in $countdown s..." else "Continue to Browser",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onAbort,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Cancel", color = JarvisTextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
