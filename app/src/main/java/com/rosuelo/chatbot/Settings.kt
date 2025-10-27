package com.rosuelo.chatbot

enum class Tools {
    WEBSEARCH,
    CALCULATOR,
}

object Settings {
    var enabledTools: Set<Tools> = emptySet()
}