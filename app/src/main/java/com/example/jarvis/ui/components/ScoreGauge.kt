package com.example.jarvis.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisSecureGreen
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisWarningAmber

@Composable
fun ScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 180
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "scoreProgress"
    )

    val (color, postureText) = when {
        score >= 85 -> Pair(JarvisSecureGreen, "OPTIMAL POSTURE")
        score >= 70 -> Pair(JarvisCyanPrimary, "SECURE BASELINE")
        score >= 50 -> Pair(JarvisWarningAmber, "HARDENING NEEDED")
        else -> Pair(JarvisCriticalRed, "CRITICAL RISK")
    }

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f
            val centerOffset = center

            // Background track
            drawCircle(
                color = Color(0x221E293B),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeWidth)
            )

            // Outer subtle glow track
            drawCircle(
                color = color.copy(alpha = 0.08f),
                radius = radius + strokeWidth * 0.7f,
                center = centerOffset,
                style = Stroke(width = 2.dp.toPx())
            )

            // Progress Arc
            val sweepAngle = 360f * animatedProgress
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        color.copy(alpha = 0.5f),
                        color,
                        color
                    )
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner display
        Box(
            modifier = Modifier
                .size((sizeDp * 0.75f).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            color.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    color = JarvisTextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "SCORE",
                    color = JarvisTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = postureText,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
