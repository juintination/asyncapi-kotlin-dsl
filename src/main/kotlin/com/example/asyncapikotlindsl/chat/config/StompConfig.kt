package com.example.asyncapikotlindsl.chat.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class StompConfig : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(
        registry: StompEndpointRegistry,
    ) {
        registry.addEndpoint(ENDPOINT)
    }

    override fun configureMessageBroker(
        registry: MessageBrokerRegistry,
    ) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    companion object {
        const val ENDPOINT = "/ws-stomp"
    }
}
