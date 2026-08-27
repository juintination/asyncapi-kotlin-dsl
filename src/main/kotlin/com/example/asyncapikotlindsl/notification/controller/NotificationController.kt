package com.example.asyncapikotlindsl.notification.controller

import com.example.asyncapikotlindsl.notification.dto.request.PublishNotificationRequest
import com.example.asyncapikotlindsl.notification.dto.response.NotificationResponse
import com.example.asyncapikotlindsl.notification.service.NotificationService
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/events/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {

    private val emitters = CopyOnWriteArrayList<SseEmitter>()
    private val idSequence = AtomicLong()

    @GetMapping
    fun subscribe(): SseEmitter {
        val emitter = SseEmitter(TIMEOUT)
        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        emitters.add(emitter)
        try {
            emitter.send(SseEmitter.event().comment("connected"))
        } catch (ex: Exception) {
            emitters.remove(emitter)
        }
        return emitter
    }

    @PostMapping
    fun publish(
        @RequestBody request: PublishNotificationRequest,
    ): ResponseEntity<Void> {
        val notification = notificationService.publish(request)
        broadcast(NotificationResponse.from(notification))
        return ResponseEntity.noContent().build()
    }

    private fun broadcast(
        response: NotificationResponse,
    ) {
        val dead = mutableListOf<SseEmitter>()
        emitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(EVENT_NAME)
                        .id(idSequence.incrementAndGet().toString())
                        .data(response, MediaType.APPLICATION_JSON),
                )
            } catch (ex: Exception) {
                dead.add(emitter)
            }
        }
        emitters.removeAll(dead)
    }

    companion object {
        private const val EVENT_NAME = "notification"
        private const val TIMEOUT = 30_000L
    }
}
