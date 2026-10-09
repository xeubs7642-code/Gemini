package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.GemsSheet
import com.example.ui.components.GeminiDrawer
import com.example.ui.components.GeminiInputBar
import com.example.ui.components.GeminiLiveOverlay
import com.example.ui.components.GeminiMessageItem
import com.example.ui.components.GeminiSettingsDialog
import com.example.ui.components.GeminiTopBar
import com.example.ui.components.GeminiWelcomeScreen
import com.example.ui.components.GoogleProfileDialog
import com.example.ui.components.ModelSelectorSheet
import com.example.ui.viewmodel.GeminiViewModel
import kotlinx.coroutines.launch

@Composable
fun GeminiMainScreen(
    viewModel: GeminiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Scroll to bottom whenever messages update
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GeminiDrawer(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversation?.id,
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onNewChatClick = {
                    viewModel.startNewConversation()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                onTogglePin = { id -> viewModel.togglePinConversation(id) },
                onOpenGems = {
                    scope.launch { drawerState.close() }
                    viewModel.toggleGemsSheet(true)
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    viewModel.toggleSettingsDialog(true)
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                GeminiTopBar(
                    selectedModel = uiState.selectedModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onModelClick = { viewModel.toggleModelSheet(true) },
                    onProfileClick = { viewModel.toggleProfileDialog(true) }
                )
            },
            bottomBar = {
                GeminiInputBar(
                    text = uiState.inputText,
                    onTextChanged = { viewModel.onInputTextChanged(it) },
                    attachedImageUri = uiState.attachedImageUri,
                    onImageSelected = { viewModel.setAttachedImage(it) },
                    onRemoveImage = { viewModel.removeAttachedImage() },
                    onSendClick = { viewModel.sendMessage() },
                    onMicClick = { viewModel.openGeminiLive() },
                    onLiveClick = { viewModel.openGeminiLive() },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.messages.isEmpty()) {
                    GeminiWelcomeScreen(
                        userName = "Alex",
                        onPromptSelected = { prompt -> viewModel.sendMessage(prompt) }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            GeminiMessageItem(
                                message = msg,
                                isSpeaking = isSpeaking,
                                onLikeClick = { isLiked -> viewModel.likeMessage(msg.id, isLiked) },
                                onSpeakClick = { viewModel.speakMessage(msg) },
                                onRegenerateClick = { viewModel.regenerateResponse(msg.id) },
                                onModifyClick = { modType -> viewModel.modifyResponse(msg.id, modType) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Gemini Live Full-Screen Modal
    AnimatedVisibility(
        visible = uiState.isGeminiLiveOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        GeminiLiveOverlay(
            liveState = uiState.liveState,
            transcript = uiState.liveTranscript,
            aiResponse = uiState.liveAiResponse,
            onCloseClick = { viewModel.closeGeminiLive() },
            onSpeechInput = { query -> viewModel.processLiveQuery(query) }
        )
    }

    // Model Selector Sheet
    if (uiState.showModelSheet) {
        ModelSelectorSheet(
            selectedModel = uiState.selectedModel,
            onModelSelect = { model -> viewModel.selectModel(model) },
            onDismiss = { viewModel.toggleModelSheet(false) }
        )
    }

    // Google Profile Dialog
    if (uiState.showProfileDialog) {
        GoogleProfileDialog(
            userEmail = "alex.rivera@gmail.com",
            userName = "Alex Rivera",
            onSettingsClick = {
                viewModel.toggleProfileDialog(false)
                viewModel.toggleSettingsDialog(true)
            },
            onDismiss = { viewModel.toggleProfileDialog(false) }
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        GeminiSettingsDialog(
            customApiKey = uiState.customApiKey,
            isDarkTheme = uiState.isDarkTheme,
            onSaveApiKey = { key -> viewModel.saveCustomApiKey(key) },
            onToggleDarkTheme = { viewModel.toggleTheme() },
            onClearHistory = {
                viewModel.clearAllHistory()
                viewModel.toggleSettingsDialog(false)
            },
            onDismiss = { viewModel.toggleSettingsDialog(false) }
        )
    }

    // Gems Sheet
    if (uiState.showGemsSheet) {
        GemsSheet(
            onSelectGem = { gem -> viewModel.startGem(gem) },
            onDismiss = { viewModel.toggleGemsSheet(false) }
        )
    }
}
