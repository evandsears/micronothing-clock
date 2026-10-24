package com.micro.nothingclock.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, NothingClockWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        if (appWidgetIds.isNotEmpty()) {
            NothingClockWidgetProvider.updateAllWidgets(context, appWidgetManager, appWidgetIds)
            NothingClockWidgetProvider.scheduleNextMinuteAlarm(context)
        }
    }
}
