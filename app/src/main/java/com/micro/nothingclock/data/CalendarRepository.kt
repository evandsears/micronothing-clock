package com.micro.nothingclock.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone

data class UpcomingCalendarEvent(
    val title: String,
    val minutesUntilStart: Long,
    val hasEvent: Boolean = true,
    val isAllDay: Boolean = false,
    val eventBeginMillis: Long = 0L,
    val isTask: Boolean = false,
    val eventEndMillis: Long = 0L
)

class CalendarRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("nothing_calendar_cache", Context.MODE_PRIVATE)

    private val quippyPlaceholders = listOf(
        "YOUR DAY IS OPEN",
        "NOTHING SCHEDULED",
        "ALL CLEAR TODAY",
        "ENJOY THE QUIET",
        "ZERO MEETINGS",
        "SCHEDULE CLEAR"
    )

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getRandomQuippyPlaceholder(): String {
        return quippyPlaceholders.random()
    }

    fun getHourlyQuippyPlaceholder(timestampMillis: Long = System.currentTimeMillis()): String {
        val index = (timestampMillis / (1000 * 60 * 60) % quippyPlaceholders.size).toInt()
        val safeIndex = if (index < 0) (index + quippyPlaceholders.size) % quippyPlaceholders.size else index
        return quippyPlaceholders[safeIndex]
    }

    private fun getTodayDateKey(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "$y-$m-$d"
    }

    fun getCachedOrDefault(
        fallbackTitle: String,
        fallbackMinutes: Int,
        includeTasks: Boolean = true
    ): UpcomingCalendarEvent {
        val now = System.currentTimeMillis()
        val todayKey = getTodayDateKey(now)
        val cachedDate = prefs.getString("cached_date", null)

        if (cachedDate == todayKey) {
            val title = prefs.getString("cached_title", null)
            val hasEvent = prefs.getBoolean("cached_has_event", false)
            val isAllDay = prefs.getBoolean("cached_is_all_day", false)
            val isTask = prefs.getBoolean("cached_is_task", false)
            val beginMillis = prefs.getLong("cached_begin_millis", 0L)
            val endMillis = prefs.getLong("cached_end_millis", 0L)
            val effectiveEnd = if (endMillis > beginMillis) endMillis else (beginMillis + 60 * 60 * 1000L)

            if (!title.isNullOrEmpty()) {
                if (!hasEvent) {
                    return UpcomingCalendarEvent(title, -1L, hasEvent = false, isAllDay = false)
                }
                if (isAllDay || isTask) {
                    return UpcomingCalendarEvent(title, -1L, hasEvent = true, isAllDay = isAllDay, isTask = isTask)
                }
                // Timed event:
                if (now in beginMillis until effectiveEnd) {
                    // Still currently happening (active between begin and end)
                    return UpcomingCalendarEvent(
                        title = title,
                        minutesUntilStart = 0L,
                        hasEvent = true,
                        isAllDay = false,
                        eventBeginMillis = beginMillis,
                        eventEndMillis = effectiveEnd
                    )
                } else if (beginMillis > now) {
                    val diff = beginMillis - now
                    val mins = (diff + 59_999L) / (1000 * 60)
                    // If cached event is more than 2 hours (120 min) away and tasks are available, prioritize task
                    if (mins > 120L && includeTasks) {
                        val tasksRepo = TasksRepository(context)
                        val task = if (tasksRepo.isConnected()) tasksRepo.getTopPriorityTask() else null
                        if (task != null) {
                            return UpcomingCalendarEvent(
                                title = task.title.uppercase(),
                                minutesUntilStart = -1L,
                                hasEvent = true,
                                isAllDay = true,
                                isTask = true
                            )
                        }
                    }
                    return UpcomingCalendarEvent(
                        title = title,
                        minutesUntilStart = mins,
                        hasEvent = true,
                        isAllDay = false,
                        eventBeginMillis = beginMillis,
                        eventEndMillis = effectiveEnd
                    )
                }
                // Event has ended
            }
        }

        // Check if a top priority task is available before falling back
        if (includeTasks) {
            val tasksRepo = TasksRepository(context)
            val task = if (tasksRepo.isConnected()) tasksRepo.getTopPriorityTask() else null
            if (task != null) {
                return UpcomingCalendarEvent(
                    title = task.title.uppercase(),
                    minutesUntilStart = -1L,
                    hasEvent = true,
                    isAllDay = true,
                    isTask = true
                )
            }
        }

        // If permission is granted and cache expired, immediately attempt live fetch
        if (hasCalendarPermission()) {
            try {
                return getNextUpcomingEvent(fallbackTitle, fallbackMinutes, includeTasks)
            } catch (_: Exception) {}
        }

        // No cache for today, day reset, or event expired:
        return if (!hasCalendarPermission()) {
            val isAllDayFallback = fallbackMinutes == -2
            UpcomingCalendarEvent(
                fallbackTitle,
                if (isAllDayFallback) -1L else fallbackMinutes.toLong(),
                hasEvent = fallbackMinutes != -1,
                isAllDay = isAllDayFallback
            )
        } else {
            // Permission granted: default to generic phrases
            UpcomingCalendarEvent(getHourlyQuippyPlaceholder(now), -1L, hasEvent = false, isAllDay = false)
        }
    }

    private fun saveCachedEvent(event: UpcomingCalendarEvent, timestamp: Long) {
        val todayKey = getTodayDateKey(timestamp)
        prefs.edit()
            .putString("cached_date", todayKey)
            .putString("cached_title", event.title)
            .putLong("cached_minutes", event.minutesUntilStart)
            .putBoolean("cached_has_event", event.hasEvent)
            .putBoolean("cached_is_all_day", event.isAllDay)
            .putBoolean("cached_is_task", event.isTask)
            .putLong("cached_begin_millis", event.eventBeginMillis)
            .putLong("cached_end_millis", event.eventEndMillis)
            .putLong("cached_timestamp", timestamp)
            .apply()
    }

    fun getNextUpcomingEvent(
        fallbackTitle: String,
        fallbackMinutes: Int,
        includeTasks: Boolean = true
    ): UpcomingCalendarEvent {
        val isAllDayFallback = fallbackMinutes == -2
        if (!hasCalendarPermission()) {
            if (includeTasks) {
                val tasksRepo = TasksRepository(context)
                val task = if (tasksRepo.isConnected()) tasksRepo.getTopPriorityTask() else null
                if (task != null) {
                    val result = UpcomingCalendarEvent(
                        title = task.title.uppercase(),
                        minutesUntilStart = -1L,
                        hasEvent = true,
                        isAllDay = true,
                        isTask = true
                    )
                    saveCachedEvent(result, System.currentTimeMillis())
                    return result
                }
            }
            return UpcomingCalendarEvent(
                fallbackTitle,
                if (isAllDayFallback) -1L else fallbackMinutes.toLong(),
                hasEvent = fallbackMinutes != -1,
                isAllDay = isAllDayFallback
            )
        }

        return try {
            val now = System.currentTimeMillis()

            // Local start of today (00:00:00.000)
            val localCal = Calendar.getInstance()
            localCal.set(Calendar.HOUR_OF_DAY, 0)
            localCal.set(Calendar.MINUTE, 0)
            localCal.set(Calendar.SECOND, 0)
            localCal.set(Calendar.MILLISECOND, 0)
            val startOfTodayLocal = localCal.timeInMillis

            // Local end of today (23:59:59.999)
            localCal.set(Calendar.HOUR_OF_DAY, 23)
            localCal.set(Calendar.MINUTE, 59)
            localCal.set(Calendar.SECOND, 59)
            localCal.set(Calendar.MILLISECOND, 999)
            val endOfTodayLocal = localCal.timeInMillis

            // UTC midnight of today (Android Calendar stores all-day events in UTC)
            val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
            }
            val todayUtcMidnight = todayUtc.timeInMillis

            // Query window covers today (from start of today or today UTC midnight to end of today)
            val windowStart = minOf(startOfTodayLocal, todayUtcMidnight) - 12 * 60 * 60 * 1000L
            val windowEnd = maxOf(endOfTodayLocal, todayUtcMidnight + 24 * 60 * 60 * 1000L) + 1000L

            val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, windowStart)
            ContentUris.appendId(builder, windowEnd)

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY
            )

            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            val oneHourMillis = 60 * 60 * 1000L
            var ongoingEvent: UpcomingCalendarEvent? = null
            var upcomingEvent: UpcomingCalendarEvent? = null
            val allDayEvents = mutableListOf<String>()

            cursor?.use {
                while (it.moveToNext()) {
                    val rawTitle = it.getString(it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE))?.trim()
                    val title = if (rawTitle.isNullOrEmpty()) "EVENT" else rawTitle
                    val begin = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN))
                    val end = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Instances.END))
                    val isAllDay = it.getInt(it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)) == 1

                    if (isAllDay) {
                        val effectiveEnd = if (end <= begin) begin + 24 * 60 * 60 * 1000L else end
                        val coversUtcToday = begin <= todayUtcMidnight && effectiveEnd > todayUtcMidnight
                        val coversLocalToday = begin <= startOfTodayLocal && effectiveEnd > startOfTodayLocal
                        if ((coversUtcToday || coversLocalToday) && title.isNotBlank()) {
                            allDayEvents.add(title.uppercase())
                        }
                        continue
                    }

                    // Timed event:
                    val effectiveEnd = if (end > begin) end else (begin + 60 * 60 * 1000L)

                    // 1. Is event happening right now?
                    // Active between start and actual end:
                    if (now in begin until effectiveEnd) {
                        // Pick the event that started most recently if multiple overlap
                        if (ongoingEvent == null || begin > ongoingEvent!!.eventBeginMillis) {
                            ongoingEvent = UpcomingCalendarEvent(
                                title = title.uppercase(),
                                minutesUntilStart = 0L,
                                hasEvent = true,
                                isAllDay = false,
                                eventBeginMillis = begin,
                                eventEndMillis = effectiveEnd
                            )
                        }
                    } else if (begin > now && begin <= endOfTodayLocal) {
                        // 2. Upcoming event later today (does not look into tomorrow)
                        val diffMillis = begin - now
                        val minutes = (diffMillis + 59_999L) / (1000 * 60)
                        if (upcomingEvent == null || begin < upcomingEvent!!.eventBeginMillis) {
                            upcomingEvent = UpcomingCalendarEvent(
                                title = title.uppercase(),
                                minutesUntilStart = minutes,
                                hasEvent = true,
                                isAllDay = false,
                                eventBeginMillis = begin,
                                eventEndMillis = effectiveEnd
                            )
                        }
                    }
                }
            }

            // Check for Google Tasks if enabled
            val topTask = if (includeTasks) {
                val tasksRepo = TasksRepository(context)
                if (tasksRepo.isConnected()) tasksRepo.getTopPriorityTask() else null
            } else null

            // 2-hour proximity threshold: Upcoming meetings within 120 minutes take urgent priority over tasks.
            // If the meeting is more than 2 hours away, open focus tasks take the spotlight.
            val isUpcomingImminent = upcomingEvent != null && upcomingEvent.minutesUntilStart <= 120L

            // Priority order:
            // 1. Event currently happening (stays current for 1 hour with "NOW")
            // 2. Imminent upcoming event starting within 2 hours (<= 120 mins)
            // 3. Google Task (actionable to-do during open work blocks)
            // 4. Farther upcoming event later today (> 120 mins)
            // 5. All-day event today
            // 6. Reset to generic phrases ("YOUR DAY IS OPEN", etc.)
            val result = when {
                ongoingEvent != null -> ongoingEvent
                isUpcomingImminent -> upcomingEvent!!
                topTask != null -> UpcomingCalendarEvent(
                    title = topTask.title.uppercase(),
                    minutesUntilStart = -1L,
                    hasEvent = true,
                    isAllDay = true,
                    isTask = true
                )
                upcomingEvent != null -> upcomingEvent
                allDayEvents.isNotEmpty() -> UpcomingCalendarEvent(
                    title = allDayEvents.first(),
                    minutesUntilStart = -1L,
                    hasEvent = true,
                    isAllDay = true
                )
                else -> {
                    val quippy = getHourlyQuippyPlaceholder(now)
                    UpcomingCalendarEvent(
                        title = quippy,
                        minutesUntilStart = -1L,
                        hasEvent = false,
                        isAllDay = false
                    )
                }
            }

            saveCachedEvent(result, now)
            result
        } catch (e: Exception) {
            getCachedOrDefault(fallbackTitle, fallbackMinutes)
        }
    }
}
