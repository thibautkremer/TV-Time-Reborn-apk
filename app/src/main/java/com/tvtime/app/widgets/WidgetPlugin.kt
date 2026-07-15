package com.tvtime.app.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.tvtime.app.R

@CapacitorPlugin(name = "WidgetPlugin")
class WidgetPlugin : Plugin() {

    @PluginMethod
    fun updateWidgetData(call: PluginCall) {
        val calendarJson = call.getString("calendarJson")
        val inprogressJson = call.getString("inprogressJson")

        val prefs = context.getSharedPreferences("TVTimeWidgetData", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (calendarJson != null) {
            editor.putString("calendar", calendarJson)
        }
        if (inprogressJson != null) {
            editor.putString("inprogress", inprogressJson)
        }
        editor.apply()

        // Notify widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        val calendarIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(calendarIds, R.id.widget_list)
        
        val mediaIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MediaWidgetProvider::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(mediaIds, R.id.widget_list)

        call.resolve()
    }
}
