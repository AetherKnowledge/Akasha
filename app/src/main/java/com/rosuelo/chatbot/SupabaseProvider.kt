package com.rosuelo.chatbot

import android.util.Log
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
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
        val content: String
    )

    @Serializable
    data class Chat(
        val id: String,
        val user_id: String,
        val title: String,
        var messages: MutableList<ChatMessage> = mutableListOf()
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

    private fun isInPreview(): Boolean {
        return try {
            Class.forName("androidx.compose.ui.tooling.preview.Preview")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }


    suspend fun getChats(userId: String): List<Chat> {
        return try{
            supabase.from("chat").select(){
                filter {
                    eq("user_id", userId)
                }
            }
                .decodeList<Chat>()
                .map {
                    chat ->

                    chat.messages.addAll(getMessages(chat.id))

                    chat
                }

        }
        catch (t: Throwable) {
            android.util.Log.e("ChatBox", "Failed to load chat history", t)
            emptyList()
        }
    }

    suspend fun getMessages(chatId: String): List<ChatMessage> {
        return try{
            supabase.from("message").select(){
                filter {
                    eq("session_id", chatId)
                }
            }.decodeList<ChatRecord>().map {
                result ->
                ChatMessage(
                    type = result.message.type,
                    content = result.message.content
                )
            }
        }
        catch (t: Throwable) {
            android.util.Log.e("ChatBox", "Failed to load chat messages", t)
            emptyList()
        }
    }

    suspend fun createNewChat(userId: String, title: String? = "New Chat"): Chat? {
        return try {
            supabase.from("chat").insert(
                mapOf(
                    "user_id" to userId,
                    "title" to title
                )
            ){
                select()
            }.decodeSingle<Chat>()
        } catch (t: Throwable) {
            android.util.Log.e("ChatBox", "Failed to create new chat", t)
            null
        }
    }


}
