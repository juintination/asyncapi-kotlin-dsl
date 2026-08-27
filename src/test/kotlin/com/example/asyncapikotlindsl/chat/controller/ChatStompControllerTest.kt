package com.example.asyncapikotlindsl.chat.controller

import com.example.asyncapikotlindsl.chat.config.StompConfig
import com.example.asyncapikotlindsl.chat.dto.request.ChatMessageRequest
import com.example.asyncapikotlindsl.chat.dto.response.ChatMessageResponse
import com.example.asyncapikotlindsl.support.asyncapi.AsyncApiDocsTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class ChatStompControllerTest : AsyncApiDocsTest() {

    @Test
    fun `app-chat 으로 보내면 topic-chat 구독자가 sentAt 붙은 메시지를 받는다`() {
        documentStomp("chat/stompChat", endpoint = StompConfig.ENDPOINT) {
            destination(
                sendTo = "/app/chat",
                subscribeTo = "/topic/chat",
                description = "STOMP 채팅",
            )
            send<ChatMessageRequest>(summary = "메시지 전송 (/app/chat)") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                payload("""{"sender":"heee","text":"STOMP 안녕"}""")
            }
            receive<ChatMessageResponse>(summary = "구독 메시지 수신 (/topic/chat)") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                field("sentAt", "서버 수신 시각(ISO-8601)")
                verify {
                    it.sender shouldBe "heee"
                    it.text shouldBe "STOMP 안녕"
                    it.sentAt.shouldNotBeEmpty()
                }
            }
        }
    }
}
