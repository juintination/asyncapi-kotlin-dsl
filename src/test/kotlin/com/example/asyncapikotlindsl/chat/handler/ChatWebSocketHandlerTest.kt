package com.example.asyncapikotlindsl.chat.handler

import com.example.asyncapikotlindsl.chat.dto.request.ChatMessageRequest
import com.example.asyncapikotlindsl.chat.dto.response.ChatMessageResponse
import com.example.asyncapikotlindsl.support.asyncapi.AsyncApiDocsTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class ChatWebSocketHandlerTest : AsyncApiDocsTest() {

    @Test
    fun `채팅 메시지를 보내면 sentAt 이 붙어 브로드캐스트된다`() {
        documentWebSocket("chat/sendMessage") {
            channel(
                path = "/ws/chat",
                protocol = "ws",
                description = "실시간 채팅 채널",
            )
            send<ChatMessageRequest>(summary = "채팅 메시지 전송") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                payload("""{"sender":"heee","text":"점심 뭐 먹지"}""")
            }
            receive<ChatMessageResponse>(summary = "브로드캐스트된 메시지 수신") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                field("sentAt", "서버 수신 시각(ISO-8601)")
                verify {
                    it.sender shouldBe "heee"
                    it.text shouldBe "점심 뭐 먹지"
                    it.sentAt.shouldNotBeEmpty()
                }
            }
        }
    }
}
