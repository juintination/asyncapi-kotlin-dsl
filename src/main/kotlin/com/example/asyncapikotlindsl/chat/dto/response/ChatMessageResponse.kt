package com.example.asyncapikotlindsl.chat.dto.response

import com.example.asyncapikotlindsl.chat.dto.request.ChatMessageRequest
import java.time.LocalDateTime

data class ChatMessageResponse(
    val sender: String,
    val text: String,
    val sentAt: String,
) {
    companion object {
        fun from(
            request: ChatMessageRequest,
        ) = ChatMessageResponse(
            sender = request.sender,
            text = request.text,
            sentAt = LocalDateTime.now().toString(),
        )
    }
}
