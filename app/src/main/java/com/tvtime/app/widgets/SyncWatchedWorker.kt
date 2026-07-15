package com.tvtime.app.widgets

import android.content.Context
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SyncWatchedWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val SUPABASE_URL = "https://vjhegncviufyguzdrpdp.supabase.co"
        private const val SUPABASE_KEY = "sb_publishable_hLYKWsVftWedOIbDinl0mQ_9uGREGsw"
        private const val USER_ID = "tvr_master_user_2026"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWatchedWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_watched_widget",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("TVTimeWidgetData", Context.MODE_PRIVATE)
        val queueJson = prefs.getString("pending_watched", "[]") ?: "[]"
        val type = object : TypeToken<MutableList<MarkWatchedReceiver.PendingAction>>() {}.type
        val queue: MutableList<MarkWatchedReceiver.PendingAction> = Gson().fromJson(queueJson, type) ?: mutableListOf()

        if (queue.isEmpty()) return Result.success()

        val client = OkHttpClient()
        val gson = Gson()

        for (action in queue) {
            try {
                // 1. Fetch current data
                val getRequest = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/user_library?user_id=eq.$USER_ID&media_id=eq.${action.mediaId}&select=media_data")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .build()

                val response = client.newCall(getRequest).execute()
                if (!response.isSuccessful) return Result.retry()

                val body = response.body?.string() ?: continue
                val rows = gson.fromJson(body, Array<JsonObject>::class.java)
                if (rows.isEmpty()) continue

                val mediaData = rows[0].getAsJsonObject("media_data")
                
                // 2. Update status
                if (action.season == -1) {
                    mediaData.addProperty("status", "Watched")
                } else {
                    val episodes = mediaData.getAsJsonArray("episodes")
                    for (ep in episodes) {
                        val e = ep.asJsonObject
                        if (e.get("season").asInt == action.season && e.get("number").asInt == action.number) {
                            e.addProperty("watched", true)
                        }
                    }
                }
                mediaData.addProperty("last_modified", System.currentTimeMillis())

                // 3. Patch back
                val patchBody = JsonObject().apply {
                    add("media_data", mediaData)
                    addProperty("last_modified", System.currentTimeMillis())
                }.toString().toRequestBody("application/json".toMediaType())

                val patchRequest = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/user_library?user_id=eq.$USER_ID&media_id=eq.${action.mediaId}")
                    .patch(patchBody)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val patchResponse = client.newCall(patchRequest).execute()
                if (!patchResponse.isSuccessful) return Result.retry()

            } catch (e: Exception) {
                return Result.retry()
            }
        }

        // Success: Clear queue
        prefs.edit().putString("pending_watched", "[]").apply()
        return Result.success()
    }
}
