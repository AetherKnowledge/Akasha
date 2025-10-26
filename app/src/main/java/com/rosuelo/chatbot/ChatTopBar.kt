package com.rosuelo.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rosuelo.chatbot.ui.theme.ChatbotTheme


@Composable
fun ChatTopBar(
    userData: UserData,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
   ) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Drawer()
                Logo()
            }

            UserIcon(userData,
                onLogoutClick = { onLogoutClick?.invoke() },
                onSettingsClick = { onSettingsClick?.invoke() }
            )
        }
    }
}

@Composable
fun Drawer(){
    Text(text = "test")
}

@Composable
fun Logo(){
    Text(
        text = "Akasha",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun UserIcon(
    userData: UserData,
    onSettingsClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val firstLetter = userData.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box {
        // Circular Button
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            Text(
                text = firstLetter,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .padding(2.dp) // adds padding inside the menu
        ) {
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    expanded = false
                    onSettingsClick?.invoke()
                }

            )
            DropdownMenuItem(
                text = { Text("Logout") },
                onClick = {
                    expanded = false
                    onLogoutClick?.invoke()
                }
            )
        }

    }
}


@Preview
@Composable
fun ChatTopBarPreview(){
    ChatbotTheme {
        ChatTopBar(
            UserData(
                id = "test",
                email = "test"
            )
        )
    }
}
