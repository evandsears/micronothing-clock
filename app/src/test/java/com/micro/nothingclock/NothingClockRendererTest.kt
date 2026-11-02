package com.micro.nothingclock

import com.micro.nothingclock.widget.NothingClockRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone

class NothingClockRendererTest {

    @Test
    fun testHandMarkerDrawables_ongoingEventNow_hasNoFingers() {
        // When an event is currently happening (minutes == 0L), fingers countdown should be empty
        val markers = NothingClockRenderer.getHandMarkerDrawables(minutes = 0L, isAllDay = false, isTask = false)
        assertTrue("Minutes == 0L (NOW) should return empty finger markers", markers.isEmpty())
    }

    @Test
    fun testHandMarkerDrawables_isTask() {
        val markers = NothingClockRenderer.getHandMarkerDrawables(minutes = -1L, isAllDay = false, isTask = true)
        assertEquals(1, markers.size)
        assertEquals(R.drawable.ic_nothing_marker_1, markers[0])
    }

    @Test
    fun testHandMarkerDrawables_isAllDay() {
        val markers = NothingClockRenderer.getHandMarkerDrawables(minutes = -1L, isAllDay = true, isTask = false)
        assertEquals(1, markers.size)
        assertEquals(R.drawable.ic_nothing_marker_2, markers[0])
    }

    @Test
    fun testHandMarkerDrawables_openDay() {
        val markers = NothingClockRenderer.getHandMarkerDrawables(minutes = -1L, isAllDay = false, isTask = false)
        assertEquals(1, markers.size)
        assertEquals(R.drawable.ic_nothing_marker_2, markers[0])
    }

    @Test
    fun testHandMarkerDrawables_minutesCountdown() {
        // 5 minutes -> marker 5
        val markers5 = NothingClockRenderer.getHandMarkerDrawables(minutes = 5L)
        assertEquals(listOf(R.drawable.ic_nothing_marker_5), markers5)

        // 6 minutes -> marker 5 + marker 1
        val markers6 = NothingClockRenderer.getHandMarkerDrawables(minutes = 6L)
        assertEquals(listOf(R.drawable.ic_nothing_marker_5, R.drawable.ic_nothing_marker_1), markers6)

        // 60 minutes -> 1 hour (marker 1)
        val markers60 = NothingClockRenderer.getHandMarkerDrawables(minutes = 60L)
        assertEquals(listOf(R.drawable.ic_nothing_marker_1), markers60)

        // 120 minutes -> 2 hours (marker 2)
        val markers120 = NothingClockRenderer.getHandMarkerDrawables(minutes = 120L)
        assertEquals(listOf(R.drawable.ic_nothing_marker_2), markers120)
    }

    @Test
    fun testCalculateHandGestures_nowStatus() {
        val gestureNow = NothingClockRenderer.calculateHandGestures(minutes = 0L)
        assertEquals("HAPPENING (NOW)", gestureNow)

        val gestureAllDay = NothingClockRenderer.calculateHandGestures(minutes = -1L, isAllDay = true)
        assertEquals("✌️ (All Day)", gestureAllDay)

        val gestureOpen = NothingClockRenderer.calculateHandGestures(minutes = -1L, isAllDay = false)
        assertEquals("✌️ (Open Day)", gestureOpen)
    }

    @Test
    fun testFormatTimezoneAbbreviation_standard() {
        val tzUtc = TimeZone.getTimeZone("UTC")
        val abbrUtc = NothingClockRenderer.formatTimezoneAbbreviation(tzUtc, Date(1700000000000L))
        assertEquals("UTC", abbrUtc)

        val tzGmt = TimeZone.getTimeZone("GMT")
        val abbrGmt = NothingClockRenderer.formatTimezoneAbbreviation(tzGmt, Date(1700000000000L))
        assertEquals("GMT", abbrGmt)
    }

    @Test
    fun testFormatTimezoneAbbreviation_numericOffsets() {
        // UTC+1
        val tzPlus1 = SimpleTimeZone(3600 * 1000, "GMT+01:00")
        val formattedPlus1 = NothingClockRenderer.formatTimezoneAbbreviation(tzPlus1, Date())
        assertEquals("+01", formattedPlus1)

        // UTC-5
        val tzMinus5 = SimpleTimeZone(-5 * 3600 * 1000, "GMT-05:00")
        val formattedMinus5 = NothingClockRenderer.formatTimezoneAbbreviation(tzMinus5, Date())
        assertEquals("-05", formattedMinus5)
    }

    @Test
    fun testResolveFloatingColors_nothingOsSlateBlue_isSlightlyBlueAndHighContrast() {
        // Nothing OS 5.0 twilight/slate-blue wallpaper from screenshot: RGB(53, 68, 77)
        val slateBg = (0xFF shl 24) or (53 shl 16) or (68 shl 8) or 77
        val colors = NothingClockRenderer.resolveFloatingColors(context = null, previewBgColor = slateBg)

        val r = (colors.textColor ushr 16) and 0xFF
        val g = (colors.textColor ushr 8) and 0xFF
        val b = colors.textColor and 0xFF

        // 1. Text must have a subtle blue tint (b > r) exactly as shown in Nothing OS 5.0
        assertTrue("Text on slate-blue background should be slightly blue (b=$b > r=$r)", b > r)

        // 2. Saturation must be subtle (pastel tint <= 18%), not oversaturated
        val hsv = FloatArray(3)
        NothingClockRenderer.rgbToHsv(r, g, b, hsv)
        assertTrue("Text saturation should be subtle (<= 0.18), was ${hsv[1]}", hsv[1] <= 0.18f)
        assertTrue("Text value should be bright (>= 0.90) for dark wallpaper, was ${hsv[2]}", hsv[2] >= 0.90f)

        // 3. Contrast ratio must satisfy WCAG AA (>= 4.5:1)
        val contrast = NothingClockRenderer.calculateContrastRatio(colors.textColor, slateBg)
        assertTrue("Contrast ratio on slate blue background should be >= 4.5:1, was $contrast", contrast >= 4.5)

        // 4. Pill background must match text color, and pill text must be dark contrasting obsidian
        assertEquals(colors.textColor, colors.pillBgColor)
        val pillTextContrast = NothingClockRenderer.calculateContrastRatio(colors.pillTextColor, colors.pillBgColor)
        assertTrue("Pill text must have high contrast against pill background (>= 7.0:1), was $pillTextContrast", pillTextContrast >= 7.0)
    }

    @Test
    fun testResolveFloatingColors_neonGreenBackground_strictlyGuardsLegibility() {
        // Neon green: RGB(57, 255, 20) -> should NEVER produce neon green text!
        val neonGreenBg = (0xFF shl 24) or (57 shl 16) or (255 shl 8) or 20
        val colors = NothingClockRenderer.resolveFloatingColors(context = null, previewBgColor = neonGreenBg)

        val r = (colors.textColor ushr 16) and 0xFF
        val g = (colors.textColor ushr 8) and 0xFF
        val b = colors.textColor and 0xFF

        // 1. Text must NOT be neon green: it must adapt to dark obsidian on light neon background
        val lum = NothingClockRenderer.calculatePerceivedLuminance(colors.textColor)
        assertTrue("On neon green background, text must be dark (luminance < 0.25), was $lum", lum < 0.25)

        // 2. Must achieve high contrast against the neon background
        val contrast = NothingClockRenderer.calculateContrastRatio(colors.textColor, neonGreenBg)
        assertTrue("Contrast against neon green must be >= 7.0:1 (WCAG AAA), was $contrast", contrast >= 7.0)
    }

    @Test
    fun testResolveFloatingColors_amoledBlackBackground_highContrastLightTone() {
        val blackBg = (0xFF shl 24) or (0 shl 16) or (0 shl 8) or 0
        val colors = NothingClockRenderer.resolveFloatingColors(context = null, previewBgColor = blackBg)

        val contrast = NothingClockRenderer.calculateContrastRatio(colors.textColor, blackBg)
        assertTrue("Contrast against AMOLED black should be >= 15:1, was $contrast", contrast >= 15.0)
        assertEquals(colors.textColor, colors.pillBgColor)
    }

    @Test
    fun testResolveFloatingColors_pureWhiteBackground_highContrastDarkTone() {
        val whiteBg = (0xFF shl 24) or (255 shl 16) or (255 shl 8) or 255
        val colors = NothingClockRenderer.resolveFloatingColors(context = null, previewBgColor = whiteBg)

        val contrast = NothingClockRenderer.calculateContrastRatio(colors.textColor, whiteBg)
        assertTrue("Contrast against pure white should be >= 15:1, was $contrast", contrast >= 15.0)
    }

    @Test
    fun testCalculateContrastRatio_whiteAndBlack() {
        val white = (0xFF shl 24) or (255 shl 16) or (255 shl 8) or 255
        val black = (0xFF shl 24) or (0 shl 16) or (0 shl 8) or 0
        val ratio = NothingClockRenderer.calculateContrastRatio(white, black)
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun testResolveFloatingColors_purpleWallpaper_pillTextIsRichDarkPurpleNotPureBlack() {
        // User screenshot purple wallpaper: RGB(146, 122, 163) (#927AA3)
        val purpleBg = (0xFF shl 24) or (146 shl 16) or (122 shl 8) or 163
        val colors = NothingClockRenderer.resolveFloatingColors(context = null, previewBgColor = purpleBg)

        val pr = (colors.pillTextColor ushr 16) and 0xFF
        val pg = (colors.pillTextColor ushr 8) and 0xFF
        val pb = colors.pillTextColor and 0xFF

        // 1. Must NOT be pure black / gray: Blue and Red must dominate Green (purple hue)
        assertTrue("Pill text must be purple hue: pb ($pb) > pg ($pg)", pb > pg)
        assertTrue("Pill text must be purple hue: pr ($pr) > pg ($pg)", pr > pg)

        val hsv = FloatArray(3)
        NothingClockRenderer.rgbToHsv(pr, pg, pb, hsv)

        // 2. Saturation must be rich (>= 0.35) so it feels clearly purple, not washed out to black
        assertTrue("Pill text should be richly saturated (>= 0.35), was ${hsv[1]}", hsv[1] >= 0.35f)

        // 3. Value should be deep dark tone (0.15 .. 0.30)
        assertTrue("Pill text value should be deep dark (between 0.15 and 0.30), was ${hsv[2]}", hsv[2] in 0.15f..0.30f)

        // 4. Must achieve WCAG AAA contrast ratio (>= 7.0:1) against the light lavender pill badge
        val contrast = NothingClockRenderer.calculateContrastRatio(colors.pillTextColor, colors.pillBgColor)
        assertTrue("Pill text contrast against badge should be >= 7.0:1, was $contrast", contrast >= 7.0)
    }
}
