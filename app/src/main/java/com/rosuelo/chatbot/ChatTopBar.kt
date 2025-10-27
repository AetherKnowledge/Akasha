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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rosuelo.chatbot.ui.theme.ChatbotTheme


@Composable
fun ChatTopBar(
    userData: UserData,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    onHamburgerClick: (() -> Unit)? = null
   ) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HamburgerButton(
                    onHamburgerClick = onHamburgerClick
                )
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
fun HamburgerButton(
    modifier: Modifier = Modifier,
    onHamburgerClick: (() -> Unit)? = null,
) {
    TextButton(
        onClick = {onHamburgerClick?.invoke()},
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_hamburger),
            contentDescription = "Hamburger",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
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
    val firstLetter = userData.name?.firstOrNull()?.uppercaseChar()?.toString() ?: userData.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = Modifier.padding(12.dp)
    ) {
        // Circular Button
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            if(userData.avatar != null){
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userData.avatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User avatar",
                )
            }
            else{
                Text(
                    text = firstLetter,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

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
