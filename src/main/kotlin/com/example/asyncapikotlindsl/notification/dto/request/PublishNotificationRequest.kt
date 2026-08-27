package com.example.asyncapikotlindsl.notification.dto.request

data class PublishNotificationRequest(
    val type: String,
    val message: String,
)
