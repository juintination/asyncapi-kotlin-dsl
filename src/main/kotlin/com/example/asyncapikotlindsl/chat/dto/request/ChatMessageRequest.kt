package com.example.asyncapikotlindsl.chat.dto.request

data class ChatMessageRequest(
    val sender: String,
    val text: String,
)
