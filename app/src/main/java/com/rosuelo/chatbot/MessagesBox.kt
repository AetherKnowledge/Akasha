package com.rosuelo.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rosuelo.chatbot.SupabaseProvider.Chat
import com.rosuelo.chatbot.SupabaseProvider.ChatMessage
import com.rosuelo.chatbot.SupabaseProvider.MessageType
import com.rosuelo.chatbot.ui.theme.ChatbotTheme
import kotlinx.coroutines.launch

private fun String.ellipsize(max: Int): String = if (length <= max) this else take(max - 1) + "…"

@Composable
fun MessagesBox(
    chats: List<Chat>,
    modifier: Modifier = Modifier,
    onChatClick: ((Chat) -> Unit)? = null,
    onNewChatClick: (() -> Unit)? = null,
    onDeleteChat: ((Chat) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item{
                NewChatItem(
                    onNewChatClick = onNewChatClick
                )
            }

            item{
                Separator()
            }

            items(chats, key = { it.id }) { chat ->
                MessageBoxItem(
                    chat = chat,
                    onChatClick = onChatClick,
                    onDeleteChat = onDeleteChat
                )
            }
        }
    }
}

@Composable
private fun NewChatItem(onNewChatClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0E4F26),
                        Color(0xFF1A6333)
                    )
                )
            )
            .clickable(enabled = onNewChatClick != null) { onNewChatClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Chat",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun Separator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        )

        Text(
            text = "Chats",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun MessageBoxItem(chat: Chat, onChatClick: ((Chat) -> Unit)?, onDeleteChat: ((Chat) -> Unit)? =null){
    val colors = MaterialTheme.colorScheme
    val lastMessage = chat.messages.lastOrNull()?.content ?: "Start a conversation"
    val coroutineScope = rememberCoroutineScope()
    var deleteLoading by remember { mutableStateOf(false) }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onChatClick != null) { onChatClick?.invoke(chat) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ){
                ListItem(
                    modifier = Modifier.weight(1f),
                    colors = ListItemDefaults.colors(
                        containerColor = colors.surface,
                        headlineColor = colors.onSurface,
                        supportingColor = colors.onSurface.copy(alpha = 0.7f)
                    ),
                    headlineContent = {
                        Text(
                            text = chat.title.ifBlank { "Untitled chat" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = lastMessage.ellipsize(80),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                )

                TextButton(

                    onClick = {
                        if (deleteLoading) return@TextButton

                        coroutineScope.launch {
                            deleteLoading = true
                            if(SupabaseProvider.deleteChat(chat.id)){
                                onDeleteChat?.invoke(chat)
                                deleteLoading = false
                            }
                        }
                    },
                ) {
                    if (deleteLoading) {
                        CircularProgressIndicator()
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete Chat",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessagesBoxPreview() {
    ChatbotTheme {
        val sample = listOf(
            Chat(
                id = "1",
                user_id = "u1",
                title = "Add loading indicator",
                messages = mutableListOf(
                    ChatMessage(MessageType.HUMAN, "How do I add a loading spinner to my Compose button?"),
                    ChatMessage(MessageType.AI, "Use CircularProgressIndicator inside Button and toggle with a state.")
                )
            ),
            Chat(
                id = "2",
                user_id = "u1",
                title = "Kotlin filter query",
                messages = mutableListOf(
                    ChatMessage(MessageType.HUMAN, "Best way to filter a list by multiple fields"),
                )
            ),
            Chat(
                id = "3",
                user_id = "u1",
                title = "Center text wrapping",
                messages = mutableListOf()
            )
        )
        MessagesBox(sample)
    }
}