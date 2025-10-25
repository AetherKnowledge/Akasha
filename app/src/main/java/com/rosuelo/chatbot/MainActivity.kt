package com.rosuelo.chatbot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.rosuelo.chatbot.SupabaseProvider.supabase
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            showCurrentUser()
        }

        setContent {
            ChatbotTheme {
                RegisterScreen()
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    ChatBox(modifier = Modifier.padding(innerPadding))
//                }
            }
        }
    }

    private suspend fun showCurrentUser(){
        supabase.auth.awaitInitialization()
        var session = supabase.auth.currentSessionOrNull()
        Log.d("auth", "Session: $session")
    }
}





