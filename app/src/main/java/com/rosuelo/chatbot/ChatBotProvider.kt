package com.rosuelo.chatbot

import android.util.Log
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
        val chatId: String,
        val text: String,
        val tools: List<Tools>
    )

    @Serializable
    data class ChatBotReply(
        val output: String
    )

    suspend fun sendChatMessage(chatId: String, message: String): SupabaseProvider.ChatMessage? {

        try {
            var reply = httpClient.post("https://n8n.safehub-lcup.uk/webhook/akasha"){
                contentType(ContentType.Application.Json)
                setBody(
                    OutgoingMessage(
                        chatId = chatId,
                        text = message,
                        tools = Settings.enabledTools.toList()
                    )
                )
            }.body<ChatBotReply>()
            return SupabaseProvider.ChatMessage(SupabaseProvider.MessageType.AI, reply.output)
        } catch (t: Throwable) {
            Log.e("ChatBox", "Failed to send message", t)
            return null
        }
    }
}