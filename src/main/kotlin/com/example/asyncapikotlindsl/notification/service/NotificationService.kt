package com.example.asyncapikotlindsl.notification.service

import com.example.asyncapikotlindsl.notification.domain.Notification
import com.example.asyncapikotlindsl.notification.dto.request.PublishNotificationRequest
import com.example.asyncapikotlindsl.notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {

    @Transactional
    fun publish(
        request: PublishNotificationRequest,
    ): Notification {
        val notification = Notification(
            type = request.type,
            message = request.message,
        )
        return notificationRepository.save(notification)
    }
}
