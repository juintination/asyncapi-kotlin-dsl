package com.example.asyncapikotlindsl.chat.config

import com.example.asyncapikotlindsl.chat.handler.ChatWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

    override fun registerWebSocketHandlers(
        registry: WebSocketHandlerRegistry,
    ) {
        registry.addHandler(ChatWebSocketHandler(), PATH)
    }

    companion object {
        const val PATH = "/ws/chat"
    }
}
