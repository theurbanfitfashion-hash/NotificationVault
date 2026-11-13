package com.notificationvault

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.notificationvault.data.VaultDatabase
import com.notificationvault.sync.SyncScheduler
import com.notificationvault.sync.VaultCloud
import com.notificationvault.sync.VaultSession
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncScheduler.schedule(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val dao = remember { VaultDatabase.get(applicationContext).notificationDao() }
                val records by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
                var cloudSignedIn by remember { mutableStateOf(!VaultSession.get(applicationContext, "access_token").isNullOrBlank()) }
                var cloudMessage by remember { mutableStateOf("") }
                VaultScreen(
                    records = records,
                    cloudSignedIn = cloudSignedIn,
                    cloudMessage = cloudMessage,
                    onAuth = { email, password, key, signUp ->
                        lifecycleScope.launch {
                            try {
                                cloudMessage = VaultCloud(applicationContext).configureAndSignIn(email, password, key, signUp)
                                cloudSignedIn = !VaultSession.get(applicationContext, "access_token").isNullOrBlank()
                                if (cloudSignedIn) {
                                    SyncScheduler.syncNow(applicationContext)
                                    cloudMessage = "Signed in. Initial sync has been queued."
                                }
                            } catch (error: Exception) {
                                cloudMessage = error.message ?: "Could not connect to cloud."
                            }
                        }
                    },
                    onSignOut = {
                        lifecycleScope.launch {
                            VaultCloud(applicationContext).signOut()
                            cloudSignedIn = false
                            cloudMessage = "Signed out. Local notifications remain on this phone."
                        }
                    },
                    onEnableAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onDelete = { id -> lifecycleScope.launch { dao.deleteById(id) } },
                    onClear = { lifecycleScope.launch { dao.clearAll() } }
                )
            }
        }
    }
}
