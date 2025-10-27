package com.rosuelo.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
            .background(colors.background)
    ) {
        Gradient()

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
private fun NewChatItem(onNewChatClick: (() -> Unit)? = null){
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = onNewChatClick != null) { onNewChatClick?.invoke() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = colors.surface,
                    headlineColor = colors.onSurface,
                    supportingColor = colors.onSurface.copy(alpha = 0.7f)
                ),
                headlineContent = {
                    Text(
                        text = "Start a new chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                supportingContent = {
                    Text(
                        text = "Click to begin a new conversation",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
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
            .clip(RoundedCornerShape(14.dp))
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