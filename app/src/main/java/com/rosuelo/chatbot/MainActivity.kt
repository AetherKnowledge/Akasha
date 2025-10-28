package com.rosuelo.chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rosuelo.chatbot.SupabaseProvider.Chat
import com.rosuelo.chatbot.SupabaseProvider.getChats
import com.rosuelo.chatbot.SupabaseProvider.supabase
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.initialize(this)
        enableEdgeToEdge()

        setContent {
            ChatbotTheme {
                val coroutineScope = rememberCoroutineScope()
                var user by remember { mutableStateOf<UserData?>(null) }
                var refreshingUser by remember { mutableStateOf(true) }

                suspend fun refreshUser(){
                    user = getCurrentUser()
                    refreshingUser = false
                }

                LaunchedEffect(Unit) {
                    refreshUser()
                }

                if(refreshingUser){
                    Loading()
                    return@ChatbotTheme
                }

                if(user != null){

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Gradient()

                        Column(modifier = Modifier.padding(innerPadding).padding(8.dp)){
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
                }
                else{
                    RegisterScreen(onRegister = {
                        refreshingUser = true
                        coroutineScope.launch {
                            refreshUser()
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun Loading(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

private enum class PanelState {
    NEW_CHAT,
    CHAT_BOX,
    MESSAGES_BOX,
    SETTINGS,
}

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

    LaunchedEffect(Unit) {
        chats = getChats(currentUser.id)
    }

    ChatTopBar(
        currentUser,
        onLogoutClick = onLogoutClick,
        onHamburgerClick = {
            currentPanel = PanelState.MESSAGES_BOX
        },
        onSettingsClick = {
            currentPanel = PanelState.SETTINGS
        }
    )

    Column {
        when (currentPanel) {
            PanelState.NEW_CHAT -> NewChat(
                userData = currentUser,
                onAsk = { chat ->
                    currentChat = chat
                    if(chat != null) {
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
                onNewChatClick = {
                    currentPanel = PanelState.NEW_CHAT
                },
                onDeleteChat = {
                    chat ->
                    coroutineScope.launch {
                        chats = chats.filter { it.id != chat.id }
                        if(currentChat?.id == chat.id){
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
