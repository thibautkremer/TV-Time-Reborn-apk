package com.tvtime.app.widgets

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tvtime.app.R

class MarkWatchedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MediaWidgetProvider.ACTION_MARK_WATCHED) {
            val mediaId = intent.getStringExtra("media_id") ?: return
            val season = intent.getIntExtra("season", -1)
            val number = intent.getIntExtra("number", -1)

            // 1. Update Local SharedPreferences instantly
            updateLocalInprogress(context, mediaId, season, number)

            // 2. Add to pending queue
            addToPendingQueue(context, mediaId, season, number)

            // 3. Notify Widget to refresh UI
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val mediaIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MediaWidgetProvider::class.java))
            appWidgetManager.notifyAppWidgetViewDataChanged(mediaIds, R.id.widget_list)

            // 4. Enqueue Sync Worker
            SyncWatchedWorker.enqueue(context)
        }
    }

    private fun updateLocalInprogress(context: Context, mediaId: String, season: Int, number: Int) {
        val prefs = context.getSharedPreferences("TVTimeWidgetData", Context.MODE_PRIVATE)
        val json = prefs.getString("inprogress", "[]") ?: "[]"
        val type = object : TypeToken<MutableList<MediaViewsFactory.MediaItem>>() {}.type
        val items: MutableList<MediaViewsFactory.MediaItem> = try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }

        val updatedItems = items.filterNot { it.id == mediaId } // Simplified: remove for now, JS will resync
        prefs.edit().putString("inprogress", Gson().toJson(updatedItems)).apply()
    }

    private fun addToPendingQueue(context: Context, mediaId: String, season: Int, number: Int) {
        val prefs = context.getSharedPreferences("TVTimeWidgetData", Context.MODE_PRIVATE)
        val queueJson = prefs.getString("pending_watched", "[]") ?: "[]"
        val type = object : TypeToken<MutableList<PendingAction>>() {}.type
        val queue: MutableList<PendingAction> = Gson().fromJson(queueJson, type) ?: mutableListOf()
        
        queue.add(PendingAction(mediaId, season, number, System.currentTimeMillis()))
        prefs.edit().putString("pending_watched", Gson().toJson(queue)).apply()
    }

    data class PendingAction(
        val mediaId: String,
        val season: Int,
        val number: Int,
        val timestamp: Long
    )
}
