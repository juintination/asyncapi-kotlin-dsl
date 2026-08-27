package com.example.asyncapikotlindsl.chat.handler

import com.example.asyncapikotlindsl.chat.dto.request.ChatMessageRequest
import com.example.asyncapikotlindsl.chat.dto.response.ChatMessageResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.concurrent.CopyOnWriteArraySet
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * `/ws/chat` raw WebSocket 핸들러.
 *
 * 접속한 모든 세션을 들고 있다가, 한 세션이 [ChatMessageRequest] 를 보내면
 * `sentAt` 을 붙인 [ChatMessageResponse] 로 전 세션에 브로드캐스트한다 (보낸 세션 포함).
 */
class ChatWebSocketHandler : TextWebSocketHandler() {

    private val objectMapper = jacksonObjectMapper()
    private val sessions = CopyOnWriteArraySet<WebSocketSession>()

    override fun afterConnectionEstablished(
        session: WebSocketSession,
    ) {
        sessions.add(session)
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        sessions.remove(session)
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage,
    ) {
        val request = objectMapper.readValue(message.payload, ChatMessageRequest::class.java)
        val json = objectMapper.writeValueAsString(ChatMessageResponse.from(request))
        sessions.filter { it.isOpen }.forEach { it.sendMessage(TextMessage(json)) }
    }
}
