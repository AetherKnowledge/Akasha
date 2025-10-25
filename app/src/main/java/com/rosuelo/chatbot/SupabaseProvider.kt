package com.rosuelo.chatbot

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SupabaseProvider {

    @Serializable
    data class ChatRecord(
        val id: Int,
        val session_id: String,
        val message: Message
    )

    @Serializable
    enum class MessageType {
        @SerialName("human")
        HUMAN,

        @SerialName("ai")
        AI
    }

    @Serializable
    data class Message(
        val type: MessageType,
        val content: String
    )

    @Serializable
    data class ChatMessage(
        val type: MessageType,
        val name: String,
        val content: String
    )

    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = "https://sofolpcidycmbmrljsus.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNvZm9scGNpZHljbWJtcmxqc3VzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5NDY0MTAsImV4cCI6MjA3NjUyMjQxMH0.I0HzZ2DgZ9LBbveEyUDDTjrkbit_zn2BPaJEkm48mPY"
        ) {
            install(Auth)
            install(Postgrest)
        }
    }


    suspend fun getChats(): List<ChatMessage> {
        return try{
            supabase.from("chathistory").select().decodeList<ChatRecord>()
                .map { record ->
                    ChatMessage(
                        type = record.message.type,
                        name = if (record.message.type == MessageType.HUMAN) "You" else "AI",
                        content = record.message.content
                    )
                }
        }
        catch (t: Throwable) {
            android.util.Log.e("ChatBox", "Failed to load chat history", t)
            emptyList()
        }
    }
}
