package com.app.newsapp.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundColor = Color(0xFF0D0D0D)
private val AccentRed       = Color(0xFFE63946)
private val TextPrimary     = Color(0xFFF5F5F5)
private val TextMuted       = Color(0xFFA0A0A0)
private val SurfaceColor    = Color(0xFF1A1A1A)
private val TrackColor      = Color(0xFF2E2E2E)

/**
 * ReadingStatsScreen — full Jetpack Compose screen.
 *
 * Assignment 5 F4:
 * - Animated circular progress ring (red arc on dark track)
 * - Linear progress bar with animated fill
 * - Stats boxes: Saved / Top Category / This Week
 * - Yearly goal tracker
 */
@Composable
fun ReadingStatsScreen(
    userName: String,
    articlesRead: Int,
    savedCount: Int,
    topCategory: String,
    onBack: () -> Unit
) {
    val yearlyGoal = 50
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = (articlesRead / yearlyGoal.toFloat()).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top bar with back arrow ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AccentRed
                    )
                }
                Text(
                    text = "Reading Stats",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Greeting ─────────────────────────────────────────────
            Text(
                text = "Great work, $userName! 📰",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Here's your reading summary",
                color = TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Animated circular progress ring ──────────────────────
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawArc(
                        color = TrackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = AccentRed,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$articlesRead",
                        color = AccentRed,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Articles Read", color = TextMuted, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Stats boxes row ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(label = "Saved",        value = "$savedCount",                       icon = "🔖")
                StatBox(label = "Top Category", value = topCategory,                          icon = "🏆")
                StatBox(label = "This Week",    value = "${(articlesRead * 0.3).toInt()}",    icon = "📅")
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Yearly goal progress bar ──────────────────────────────
            Text(
                text = "Yearly Goal: $yearlyGoal Articles",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = AccentRed,
                trackColor = TrackColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(animatedProgress.value * 100).toInt()}% of yearly goal reached",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatBox(label: String, value: String, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(SurfaceColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(min = 88.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = AccentRed,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = TextMuted, fontSize = 11.sp)
    }
}
