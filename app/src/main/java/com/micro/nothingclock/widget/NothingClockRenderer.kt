package com.micro.nothingclock.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.micro.nothingclock.R
import com.micro.nothingclock.data.ClockThemePreset
import com.micro.nothingclock.data.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NothingClockRenderer {

    private var cachedGeistMono: Typeface? = null
    private var cachedGeistMonoBold: Typeface? = null

    fun getGeistTypeface(context: Context?, bold: Boolean = false): Typeface {
        if (bold && cachedGeistMonoBold != null) return cachedGeistMonoBold!!
        if (!bold && cachedGeistMono != null) return cachedGeistMono!!

        if (context != null) {
            try {
                val resId = if (bold) R.font.geist_mono_bold else R.font.geist_mono
                val tf = ResourcesCompat.getFont(context, resId)
                if (tf != null) {
                    if (bold) cachedGeistMonoBold = tf else cachedGeistMono = tf
                    return tf
                }
            } catch (_: Exception) {
                // Fallback to monospace
            }
        }
        return if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        else Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    fun calculateHandGestures(minutes: Long, isAllDay: Boolean = false): String {
        if (isAllDay) return "✌️ (All Day)"
        if (minutes < 0) return "✌️ (Open Day)"
        if (minutes == 0L) return "👌 (Starting Now)"
        val drawables = getHandMarkerDrawables(minutes, isAllDay)
        val emojis = drawables.joinToString(" ") { resId ->
            when (resId) {
                R.drawable.ic_nothing_marker_1 -> "☝️"
                R.drawable.ic_nothing_marker_2 -> "✌️"
                R.drawable.ic_nothing_marker_3 -> "🤟"
                R.drawable.ic_nothing_marker_4 -> "🖖"
                else -> "🖐️"
            }
        }
        return "$emojis (${minutes}MIN)"
    }

    fun getDayDrawableRes(day: Int): Int {
        return when (day) {
            1 -> R.drawable.ic_nothing_day_1
            2 -> R.drawable.ic_nothing_day_2
            3 -> R.drawable.ic_nothing_day_3
            4 -> R.drawable.ic_nothing_day_4
            5 -> R.drawable.ic_nothing_day_5
            6 -> R.drawable.ic_nothing_day_6
            7 -> R.drawable.ic_nothing_day_7
            8 -> R.drawable.ic_nothing_day_8
            9 -> R.drawable.ic_nothing_day_9
            10 -> R.drawable.ic_nothing_day_10
            11 -> R.drawable.ic_nothing_day_11
            12 -> R.drawable.ic_nothing_day_12
            13 -> R.drawable.ic_nothing_day_13
            14 -> R.drawable.ic_nothing_day_14
            15 -> R.drawable.ic_nothing_day_15
            16 -> R.drawable.ic_nothing_day_16
            17 -> R.drawable.ic_nothing_day_17
            18 -> R.drawable.ic_nothing_day_18
            19 -> R.drawable.ic_nothing_day_19
            20 -> R.drawable.ic_nothing_day_20
            21 -> R.drawable.ic_nothing_day_21
            22 -> R.drawable.ic_nothing_day_22
            23 -> R.drawable.ic_nothing_day_23
            24 -> R.drawable.ic_nothing_day_24
            25 -> R.drawable.ic_nothing_day_25
            26 -> R.drawable.ic_nothing_day_26
            27 -> R.drawable.ic_nothing_day_27
            28 -> R.drawable.ic_nothing_day_28
            29 -> R.drawable.ic_nothing_day_29
            30 -> R.drawable.ic_nothing_day_30
            31 -> R.drawable.ic_nothing_day_31
            else -> R.drawable.ic_nothing_day_15
        }
    }

    fun getMonthDrawableRes(monthIndex: Int): Int {
        return when (monthIndex) {
            Calendar.JANUARY -> R.drawable.ic_nothing_month_jan
            Calendar.FEBRUARY -> R.drawable.ic_nothing_month_feb
            Calendar.MARCH -> R.drawable.ic_nothing_month_mar
            Calendar.APRIL -> R.drawable.ic_nothing_month_apr
            Calendar.MAY -> R.drawable.ic_nothing_month_may
            Calendar.JUNE -> R.drawable.ic_nothing_month_jun
            Calendar.JULY -> R.drawable.ic_nothing_month_jul
            Calendar.AUGUST -> R.drawable.ic_nothing_month_aug
            Calendar.SEPTEMBER -> R.drawable.ic_nothing_month_sep
            Calendar.OCTOBER -> R.drawable.ic_nothing_month_oct
            Calendar.NOVEMBER -> R.drawable.ic_nothing_month_nov
            Calendar.DECEMBER -> R.drawable.ic_nothing_month_dec
            else -> R.drawable.ic_nothing_month_aug
        }
    }

    fun getTempBarDrawableRes(temp: Int, isFahrenheit: Boolean): Int {
        val tempF = if (isFahrenheit) temp else (temp * 9 / 5 + 32)
        return when {
            tempF >= 80 -> R.drawable.ic_nothing_temp_bar_100_80
            tempF >= 60 -> R.drawable.ic_nothing_temp_bar_70_60
            tempF >= 30 -> R.drawable.ic_nothing_temp_bar_50_30
            tempF >= 10 -> R.drawable.ic_nothing_temp_bar_20_10
            else -> R.drawable.ic_nothing_temp_bar_9_0
        }
    }

    fun getMeasureDrawableRes(unit: String): Int {
        return when {
            unit.contains("F", ignoreCase = true) -> R.drawable.ic_nothing_measure_f
            unit.contains("K", ignoreCase = true) -> R.drawable.ic_nothing_measure_k
            else -> R.drawable.ic_nothing_measure_c
        }
    }

    fun getWeatherSpecDrawableRes(condition: String): Int {
        val c = condition.uppercase().trim()
        return when {
            c.contains("SUN") || c.contains("CLEAR") -> R.drawable.ic_nothing_weather_sunny
            c.contains("RAIN") || c.contains("DRIZZLE") || c.contains("SHOWER") -> R.drawable.ic_nothing_weather_rain
            c.contains("CLOUD") || c.contains("OVERCAST") || c.contains("FOG") -> R.drawable.ic_nothing_weather_cloud
            c.contains("SNOW") || c.contains("BLIZZARD") || c.contains("ICE") || c.contains("FLURRY") -> R.drawable.ic_nothing_weather_snow
            c.contains("THUNDER") -> R.drawable.ic_nothing_weather_thunder
            c.contains("STORM") -> R.drawable.ic_nothing_weather_storm
            c.contains("WIND") || c.contains("BREEZ") -> R.drawable.ic_nothing_weather_breezy
            c.contains("HAZE") || c.contains("SMOKE") || c.contains("MIST") -> R.drawable.ic_nothing_weather_haze
            c.contains("HAIL") -> R.drawable.ic_nothing_weather_hail
            c.contains("FLOOD") -> R.drawable.ic_nothing_weather_flood
            c.contains("HURRICANE") -> R.drawable.ic_nothing_weather_hurricane
            c.contains("TORNADO") -> R.drawable.ic_nothing_weather_tornado
            c.contains("SEVERE") -> R.drawable.ic_nothing_weather_severe
            else -> R.drawable.ic_nothing_weather_sunny
        }
    }

    fun getHandMarkerDrawables(minutes: Long, isAllDay: Boolean = false): List<Int> {
        if (isAllDay) return listOf(R.drawable.ic_nothing_marker_2)
        if (minutes < 0) return listOf(R.drawable.ic_nothing_marker_2) // Peace sign for open day!
        if (minutes == 0L) return listOf(R.drawable.ic_nothing_marker_1)

        val list = mutableListOf<Int>()
        val tens = (minutes / 10).toInt().coerceIn(0, 5)
        val units = (minutes % 10).toInt()

        // 1. Tens place (10-50 min): Hand showing the tens digit
        if (tens > 0) {
            list.add(getMarkerForDigit(tens))
        }

        // 2. Units place (0-9 min):
        // 0 -> no extra hand needed (e.g. 30 -> (3))
        // 1..5 -> single hand with 1..5 fingers (e.g. 35 -> (3) (5))
        // 6..9 -> overflows a single 5-finger hand into (5) + (remainder)
        //         e.g. 38 -> (3) (5) (3), 49 -> (4) (5) (4), 6 -> (5) (1)
        when (units) {
            in 1..5 -> list.add(getMarkerForDigit(units))
            in 6..9 -> {
                list.add(R.drawable.ic_nothing_marker_5)
                list.add(getMarkerForDigit(units - 5))
            }
        }

        return if (list.isEmpty()) listOf(R.drawable.ic_nothing_marker_1) else list
    }

    private fun getMarkerForDigit(digit: Int): Int {
        return when (digit) {
            1 -> R.drawable.ic_nothing_marker_1
            2 -> R.drawable.ic_nothing_marker_2
            3 -> R.drawable.ic_nothing_marker_3
            4 -> R.drawable.ic_nothing_marker_4
            else -> R.drawable.ic_nothing_marker_5
        }
    }

    fun drawAsset(
        context: Context?,
        canvas: Canvas,
        resId: Int,
        x: Float,
        yCenter: Float,
        targetHeight: Float,
        tintColor: Int? = null,
        targetWidth: Float? = null
    ): Float {
        if (context == null) return 0f
        val drawable = ContextCompat.getDrawable(context, resId) ?: return 0f
        if (tintColor != null) {
            drawable.mutate().setTint(tintColor)
        }
        val intrinsicW = drawable.intrinsicWidth.toFloat().coerceAtLeast(1f)
        val intrinsicH = drawable.intrinsicHeight.toFloat().coerceAtLeast(1f)
        val finalW = targetWidth ?: (targetHeight * (intrinsicW / intrinsicH))
        val yTop = yCenter - targetHeight / 2f

        canvas.save()
        canvas.translate(x, yTop)
        canvas.scale(finalW / intrinsicW, targetHeight / intrinsicH)
        drawable.setBounds(0, 0, intrinsicW.toInt(), intrinsicH.toInt())
        drawable.draw(canvas)
        canvas.restore()

        return finalW
    }

    /**
     * Splits an event title into two lines for responsive tall widgets.
     * Line 1 gets as many whole words as can fit within maxWidth1.
     * Line 2 gets the remaining words (or is truncated with '…' if it exceeds maxWidth2).
     */
    fun splitEventIntoTwoLines(
        text: String,
        paint: Paint,
        maxWidth1: Float,
        maxWidth2: Float
    ): Pair<String, String> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return Pair("", "")
        if (words.size == 1) {
            val charsFit = paint.breakText(words[0], true, maxWidth1, null)
            val l1 = words[0].substring(0, charsFit.coerceAtLeast(1))
            var l2 = words[0].substring(charsFit.coerceAtLeast(1)).trim()
            if (l2.isNotEmpty() && paint.measureText(l2) > maxWidth2) {
                val fit2 = paint.breakText(l2, true, maxWidth2, null)
                l2 = l2.substring(0, (fit2 - 1).coerceAtLeast(0)) + "…"
            }
            return Pair(l1, l2)
        }

        var line1 = ""
        var breakIndex = 0

        for (i in words.indices) {
            val candidate = if (line1.isEmpty()) words[i] else "$line1 ${words[i]}"
            if (paint.measureText(candidate) <= maxWidth1) {
                line1 = candidate
                breakIndex = i + 1
            } else {
                break
            }
        }

        // If even the first word does not fit in line 1:
        if (line1.isEmpty()) {
            val charsFit = paint.breakText(words[0], true, maxWidth1, null)
            line1 = words[0].substring(0, charsFit.coerceAtLeast(1))
            val restOfFirst = words[0].substring(charsFit.coerceAtLeast(1)).trim()
            val remaining = (if (restOfFirst.isNotEmpty()) listOf(restOfFirst) else emptyList()) + words.drop(1)
            var line2 = remaining.joinToString(" ")
            if (paint.measureText(line2) > maxWidth2) {
                val fit2 = paint.breakText(line2, true, maxWidth2, null)
                line2 = line2.substring(0, (fit2 - 1).coerceAtLeast(0)) + "…"
            }
            return Pair(line1, line2)
        }

        val remainingWords = words.subList(breakIndex, words.size)
        var line2 = remainingWords.joinToString(" ")
        if (paint.measureText(line2) > maxWidth2) {
            val fit2 = paint.breakText(line2, true, maxWidth2, null)
            line2 = line2.substring(0, (fit2 - 1).coerceAtLeast(0)) + "…"
        }
        return Pair(line1, line2)
    }

    private fun drawBlueprintGrid(canvas: Canvas, width: Float, height: Float, cornerRadius: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(146, 166, 184) // Authentic blueprint slate blue
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, 0f, width, height)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Draw fine blueprint grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            strokeWidth = width * 0.0014f
            style = Paint.Style.STROKE
        }
        val step = height * 0.088f
        var x = step
        while (x < width) {
            canvas.drawLine(x, 0f, x, height, gridPaint)
            x += step
        }
        var y = step
        while (y < height) {
            canvas.drawLine(0f, y, width, y, gridPaint)
            y += step
        }
    }

    private var frostedNoiseShader: BitmapShader? = null

    private fun getFrostedNoiseShader(): BitmapShader {
        frostedNoiseShader?.let { return it }
        val size = 96
        val noiseBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val random = java.util.Random(1337L) // Deterministic seed for zero jitter
        val pixels = IntArray(size * size)
        for (i in pixels.indices) {
            val isHighlight = random.nextBoolean()
            val alpha = random.nextInt(14) + 4 // 4 to 17 alpha
            pixels[i] = if (isHighlight) {
                Color.argb(alpha, 255, 255, 255)
            } else {
                Color.argb(alpha / 2, 170, 185, 205)
            }
        }
        noiseBitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        val shader = BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        frostedNoiseShader = shader
        return shader
    }

    /**
     * Renders authentic multi-layered frosted glass with:
     * 1. Multi-stop light diffusion gradient (specular reflection from top-left)
     * 2. Acid-etched satin micro-grain texture
     * 3. Beveled cut-glass specular refraction rim
     * 4. Secondary subsurface inner glass thickness reflection
     */
    private fun drawFrostedGlassSurface(
        canvas: Canvas,
        width: Float,
        height: Float,
        cornerRadius: Float,
        opacityPercent: Int
    ) {
        val cardRect = RectF(0f, 0f, width, height)

        // 1. Calculate realistic optical density floor (WCAG AAA contrast floor)
        val opacityFactor = (opacityPercent / 100f).coerceIn(0.40f, 1.0f)
        val topAlpha = (235 * opacityFactor).toInt().coerceIn(95, 248)
        val midAlpha = (205 * opacityFactor).toInt().coerceIn(80, 228)
        val btmAlpha = (175 * opacityFactor).toInt().coerceIn(65, 210)

        // 2. Multi-stop light diffusion gradient (specular ambient light from top-left)
        val glassGradient = LinearGradient(
            0f, 0f,
            width, height,
            intArrayOf(
                Color.argb(topAlpha, 255, 255, 255), // Top-left luminous specular sheen
                Color.argb(midAlpha, 246, 249, 253), // Diffuse frosted ice
                Color.argb(btmAlpha, 234, 239, 246)  // Cooler refractive body
            ),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = glassGradient
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, glassPaint)

        // 3. Etched satin frosted micro-texture (procedural sandblasted physical grain)
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        val noisePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = getFrostedNoiseShader()
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, noisePaint)
        canvas.restore()

        // 4. Primary Specular Bevel Edge (Cut-glass refraction highlight rim)
        val rimStrokeWidth = (width * 0.0035f).coerceIn(2.5f, 5.0f)
        val rimGradient = LinearGradient(
            0f, 0f,
            width, height,
            intArrayOf(
                Color.argb(235, 255, 255, 255), // Brilliant specular highlight at top-left
                Color.argb(135, 255, 255, 255), // Clear glass refraction
                Color.argb(55, 255, 255, 255),  // Ambient rim falloff
                Color.argb(30, 255, 255, 255)   // Shadow edge
            ),
            floatArrayOf(0.0f, 0.30f, 0.70f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = rimStrokeWidth
            shader = rimGradient
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, rimPaint)

        // 5. Secondary Inner Refraction Bevel (4mm Physical Glass Thickness)
        val innerInset = rimStrokeWidth * 1.15f
        val innerRect = RectF(
            cardRect.left + innerInset,
            cardRect.top + innerInset,
            cardRect.right - innerInset,
            cardRect.bottom - innerInset
        )
        val innerCornerRadius = (cornerRadius - innerInset).coerceAtLeast(4f)
        val innerGradient = LinearGradient(
            0f, innerRect.top,
            0f, innerRect.top + height * 0.35f,
            intArrayOf(
                Color.argb(90, 255, 255, 255), // Soft subsurface rim catch
                Color.argb(0, 255, 255, 255)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (width * 0.0018f).coerceIn(1.5f, 3.0f)
            shader = innerGradient
        }
        canvas.drawRoundRect(innerRect, innerCornerRadius, innerCornerRadius, innerPaint)
    }

    fun renderToBitmap(
        context: Context?,
        width: Int,
        height: Int,
        config: WidgetConfig,
        eventTitle: String = "WEEKLY DESIGN SYNC",
        minutesLeft: Long = 6,
        temp: Int = 27,
        condition: String = "SUNNY",
        isAllDay: Boolean = false
    ): Bitmap {
        val safeW = if (width > 0) width else 1024
        val safeH = if (height > 0) height else 461
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val geistMonoBold = getGeistTypeface(context, bold = true)

        // Resolve Theme, Surface & Contrast Legibility
        var cardBgColor: Int = Color.TRANSPARENT
        var cardBorderColor: Int = Color.TRANSPARENT
        var textColor: Int = Color.WHITE
        var pillBgColor: Int = Color.WHITE
        var pillTextColor: Int = Color.BLACK
        var isBlueprint = false

        when (config.themePreset) {
            ClockThemePreset.OS5_BLUEPRINT -> {
                isBlueprint = true
                textColor = Color.WHITE
                pillBgColor = Color.WHITE
                pillTextColor = Color.rgb(146, 166, 184)
            }
            ClockThemePreset.MATERIAL_YOU -> {
                // Dynamic Material You color resolution
                var dynamicBg = Color.rgb(28, 32, 40)
                if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        dynamicBg = ContextCompat.getColor(context, android.R.color.system_accent1_800)
                    } catch (_: Exception) {}
                }
                // Calculate luminance for contrast: 0.299R + 0.587G + 0.114B
                val lum = (Color.red(dynamicBg) * 0.299 + Color.green(dynamicBg) * 0.587 + Color.blue(dynamicBg) * 0.114) / 255.0
                cardBgColor = dynamicBg
                cardBorderColor = if (lum > 0.5) Color.argb(60, 0, 0, 0) else Color.argb(60, 255, 255, 255)
                textColor = if (lum > 0.5) Color.rgb(20, 20, 24) else Color.WHITE
                pillBgColor = textColor
                pillTextColor = if (lum > 0.5) Color.WHITE else Color.rgb(20, 20, 24)
            }
            ClockThemePreset.FROSTED_GLASS -> {
                // High-contrast deep obsidian typography and solid badges over luminous frosted glass
                textColor = Color.rgb(18, 20, 24)
                pillBgColor = Color.rgb(20, 22, 26)
                pillTextColor = Color.WHITE
            }
            ClockThemePreset.AMOLED_DARK -> {
                cardBgColor = Color.BLACK
                cardBorderColor = Color.argb(45, 255, 255, 255)
                textColor = Color.WHITE
                pillBgColor = Color.WHITE
                pillTextColor = Color.BLACK
            }
            ClockThemePreset.NOTHING_RED -> {
                cardBgColor = Color.rgb(20, 7, 8)
                cardBorderColor = Color.argb(120, 215, 25, 32)
                textColor = Color.WHITE
                pillBgColor = Color.rgb(215, 25, 32)
                pillTextColor = Color.WHITE
            }
            ClockThemePreset.TRANSPARENT_FLOATING -> {
                // Floating mode: no card surface
                cardBgColor = Color.TRANSPARENT
                cardBorderColor = Color.TRANSPARENT
                textColor = Color.WHITE
                pillBgColor = Color.WHITE
                pillTextColor = Color.rgb(20, 20, 24)
            }
        }

        val cornerRadius = safeW * (config.cornerRadiusDp.toFloat() / 400f).coerceIn(0.04f, 0.12f)
        val cardRect = RectF(0f, 0f, safeW.toFloat(), safeH.toFloat())

        // 1. Draw Background
        if (isBlueprint) {
            drawBlueprintGrid(canvas, safeW.toFloat(), safeH.toFloat(), cornerRadius)
        } else if (config.themePreset == ClockThemePreset.FROSTED_GLASS) {
            drawFrostedGlassSurface(canvas, safeW.toFloat(), safeH.toFloat(), cornerRadius, config.cardOpacityPercent)
        } else if (config.themePreset != ClockThemePreset.TRANSPARENT_FLOATING && cardBgColor != Color.TRANSPARENT) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cardBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

            if (cardBorderColor != Color.TRANSPARENT) {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = cardBorderColor
                    style = Paint.Style.STROKE
                    strokeWidth = safeW * 0.003f
                }
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)
            }
        }

        // Geometry: Responsive vertical layout matching MicroNothing
        val contentLeft = safeW * 0.040f
        val contentRight = safeW * 0.960f
        val heightRatio = safeH.toFloat() / safeW.toFloat()

        // Adaptive top/bottom padding depending on aspect ratio (shallow banners vs standard vs square/tall)
        val topPadding = if (heightRatio < 0.38f) {
            (safeH * 0.08f).coerceIn(10f, safeW * 0.05f)
        } else {
            (safeH * 0.10f).coerceIn(18f, safeW * 0.08f)
        }
        val bottomPadding = if (heightRatio < 0.38f) {
            (safeH * 0.06f).coerceIn(8f, safeW * 0.04f)
        } else {
            (safeH * 0.08f).coerceIn(14f, safeW * 0.06f)
        }
        val availableHeight = (safeH - topPadding - bottomPadding).coerceAtLeast(80f)

        val isTallEnough = heightRatio >= 0.55f

        // Initial measurement for event title length
        val displayEvent = eventTitle.uppercase().trim()
        val tentativeRowHeight = availableHeight / 4.0f
        val tentativeFontSize = (tentativeRowHeight * 0.62f).coerceIn(16f, safeW * 0.058f)
        val tentativeBadgeHeight = (tentativeFontSize * 1.18f).coerceIn(20f, (tentativeRowHeight * 0.78f).coerceAtLeast(22f))

        val tentativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = geistMonoBold
            textSize = tentativeFontSize
            letterSpacing = 0.04f
        }
        val followupEstWidth = tentativeBadgeHeight * (16f / 11f)
        val speakEstWidth = tentativeBadgeHeight * 0.88f
        val singleLineMaxWidth = (contentRight - (contentLeft + speakEstWidth + safeW * 0.024f) - followupEstWidth - safeW * 0.028f).coerceAtLeast(safeW * 0.2f)
        val isTwoLineEvent = isTallEnough && tentativePaint.measureText(displayEvent) > singleLineMaxWidth

        // Row Heights and centers: for square or tall aspect ratios (heightRatio >= 0.70f),
        // we bound row spacing so rows don't stretch apart awkwardly, and center the clock block vertically.
        val isSquareOrTall = heightRatio >= 0.70f
        val rawUnit = if (isTwoLineEvent) (availableHeight / 4.6f) else (availableHeight / 4.0f)
        val baseFontSize = (rawUnit * 0.62f).coerceIn(16f, safeW * 0.058f)
        val badgeHeight = (baseFontSize * 1.18f).coerceIn(20f, (rawUnit * 0.80f).coerceAtLeast(22f))

        val maxRowSpacing = baseFontSize * 2.15f
        val rowHeight = if (isSquareOrTall) rawUnit.coerceAtMost(maxRowSpacing) else rawUnit
        val totalBlockHeight = rowHeight * (if (isTwoLineEvent) 4.6f else 4.0f)
        val verticalOffset = if (isSquareOrTall) ((availableHeight - totalBlockHeight) / 2f).coerceAtLeast(0f) else 0f
        val effectiveTop = topPadding + verticalOffset

        val row1CenterY: Float
        val row2CenterY: Float
        val row3CenterY: Float
        val eventLine1CenterY: Float
        val eventLine2CenterY: Float
        val row4CenterY: Float

        if (isTwoLineEvent) {
            row1CenterY = effectiveTop + rowHeight * 0.5f
            row2CenterY = effectiveTop + rowHeight * 1.5f
            val eventCenterY = effectiveTop + rowHeight * 2.8f
            row4CenterY = effectiveTop + rowHeight * 4.1f
            val lineSpacing = (baseFontSize * 1.35f).coerceAtMost(rowHeight * 0.75f)
            eventLine1CenterY = eventCenterY - lineSpacing * 0.5f
            eventLine2CenterY = eventCenterY + lineSpacing * 0.5f
            row3CenterY = eventCenterY
        } else {
            row1CenterY = effectiveTop + rowHeight * 0.5f
            row2CenterY = effectiveTop + rowHeight * 1.5f
            row3CenterY = effectiveTop + rowHeight * 2.5f
            row4CenterY = effectiveTop + rowHeight * 3.5f
            eventLine1CenterY = row3CenterY
            eventLine2CenterY = row3CenterY
        }

        val calendar = Calendar.getInstance()
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US)
        val timeFormat = if (config.is24Hour) SimpleDateFormat("HH:mm", Locale.US) else SimpleDateFormat("hh:mm", Locale.US)

        val dayOfWeekStr = dayOfWeekFormat.format(calendar.time).uppercase()
        val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
        val monthIndex = calendar.get(Calendar.MONTH)
        val rawTimeStr = timeFormat.format(calendar.time)
        val timeStr = if (!config.is24Hour && rawTimeStr.startsWith("0")) rawTimeStr.substring(1) else rawTimeStr

        // Resolve Dynamic Timezone Abbreviation
        val tz = TimeZone.getDefault()
        val isDst = tz.inDaylightTime(Date())
        val tzAbbr = tz.getDisplayName(isDst, TimeZone.SHORT, Locale.US).uppercase()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = geistMonoBold
            color = textColor
            textSize = baseFontSize
            letterSpacing = 0.04f
        }
        val textMetrics = textPaint.fontMetrics
        val textBaselineOffset = -(textMetrics.ascent + textMetrics.descent) / 2f

        // ================= ROW 1: SAT ( 15 ) ( AUG ) <EST> 10:53 =================
        var cursorX = contentLeft

        // 1. Day of Week (SAT)
        canvas.drawText(dayOfWeekStr, cursorX, row1CenterY + textBaselineOffset, textPaint)
        cursorX += textPaint.measureText(dayOfWeekStr) + safeW * 0.018f

        // 2. Day Number Solid Pill Badge ( 15 )
        val dayRes = getDayDrawableRes(dayNum)
        val dayW = drawAsset(context, canvas, dayRes, cursorX, row1CenterY, targetHeight = badgeHeight, tintColor = pillBgColor)
        cursorX += dayW + safeW * 0.018f

        // 3. Month Outline Pill Badge ( AUG )
        val monthRes = getMonthDrawableRes(monthIndex)
        val monthW = drawAsset(context, canvas, monthRes, cursorX, row1CenterY, targetHeight = badgeHeight, tintColor = textColor)
        cursorX += monthW + safeW * 0.018f

        // 4. Dynamic Timezone Starburst Badge (<EST>, <PST>, <GMT>, etc.)
        if (tzAbbr == "EST") {
            val tzW = drawAsset(context, canvas, R.drawable.ic_nothing_timezone, cursorX, row1CenterY, targetHeight = badgeHeight * 1.05f, tintColor = pillBgColor)
            cursorX += tzW + safeW * 0.024f
        } else {
            // Render Starburst Base with dynamic timezone text
            val tzW = drawAsset(context, canvas, R.drawable.ic_nothing_timezone_base, cursorX, row1CenterY, targetHeight = badgeHeight * 1.05f, tintColor = pillBgColor)
            val tzTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = geistMonoBold
                color = pillTextColor
                textSize = badgeHeight * 0.40f
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.02f
            }
            val maxTzTextW = tzW * 0.72f
            val measuredTzW = tzTextPaint.measureText(tzAbbr)
            if (measuredTzW > maxTzTextW && maxTzTextW > 0f) {
                tzTextPaint.textSize = (tzTextPaint.textSize * (maxTzTextW / measuredTzW)).coerceAtLeast(8f)
            }
            val tzOffset = -(tzTextPaint.fontMetrics.ascent + tzTextPaint.fontMetrics.descent) / 2f
            canvas.drawText(tzAbbr, cursorX + tzW / 2f, row1CenterY + tzOffset, tzTextPaint)
            cursorX += tzW + safeW * 0.024f
        }

        // 5. Digital Time (10:53)
        canvas.drawText(timeStr, cursorX, row1CenterY + textBaselineOffset, textPaint)
        cursorX += textPaint.measureText(timeStr)

        if (config.showCursor) {
            cursorX += safeW * 0.01f
            val cursorW = safeW * 0.018f
            val cursorH = baseFontSize * 0.90f
            val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(cursorX, row1CenterY - cursorH / 2f, cursorX + cursorW, row1CenterY + cursorH / 2f, cursorPaint)
        }

        // ================= ROW 2: I ◂ 80°(F) [Sun] [SUNNY bubble] =================
        cursorX = contentLeft

        val isFahrenheit = config.tempUnit.contains("F", ignoreCase = true)
        val displayTemp = temp

        // 1. Temp Bar Thermometer Gauge
        val tempBarRes = getTempBarDrawableRes(displayTemp, isFahrenheit)
        val tempBarW = drawAsset(context, canvas, tempBarRes, cursorX, row2CenterY, targetHeight = badgeHeight * 0.90f, tintColor = textColor)
        cursorX += tempBarW + safeW * 0.022f

        // 2. Temp Number with Degree Symbol (80°)
        val tempStr = "$displayTemp°"
        canvas.drawText(tempStr, cursorX, row2CenterY + textBaselineOffset, textPaint)
        cursorX += textPaint.measureText(tempStr) + safeW * 0.005f

        // 3. Measure Unit Badge ((F) or (C))
        val measureRes = getMeasureDrawableRes(config.tempUnit)
        val measureW = drawAsset(context, canvas, measureRes, cursorX, row2CenterY, targetHeight = badgeHeight, tintColor = textColor)
        cursorX += measureW + safeW * 0.028f

        // 4. Weather Spec Icon & Speech Bubble (Sunny Sun & SUNNY bubble)
        val specRes = getWeatherSpecDrawableRes(condition)
        drawAsset(context, canvas, specRes, cursorX, row2CenterY, targetHeight = badgeHeight, tintColor = textColor)

        // ================= ROW 3: [Speak] EVENT OR QUIPPY PLACEHOLDER ( ➔ ) =================
        if (!isTwoLineEvent) {
            cursorX = contentLeft

            // 1. Speaking Head Profile Icon with Waves
            val speakW = drawAsset(context, canvas, R.drawable.ic_nothing_speak, cursorX, row3CenterY, targetHeight = badgeHeight * 0.88f, tintColor = textColor)
            cursorX += speakW + safeW * 0.024f

            // 2. Event Title or Quippy Placeholder
            val maxEventWidth = (contentRight - cursorX - followupEstWidth - safeW * 0.028f).coerceAtLeast(safeW * 0.2f)
            val truncatedLength = textPaint.breakText(displayEvent, true, maxEventWidth, null)
            val finalEventText = if (truncatedLength < displayEvent.length) {
                displayEvent.substring(0, (truncatedLength - 1).coerceAtLeast(0)) + "…"
            } else {
                displayEvent
            }
            canvas.drawText(finalEventText, cursorX, row3CenterY + textBaselineOffset, textPaint)
            cursorX += textPaint.measureText(finalEventText) + safeW * 0.028f

            // 3. Action Badge Arrow Pill ( ➔ )
            drawAsset(context, canvas, R.drawable.ic_nothing_followup, cursorX, row3CenterY, targetHeight = badgeHeight, tintColor = pillBgColor)
        } else {
            // Two-line responsive event layout for tall widgets
            val maxLineWidth1 = contentRight - (contentLeft + speakEstWidth + safeW * 0.024f)
            val maxLineWidth2 = contentRight - (contentLeft + speakEstWidth + safeW * 0.024f) - followupEstWidth - safeW * 0.028f

            val (line1, line2) = splitEventIntoTwoLines(displayEvent, textPaint, maxLineWidth1, maxLineWidth2)

            cursorX = contentLeft

            // Line 1: Speak icon + Line 1 text
            val actualSpeakW = drawAsset(context, canvas, R.drawable.ic_nothing_speak, cursorX, eventLine1CenterY, targetHeight = badgeHeight * 0.88f, tintColor = textColor)
            val textStartX = cursorX + actualSpeakW + safeW * 0.024f
            canvas.drawText(line1, textStartX, eventLine1CenterY + textBaselineOffset, textPaint)

            // Line 2: Line 2 text (indented to align with Line 1 text) + Followup Arrow Pill
            canvas.drawText(line2, textStartX, eventLine2CenterY + textBaselineOffset, textPaint)
            val arrowX = textStartX + textPaint.measureText(line2) + safeW * 0.024f
            drawAsset(context, canvas, R.drawable.ic_nothing_followup, arrowX, eventLine2CenterY, targetHeight = badgeHeight, tintColor = pillBgColor)
        }

        // ================= ROW 4: STARTING [Hand1] [Hand5] └────── 6MIN =================
        cursorX = contentLeft

        val isEventActive = minutesLeft >= 0

        // 1. Status Label: "STARTING" when meeting upcoming, "ALL DAY" when all-day event, or "ALL CLEAR" when open
        val statusLabel = when {
            isAllDay -> "ALL DAY"
            isEventActive -> "STARTING"
            else -> "ALL CLEAR"
        }
        canvas.drawText(statusLabel, cursorX, row4CenterY + textBaselineOffset, textPaint)
        cursorX += textPaint.measureText(statusLabel) + safeW * 0.028f

        // 2. Hand Gesture Tally Badges
        val handDrawables = getHandMarkerDrawables(minutesLeft, isAllDay)
        for (hRes in handDrawables) {
            val hW = drawAsset(context, canvas, hRes, cursorX, row4CenterY, targetHeight = badgeHeight * 0.90f, tintColor = pillBgColor)
            cursorX += hW + safeW * 0.016f
        }
        cursorX += safeW * 0.018f

        // 3. Cradle Bracket Underline & Countdown / Status Text
        val countdownText = when {
            isAllDay -> "TODAY"
            isEventActive -> "${minutesLeft}MIN"
            else -> "OPEN"
        }
        val minWidth = textPaint.measureText(countdownText)
        val cradleHeight = baseFontSize * 1.05f

        // Draw the authentic L-bracket cradle
        val cradleCornerW = drawAsset(
            context,
            canvas,
            R.drawable.ic_nothing_cradle,
            cursorX,
            row4CenterY + cradleHeight * 0.04f,
            targetHeight = cradleHeight,
            tintColor = textColor
        )

        // Underline line extending under and past the countdown text
        val baselineY = row4CenterY + cradleHeight * 0.50f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            style = Paint.Style.STROKE
            strokeWidth = (baseFontSize * 0.08f).coerceIn(2.5f, 6.0f)
            strokeCap = Paint.Cap.ROUND
        }
        val textStartX = cursorX + cradleCornerW * 0.75f
        val lineEndX = (textStartX + minWidth + safeW * 0.022f).coerceAtMost(contentRight)

        canvas.drawLine(textStartX, baselineY, lineEndX, baselineY, linePaint)

        // Draw Countdown / Open text resting inside the cradle
        canvas.drawText(countdownText, textStartX + safeW * 0.005f, row4CenterY + textBaselineOffset - cradleHeight * 0.02f, textPaint)

        return bitmap
    }
}
