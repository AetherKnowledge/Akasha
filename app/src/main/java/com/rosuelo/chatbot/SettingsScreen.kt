package com.rosuelo.chatbot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.InputStream

@Composable
fun SettingsScreen(
    userData: UserData,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onUserUpdate: ((UserData) -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            
            // Profile Section
            ProfileSection(
                userData = userData,
                onUserUpdate = onUserUpdate
            )

            Spacer(Modifier.height(24.dp))

            // Tools Section
            ToolsSection()

            Spacer(Modifier.weight(1f))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onBack?.invoke() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3A4D4D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Return", style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = { onLogoutClick?.invoke() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC3545),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileSection(
    userData: UserData,
    onUserUpdate: ((UserData) -> Unit)? = null
) {
    var displayName by remember { mutableStateOf(userData.name ?: displayNameFromEmail(userData.email)) }
    var newImageUri = remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var scope = rememberCoroutineScope()
    var isEditingName by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageData = remember { mutableStateOf<ImageData?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    newImageUri.value = uri.toString()
                    imageData.value = readBytesAndMime(context, uri)
                }
            }
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Profile Image with Edit Icon
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF4CAF50), CircleShape)
                    .clickable { if (!updating) imagePicker.launch("image/*") }
            ) {
                if (newImageUri.value?.toString() != null || userData.avatar != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(newImageUri.value?.toString() ?: userData.avatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF3A4D4D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userData.email.first().uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Name with Edit Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEditingName) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.width(200.dp)
                )
            } else {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            IconButton(
                onClick = { 
                    if (isEditingName && displayName != userData.name) {
                        updating = true
                        scope.launch {
                            val updated = SupabaseProvider.updateProfile(
                                userData,
                                name = displayName,
                                imageData = imageData.value
                            )
                            if (updated != null) {
                                onUserUpdate?.invoke(updated)
                            }
                            updating = false
                            isEditingName = false
                        }
                    } else {
                        isEditingName = !isEditingName
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                if (updating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit name",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolToggleRow(
    tool: Tools,
    label: String,
    description: String,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurface.copy(alpha = 0.7f))
        }
        Switch(
            checked = Settings.enabledTools.contains(tool),
            onCheckedChange = { checked ->
                CoroutineScope(Dispatchers.IO).launch {
                    Settings.toggle(context, tool, checked)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary
            )
        )
    }
}

@Composable
private fun ToolsSection() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Separator()

        Spacer(Modifier.height(16.dp))

        // Web Search Toggle
        ToolToggleRow(
            tool = Tools.WEBSEARCH,
            label = "Web Search",
            description = "Allow Akasha to browse the web for answers",
        )
    }
}

// ImageData moved to ImageData.kt for shared use

private fun displayNameFromEmail(email: String): String {
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
private fun SettingsScreenPreview() {
    val user = UserData(id = "1", email = "john@example.com", name = "John", avatar = null)
    SettingsScreen(userData = user)
}

private suspend fun readBytesAndMime(context: android.content.Context, uri: Uri): ImageData {
    return withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var mimeType = resolver.getType(uri)
            val input: InputStream? = resolver.openInputStream(uri)
            val bytes = input?.use { it.readBytes() }
            ImageData(bytes, mimeType)
        } catch (t: Throwable) {
            Log.e("readBytesAndMime", "Failed to read bytes from URI "+ t.localizedMessage, t)
            ImageData(null, null)
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
            text = "Tools",
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
