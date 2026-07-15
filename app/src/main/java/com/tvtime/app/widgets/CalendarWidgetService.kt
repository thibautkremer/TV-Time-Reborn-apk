package com.tvtime.app.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tvtime.app.R

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CalendarViewsFactory(applicationContext)
    }
}

class CalendarViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<CalendarItem> = listOf()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences("TVTimeWidgetData", Context.MODE_PRIVATE)
        val json = prefs.getString("calendar", "[]")
        val type = object : TypeToken<List<CalendarItem>>() {}.type
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
        val views = RemoteViews(context.packageName, R.layout.widget_calendar_item)
        
        views.setTextViewText(R.id.item_date, item.formattedDate)
        views.setTextViewText(R.id.item_title, item.title)
        views.setTextViewText(R.id.item_info, item.info)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true

    data class CalendarItem(
        val formattedDate: String,
        val title: String,
        val info: String
    )
}
