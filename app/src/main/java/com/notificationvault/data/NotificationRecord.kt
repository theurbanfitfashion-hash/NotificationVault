package com.notificationvault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationRecord(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val synced: Boolean = false
)