package com.rosuelo.chatbot

import ChatBotProvider.sendChatMessage
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
import com.rosuelo.chatbot.SupabaseProvider.ChatMessage
import com.rosuelo.chatbot.SupabaseProvider.createNewChat
import com.rosuelo.chatbot.SupabaseProvider.supabase
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChatbotTheme {
                val coroutineScope = rememberCoroutineScope()
                var userExists by remember { mutableStateOf<Boolean?>(null) }

                suspend fun refreshUser(){
                    userExists = doesCurrentUserExist()
                }

                LaunchedEffect(Unit) {
                    refreshUser()
                }

                when (userExists) {
                    null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    true -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            Column(modifier = Modifier.padding(innerPadding).padding(8.dp)){
                                var currentUser = getCurrentUser()

                                ChatTopBar(currentUser, onLogoutClick = {
                                    coroutineScope.launch {
                                        supabase.auth.signOut()
                                        refreshUser()
                                    }
                                })
                                PanelSwitcher(currentUser)
                            }
                        }
                    }
                    false -> {
                        RegisterScreen(onRegister = {
                            coroutineScope.launch {
                                refreshUser()
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun PanelSwitcher(currentUser: UserData) {
    var currentPanel by remember { mutableStateOf("new") }
    var currentChat by remember { mutableStateOf<Chat?>(null) }

    if(currentChat == null){
        currentPanel = "new"
    }

    Column {
        when (currentPanel) {
            "new" -> NewChat(
                userData = currentUser,
                onAsk = { chat ->
                    currentChat = chat
                    if(chat != null) {
                        currentPanel = "chats"
                    }
                }
            )
            "chats" -> ChatBox(currentUser, currentChat!!)
        }
    }
}


