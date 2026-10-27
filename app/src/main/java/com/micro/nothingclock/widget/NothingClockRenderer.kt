package com.micro.nothingclock.widget

import android.annotation.SuppressLint
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
import com.micro.nothingclock.data.WidgetFontWeight
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

object NothingClockRenderer {

    private val typefaceCache = ConcurrentHashMap<Int, Typeface>()
    private var cachedVariableFontFile: File? = null

    @SuppressLint("ResourceType")
    private fun getVariableFontFile(context: Context): File? {
        val existing = cachedVariableFontFile
        if (existing != null && existing.exists() && existing.length() > 0) {
            return existing
        }
        return try {
            val file = File(context.cacheDir, "geist_mono_variable.ttf")
            if (!file.exists() || file.length() == 0L) {
                context.resources.openRawResource(R.font.geist_mono).use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            cachedVariableFontFile = file
            file
        } catch (_: Exception) {
            null
        }
    }

    fun getGeistTypeface(context: Context?, weight: WidgetFontWeight = WidgetFontWeight.BOLD): Typeface {
        return getGeistTypeface(context, weight.weight)
    }

    fun getGeistTypeface(context: Context?, weightInt: Int): Typeface {
        typefaceCache[weightInt]?.let { return it }

        if (context != null) {
            // 1. Direct static fonts for exact matches when available
            if (weightInt >= 700) {
                try {
                    val tf = ResourcesCompat.getFont(context, R.font.geist_mono_bold)
                    if (tf != null) {
                        typefaceCache[weightInt] = tf
                        return tf
                    }
                } catch (_: Exception) {}
            } else if (weightInt == 400) {
                try {
                    val tf = ResourcesCompat.getFont(context, R.font.geist_mono_regular)
                    if (tf != null) {
                        typefaceCache[weightInt] = tf
                        return tf
                    }
                } catch (_: Exception) {}
            }

            // 2. Variable Font variation builder for API 26+ (Oreo+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val fontFile = getVariableFontFile(context)
                    if (fontFile != null && fontFile.exists()) {
                        val tf = Typeface.Builder(fontFile)
                            .setFontVariationSettings("'wght' $weightInt")
                            .setWeight(weightInt)
                            .build()
                        if (tf != null) {
                            typefaceCache[weightInt] = tf
                            return tf
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Android P (API 28+) Typeface.create with weight on variable font
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val baseTf = ResourcesCompat.getFont(context, R.font.geist_mono)
                    if (baseTf != null) {
                        val tf = Typeface.create(baseTf, weightInt, false)
                        typefaceCache[weightInt] = tf
                        return tf
                    }
                } catch (_: Exception) {}
            }

            // 4. Fallback to static font files
            try {
                val resId = if (weightInt >= 600) R.font.geist_mono_bold else R.font.geist_mono_regular
                val tf = ResourcesCompat.getFont(context, resId)
                if (tf != null) {
                    typefaceCache[weightInt] = tf
                    return tf
                }
            } catch (_: Exception) {}
        }

        // 5. System monospace fallback
        val fallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(Typeface.MONOSPACE, weightInt, false)
        } else {
            if (weightInt >= 600) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            else Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        typefaceCache[weightInt] = fallback
        return fallback
    }

    fun getGeistTypeface(context: Context?, bold: Boolean): Typeface {
        return getGeistTypeface(context, if (bold) WidgetFontWeight.BOLD else WidgetFontWeight.REGULAR)
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
        val label = if (minutes >= 60) {
            val hours = ((minutes + 30) / 60).coerceAtLeast(1)
            if (hours <= 1L) "1 HOUR" else "$hours HOURS"
        } else {
            "${minutes}MIN"
        }
        return "$emojis ($label)"
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
            c.contains("CLEAR NIGHT") || c.contains("NIGHT") || c.contains("MOON") -> R.drawable.ic_nothing_weather_clear_night
            c.contains("CLEAR") -> R.drawable.ic_nothing_weather_clear
            c.contains("SUN") -> R.drawable.ic_nothing_weather_sunny
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

        if (minutes >= 60) {
            val hours = ((minutes + 30) / 60).coerceAtLeast(1)
            val list = mutableListOf<Int>()
            if (hours <= 5) {
                list.add(getMarkerForDigit(hours.toInt()))
            } else if (hours <= 9) {
                list.add(R.drawable.ic_nothing_marker_5)
                list.add(getMarkerForDigit((hours - 5).toInt()))
            } else {
                val tens = (hours / 10).toInt().coerceIn(1, 5)
                val units = (hours % 10).toInt()
                list.add(getMarkerForDigit(tens))
                when (units) {
                    in 1..5 -> list.add(getMarkerForDigit(units))
                    in 6..9 -> {
                        list.add(R.drawable.ic_nothing_marker_5)
                        list.add(getMarkerForDigit(units - 5))
                    }
                }
            }
            return list
        }

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

        val geistMonoTypeface = getGeistTypeface(context, config.fontWeight)

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

        val showCalendar = config.showCalendar
        val displayEvent = eventTitle.uppercase().trim()

        // Row Heights and centers:
        // Tighten the line-height / row spacing slightly (~11% closer) so lines sit closer together
        // with cohesive Nothing OS typography, bounded by maxRowSpacing on tall aspect ratios,
        // and centered vertically within the card.
        val numRows = if (!showCalendar) 2.0f else 4.0f
        val rawUnit = availableHeight / numRows
        val maxFontSize = if (!showCalendar) safeW * 0.062f else safeW * 0.058f
        val baseFontSize = (rawUnit * 0.62f).coerceIn(16f, maxFontSize)
        val badgeHeight = (baseFontSize * 1.18f).coerceIn(20f, (rawUnit * 0.80f).coerceAtLeast(22f))

        val maxRowSpacing = baseFontSize * 1.52f
        val compactUnit = rawUnit * 0.89f
        val rowHeight = compactUnit.coerceAtMost(maxRowSpacing)
        val totalBlockHeight = rowHeight * numRows
        val verticalOffset = ((availableHeight - totalBlockHeight) / 2f).coerceAtLeast(0f)
        val effectiveTop = topPadding + verticalOffset

        val row1CenterY = effectiveTop + rowHeight * 0.5f
        val row2CenterY = effectiveTop + rowHeight * 1.5f
        val row3CenterY = if (showCalendar) effectiveTop + rowHeight * 2.5f else 0f
        val row4CenterY = if (showCalendar) effectiveTop + rowHeight * 3.5f else 0f

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
            typeface = geistMonoTypeface
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
                typeface = geistMonoTypeface
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

        // ================= ROWS 3 & 4: CALENDAR EVENT & COUNTDOWN =================
        if (showCalendar) {
            // ================= ROW 3: [Speak] EVENT OR QUIPPY PLACEHOLDER ( ➔ ) =================
            cursorX = contentLeft

            // 1. Speaking Head Profile Icon with Waves
            val speakW = drawAsset(context, canvas, R.drawable.ic_nothing_speak, cursorX, row3CenterY, targetHeight = badgeHeight * 0.88f, tintColor = textColor)
            cursorX += speakW + safeW * 0.024f

            // 2. Event Title or Quippy Placeholder (clean single-line with ellipsis)
            val followupWidth = badgeHeight * (16f / 11f)
            val maxEventWidth = (contentRight - cursorX - followupWidth - safeW * 0.028f).coerceAtLeast(safeW * 0.2f)
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
                isEventActive -> {
                    if (minutesLeft >= 60) {
                        val hours = ((minutesLeft + 30) / 60).coerceAtLeast(1)
                        if (hours <= 1L) "1 HOUR" else "$hours HOURS"
                    } else {
                        "${minutesLeft}MIN"
                    }
                }
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
            val lineWeightRatio = (config.fontWeight.weight.toFloat() / 700f).coerceIn(0.65f, 1.0f)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                style = Paint.Style.STROKE
                strokeWidth = (baseFontSize * 0.08f * lineWeightRatio).coerceIn(2.0f, 6.0f)
                strokeCap = Paint.Cap.ROUND
            }
            val textStartX = cursorX + cradleCornerW * 0.75f
            val lineEndX = (textStartX + minWidth + safeW * 0.022f).coerceAtMost(contentRight)

            canvas.drawLine(textStartX, baselineY, lineEndX, baselineY, linePaint)

            // Draw Countdown / Open text resting inside the cradle
            canvas.drawText(countdownText, textStartX + safeW * 0.005f, row4CenterY + textBaselineOffset - cradleHeight * 0.02f, textPaint)
        }

        return bitmap
    }
}
