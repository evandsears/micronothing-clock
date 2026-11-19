package com.micro.nothingclock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.micro.nothingclock.MainActivity
import com.micro.nothingclock.R
import com.micro.nothingclock.data.CalendarRepository
import com.micro.nothingclock.data.TasksRepository
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
        if (appWidgetIds.isNotEmpty()) {
            val pendingResult = goAsync()
            updateAllWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
            scheduleNextMinuteAlarm(context)
        }
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

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_PROVIDER_CHANGED,
            Intent.ACTION_WALLPAPER_CHANGED,
            "com.micro.nothingclock.ACTION_REFRESH_WIDGET" -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, NothingClockWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds.isNotEmpty()) {
                    val pendingResult = goAsync()
                    updateAllWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
                    scheduleNextMinuteAlarm(context)
                }
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
            isAllDay: Boolean = false,
            isTask: Boolean = false,
            hasEvent: Boolean = true
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
                isAllDay = isAllDay,
                isTask = isTask,
                hasEvent = hasEvent
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

            // 2. Weather hotspot -> open Google Weather full forecast
            val weatherIntent = WeatherRepository.createGoogleWeatherIntent(context)
            val weatherPendingIntent = PendingIntent.getActivity(
                context,
                104,
                weatherIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 3. Calendar or Tasks hotspot -> open tasks if task item, else open calendar
            val targetAgendaIntent = if (isTask) {
                context.packageManager.getLaunchIntentForPackage("com.google.android.apps.tasks")
                    ?: Intent(Intent.ACTION_VIEW).apply {
                        data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
            } else {
                Intent(Intent.ACTION_VIEW).apply {
                    data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            val calendarPendingIntent = PendingIntent.getActivity(
                context,
                102,
                targetAgendaIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val shouldShowCalendar = NothingClockRenderer.shouldShowCalendar(config, hasEvent)
            if (shouldShowCalendar) {
                views.setViewVisibility(R.id.hotspot_container_calendar, View.VISIBLE)
                views.setViewVisibility(R.id.hotspot_container_no_calendar, View.GONE)

                views.setOnClickPendingIntent(R.id.hotspot_clock, clockPendingIntent)
                views.setOnClickPendingIntent(R.id.hotspot_weather, weatherPendingIntent)
                views.setOnClickPendingIntent(R.id.hotspot_calendar, calendarPendingIntent)
            } else {
                views.setViewVisibility(R.id.hotspot_container_calendar, View.GONE)
                views.setViewVisibility(R.id.hotspot_container_no_calendar, View.VISIBLE)

                views.setOnClickPendingIntent(R.id.hotspot_clock_no_cal, clockPendingIntent)
                views.setOnClickPendingIntent(R.id.hotspot_weather_no_cal, weatherPendingIntent)
            }

            // 4. Root click -> open app settings
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

        fun buildPreviewRemoteViews(
            context: Context,
            config: WidgetConfig,
            canvasWidth: Int = 1024,
            canvasHeight: Int = 461
        ): RemoteViews {
            val calendarRepo = CalendarRepository(context)
            val useLiveCal = config.showCalendar && (config.useLiveCalendar || calendarRepo.hasCalendarPermission() || config.includeGoogleTasks)
            val isAllDayFallback = config.fallbackMinutes == -2
            val event = if (!config.showCalendar) {
                UpcomingCalendarEvent("", -1L, hasEvent = false, isAllDay = false)
            } else if (useLiveCal) {
                calendarRepo.getCachedOrDefault(config.fallbackEventTitle, config.fallbackMinutes, config.includeGoogleTasks)
            } else {
                UpcomingCalendarEvent(
                    config.fallbackEventTitle,
                    if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                    hasEvent = config.fallbackMinutes != -1,
                    isAllDay = isAllDayFallback
                )
            }

            return buildRemoteViews(
                context = context,
                config = config,
                eventTitle = event.title,
                minutesLeft = event.minutesUntilStart,
                temp = config.tempCelsius,
                condition = config.weatherCondition,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                isAllDay = event.isAllDay,
                isTask = event.isTask,
                hasEvent = event.hasEvent
            )
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
            val calendarRepo = CalendarRepository(context)
            val useLiveCal = config.showCalendar && (config.useLiveCalendar || calendarRepo.hasCalendarPermission() || config.includeGoogleTasks)
            val initialCalendar = if (!config.showCalendar) {
                UpcomingCalendarEvent("", -1L, hasEvent = false, isAllDay = false)
            } else if (useLiveCal) {
                calendarRepo.getCachedOrDefault(config.fallbackEventTitle, config.fallbackMinutes, config.includeGoogleTasks)
            } else {
                UpcomingCalendarEvent(
                    config.fallbackEventTitle,
                    if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                    hasEvent = config.fallbackMinutes != -1,
                    isAllDay = isAllDayFallback
                )
            }

            // Fast synchronous render with cached data
            try {
                val initialViews = buildRemoteViews(
                    context = context,
                    config = config,
                    eventTitle = initialCalendar.title,
                    minutesLeft = initialCalendar.minutesUntilStart,
                    temp = cachedWeather.temperature,
                    condition = cachedWeather.condition,
                    canvasWidth = w,
                    canvasHeight = h,
                    isAllDay = initialCalendar.isAllDay,
                    isTask = initialCalendar.isTask,
                    hasEvent = initialCalendar.hasEvent
                )
                appWidgetManager.updateAppWidget(appWidgetId, initialViews)
            } catch (e: Exception) {
                android.util.Log.e("MicroNothingWidget", "Single fast render failed", e)
            }

            // Background live update if live calendar, tasks, or weather enabled
            if (useLiveCal || config.useLiveWeather || config.includeGoogleTasks) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Immediately query fresh device calendar events
                        val nextEvent = if (useLiveCal) {
                            try {
                                calendarRepo.getNextUpcomingEvent(config.fallbackEventTitle, config.fallbackMinutes, config.includeGoogleTasks)
                            } catch (e: Exception) {
                                initialCalendar
                            }
                        } else {
                            initialCalendar
                        }

                        // Immediate UI update if event differed from cache
                        if (nextEvent != initialCalendar) {
                            val intermediateViews = buildRemoteViews(
                                context = context,
                                config = config,
                                eventTitle = nextEvent.title,
                                minutesLeft = nextEvent.minutesUntilStart,
                                temp = cachedWeather.temperature,
                                condition = cachedWeather.condition,
                                canvasWidth = w,
                                canvasHeight = h,
                                isAllDay = nextEvent.isAllDay,
                                isTask = nextEvent.isTask,
                                hasEvent = nextEvent.hasEvent
                            )
                            appWidgetManager.updateAppWidget(appWidgetId, intermediateViews)
                        }

                        val tasksRepo = TasksRepository(context)
                        if (config.includeGoogleTasks && tasksRepo.isConnected()) {
                            try { tasksRepo.syncTasks() } catch (_: Exception) {}
                        }

                        val finalEvent = if (config.includeGoogleTasks && useLiveCal) {
                            try {
                                calendarRepo.getNextUpcomingEvent(config.fallbackEventTitle, config.fallbackMinutes, config.includeGoogleTasks)
                            } catch (_: Exception) {
                                nextEvent
                            }
                        } else {
                            nextEvent
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
                            eventTitle = finalEvent.title,
                            minutesLeft = finalEvent.minutesUntilStart,
                            temp = weather.temperature,
                            condition = weather.condition,
                            canvasWidth = w,
                            canvasHeight = h,
                            isAllDay = finalEvent.isAllDay,
                            isTask = finalEvent.isTask,
                            hasEvent = finalEvent.hasEvent
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
            appWidgetIds: IntArray,
            pendingResult: BroadcastReceiver.PendingResult? = null
        ) {
            if (appWidgetIds.isEmpty()) {
                pendingResult?.finish()
                return
            }

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
            val calendarRepo = CalendarRepository(context)
            val useLiveCal = config.showCalendar && (config.useLiveCalendar || calendarRepo.hasCalendarPermission() || config.includeGoogleTasks)
            val initialCalendar = if (!config.showCalendar) {
                UpcomingCalendarEvent("", -1L, hasEvent = false, isAllDay = false)
            } else if (useLiveCal) {
                calendarRepo.getCachedOrDefault(config.fallbackEventTitle, config.fallbackMinutes, config.includeGoogleTasks)
            } else {
                UpcomingCalendarEvent(
                    config.fallbackEventTitle,
                    if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                    hasEvent = config.fallbackMinutes != -1,
                    isAllDay = isAllDayFallback
                )
            }

            // 1. Fast immediate synchronous render per widget dimension
            for (id in appWidgetIds) {
                val opt = appWidgetManager.getAppWidgetOptions(id)
                val (w, h) = getWidgetDimensions(context, opt)
                try {
                    val initialViews = buildRemoteViews(
                        context = context,
                        config = config,
                        eventTitle = initialCalendar.title,
                        minutesLeft = initialCalendar.minutesUntilStart,
                        temp = cachedWeather.temperature,
                        condition = cachedWeather.condition,
                        canvasWidth = w,
                        canvasHeight = h,
                        isAllDay = initialCalendar.isAllDay,
                        isTask = initialCalendar.isTask,
                        hasEvent = initialCalendar.hasEvent
                    )
                    appWidgetManager.updateAppWidget(id, initialViews)
                } catch (e: Exception) {
                    android.util.Log.e("MicroNothingWidget", "Fast render failed for widget $id", e)
                }
            }

            // 2. Background update for live calendar and/or live weather
            if (useLiveCal || config.useLiveWeather || config.includeGoogleTasks) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Immediately query fresh device calendar events
                        val nextEvent = if (useLiveCal) {
                            try {
                                calendarRepo.getNextUpcomingEvent(config.fallbackEventTitle, config.fallbackMinutes, config.includeGoogleTasks)
                            } catch (e: Exception) {
                                initialCalendar
                            }
                        } else {
                            initialCalendar
                        }

                        // Immediate UI update if event differed from cache
                        if (nextEvent != initialCalendar) {
                            for (appWidgetId in appWidgetIds) {
                                val opt = appWidgetManager.getAppWidgetOptions(appWidgetId)
                                val (w, h) = getWidgetDimensions(context, opt)
                                val intermediateViews = buildRemoteViews(
                                    context = context,
                                    config = config,
                                    eventTitle = nextEvent.title,
                                    minutesLeft = nextEvent.minutesUntilStart,
                                    temp = cachedWeather.temperature,
                                    condition = cachedWeather.condition,
                                    canvasWidth = w,
                                    canvasHeight = h,
                                    isAllDay = nextEvent.isAllDay,
                                    isTask = nextEvent.isTask,
                                    hasEvent = nextEvent.hasEvent
                                )
                                appWidgetManager.updateAppWidget(appWidgetId, intermediateViews)
                            }
                        }

                        val tasksRepo = TasksRepository(context)
                        if (config.includeGoogleTasks && tasksRepo.isConnected()) {
                            try { tasksRepo.syncTasks() } catch (_: Exception) {}
                        }

                        val finalEvent = if (config.includeGoogleTasks && useLiveCal) {
                            try {
                                calendarRepo.getNextUpcomingEvent(config.fallbackEventTitle, config.fallbackMinutes, config.includeGoogleTasks)
                            } catch (_: Exception) {
                                nextEvent
                            }
                        } else {
                            nextEvent
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
                                eventTitle = finalEvent.title,
                                minutesLeft = finalEvent.minutesUntilStart,
                                temp = weather.temperature,
                                condition = weather.condition,
                                canvasWidth = w,
                                canvasHeight = h,
                                isAllDay = finalEvent.isAllDay,
                                isTask = finalEvent.isTask,
                                hasEvent = finalEvent.hasEvent
                            )
                            appWidgetManager.updateAppWidget(appWidgetId, liveViews)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MicroNothingWidget", "Live background update failed", e)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            } else {
                pendingResult?.finish()
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

            // Trigger at next exact minute boundary strictly in the future
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.MINUTE, 1)
            }

            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            try {
                if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else if (canExact) {
                    alarmManager.setExact(
                        AlarmManager.RTC,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback when exact alarms not permitted
                    alarmManager.set(AlarmManager.RTC, calendar.timeInMillis, pendingIntent)
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
