package com.example.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.data.model.FindingSeverity
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisCriticalRedBg
import com.example.ui.theme.JarvisInfoBlue
import com.example.ui.theme.JarvisInfoBlueBg
import com.example.ui.theme.JarvisSecureGreen
import com.example.ui.theme.JarvisSecureGreenBg
import com.example.ui.theme.JarvisWarningAmber
import com.example.ui.theme.JarvisWarningAmberBg

@Composable
fun SeverityBadge(
    severity: FindingSeverity,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, strokeColor) = when (severity) {
        FindingSeverity.CRITICAL -> Triple(JarvisCriticalRedBg, JarvisCriticalRed, JarvisCriticalRed.copy(alpha = 0.5f))
        FindingSeverity.WARNING -> Triple(JarvisWarningAmberBg, JarvisWarningAmber, JarvisWarningAmber.copy(alpha = 0.5f))
        FindingSeverity.INFORMATIONAL -> Triple(JarvisInfoBlueBg, JarvisInfoBlue, JarvisInfoBlue.copy(alpha = 0.5f))
        FindingSeverity.VERIFIED_SECURE -> Triple(JarvisSecureGreenBg, JarvisSecureGreen, JarvisSecureGreen.copy(alpha = 0.5f))
        FindingSeverity.NOT_AVAILABLE -> Triple(Color(0x2294A3B8), Color(0xFF94A3B8), Color(0x3394A3B8))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, strokeColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = severity.label.uppercase(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
    }
}
