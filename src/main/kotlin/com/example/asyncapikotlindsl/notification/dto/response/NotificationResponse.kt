package com.example.asyncapikotlindsl.notification.dto.response

import com.example.asyncapikotlindsl.notification.domain.Notification

data class NotificationResponse(
    val type: String,
    val message: String,
) {
    companion object {
        fun from(
            notification: Notification,
        ) = NotificationResponse(
            type = notification.type,
            message = notification.message,
        )
    }
}
