package com.micro.nothingclock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.SizeF
import android.widget.RemoteViews
import com.micro.nothingclock.MainActivity
import com.micro.nothingclock.R
import com.micro.nothingclock.data.CalendarRepository
import com.micro.nothingclock.data.UpcomingCalendarEvent
import com.micro.nothingclock.data.WeatherRepository
import com.micro.nothingclock.data.WidgetConfig
import com.micro.nothingclock.data.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NothingClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
        scheduleNextMinuteAlarm(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextMinuteAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMinuteAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            "com.micro.nothingclock.ACTION_REFRESH_WIDGET" -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, NothingClockWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
                scheduleNextMinuteAlarm(context)
            }
        }
    }

    companion object {
        fun getWidgetDimensions(context: Context, options: Bundle?): Pair<Int, Int> {
            val defaultW = 320
            val defaultH = 144
            if (options == null) return Pair(1024, 461)

            val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            var widthDp = if (isLandscape) {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            } else {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            }
            var heightDp = if (isLandscape) {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            } else {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                }
                if (!sizes.isNullOrEmpty()) {
                    val currentSize = if (isLandscape) {
                        sizes.maxByOrNull { it.width }
                    } else {
                        sizes.maxByOrNull { it.height }
                    }
                    if (currentSize != null && currentSize.width > 0 && currentSize.height > 0) {
                        widthDp = currentSize.width.toInt()
                        heightDp = currentSize.height.toInt()
                    }
                }
            }

            if (widthDp <= 0) widthDp = defaultW
            if (heightDp <= 0) heightDp = defaultH

            // High-resolution base width of 1024, height strictly proportional to widget aspect ratio
            val canvasW = 1024
            val canvasH = ((1024f * heightDp.toFloat()) / widthDp.toFloat()).toInt().coerceIn(240, 2048)
            return Pair(canvasW, canvasH)
        }

        private fun buildRemoteViews(
            context: Context,
            config: WidgetConfig,
            eventTitle: String,
            minutesLeft: Long,
            temp: Int,
            condition: String,
            canvasWidth: Int = 1024,
            canvasHeight: Int = 461,
            isAllDay: Boolean = false
        ): RemoteViews {
            val bitmap = NothingClockRenderer.renderToBitmap(
                context = context,
                width = canvasWidth,
                height = canvasHeight,
                config = config,
                eventTitle = eventTitle,
                minutesLeft = minutesLeft,
                temp = temp,
                condition = condition,
                isAllDay = isAllDay
            )

            val views = RemoteViews(context.packageName, R.layout.widget_nothing_clock)
            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            // 1. Clock hotspot -> open system clock
            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val clockPendingIntent = PendingIntent.getActivity(
                context,
                101,
                clockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.hotspot_clock, clockPendingIntent)

            // 2. Calendar hotspot -> open calendar
            val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val calendarPendingIntent = PendingIntent.getActivity(
                context,
                102,
                calendarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.hotspot_calendar, calendarPendingIntent)

            // 3. Root click -> open app settings
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                103,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            return views
        }

        fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            options: Bundle? = null
        ) {
            val opt = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
            val (w, h) = getWidgetDimensions(context, opt)

            val prefs = WidgetPreferences(context)
            val config = prefs.loadConfig()
            val isF = config.tempUnit.contains("F", ignoreCase = true)
            val weatherRepo = WeatherRepository(context)
            val cachedWeather = weatherRepo.getCachedOrDefault(
                fallbackTemp = config.tempCelsius,
                fallbackCondition = config.weatherCondition,
                isFahrenheit = isF
            )

            val isAllDayFallback = config.fallbackMinutes == -2
            // Fast synchronous render with cached data
            try {
                val initialViews = buildRemoteViews(
                    context = context,
                    config = config,
                    eventTitle = config.fallbackEventTitle,
                    minutesLeft = if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                    temp = cachedWeather.temperature,
                    condition = cachedWeather.condition,
                    canvasWidth = w,
                    canvasHeight = h,
                    isAllDay = isAllDayFallback
                )
                appWidgetManager.updateAppWidget(appWidgetId, initialViews)
            } catch (e: Exception) {
                android.util.Log.e("MicroNothingWidget", "Single fast render failed", e)
            }

            // Background live update if live calendar or weather enabled
            if (config.useLiveCalendar || config.useLiveWeather) {
                val calendarRepo = CalendarRepository(context)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val nextEvent = if (config.useLiveCalendar) {
                            calendarRepo.getNextUpcomingEvent(config.fallbackEventTitle, config.fallbackMinutes)
                        } else {
                            UpcomingCalendarEvent(
                                config.fallbackEventTitle,
                                if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                                hasEvent = config.fallbackMinutes != -1,
                                isAllDay = isAllDayFallback
                            )
                        }

                        val weather = if (config.useLiveWeather) {
                            weatherRepo.fetchWeather(
                                isFahrenheit = isF,
                                refreshIntervalMinutes = config.weatherRefreshIntervalMinutes,
                                fallbackTemp = config.tempCelsius,
                                fallbackCondition = config.weatherCondition
                            )
                        } else {
                            cachedWeather
                        }

                        val liveViews = buildRemoteViews(
                            context = context,
                            config = config,
                            eventTitle = nextEvent.title,
                            minutesLeft = nextEvent.minutesUntilStart,
                            temp = weather.temperature,
                            condition = weather.condition,
                            canvasWidth = w,
                            canvasHeight = h,
                            isAllDay = nextEvent.isAllDay
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, liveViews)
                    } catch (e: Exception) {
                        android.util.Log.e("MicroNothingWidget", "Single live update failed", e)
                    }
                }
            }
        }

        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            val prefs = WidgetPreferences(context)
            val config = prefs.loadConfig()
            val isF = config.tempUnit.contains("F", ignoreCase = true)
            val weatherRepo = WeatherRepository(context)
            val cachedWeather = weatherRepo.getCachedOrDefault(
                fallbackTemp = config.tempCelsius,
                fallbackCondition = config.weatherCondition,
                isFahrenheit = isF
            )

            val isAllDayFallback = config.fallbackMinutes == -2
            // 1. Fast immediate synchronous render per widget dimension
            for (id in appWidgetIds) {
                val opt = appWidgetManager.getAppWidgetOptions(id)
                val (w, h) = getWidgetDimensions(context, opt)
                try {
                    val initialViews = buildRemoteViews(
                        context = context,
                        config = config,
                        eventTitle = config.fallbackEventTitle,
                        minutesLeft = if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                        temp = cachedWeather.temperature,
                        condition = cachedWeather.condition,
                        canvasWidth = w,
                        canvasHeight = h,
                        isAllDay = isAllDayFallback
                    )
                    appWidgetManager.updateAppWidget(id, initialViews)
                } catch (e: Exception) {
                    android.util.Log.e("MicroNothingWidget", "Fast render failed for widget $id", e)
                }
            }

            // 2. Background update for live calendar and/or live weather
            if (config.useLiveCalendar || config.useLiveWeather) {
                val calendarRepo = CalendarRepository(context)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val nextEvent = if (config.useLiveCalendar) {
                            calendarRepo.getNextUpcomingEvent(config.fallbackEventTitle, config.fallbackMinutes)
                        } else {
                            UpcomingCalendarEvent(
                                config.fallbackEventTitle,
                                if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                                hasEvent = config.fallbackMinutes != -1,
                                isAllDay = isAllDayFallback
                            )
                        }

                        val weather = if (config.useLiveWeather) {
                            weatherRepo.fetchWeather(
                                isFahrenheit = isF,
                                refreshIntervalMinutes = config.weatherRefreshIntervalMinutes,
                                fallbackTemp = config.tempCelsius,
                                fallbackCondition = config.weatherCondition
                            )
                        } else {
                            cachedWeather
                        }

                        for (appWidgetId in appWidgetIds) {
                            val opt = appWidgetManager.getAppWidgetOptions(appWidgetId)
                            val (w, h) = getWidgetDimensions(context, opt)
                            val liveViews = buildRemoteViews(
                                context = context,
                                config = config,
                                eventTitle = nextEvent.title,
                                minutesLeft = nextEvent.minutesUntilStart,
                                temp = weather.temperature,
                                condition = weather.condition,
                                canvasWidth = w,
                                canvasHeight = h,
                                isAllDay = nextEvent.isAllDay
                            )
                            appWidgetManager.updateAppWidget(appWidgetId, liveViews)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MicroNothingWidget", "Live widget update failed", e)
                    }
                }
            }
        }

        fun scheduleNextMinuteAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WidgetAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                201,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Trigger at next exact minute boundary
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 1)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                // If exact alarms not permitted, fallback to inexact
                alarmManager.set(AlarmManager.RTC, calendar.timeInMillis, pendingIntent)
            }
        }

        fun cancelMinuteAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WidgetAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                201,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
