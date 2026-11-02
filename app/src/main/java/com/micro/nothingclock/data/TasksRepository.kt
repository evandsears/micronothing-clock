package com.micro.nothingclock.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GoogleTaskItem(
    val id: String,
    val title: String,
    val dueMillis: Long? = null,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val listTitle: String = ""
)

@Suppress("DEPRECATION")
class TasksRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("nothing_tasks_cache", Context.MODE_PRIVATE)

    companion object {
        const val TASKS_SCOPE = "https://www.googleapis.com/auth/tasks.readonly"
        private const val OAUTH_SCOPE_PREFIX = "oauth2:"
        private const val API_BASE_URL = "https://tasks.googleapis.com/tasks/v1"
    }

    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(TASKS_SCOPE))
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(TASKS_SCOPE))) {
            account
        } else {
            null
        }
    }

    fun isConnected(): Boolean {
        return getSignedInAccount() != null
    }

    fun getConnectedAccountEmail(): String? {
        return getSignedInAccount()?.email ?: prefs.getString("connected_email", null)
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                prefs.edit()
                    .putString("connected_email", account.email)
                    .putBoolean("is_connected", true)
                    .apply()
                Result.success(account)
            } else {
                Result.failure(Exception("Sign-in failed: Account was null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect(activity: Activity, onComplete: () -> Unit) {
        val client = getGoogleSignInClient(activity)
        client.signOut().addOnCompleteListener {
            clearCache()
            onComplete()
        }
    }

    fun clearCache() {
        prefs.edit().clear().apply()
    }

    suspend fun syncTasks(): Result<List<GoogleTaskItem>> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount() ?: return@withContext Result.failure(Exception("Not signed in to Google"))
        val androidAccount = account.account ?: return@withContext Result.failure(Exception("No system Google account available"))

        try {
            val token = GoogleAuthUtil.getToken(context, androidAccount, "$OAUTH_SCOPE_PREFIX$TASKS_SCOPE")
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Unable to acquire OAuth token"))
            }

            // 1. Fetch task lists
            val listsUrl = URL("$API_BASE_URL/users/@me/lists?maxResults=10")
            val listsJson = httpGet(listsUrl, token)
            val listItems = mutableListOf<Pair<String, String>>() // (id, title)

            if (listsJson != null) {
                val itemsArr = listsJson.optJSONArray("items")
                if (itemsArr != null) {
                    for (i in 0 until itemsArr.length()) {
                        val obj = itemsArr.getJSONObject(i)
                        val id = obj.optString("id")
                        val title = obj.optString("title", "Tasks")
                        if (id.isNotEmpty()) {
                            listItems.add(Pair(id, title))
                        }
                    }
                }
            }

            if (listItems.isEmpty()) {
                listItems.add(Pair("@default", "My Tasks"))
            }

            // 2. Fetch uncompleted tasks for each list (up to 3 lists to avoid excessive latency)
            val allTasks = mutableListOf<GoogleTaskItem>()
            for ((listId, listTitle) in listItems.take(3)) {
                val tasksUrl = URL("$API_BASE_URL/lists/$listId/tasks?showCompleted=false&maxResults=20")
                val tasksJson = httpGet(tasksUrl, token) ?: continue
                val itemsArr = tasksJson.optJSONArray("items") ?: continue

                for (i in 0 until itemsArr.length()) {
                    val taskObj = itemsArr.getJSONObject(i)
                    val id = taskObj.optString("id")
                    val title = taskObj.optString("title").trim()
                    val status = taskObj.optString("status")
                    val dueStr = taskObj.optString("due")
                    val notes = taskObj.optString("notes")

                    // Only keep non-empty, uncompleted tasks
                    if (title.isNotEmpty() && status != "completed") {
                        val dueMillis = parseRfc3339Date(dueStr)
                        allTasks.add(
                            GoogleTaskItem(
                                id = id,
                                title = title,
                                dueMillis = dueMillis,
                                isCompleted = false,
                                notes = notes,
                                listTitle = listTitle
                            )
                        )
                    }
                }
            }

            saveCachedTasks(allTasks)
            Result.success(allTasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun httpGet(url: URL, bearerToken: String): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $bearerToken")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseRfc3339Date(dateStr: String?): Long? {
        if (dateStr.isNullOrEmpty()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(dateStr)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun saveCachedTasks(tasks: List<GoogleTaskItem>) {
        val arr = JSONArray()
        for (t in tasks) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                t.dueMillis?.let { put("dueMillis", it) }
                put("isCompleted", t.isCompleted)
                put("notes", t.notes)
                put("listTitle", t.listTitle)
            }
            arr.put(obj)
        }
        prefs.edit()
            .putString("cached_tasks_json", arr.toString())
            .putLong("last_sync_time", System.currentTimeMillis())
            .apply()
    }

    fun getCachedTasks(): List<GoogleTaskItem> {
        val jsonStr = prefs.getString("cached_tasks_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<GoogleTaskItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    GoogleTaskItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        dueMillis = if (obj.has("dueMillis")) obj.getLong("dueMillis") else null,
                        isCompleted = obj.optBoolean("isCompleted", false),
                        notes = obj.optString("notes", ""),
                        listTitle = obj.optString("listTitle", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Finds the next most relevant task for today.
     * Priority:
     * 1. Overdue tasks (due before today and still open)
     * 2. Tasks due today
     * 3. Any uncompleted tasks (undated or next upcoming)
     */
    fun getTopPriorityTask(): GoogleTaskItem? {
        val tasks = getCachedTasks()
        if (tasks.isEmpty()) return null

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfToday = cal.timeInMillis

        // 1. Tasks due today
        val dueToday = tasks.firstOrNull { it.dueMillis != null && it.dueMillis in startOfToday..endOfToday }
        if (dueToday != null) return dueToday

        // 2. Overdue tasks
        val overdue = tasks.firstOrNull { it.dueMillis != null && it.dueMillis < startOfToday }
        if (overdue != null) return overdue

        // 3. Undated tasks
        val undated = tasks.firstOrNull { it.dueMillis == null }
        if (undated != null) return undated

        // 4. Any other task
        return tasks.firstOrNull()
    }
}
