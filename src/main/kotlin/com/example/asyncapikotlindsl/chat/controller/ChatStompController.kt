package com.example.asyncapikotlindsl.chat.controller

import com.example.asyncapikotlindsl.chat.dto.request.ChatMessageRequest
import com.example.asyncapikotlindsl.chat.dto.response.ChatMessageResponse
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller

/**
 * STOMP 채팅. `/app/chat` 로 들어온 메시지에 `sentAt` 을 붙여 `/topic/chat` 구독자에게 브로드캐스트한다.
 */
@Controller
class ChatStompController {

    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    fun chat(
        request: ChatMessageRequest,
    ) = ChatMessageResponse.from(request)
}
