package com.rosuelo.chatbot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
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
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            ProfileChanger(onUserUpdate = onUserUpdate, userData = userData)
            ToolChanger()

            Spacer(Modifier.height(12.dp))
            Button(onClick = { onBack?.invoke() }) { Text("Back") }
        }
    }
}

@Serializable
data class ImageData(
    val bytes: ByteArray?,
    val mimeType: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageData

        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}


@Composable
private fun ProfileChanger(userData: UserData, onUserUpdate: ((UserData) -> Unit)? = null) {
    var displayName by remember { mutableStateOf(userData.name ?: displayNameFromEmail(userData.email)) }
    var newImageUri = remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val context = LocalContext.current
    val imageData = remember { mutableStateOf<ImageData?>(null) }


    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    newImageUri.value = uri.toString()
                    imageData.value = readBytesAndMime(context, uri)

                    Log.d("ProfileChanger", "Picked image URI: $uri")
                    Log.d("ProfileChanger", "Read image data: ${imageData.value}")
                }
            }
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AvatarPreview(
                    avatarUrl = newImageUri.value?.toString() ?: userData.avatar,
                    fallbackLetter = userData.email.first().uppercase(),
                    onClick = {
                        if (!updating) {
                            imagePicker.launch("image/*")
                        }
                    }

                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        if(updating) return@Button

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
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) { Text(text = if (updating) "Saving..." else "Save Changes") }
            }
        }
    }

}

@Composable
private fun ToolChanger(){
    var webSearch by remember {
        mutableStateOf(Settings.enabledTools.contains(Tools.WEBSEARCH))
    }
    var calculator by remember {
        mutableStateOf(Settings.enabledTools.contains(Tools.CALCULATOR))
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            ToolToggleRow(
                checked = webSearch,
                label = "Web search",
                description = "Allow Akasha to browse the web for answers",
                onCheckedChange = { checked ->
                    webSearch = checked
                    updateEnabledTools(webSearch, calculator)
                }
            )

            ToolToggleRow(
                checked = calculator,
                label = "Calculator",
                description = "Enable math operations and evaluation",
                onCheckedChange = { checked ->
                    calculator = checked
                    updateEnabledTools(webSearch, calculator)
                }
            )
        }
    }
}

private fun updateEnabledTools(webSearch: Boolean, calculator: Boolean) {
    Settings.enabledTools = buildSet {
        if (webSearch) add(Tools.WEBSEARCH)
        if (calculator) add(Tools.CALCULATOR)
    }
}

@Composable
private fun AvatarPreview(avatarUrl: String?, fallbackLetter: String, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    IconButton(
        onClick = {onClick?.invoke()},
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(colors.primary),
    ) {
        if (avatarUrl != null && avatarUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                modifier = Modifier.size(64.dp).clip(CircleShape),
                contentDescription = "Avatar preview",
            )
        } else {
            Text(
                text = fallbackLetter,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ToolToggleRow(
    checked: Boolean,
    label: String,
    description: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
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
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary
            )
        )
    }
}

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
