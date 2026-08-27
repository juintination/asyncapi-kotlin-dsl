package com.example.asyncapikotlindsl.notification.repository

import com.example.asyncapikotlindsl.notification.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, String>
