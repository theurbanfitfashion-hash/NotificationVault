package com.notificationvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.notificationvault.data.NotificationRecord
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    records: List<NotificationRecord>,
    cloudSignedIn: Boolean,
    cloudMessage: String,
    onAuth: (String, String, String, Boolean) -> Unit,
    onSignOut: () -> Unit,
    onEnableAccess: () -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var showSignUp by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Notification Vault") },
            actions = { Icon(Icons.Default.Cloud, contentDescription = "Cloud sync") }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Your notification archive", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Notifications are stored on this phone first. Grant access to capture new alerts.")
                    Button(onClick = onEnableAccess, modifier = Modifier.fillMaxWidth()) {
                        Text("Enable notification access")
                    }
                }
            }

            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cloud sync", style = MaterialTheme.typography.titleMedium)
                    if (cloudSignedIn) {
                        Text(cloudMessage.ifBlank { "Signed in. Sync runs automatically when internet is available." })
                        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                    } else {
                        Text("Sign in with the same account on every device to share your archive.")
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("Supabase publishable key (sb_publishable_...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { onAuth(email.trim(), password, apiKey.trim(), false) }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
                        OutlinedButton(onClick = { onAuth(email.trim(), password, apiKey.trim(), true) }, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
                    }
                    if (cloudMessage.isNotBlank() && !cloudSignedIn) {
                        Text(cloudMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${records.size} saved notifications", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClear, enabled = records.isNotEmpty()) { Text("Clear all") }
            }
            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your captured notifications will appear here.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records, key = { it.id }) { item -> NotificationCard(item, onDelete) }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationRecord, onDelete: (String) -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(item.appName, style = MaterialTheme.typography.labelLarge)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.postedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.title.isNotBlank()) Text(item.title, style = MaterialTheme.typography.titleSmall)
            if (item.body.isNotBlank()) Text(item.body, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onDelete(item.id) }, modifier = Modifier.align(Alignment.End)) { Text("Delete") }
        }
    }
}
