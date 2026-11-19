package com.micro.nothingclock

import com.micro.nothingclock.data.GoogleTaskItem
import com.micro.nothingclock.data.TasksRepository
import com.micro.nothingclock.data.UpcomingCalendarEvent
import com.micro.nothingclock.data.WidgetConfig
import com.micro.nothingclock.widget.NothingClockRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsAndConfigTest {

    @Test
    fun testUpcomingCalendarEvent_defaults() {
        val event = UpcomingCalendarEvent(
            title = "TEAM STANDUP",
            minutesUntilStart = 0L
        )

        assertEquals("TEAM STANDUP", event.title)
        assertEquals(0L, event.minutesUntilStart)
        assertTrue(event.hasEvent)
        assertFalse(event.isAllDay)
        assertFalse(event.isTask)
        assertEquals(0L, event.eventBeginMillis)
    }

    @Test
    fun testUpcomingCalendarEvent_taskFlag() {
        val taskEvent = UpcomingCalendarEvent(
            title = "SUBMIT REPORT",
            minutesUntilStart = -1L,
            hasEvent = true,
            isAllDay = true,
            isTask = true
        )

        assertTrue(taskEvent.isTask)
        assertTrue(taskEvent.isAllDay)
        assertEquals("SUBMIT REPORT", taskEvent.title)
    }

    @Test
    fun testGoogleTaskItem_creation() {
        val item = GoogleTaskItem(
            id = "task-123",
            title = "Review PR",
            dueMillis = 1700000000000L,
            isCompleted = false,
            notes = "Check edge cases",
            listTitle = "Work"
        )

        assertEquals("task-123", item.id)
        assertEquals("Review PR", item.title)
        assertEquals(1700000000000L, item.dueMillis)
        assertFalse(item.isCompleted)
        assertEquals("Check edge cases", item.notes)
        assertEquals("Work", item.listTitle)
    }

    @Test
    fun testTasksRepositoryConstants() {
        assertEquals("https://www.googleapis.com/auth/tasks.readonly", TasksRepository.TASKS_SCOPE)
    }

    @Test
    fun testWidgetConfig_defaultTasksEnabled() {
        val config = WidgetConfig()
        assertTrue("Google Tasks integration should be enabled by default", config.includeGoogleTasks)
        assertEquals("WEEKLY DESIGN SYNC", config.fallbackEventTitle)
        assertEquals(6, config.fallbackMinutes)
        assertTrue("showCalendar should be true by default", config.showCalendar)
        assertFalse("hideCalendarWhenNoEvents should be false by default to always show the two lines", config.hideCalendarWhenNoEvents)
    }

    @Test
    fun testShouldShowCalendar_defaultBehaviorAlwaysShowsLines() {
        val defaultConfig = WidgetConfig(showCalendar = true, hideCalendarWhenNoEvents = false)
        // With default setting, 2 calendar lines are shown even if hasEvent is false (open day placeholder)
        assertTrue(NothingClockRenderer.shouldShowCalendar(defaultConfig, hasEvent = true))
        assertTrue(NothingClockRenderer.shouldShowCalendar(defaultConfig, hasEvent = false))
    }

    @Test
    fun testShouldShowCalendar_hideWhenNoEventsToggle() {
        val cleanConfig = WidgetConfig(showCalendar = true, hideCalendarWhenNoEvents = true)
        // When toggle enabled: shows 2 lines if hasEvent is true, hides lines if hasEvent is false
        assertTrue("Should show lines when there is an active event or task", NothingClockRenderer.shouldShowCalendar(cleanConfig, hasEvent = true))
        assertFalse("Should hide lines when there are no events or tasks", NothingClockRenderer.shouldShowCalendar(cleanConfig, hasEvent = false))
    }

    @Test
    fun testShouldShowCalendar_masterSwitchDisabledOverridesToggle() {
        val disabledConfig = WidgetConfig(showCalendar = false, hideCalendarWhenNoEvents = false)
        assertFalse(NothingClockRenderer.shouldShowCalendar(disabledConfig, hasEvent = true))
        assertFalse(NothingClockRenderer.shouldShowCalendar(disabledConfig, hasEvent = false))

        val disabledWithToggle = WidgetConfig(showCalendar = false, hideCalendarWhenNoEvents = true)
        assertFalse(NothingClockRenderer.shouldShowCalendar(disabledWithToggle, hasEvent = true))
        assertFalse(NothingClockRenderer.shouldShowCalendar(disabledWithToggle, hasEvent = false))
    }
}
