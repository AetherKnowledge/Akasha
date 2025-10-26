package com.rosuelo.chatbot

import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.rosuelo.chatbot.SupabaseProvider.supabase
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
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
                                ChatBox(currentUser)
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





