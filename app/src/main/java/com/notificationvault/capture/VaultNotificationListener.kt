package com.notificationvault.capture

import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notificationvault.data.NotificationRecord
import com.notificationvault.data.VaultDatabase
import com.notificationvault.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class VaultNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras: Bundle = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val body = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val finalBody = bigText.ifBlank { body }
        if (title.isBlank() && finalBody.isBlank()) return

        val appName = try {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            sbn.packageName
        }

        val record = NotificationRecord(
            id = UUID.randomUUID().toString(),
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            body = finalBody,
            postedAt = sbn.postTime
        )
        scope.launch {
            VaultDatabase.get(applicationContext).notificationDao().insert(record)
            SyncScheduler.syncNow(applicationContext)
        }
    }
}