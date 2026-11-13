package com.notificationvault.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notificationvault.data.VaultDatabase

/**
 * Cloud upload is intentionally gated until authenticated Supabase sign-in
 * and token refresh are implemented. Never embed service_role in this app.
 */
class NotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("vault_cloud", Context.MODE_PRIVATE)
        val url = prefs.getString("supabase_url", null)
        val key = prefs.getString("publishable_key", null)
        val accessToken = prefs.getString("access_token", null)
        if (url.isNullOrBlank() || key.isNullOrBlank() || accessToken.isNullOrBlank()) {
            return Result.success()
        }
        val pending = VaultDatabase.get(applicationContext).notificationDao().pendingSync()
        return if (pending.isEmpty()) Result.success() else Result.retry()
    }
}