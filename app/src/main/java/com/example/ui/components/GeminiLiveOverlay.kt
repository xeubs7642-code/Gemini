package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiGradientBrush
import com.example.ui.theme.GeminiIndigo
import com.example.ui.theme.GeminiPink
import com.example.ui.theme.GeminiPurple
import com.example.ui.viewmodel.LiveState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GeminiLiveOverlay(
    liveState: LiveState,
    transcript: String,
    aiResponse: String,
    onCloseClick: () -> Unit,
    onSpeechInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMuted by remember { mutableStateOf(false) }
    var quickInputText by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("gemini_live_overlay"),
        color = Color(0xFF0C0D11)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (liveState) {
                                    LiveState.SPEAKING -> GeminiPink
                                    LiveState.THINKING -> GeminiPurple
                                    LiveState.LISTENING -> GeminiBlue
                                    else -> Color.Gray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini Live",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.testTag("close_live_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Live",
                        tint = Color.White
                    )
                }
            }

            // Central Area: Glowing pulsating dynamic waveform visualizer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                GeminiLiveOrb(
                    state = liveState,
                    modifier = Modifier.size(240.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Status text / live transcription
                Text(
                    text = when (liveState) {
                        LiveState.LISTENING -> "Listening..."
                        LiveState.THINKING -> "Gemini is thinking..."
                        LiveState.SPEAKING -> "Gemini is speaking"
                        LiveState.IDLE -> "Talk with Gemini"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = when (liveState) {
                            LiveState.LISTENING -> GeminiBlue
                            LiveState.THINKING -> GeminiPurple
                            LiveState.SPEAKING -> GeminiPink
                            LiveState.IDLE -> Color.LightGray
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (aiResponse.isNotBlank() && liveState == LiveState.SPEAKING) {
                        aiResponse.take(180) + if (aiResponse.length > 180) "..." else ""
                    } else {
                        transcript
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFFC4C7C5),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick voice prompt suggestions when idle
                if (liveState == LiveState.IDLE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        LiveSuggestionChip("Tell me a story") {
                            onSpeechInput("Tell me a captivating 1-minute sci-fi story.")
                        }
                        LiveSuggestionChip("Quick brain teaser") {
                            onSpeechInput("Give me a fun brain teaser puzzle.")
                        }
                    }
                }
            }

            // Optional Quick Text input row for Live interaction
            if (showTextInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickInputText,
                        onValueChange = { quickInputText = it },
                        placeholder = { Text("Speak or type to Gemini Live...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GeminiBlue,
                            unfocusedBorderColor = Color(0xFF333333)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (quickInputText.isNotBlank()) {
                                onSpeechInput(quickInputText)
                                quickInputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GeminiBlue)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }

            // Bottom Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic toggle
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1F22))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute mic",
                        tint = if (isMuted) Color(0xFFEF5350) else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center main talk trigger
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(GeminiGradientBrush)
                        .clickable {
                            if (liveState == LiveState.LISTENING) {
                                onSpeechInput("Hello Gemini, how are you?")
                            } else {
                                onSpeechInput("Tell me something fascinating about the universe.")
                            }
                        }
                        .testTag("live_talk_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Talk to Gemini",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // End session button
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                        .testTag("end_live_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Live conversation",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveSuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color(0xFF1E1F22),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE3E3E3)),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun GeminiLiveOrb(
    state: LiveState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "gemini_live_orb")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (size.minDimension / 2.6f) * pulseScale

        // Ambient radial background glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    when (state) {
                        LiveState.SPEAKING -> GeminiPink.copy(alpha = 0.45f)
                        LiveState.THINKING -> GeminiPurple.copy(alpha = 0.45f)
                        LiveState.LISTENING -> GeminiCyan.copy(alpha = 0.45f)
                        LiveState.IDLE -> GeminiBlue.copy(alpha = 0.35f)
                    },
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 1.5f
            ),
            radius = baseRadius * 1.5f,
            center = center
        )

        // Rotating harmonic gradient arcs
        rotate(rotation, pivot = center) {
            // Outer Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(GeminiBlue, GeminiPurple, GeminiPink, GeminiCyan, GeminiBlue),
                    center = center
                ),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner Ring 1
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(GeminiPink, GeminiCyan, GeminiPurple, GeminiBlue, GeminiPink),
                    center = center
                ),
                radius = baseRadius * 0.75f,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Animated Harmonic Waveform Core
        rotate(-rotation * 0.8f, pivot = center) {
            val pointCount = 36
            val step = (2 * Math.PI) / pointCount
            for (i in 0 until pointCount) {
                val angle = i * step
                val waveOffset = sin(angle * 3 + wavePhase) * (14.dp.toPx())
                val r = baseRadius * 0.55f + waveOffset
                val x = center.x + (r * cos(angle)).toFloat()
                val y = center.y + (r * sin(angle)).toFloat()

                drawCircle(
                    color = if (i % 2 == 0) GeminiCyan else GeminiPurple,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Inner glowing core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    GeminiBlue.copy(alpha = 0.6f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 0.45f
            ),
            radius = baseRadius * 0.45f,
            center = center
        )
    }
}
