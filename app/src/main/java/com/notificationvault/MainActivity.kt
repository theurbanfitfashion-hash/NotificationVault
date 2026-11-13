package com.notificationvault

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.notificationvault.data.VaultDatabase
import com.notificationvault.sync.SyncScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncScheduler.schedule(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val dao = remember { VaultDatabase.get(applicationContext).notificationDao() }
                val records by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
                VaultScreen(
                    records = records,
                    onEnableAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onDelete = { id -> lifecycleScope.launch { dao.deleteById(id) } },
                    onClear = { lifecycleScope.launch { dao.clearAll() } }
                )
            }
        }
    }
}