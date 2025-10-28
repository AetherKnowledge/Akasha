package com.rosuelo.chatbot

import com.rosuelo.chatbot.ChatBotProvider.sendChatMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rosuelo.chatbot.SupabaseProvider.Chat
import com.rosuelo.chatbot.SupabaseProvider.ChatMessage
import com.rosuelo.chatbot.SupabaseProvider.createNewChat
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import kotlinx.coroutines.launch

@Composable
fun NewChat(
    userData: UserData,
    modifier: Modifier = Modifier,
    onAsk: ((Chat?) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    colors.secondary,
                    colors.primary
                )
            )

            BasicText(
                text = "Hello, ${userData.name ?: displayNameFrom(userData)}!",
                style = TextStyle(
                    brush = gradient,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Ask box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = "Ask Akasha",
                                color = colors.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = colors.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if(loading) return@Button

                            loading = true
                            coroutineScope.launch {
                                val text = query.trim()
                                if (text.isNotEmpty()) {
                                    var newChat = createNewChatandSendMessage(
                                        userId = userData.id,
                                        prompt = text
                                    )
                                    onAsk?.invoke(newChat)

                                    loading = false
                                    query = ""
                                }
                            }

                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onSurface
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = colors.onSurface,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(2.dp)
                            )
                        } else {
                            Text("Ask")
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

private suspend fun createNewChatandSendMessage(userId: String, prompt: String): Chat? {
    var newChat = createNewChat(userId, title = prompt.take(20))
    if (newChat == null) {
        return null
    }

    newChat.messages.add(
        ChatMessage(
            type = SupabaseProvider.MessageType.HUMAN,
            content = prompt
        )
    )

    val reply = sendChatMessage(newChat.id, prompt)
    if (reply == null) {
        newChat.messages.removeAt(newChat.messages.size - 1)
    }
    else {
        newChat.messages.add(reply)
    }

    return newChat
}

private fun displayNameFrom(userData: UserData): String {
    val email = userData.email
    val local = email.substringBefore('@')
    val parts = local.split('.', '_', '-', '+')
        .filter { it.isNotBlank() }
        .take(3)
    if (parts.isEmpty()) return email
    return parts.joinToString(" ") { part ->
        part.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewChatPreview() {
    ChatbotTheme {
        NewChat(
            userData = UserData(id = "1", email = "john.christian@example.com"),
        )
    }
}
