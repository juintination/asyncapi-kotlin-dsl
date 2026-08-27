package com.example.asyncapikotlindsl.notification.controller

import com.example.asyncapikotlindsl.notification.dto.response.NotificationResponse
import com.example.asyncapikotlindsl.notification.repository.NotificationRepository
import com.example.asyncapikotlindsl.support.asyncapi.AsyncApiDocsTest
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NotificationControllerTest : AsyncApiDocsTest() {

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Test
    fun `구독자가 연결된 상태에서 알림을 발행하면 구독자가 수신하고 알림이 저장된다`() {
        documentSse("notifications/receiveNotification") {
            channel(
                path = "/events/notifications",
                protocol = "https",
                description = "서버가 발행하는 실시간 알림 이벤트",
            )
            operation {
                summary("서버가 발행하는 알림 이벤트를 수신한다")
            }
            receive<NotificationResponse> {
                field("type", "알림 종류 (notification 고정)")
                field("message", "알림 본문")
                trigger {
                    httpPost(
                        path = "/events/notifications",
                        jsonBody = """{"type":"notification","message":"점심 스터디 시작"}""",
                    ) shouldBe 204
                }
                verify {
                    it.type shouldBe "notification"
                    it.message shouldBe "점심 스터디 시작"
                }
            }
        }

        notificationRepository.findAll().map { it.message } shouldContain "점심 스터디 시작"
    }

    @Test
    fun `구독자가 없을 때 알림을 발행해도 204 를 반환하고 알림이 저장된다`() {
        val status = httpPost(
            path = "/events/notifications",
            jsonBody = """{"type":"notification","message":"조용한 발행"}""",
        )

        status shouldBe 204
        notificationRepository.findAll().map { it.message } shouldContain "조용한 발행"
    }
}
