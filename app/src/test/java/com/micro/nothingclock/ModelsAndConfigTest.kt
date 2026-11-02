package com.micro.nothingclock

import com.micro.nothingclock.data.GoogleTaskItem
import com.micro.nothingclock.data.TasksRepository
import com.micro.nothingclock.data.UpcomingCalendarEvent
import com.micro.nothingclock.data.WidgetConfig
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
    }
}
