package com.notificationvault.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notificationvault.data.VaultDatabase
import java.io.IOException

class NotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (VaultSession.get(applicationContext, "access_token").isNullOrBlank()) return Result.success()
        return try {
            val dao = VaultDatabase.get(applicationContext).notificationDao()
            val pending = dao.pendingSync()
            val cloudRecords = VaultCloud(applicationContext).sync(pending)
            if (pending.isNotEmpty()) dao.markSynced(pending.map { it.id })
            if (cloudRecords.isNotEmpty()) dao.insertAll(cloudRecords)
            Result.success()
        } catch (error: IOException) {
            Result.retry()
        } catch (error: Exception) {
            val message = error.message.orEmpty()
            if (message.contains("expired", true) || message.contains("sign in", true)) Result.failure()
            else Result.retry()
        }
    }
}
