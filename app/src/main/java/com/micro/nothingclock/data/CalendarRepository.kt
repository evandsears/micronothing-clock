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
    val isAllDay: Boolean = false
)

class CalendarRepository(private val context: Context) {

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

    fun getNextUpcomingEvent(fallbackTitle: String, fallbackMinutes: Int): UpcomingCalendarEvent {
        val isAllDayFallback = fallbackMinutes == -2
        if (!hasCalendarPermission()) {
            return UpcomingCalendarEvent(
                fallbackTitle,
                if (isAllDayFallback) -1L else fallbackMinutes.toLong(),
                hasEvent = fallbackMinutes != -1,
                isAllDay = isAllDayFallback
            )
        }

        return try {
            val now = System.currentTimeMillis()

            // Local start of today
            val localCal = Calendar.getInstance()
            localCal.set(Calendar.HOUR_OF_DAY, 0)
            localCal.set(Calendar.MINUTE, 0)
            localCal.set(Calendar.SECOND, 0)
            localCal.set(Calendar.MILLISECOND, 0)
            val startOfTodayLocal = localCal.timeInMillis

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

            // Expand query window: look back enough to include any all-day events covering today,
            // and look forward at least 24 hours.
            val windowStart = minOf(startOfTodayLocal, todayUtcMidnight) - 12 * 60 * 60 * 1000L
            val windowEnd = maxOf(now + 24 * 60 * 60 * 1000L, todayUtcMidnight + 48 * 60 * 60 * 1000L)

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

            var upcomingHourEvent: UpcomingCalendarEvent? = null
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
                    // Check if event is ongoing right now or starting in the next 60 minutes
                    if (upcomingHourEvent == null) {
                        if (now in begin until end) {
                            // Ongoing meeting happening right now
                            upcomingHourEvent = UpcomingCalendarEvent(title.uppercase(), 0L, hasEvent = true, isAllDay = false)
                        } else {
                            val diffMillis = begin - now
                            if (diffMillis >= 0) {
                                val minutes = diffMillis / (1000 * 60)
                                if (minutes in 0..60) {
                                    upcomingHourEvent = UpcomingCalendarEvent(title.uppercase(), minutes, hasEvent = true, isAllDay = false)
                                }
                            }
                        }
                    }
                }
            }

            // 1. If there's an event in the next hour, display it
            if (upcomingHourEvent != null) {
                return upcomingHourEvent
            }

            // 2. If there's no event in the next hour:
            // Display any all-day event if present
            if (allDayEvents.isNotEmpty()) {
                return UpcomingCalendarEvent(allDayEvents.first(), -1L, hasEvent = true, isAllDay = true)
            }

            // 3. Otherwise display the default quippy placeholder
            val quippy = quippyPlaceholders[(now / (1000 * 60 * 60) % quippyPlaceholders.size).toInt()]
            UpcomingCalendarEvent(quippy, -1L, hasEvent = false, isAllDay = false)
        } catch (e: Exception) {
            UpcomingCalendarEvent(fallbackTitle, if (isAllDayFallback) -1L else fallbackMinutes.toLong(), hasEvent = fallbackMinutes != -1, isAllDay = isAllDayFallback)
        }
    }
}
