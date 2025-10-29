package com.rosuelo.chatbot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.rosuelo.chatbot.SupabaseProvider.Chat
import com.rosuelo.chatbot.SupabaseProvider.getChats
import com.rosuelo.chatbot.SupabaseProvider.supabase
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// CompositionLocal to access activity from Composables
private val LocalActivityHolder = compositionLocalOf<Any?> { null }

class MainActivity : AppCompatActivity(), ChatTopBarFragment.Listener {
    private var composeContainer: ComposeView? = null
    private var lastHamburgerClick: (() -> Unit)? = null
    private var lastSettingsClick: (() -> Unit)? = null
    private var lastLogoutClick: (() -> Unit)? = null

    fun installTopBarCallbacks(
        onHamburger: (() -> Unit)?,
        onSettings: (() -> Unit)?,
        onLogout: (() -> Unit)?
    ) {
        lastHamburgerClick = onHamburger
        lastSettingsClick = onSettings
        lastLogoutClick = onLogout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.initialize(this)

        // Draw edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)
        val root = findViewById<android.view.View>(android.R.id.content)
        // Dark status bar icons off (light icons on dark bg)
        WindowInsetsControllerCompat(window, root).isAppearanceLightStatusBars = false

        // Ensure top bar fragment exists
        val existing = supportFragmentManager.findFragmentById(R.id.topBarContainer)
        if (existing == null) {
            supportFragmentManager.commit {
                replace(R.id.topBarContainer, ChatTopBarFragment())
            }
        }
        (supportFragmentManager.findFragmentById(R.id.topBarContainer) as? ChatTopBarFragment)?.let { it.listener = this }

        composeContainer = findViewById(R.id.composeContainer)
    composeContainer?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        composeContainer?.setContent {
            CompositionLocalProvider(LocalActivityHolder provides this) {
                ChatbotTheme {
                    val coroutineScope = rememberCoroutineScope()
                    var user by remember { mutableStateOf<UserData?>(null) }
                    var refreshingUser by remember { mutableStateOf(true) }

                    suspend fun refreshUser(){
                        user = getCurrentUser()
                        refreshingUser = false
                    }

                    LaunchedEffect(Unit) { refreshUser() }

                    if (refreshingUser) {
                        Loading()
                        return@ChatbotTheme
                    }

                    if (user != null) {
                        // Show top bar and bind user
                        findViewById<android.view.View>(R.id.topBarContainer)?.visibility = android.view.View.VISIBLE
                        (supportFragmentManager.findFragmentById(R.id.topBarContainer) as? ChatTopBarFragment)?.bindUser(user!!)

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent
                        ) { innerPadding ->

                            Column(modifier = Modifier.padding(innerPadding).padding(8.dp)) {
                                PanelSwitcher(
                                    user!!,
                                    onLogoutClick = {
                                        refreshingUser = true
                                        coroutineScope.launch {
                                            supabase.auth.signOut()
                                            refreshUser()
                                        }
                                    },
                                    onUserUpdate = { updatedUser ->
                                        user = updatedUser
                                    }
                                )
                            }
                        }
                    } else {
                        // Hide top bar on Register screen
                        findViewById<android.view.View>(R.id.topBarContainer)?.visibility = android.view.View.GONE
                        RegisterScreen(onRegister = {
                            refreshingUser = true
                            coroutineScope.launch { refreshUser() }
                        })
                    }
                }
            }
        }
    }

    // Fragment listener events -> forward to current Compose screen via lambdas that PanelSwitcher can set
    override fun onHamburgerClick() { lastHamburgerClick?.invoke() }
    override fun onSettingsClick() { lastSettingsClick?.invoke() }
    override fun onLogoutClick() { lastLogoutClick?.invoke() }
}

@Composable
fun Loading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator() }
}

private enum class PanelState { NEW_CHAT, CHAT_BOX, MESSAGES_BOX, SETTINGS }

@Composable
fun PanelSwitcher(
    currentUser: UserData,
    onLogoutClick: (() -> Unit)? = null,
    onUserUpdate: ((UserData) -> Unit)? = null
) {
    var currentPanel by remember { mutableStateOf(PanelState.NEW_CHAT) }
    var currentChat by remember { mutableStateOf<Chat?>(null) }
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { chats = getChats(currentUser.id) }

    // Top bar is now an XML Fragment. Register handlers via MainActivity.
    (LocalActivityHolder.current as? MainActivity)?.installTopBarCallbacks(
        onHamburger = { currentPanel = PanelState.MESSAGES_BOX },
        onSettings = { currentPanel = PanelState.SETTINGS },
        onLogout = { onLogoutClick?.invoke() }
    )

    Column {
        when (currentPanel) {
            PanelState.NEW_CHAT -> NewChat(
                userData = currentUser,
                onAsk = { chat ->
                    currentChat = chat
                    if (chat != null) {
                        chats = chats + chat
                        currentChat = chat
                        currentPanel = PanelState.CHAT_BOX
                    }
                }
            )
            PanelState.CHAT_BOX -> ChatBox(currentChat!!,
                onUpdateChat = { updatedChat ->
                    chats = chats.map { if (it.id == updatedChat.id) updatedChat else it }
                    currentChat = updatedChat
                }
            )
            PanelState.MESSAGES_BOX -> MessagesBox(
                chats = chats,
                onChatClick = { chat ->
                    currentChat = chat
                    currentPanel = PanelState.CHAT_BOX
                },
                onNewChatClick = { currentPanel = PanelState.NEW_CHAT },
                onDeleteChat = { chat ->
                    coroutineScope.launch {
                        chats = chats.filter { it.id != chat.id }
                        if (currentChat?.id == chat.id) {
                            currentPanel = PanelState.MESSAGES_BOX
                            currentChat = null
                        }
                    }
                }
            )
            PanelState.SETTINGS -> SettingsScreen(
                userData = currentUser,
                onBack = { currentPanel = PanelState.NEW_CHAT },
                onUserUpdate = onUserUpdate,
                onLogoutClick = onLogoutClick,
            )
        }
    }
}
