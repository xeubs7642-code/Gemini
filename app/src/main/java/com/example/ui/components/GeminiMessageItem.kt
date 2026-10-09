package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCodeBlockBackground
import com.example.ui.theme.GeminiUserBubble

@Composable
fun GeminiMessageItem(
    message: ChatMessage,
    isSpeaking: Boolean,
    onLikeClick: (Boolean) -> Unit,
    onSpeakClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    onModifyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (message.role == MessageRole.USER) {
        // User Message Pill on the right
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 48.dp)
            ) {
                // Attached image thumbnail if present
                if (message.imageUri != null) {
                    AsyncImage(
                        model = Uri.parse(message.imageUri),
                        contentDescription = "User uploaded image",
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                if (message.text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                            .background(GeminiUserBubble)
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    } else {
        // Gemini Response on the left
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            // Spark avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gemini_spark),
                    contentDescription = "Gemini",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Formatted Markdown Content
                if (message.text.isEmpty() && message.isStreaming) {
                    ThinkingIndicator()
                } else {
                    MarkdownContent(
                        text = message.text,
                        isStreaming = message.isStreaming
                    )
                }

                // Action Bar under assistant response (Copy, Thumbs, TTS, Regenerate, Modify, Share)
                if (!message.isStreaming && message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GeminiMessageActionBar(
                        message = message,
                        isSpeaking = isSpeaking,
                        onLikeClick = onLikeClick,
                        onSpeakClick = onSpeakClick,
                        onRegenerateClick = onRegenerateClick,
                        onModifyClick = onModifyClick
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = GeminiBlue,
            modifier = Modifier
                .size(16.dp)
                .alpha(alpha)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Gemini is thinking...",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.alpha(alpha)
        )
    }
}

@Composable
fun MarkdownContent(
    text: String,
    isStreaming: Boolean
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column {
        blocks.forEach { block ->
            when (block) {
                is Block.Heading -> {
                    Text(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                is Block.CodeSnippet -> {
                    CodeSnippetBlock(
                        language = block.language,
                        code = block.code
                    )
                }
                is Block.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = GeminiBlue
                        )
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is Block.Paragraph -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }

        if (isStreaming) {
            val transition = rememberInfiniteTransition(label = "cursor")
            val alpha by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "cursorAlpha"
            )
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 18.dp)
                    .alpha(alpha)
                    .background(GeminiBlue)
            )
        }
    }
}

@Composable
fun CodeSnippetBlock(
    language: String,
    code: String
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GeminiCodeBlockBackground)
    ) {
        // Code Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF232328))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifEmpty { "code" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Row(
                modifier = Modifier
                    .clickable {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", code)
                        clipboard.setPrimaryClip(clip)
                        isCopied = true
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (isCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCopied) "Copied" else "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Code body
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFFF1F1F1)
            ),
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun GeminiMessageActionBar(
    message: ChatMessage,
    isSpeaking: Boolean,
    onLikeClick: (Boolean) -> Unit,
    onSpeakClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    onModifyClick: (String) -> Unit
) {
    val context = LocalContext.current
    var showModifyMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Thumbs Up
        IconButton(
            onClick = { onLikeClick(true) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (message.liked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "Good response",
                tint = if (message.liked == true) GeminiBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Thumbs Down
        IconButton(
            onClick = { onLikeClick(false) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (message.liked == false) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = "Bad response",
                tint = if (message.liked == false) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Copy Text
        IconButton(
            onClick = {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Gemini response", message.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy text",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // TTS Read Aloud
        IconButton(
            onClick = onSpeakClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isSpeaking) "Stop speaking" else "Read aloud",
                tint = if (isSpeaking) GeminiBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Regenerate
        IconButton(
            onClick = onRegenerateClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Regenerate response",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Modify response (shorter, longer, casual, professional)
        Box {
            IconButton(
                onClick = { showModifyMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modify response",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = showModifyMenu,
                onDismissRequest = { showModifyMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Shorter") },
                    onClick = {
                        showModifyMenu = false
                        onModifyClick("Shorter")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Longer") },
                    onClick = {
                        showModifyMenu = false
                        onModifyClick("Longer")
                    }
                )
                DropdownMenuItem(
                    text = { Text("More casual") },
                    onClick = {
                        showModifyMenu = false
                        onModifyClick("More casual")
                    }
                )
                DropdownMenuItem(
                    text = { Text("More professional") },
                    onClick = {
                        showModifyMenu = false
                        onModifyClick("More professional")
                    }
                )
            }
        }

        // Share
        IconButton(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, message.text)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Gemini response"))
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Sealed hierarchy for lightweight markdown rendering
sealed class Block {
    data class Heading(val level: Int, val text: String) : Block()
    data class CodeSnippet(val language: String, val code: String) : Block()
    data class BulletItem(val text: String) : Block()
    data class Paragraph(val text: String) : Block()
}

fun parseMarkdownBlocks(raw: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val lines = raw.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block starts
        if (line.trim().startsWith("```")) {
            val lang = line.trim().removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeBuilder.append(lines[i]).append("\n")
                i++
            }
            blocks.add(Block.CodeSnippet(language = lang, code = codeBuilder.toString().trimEnd()))
            i++
            continue
        }

        // Headings
        val trimmed = line.trim()
        when {
            trimmed.startsWith("### ") -> {
                blocks.add(Block.Heading(level = 3, text = trimmed.removePrefix("### ")))
            }
            trimmed.startsWith("## ") -> {
                blocks.add(Block.Heading(level = 2, text = trimmed.removePrefix("## ")))
            }
            trimmed.startsWith("# ") -> {
                blocks.add(Block.Heading(level = 1, text = trimmed.removePrefix("# ")))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                blocks.add(Block.BulletItem(text = trimmed.drop(2).trim()))
            }
            trimmed.isNotBlank() -> {
                // Strip bold markers for clean paragraph text
                val cleanPara = trimmed.replace("**", "")
                blocks.add(Block.Paragraph(text = cleanPara))
            }
        }
        i++
    }

    return blocks
}
