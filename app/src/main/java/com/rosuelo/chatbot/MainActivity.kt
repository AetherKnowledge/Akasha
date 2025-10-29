package com.rosuelo.chatbot

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import android.view.View
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

    fun clearAllScreen(){
        this.findViewById<View>(R.id.newChatContainer)?.visibility = View.GONE
        this.findViewById<View>(R.id.chatBoxContainer)?.visibility = View.GONE
        this.findViewById<View>(R.id.settingsContainer)?.visibility = View.GONE
        this.findViewById<View>(R.id.messagesContainer)?.visibility = View.GONE
        this.findViewById<View>(R.id.composeContainer)?.visibility = View.GONE
        this.findViewById<View>(R.id.registerContainer)?.visibility = View.GONE
    }

    fun showChatTopBar(show: Boolean){
        this.findViewById<View>(R.id.topBarContainer)?.visibility = if (show) View.VISIBLE else View.GONE
    }

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
        val root = findViewById<View>(android.R.id.content)
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
                        clearAllScreen()
                        (LocalActivityHolder.current as? MainActivity)
                            ?.findViewById<View>(R.id.topBarContainer)
                            ?.visibility = View.GONE
                        Loading()
                        return@ChatbotTheme
                    }

                    if (user != null) {
                        // Remove register fragment if present and show compose
                        (LocalActivityHolder.current as? MainActivity)?.let { activity ->
                            clearAllScreen()
                            activity.supportFragmentManager.findFragmentByTag("register")?.let { frag ->
                                activity.supportFragmentManager.commit { remove(frag) }
                            }
                            activity.findViewById<View>(R.id.composeContainer)?.visibility = View.VISIBLE
                        }
                        // Show top bar and bind user
                        (LocalActivityHolder.current as? MainActivity)?.let { activity ->
                            activity.findViewById<View>(R.id.topBarContainer)?.visibility = View.VISIBLE
                            (activity.supportFragmentManager.findFragmentById(R.id.topBarContainer) as? ChatTopBarFragment)?.bindUser(user!!)
                        }

                        Column {
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
                    } else {
                        // Hide top bar on Register screen and show XML fragment
                        (LocalActivityHolder.current as? MainActivity)?.let { activity ->
                            showChatTopBar(false)
                            clearAllScreen()
                            val tag = "register"
                            val existingRegister = activity.supportFragmentManager.findFragmentByTag(tag)
                            if (existingRegister == null) {
                                activity.supportFragmentManager
                                    .beginTransaction()
                                    .setReorderingAllowed(true)
                                    .replace(R.id.registerContainer, RegisterFragment(), tag)
                                    .commitNow()
                            }
                            activity.findViewById<View>(R.id.registerContainer)?.visibility = View.VISIBLE
                            (activity.supportFragmentManager.findFragmentByTag(tag) as? RegisterFragment)?.let { frag ->
                                frag.listener = object : RegisterFragment.Listener {
                                    override fun onAuthSuccess(userData: UserData) {
                                        refreshingUser = true
                                        // Immediately switch containers so the UI responds right away
                                        activity.findViewById<View>(R.id.registerContainer)?.visibility = View.GONE
                                        activity.findViewById<View>(R.id.composeContainer)?.visibility = View.VISIBLE
                                        coroutineScope.launch { refreshUser() }
                                    }
                                }
                            }
                        }
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
    val activity = LocalActivityHolder.current as? MainActivity

    LaunchedEffect(Unit) { chats = getChats(currentUser.id) }

    // Top bar is now an XML Fragment. Register handlers via MainActivity.
    (LocalActivityHolder.current as? MainActivity)?.installTopBarCallbacks(
        onHamburger = { currentPanel = PanelState.MESSAGES_BOX },
        onSettings = { currentPanel = PanelState.SETTINGS },
        onLogout = { onLogoutClick?.invoke() }
    )

    Column {
        when (currentPanel) {
            PanelState.NEW_CHAT -> {
                activity?.clearAllScreen()
                activity?.findViewById<View>(R.id.newChatContainer)?.visibility = View.VISIBLE
                val tag = "new_chat"
                val displayName = currentUser.name ?: displayNameFromEmail(currentUser.email)
                val existing = activity?.supportFragmentManager?.findFragmentByTag(tag) as? NewChatFragment
                if (existing == null && activity != null) {
                    // Create with required arguments to avoid runtime crashes
                    val fragment = NewChatFragment.newInstance(currentUser.id, displayName)
                    activity.supportFragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.newChatContainer, fragment, tag)
                        .commitNow()
                }
                (activity?.supportFragmentManager?.findFragmentByTag(tag) as? NewChatFragment)?.let {frag ->
                    frag.listener = object : NewChatFragment.Listener {
                        override fun onNewChatCreated(chat: Chat?) {
                            currentChat = chat
                            if (chat != null) {
                                chats = listOf(chat) + chats
                                Log.d(
                                    "PanelSwitcher",
                                    "New chat created with id=${chat.id}, switching to CHAT_BOX"
                                )
                                currentPanel = PanelState.CHAT_BOX
                            }
                        }
                    }

                    // Ensure fragment has correct display name and user id even if it already existed
                    frag.setDisplayName(displayName)
                    frag.setUserId(currentUser.id)
                }
            }
            PanelState.CHAT_BOX -> {
                activity?.clearAllScreen()
                activity?.findViewById<View>(R.id.composeContainer)?.visibility = View.VISIBLE
                ChatBox(currentChat!!,
                    onUpdateChat = { updatedChat ->
                        chats = chats.map { if (it.id == updatedChat.id) updatedChat else it }
                        currentChat = updatedChat
                    }
                )
            }
            PanelState.MESSAGES_BOX -> {
                activity?.clearAllScreen()
                val tag = "messages"
                val existing = activity?.supportFragmentManager?.findFragmentByTag(tag) as? MessagesBoxFragment
                if (existing == null && activity != null) {
                    activity.supportFragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.messagesContainer, MessagesBoxFragment(), tag)
                        .commitNow()
                }
                activity?.findViewById<View>(R.id.messagesContainer)?.visibility = View.VISIBLE
                (activity?.supportFragmentManager?.findFragmentByTag(tag) as? MessagesBoxFragment)?.let { frag ->
                    frag.listener = object : MessagesBoxFragment.Listener {
                        override fun onNewChat() { currentPanel = PanelState.NEW_CHAT }
                        override fun onChatClick(chat: Chat) {
                            currentChat = chat
                            currentPanel = PanelState.CHAT_BOX
                            Log.d("PanelSwitcher", "Switched to CHAT_BOX for chat id=${chat.id}")
                        }
                        override fun onDeleteChat(chat: Chat) {
                            coroutineScope.launch {
                                SupabaseProvider.deleteChat(chat.id)
                                val list = chats.filter { it.id != chat.id }
                                chats = list
                                frag.setChats(list)
                                if (currentChat?.id == chat.id) {
                                    currentChat = null
                                }
                            }
                        }
                    }
                    // Keep fragment list in sync with compose state if already loaded
                    frag.setChats(chats)
                }
            }
            PanelState.SETTINGS -> {
                activity?.clearAllScreen()
                val tag = "settings"
                val existing = activity?.supportFragmentManager?.findFragmentByTag(tag) as? SettingsFragment
                if (existing == null && activity != null) {
                    activity.supportFragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.settingsContainer, SettingsFragment.newInstance(currentUser), tag)
                        .commitNow()
                }
                activity?.findViewById<View>(R.id.settingsContainer)?.visibility = View.VISIBLE
                (activity?.supportFragmentManager?.findFragmentByTag(tag) as? SettingsFragment)?.let { frag ->
                    frag.listener = object : SettingsFragment.Listener {
                        override fun onBack() {
                            activity.clearAllScreen()
                            currentPanel = PanelState.NEW_CHAT
                        }
                        override fun onLogout() { onLogoutClick?.invoke() }
                        override fun onUserUpdate(userData: UserData) {
                            onUserUpdate?.invoke(userData)
                            // Also immediately update the top bar avatar
                            (activity.supportFragmentManager.findFragmentById(R.id.topBarContainer) as? ChatTopBarFragment)
                                ?.bindUser(userData)
                        }
                    }
                }
            }
        }
    }
}

private fun displayNameFromEmail(email: String): String {
    val local = email.substringBefore('@')
    val parts = local.split('.', '_', '-', '+')
        .filter { it.isNotBlank() }
        .take(3)
    if (parts.isEmpty()) return email
    return parts.joinToString(" ") { part ->
        part.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
