import com.rosuelo.chatbot.SupabaseProvider.ChatMessage
import com.rosuelo.chatbot.SupabaseProvider.MessageType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import android.util.Log;
import kotlinx.serialization.json.Json

object ChatBotProvider {

    val httpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    @Serializable
    data class OutgoingMessage(
        val userId: String,
        val text: String
    )

    @Serializable
    data class ChatBotReply(
        val output: String
    )

    suspend fun sendChatMessage(chatMessage: OutgoingMessage): ChatMessage? {
        Log.d("test", chatMessage.toString())
        try {
            var reply = httpClient.post("https://n8n.safehub-lcup.uk/webhook/chatbot"){
                contentType(ContentType.Application.Json)
                setBody(chatMessage)
            }.body<ChatBotReply>()
            return ChatMessage(MessageType.AI, "AI", reply.output);
        } catch (t: Throwable) {
            android.util.Log.e("ChatBox", "Failed to send message", t)
            return null
        }
    }
}