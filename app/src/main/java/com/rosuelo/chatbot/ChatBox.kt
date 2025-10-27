package com.rosuelo.chatbot

import ChatBotProvider.sendChatMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.rosuelo.chatbot.SupabaseProvider.ChatMessage
import com.rosuelo.chatbot.SupabaseProvider.MessageType
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.TextFieldDefaults
import com.rosuelo.chatbot.SupabaseProvider.Chat

@Composable
fun ChatBox(
    chat: Chat,
    modifier: Modifier = Modifier,
    onUpdateChat: ((Chat) -> Unit)? = null
) {
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        chatMessages.addAll(chat.messages)
    }
    Gradient()

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .weight(1f),
            reverseLayout = true
        ) {
            items(chatMessages.reversed()) { chatRecord ->
                MessageBubble(chatRecord)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.height(55.dp),
                onClick = {
                    if (userInput.text.isNotBlank()) {
                        val userMessage = userInput.text

                        // Add user's message
                        chatMessages.add(
                            ChatMessage(
                                type = MessageType.HUMAN,
                                content = userMessage
                            )
                        )

                        coroutineScope.launch {
                            val reply = sendChatMessage(
                                ChatBotProvider.OutgoingMessage(chat.id, userMessage)
                            )
                            if (reply == null) {
                                chatMessages.removeAt(chatMessages.size - 1)
                            } else {
                                chatMessages.add(reply)
                            }

                            onUpdateChat?.invoke(chat.copy(messages = chatMessages.toMutableList()))
                        }

                        userInput = TextFieldValue("")
                    }
                }
            ,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )

            ) {
                Text("Send", modifier = Modifier.padding(horizontal = 4.dp), MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}


@Composable
fun MessageBubble(chatMessage: ChatMessage) {
    val isHuman = chatMessage.type == MessageType.HUMAN
    val colors = MaterialTheme.colorScheme
    val backgroundColor = if (isHuman) colors.primaryContainer else colors.secondaryContainer
    val contentColor = if (isHuman) colors.onPrimaryContainer else colors.onSecondaryContainer
    val shape = if (isHuman) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomEnd = 12.dp, bottomStart = 12.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isHuman) Arrangement.End else Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape)
                .padding(12.dp)
                .widthIn(max = if (isHuman) 320.dp else 560.dp)
                .then(if (!isHuman) Modifier.fillMaxWidth(0.9f) else Modifier)
        ) {
            Text(chatMessage.content, color = contentColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatBoxPreview() {
    ChatbotTheme {
        ChatBox(
            chat = Chat(
                id = "chat1",
                user_id = "test",
                title = "Sample Chat",
                messages = mutableListOf(
                    ChatMessage(
                        type = MessageType.HUMAN,
                        content = "Hello, how are you?"
                    ),
                    ChatMessage(
                        type = MessageType.AI,
                        content = "I'm good, thank you! How can I assist you today?"
                    )
                )
            ),
        )
    }
}
