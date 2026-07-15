package com.tvtime.app.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tvtime.app.R

class MediaWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MediaViewsFactory(applicationContext)
    }
}

class MediaViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<MediaItem> = listOf()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences("TVTimeWidgetData", Context.MODE_PRIVATE)
        val json = prefs.getString("inprogress", "[]")
        val type = object : TypeToken<List<MediaItem>>() {}.type
        items = try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            listOf()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_media_item)

        views.setTextViewText(R.id.item_title, item.title)
        
        val infoText = if (item.type == "series" && item.nextEpisode != null) {
            "S${item.nextEpisode.season.toString().padStart(2, '0')}E${item.nextEpisode.number.toString().padStart(2, '0')} · ${item.nextEpisode.name}"
        } else {
            "Film"
        }
        views.setTextViewText(R.id.item_info, infoText)
        views.setTextViewText(R.id.item_rating, "★ ${item.rating}")

        // Fill-in intent for the check button
        val fillInIntent = Intent().apply {
            putExtra("media_id", item.id)
            putExtra("media_title", item.title)
            putExtra("is_movie", item.type == "movie")
            if (item.nextEpisode != null) {
                putExtra("season", item.nextEpisode.season)
                putExtra("number", item.nextEpisode.number)
            }
        }
        views.setOnClickFillInIntent(R.id.btn_check, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true

    data class MediaItem(
        val id: String,
        val title: String,
        val type: String,
        val rating: Float,
        val nextEpisode: Episode?
    )

    data class Episode(
        val season: Int,
        val number: Int,
        val name: String
    )
}
