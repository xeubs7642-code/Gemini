package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.QuickPrompt
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiGradientBrush
import com.example.ui.theme.GeminiPink
import com.example.ui.theme.GeminiPurple

@OptIn(ExperimentalTextApi::class)
@Composable
fun GeminiWelcomeScreen(
    userName: String = "Alex",
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        QuickPrompt(
            category = "Understand",
            title = "Explain how Kotlin Coroutines work simply",
            prompt = "Explain how Kotlin Coroutines and StateFlow work in Android with clear code examples.",
            iconName = "code"
        ),
        QuickPrompt(
            category = "Brainstorm",
            title = "Creative names for an AI productivity app",
            prompt = "Brainstorm 10 creative, catchy names and branding slogans for an AI productivity mobile app.",
            iconName = "lightbulb"
        ),
        QuickPrompt(
            category = "Draft",
            title = "Write a persuasive job application pitch",
            prompt = "Help me write a concise, compelling cover email for a Senior Android Engineer role.",
            iconName = "write"
        ),
        QuickPrompt(
            category = "Explore",
            title = "Plan a 3-day cultural tour of Tokyo",
            prompt = "Plan a detailed 3-day itinerary in Tokyo focusing on food, art, and historic neighborhoods.",
            iconName = "travel"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Gemini Spark Logo
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.gemini_spark),
                contentDescription = "Gemini Logo",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Official Gemini Gradient Title: "Hello, Alex"
        Text(
            text = "Hello, $userName",
            style = TextStyle(
                fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        GeminiBlue,
                        GeminiPurple,
                        GeminiPink
                    )
                )
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Suggestion Cards Grid / Column
        prompts.forEach { item ->
            SuggestionCard(
                quickPrompt = item,
                onClick = { onPromptSelected(item.prompt) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(100.dp)) // padding for bottom floating input bar
    }
}

@Composable
fun SuggestionCard(
    quickPrompt: QuickPrompt,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (quickPrompt.iconName) {
                        "code" -> Icons.Default.Code
                        "lightbulb" -> Icons.Default.Lightbulb
                        "write" -> Icons.Default.RateReview
                        else -> Icons.Default.FlightTakeoff
                    },
                    contentDescription = null,
                    tint = GeminiBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quickPrompt.category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = quickPrompt.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2
                )
            }
        }
    }
}
