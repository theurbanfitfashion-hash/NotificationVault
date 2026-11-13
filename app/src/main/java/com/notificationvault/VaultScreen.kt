package com.notificationvault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.History
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

private enum class VaultPage { HOME, HISTORY }

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
    var page by remember { mutableStateOf(VaultPage.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (page == VaultPage.HOME) "Notification Vault" else "Notification History") },
                actions = { Icon(Icons.Default.Cloud, contentDescription = "Cloud sync") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = page == VaultPage.HOME,
                    onClick = { page = VaultPage.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = page == VaultPage.HISTORY,
                    onClick = { page = VaultPage.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") },
                    badge = {
                        if (records.isNotEmpty()) {
                            Badge { Text(if (records.size > 99) "99+" else records.size.toString()) }
                        }
                    }
                )
            }
        }
    ) { padding ->
        when (page) {
            VaultPage.HOME -> HomePage(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                recordCount = records.size,
                cloudSignedIn = cloudSignedIn,
                cloudMessage = cloudMessage,
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                onAuth = onAuth,
                onSignOut = onSignOut,
                onEnableAccess = onEnableAccess,
                onOpenHistory = { page = VaultPage.HISTORY }
            )
            VaultPage.HISTORY -> HistoryPage(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
                records = records,
                onDelete = onDelete,
                onClear = onClear
            )
        }
    }
}

@Composable
private fun HomePage(
    modifier: Modifier,
    recordCount: Int,
    cloudSignedIn: Boolean,
    cloudMessage: String,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onAuth: (String, String, String, Boolean) -> Unit,
    onSignOut: () -> Unit,
    onEnableAccess: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Your notification archive", style = MaterialTheme.typography.titleMedium)
                }
                Text("Saved notifications", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(recordCount.toString(), style = MaterialTheme.typography.headlineLarge)
                OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("View notification history")
                }
                Button(onClick = onEnableAccess, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable notification access")
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cloud sync", style = MaterialTheme.typography.titleMedium)
                if (cloudSignedIn) {
                    Text(cloudMessage.ifBlank { "Signed in. Sync runs when internet is available." })
                    OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                } else {
                    Text("Use the same account on every device to share notification history.")
                    OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = apiKey, onValueChange = onApiKeyChange, label = { Text("Supabase publishable key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onAuth(email.trim(), password, apiKey.trim(), false) }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
                    OutlinedButton(onClick = { onAuth(email.trim(), password, apiKey.trim(), true) }, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
                }
                if (cloudMessage.isNotBlank() && !cloudSignedIn) {
                    Text(cloudMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun HistoryPage(
    modifier: Modifier,
    records: List<NotificationRecord>,
    onDelete: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("All notifications", style = MaterialTheme.typography.titleLarge)
                Text("${records.size} saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClear, enabled = records.isNotEmpty()) { Text("Clear all") }
        }
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp))
                    Text("No notification history yet", style = MaterialTheme.typography.titleMedium)
                    Text("New notifications will appear here after access is enabled.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(records, key = { it.id }) { item -> NotificationCard(item, onDelete) }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationRecord, onDelete: (String) -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item.appName.ifBlank { item.packageName }, style = MaterialTheme.typography.labelLarge)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.postedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.title.isNotBlank()) Text(item.title, style = MaterialTheme.typography.titleSmall)
            if (item.body.isNotBlank()) Text(item.body, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onDelete(item.id) }, modifier = Modifier.align(Alignment.End)) { Text("Delete") }
        }
    }
}
