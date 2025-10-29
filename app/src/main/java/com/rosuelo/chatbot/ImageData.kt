package com.rosuelo.chatbot

import kotlinx.serialization.Serializable

@Serializable
data class ImageData(
    val bytes: ByteArray?,
    val mimeType: String?
)