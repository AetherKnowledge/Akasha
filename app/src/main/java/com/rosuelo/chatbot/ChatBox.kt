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
import com.rosuelo.chatbot.SupabaseProvider.getChats
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import kotlinx.coroutines.launch

@Composable
fun ChatBox(modifier: Modifier = Modifier) {
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        chatMessages.addAll(getChats())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (userInput.text.isNotBlank()) {
                        val userMessage = userInput.text

                        // Add user's message
                        chatMessages.add(
                            ChatMessage(
                                type = MessageType.HUMAN,
                                name = "You",
                                content = userMessage
                            )
                        )

                        coroutineScope.launch {
                            var reply = sendChatMessage(ChatBotProvider.OutgoingMessage("test", userMessage))
                            if(reply == null){
                                chatMessages.removeAt(chatMessages.size - 1)
                            }
                            else{
                                chatMessages.add(reply)
                            }
                        }

                        userInput = TextFieldValue("")
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}


@Composable
fun MessageBubble(chatMessage: ChatMessage) {
    val isHuman = chatMessage.type == MessageType.HUMAN
    val backgroundColor = if (isHuman) Color(0xFFDCF8C6) else Color(0xFFE3E3E3)
    val alignment = if (isHuman) Alignment.End else Alignment.Start
    val shape = if (isHuman) {
        RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp)
    } else {
        RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isHuman) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(chatMessage.content)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatBoxPreview() {
    ChatbotTheme {
        ChatBox()
    }
}
