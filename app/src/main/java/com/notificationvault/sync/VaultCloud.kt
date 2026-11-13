package com.notificationvault.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.notificationvault.data.NotificationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object VaultSession {
    private const val PREFS = "vault_secure"
    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun get(context: Context, key: String): String? = prefs(context).getString(key, null)
    fun put(context: Context, values: Map<String, String>) {
        prefs(context).edit().apply { values.forEach { (k, v) -> putString(k, v) } }.apply()
    }
    fun clear(context: Context) { prefs(context).edit().clear().apply() }
    fun deviceId(context: Context): String {
        val p = prefs(context)
        return p.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            p.edit().putString("device_id", it).apply()
        }
    }
}

class VaultCloud(private val context: Context) {
    private val baseUrl = "https://qijinaxofwfxwcbvsvjl.supabase.co"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json".toMediaType()

    suspend fun configureAndSignIn(email: String, password: String, apiKey: String, signUp: Boolean): String =
        withContext(Dispatchers.IO) {
            require(email.isNotBlank() && password.length >= 6) { "Enter a valid email and password (at least 6 characters)." }
            require(apiKey.startsWith("sb_publishable_")) { "Use the Supabase publishable key (sb_publishable_...). Never paste a secret/service-role key." }
            VaultSession.put(context, mapOf("api_key" to apiKey))
            val endpoint = if (signUp) "/auth/v1/signup" else "/auth/v1/token?grant_type=password"
            val response = request(endpoint, "POST", JSONObject().put("email", email).put("password", password), null)
            val access = response.optString("access_token")
            if (access.isBlank()) {
                if (signUp && (response.has("id") || response.has("user"))) "Account created. Check your email if confirmation is enabled, then sign in."
                else throw IllegalStateException(response.optString("msg", response.optString("message", "Authentication failed.")))
            } else {
                saveSession(response)
                "Signed in as ${response.optJSONObject("user")?.optString("email").orEmpty()}"
            }
        }

    suspend fun signOut() = withContext(Dispatchers.IO) { VaultSession.clear(context) }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        val access = validAccessToken()
        val key = VaultSession.get(context, "api_key") ?: error("Supabase key is missing.")
        execute("/rest/v1/notifications?id=eq.$id", "DELETE", null, access, key, mapOf("Prefer" to "return=minimal"))
    }

    suspend fun clearNotifications() = withContext(Dispatchers.IO) {
        val access = validAccessToken()
        val key = VaultSession.get(context, "api_key") ?: error("Supabase key is missing.")
        execute("/rest/v1/notifications?id=not.is.null", "DELETE", null, access, key, mapOf("Prefer" to "return=minimal"))
    }

    suspend fun sync(records: List<NotificationRecord>): List<NotificationRecord> = withContext(Dispatchers.IO) {
        val access = validAccessToken()
        val key = VaultSession.get(context, "api_key") ?: error("Supabase key is missing. Sign in again.")
        val device = VaultSession.deviceId(context)
        if (records.isNotEmpty()) {
            val payload = JSONArray()
            records.forEach { r ->
                payload.put(JSONObject()
                    .put("id", r.id)
                    .put("user_id", VaultSession.get(context, "user_id"))
                    .put("device_id", device)
                    .put("package_name", r.packageName)
                    .put("app_name", r.appName)
                    .put("title", r.title)
                    .put("body", r.body)
                    .put("posted_at", java.time.Instant.ofEpochMilli(r.postedAt).toString()))
            }
            request("/rest/v1/notifications?on_conflict=id", "POST", payload, access, key, mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal"))
        }
        val merged = mutableListOf<NotificationRecord>()
        var offset = 0
        while (true) {
            val pageResponse = request(
                "/rest/v1/notifications?select=id,package_name,app_name,title,body,posted_at&order=posted_at.desc&limit=1000&offset=$offset",
                "GET", null as JSONObject?, access, key
            )
            val page = pageResponse.optJSONArray("rows") ?: JSONArray()
            for (i in 0 until page.length()) {
                val item = page.getJSONObject(i)
                val time = try { java.time.Instant.parse(item.getString("posted_at")).toEpochMilli() } catch (_: Exception) { System.currentTimeMillis() }
                merged += NotificationRecord(
                    id = item.getString("id"),
                    packageName = item.optString("package_name"),
                    appName = item.optString("app_name"),
                    title = item.optString("title"),
                    body = item.optString("body"),
                    postedAt = time,
                    synced = true
                )
            }
            if (page.length() < 1000) break
            offset += 1000
        }
        merged
    }

    private fun saveSession(json: JSONObject) {
        val values = mutableMapOf(
            "access_token" to json.getString("access_token"),
            "user_id" to (json.optJSONObject("user")?.optString("id") ?: VaultSession.get(context, "user_id").orEmpty()),
            "refresh_token" to json.getString("refresh_token"),
            "expires_at" to (System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000L).toString()
        )
        VaultSession.put(context, values)
    }

    private fun validAccessToken(): String {
        val current = VaultSession.get(context, "access_token") ?: error("Sign in to enable cloud sync.")
        val expiry = VaultSession.get(context, "expires_at")?.toLongOrNull() ?: 0L
        if (expiry > System.currentTimeMillis() + 60_000) return current
        val refresh = VaultSession.get(context, "refresh_token") ?: error("Session expired. Sign in again.")
        val key = VaultSession.get(context, "api_key") ?: error("Supabase key is missing.")
        val result = request("/auth/v1/token?grant_type=refresh_token", "POST", JSONObject().put("refresh_token", refresh), null, key)
        saveSession(result)
        return result.getString("access_token")
    }

    private fun request(
        path: String,
        method: String,
        body: JSONObject?,
        token: String?,
        apiKeyOverride: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): JSONObject = execute(path, method, body?.toString(), token, apiKeyOverride, extraHeaders)

    private fun request(
        path: String,
        method: String,
        body: JSONArray?,
        token: String?,
        apiKeyOverride: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): JSONObject = execute(path, method, body?.toString(), token, apiKeyOverride, extraHeaders)

    private fun execute(path: String, method: String, body: String?, token: String?, apiKeyOverride: String?, extraHeaders: Map<String, String>): JSONObject {
        val key = apiKeyOverride ?: VaultSession.get(context, "api_key") ?: error("Enter your Supabase publishable key.")
        val builder = Request.Builder().url(baseUrl + path)
            .addHeader("apikey", key)
            .addHeader("Accept", "application/json")
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        extraHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
        val requestBody = body?.toRequestBody(jsonType)
        when (method) {
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(jsonType))
            "GET" -> builder.get()
            "DELETE" -> builder.delete(requestBody)
            else -> error("Unsupported request")
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = try { JSONObject(text).optString("message", JSONObject(text).optString("msg", text)) } catch (_: Exception) { text }
                throw IllegalStateException(message.ifBlank { "Cloud request failed (${response.code})." })
            }
            if (text.isBlank()) return JSONObject()
            return if (text.trimStart().startsWith("[")) JSONObject().put("rows", JSONArray(text)) else JSONObject(text)
        }
    }
}
