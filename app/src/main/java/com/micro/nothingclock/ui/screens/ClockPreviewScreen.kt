package com.micro.nothingclock.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.micro.nothingclock.data.CalendarRepository
import com.micro.nothingclock.data.ClockThemePreset
import com.micro.nothingclock.data.UpcomingCalendarEvent
import com.micro.nothingclock.data.WeatherInfo
import com.micro.nothingclock.data.WeatherRepository
import com.micro.nothingclock.data.WidgetConfig
import com.micro.nothingclock.data.WidgetPreferences
import com.micro.nothingclock.ui.theme.GeistMonoFamily
import com.micro.nothingclock.ui.theme.NothingCard
import com.micro.nothingclock.ui.theme.NothingDark
import com.micro.nothingclock.ui.theme.NothingRed
import com.micro.nothingclock.ui.theme.TextMuted
import com.micro.nothingclock.widget.NothingClockRenderer
import com.micro.nothingclock.widget.NothingClockWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClockPreviewScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { WidgetPreferences(context) }
    var config by remember { mutableStateOf(prefs.loadConfig()) }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var previewHeight by remember { mutableIntStateOf(461) }
    val weatherRepo = remember { WeatherRepository(context) }
    var isWeatherRefreshing by remember { mutableStateOf(false) }

    // Auto-tick every 1s for live clock preview
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // Permission states
    var hasCalPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var canExactAlarm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.canScheduleExactAlarms() ?: true
            } else true
        )
    }

    // Real-time Lifecycle observer: automatically refreshes permission statuses when returning from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val calGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
                val wasCalGranted = hasCalPerm
                hasCalPerm = calGranted

                val locGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val wasLocGranted = hasLocPerm
                hasLocPerm = locGranted

                val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    alarmManager?.canScheduleExactAlarms() ?: true
                } else true
                val wasAlarmGranted = canExactAlarm
                canExactAlarm = alarmGranted

                if (alarmGranted && !wasAlarmGranted) {
                    NothingClockWidgetProvider.scheduleNextMinuteAlarm(context)
                    sendWidgetRefresh(context)
                }

                if (calGranted && !wasCalGranted && !config.useLiveCalendar) {
                    config = config.copy(useLiveCalendar = true)
                    prefs.saveConfig(config)
                    sendWidgetRefresh(context)
                }

                if (locGranted && !wasLocGranted && !config.useLiveWeather) {
                    config = config.copy(useLiveWeather = true)
                    prefs.saveConfig(config)
                    sendWidgetRefresh(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val calPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalPerm = granted
        if (granted) {
            config = config.copy(useLiveCalendar = true)
            prefs.saveConfig(config)
            sendWidgetRefresh(context)
            Toast.makeText(context, "Calendar access granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please enable Calendar in App Settings", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocPerm = granted
        if (granted) {
            config = config.copy(useLiveWeather = true)
            prefs.saveConfig(config)
            sendWidgetRefresh(context)
            Toast.makeText(context, "Location access granted for weather!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please enable Location in App Settings", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    val calendarRepo = remember { CalendarRepository(context) }
    val event = remember(currentTimeMillis, config.useLiveCalendar, hasCalPerm, config.fallbackEventTitle, config.fallbackMinutes) {
        if (config.useLiveCalendar && hasCalPerm) {
            calendarRepo.getNextUpcomingEvent(config.fallbackEventTitle, config.fallbackMinutes)
        } else {
            val isAllDayFallback = config.fallbackMinutes == -2
            UpcomingCalendarEvent(
                config.fallbackEventTitle,
                if (isAllDayFallback) -1L else config.fallbackMinutes.toLong(),
                hasEvent = config.fallbackMinutes != -1,
                isAllDay = isAllDayFallback
            )
        }
    }

    // Live weather state initialized from cache or fallback
    val isFahrenheit = config.tempUnit.contains("F", ignoreCase = true)
    var liveWeather by remember {
        mutableStateOf(
            weatherRepo.getCachedOrDefault(
                fallbackTemp = config.tempCelsius,
                fallbackCondition = config.weatherCondition,
                isFahrenheit = isFahrenheit
            )
        )
    }

    // Fetch live weather when enabled or when settings change
    LaunchedEffect(config.useLiveWeather, config.tempUnit, config.weatherRefreshIntervalMinutes, hasLocPerm) {
        if (config.useLiveWeather && hasLocPerm) {
            val isF = config.tempUnit.contains("F", ignoreCase = true)
            val result = weatherRepo.fetchWeather(
                isFahrenheit = isF,
                refreshIntervalMinutes = config.weatherRefreshIntervalMinutes,
                fallbackTemp = config.tempCelsius,
                fallbackCondition = config.weatherCondition
            )
            liveWeather = result
        }
    }

    val refreshWeatherAction: () -> Unit = {
        coroutineScope.launch {
            isWeatherRefreshing = true
            Toast.makeText(context, "Fetching latest weather...", Toast.LENGTH_SHORT).show()
            val isF = config.tempUnit.contains("F", ignoreCase = true)
            val result = weatherRepo.fetchWeather(
                isFahrenheit = isF,
                refreshIntervalMinutes = config.weatherRefreshIntervalMinutes,
                forceRefresh = true,
                fallbackTemp = config.tempCelsius,
                fallbackCondition = config.weatherCondition
            )
            liveWeather = result
            config = config.copy(tempCelsius = result.temperature, weatherCondition = result.condition)
            prefs.saveConfig(config)
            sendWidgetRefresh(context)
            isWeatherRefreshing = false
            Toast.makeText(context, "Weather updated: ${result.temperature}°${if (isF) "F" else "C"} ${result.condition}", Toast.LENGTH_SHORT).show()
        }
    }

    val displayTemp = if (config.useLiveWeather && hasLocPerm) liveWeather.temperature else config.tempCelsius
    val displayCond = if (config.useLiveWeather && hasLocPerm) liveWeather.condition else config.weatherCondition

    val previewBitmap = remember(currentTimeMillis, config, event, displayTemp, displayCond, previewHeight) {
        NothingClockRenderer.renderToBitmap(
            context = context,
            width = 1024,
            height = previewHeight,
            config = config,
            eventTitle = event.title,
            minutesLeft = event.minutesUntilStart,
            temp = displayTemp,
            condition = displayCond,
            isAllDay = event.isAllDay
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingDark)
    ) {
        val isTabletLandscape = maxWidth >= 840.dp
        val isCompactWidth = maxWidth < 400.dp
        val horizontalPadding = if (isCompactWidth) 12.dp else 20.dp

        val onManualRefresh: () -> Unit = {
            hasCalPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED

            hasLocPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.canScheduleExactAlarms() ?: true
            } else true

            if (canExactAlarm) {
                NothingClockWidgetProvider.scheduleNextMinuteAlarm(context)
            }
            if (config.useLiveWeather && hasLocPerm) {
                refreshWeatherAction()
            } else {
                sendWidgetRefresh(context)
                Toast.makeText(context, "Widget refreshed!", Toast.LENGTH_SHORT).show()
            }
        }

        if (isTabletLandscape) {
            // Tablet Landscape & Desktop: 2-pane dashboard
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Pane: Sticky Preview, Header, and Add Button
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppHeader(context = context, onManualRefresh = onManualRefresh)
                    PreviewSizeSelector(
                        selectedHeight = previewHeight,
                        onHeightSelected = { previewHeight = it }
                    )
                    LivePreviewCard(
                        config = config,
                        bitmap = previewBitmap,
                        previewHeight = previewHeight
                    )
                    AddToHomeScreenButton(context)
                }

                // Right Pane: Settings & Customizers
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PermissionsCard(
                        context = context,
                        hasCalPerm = hasCalPerm,
                        calPermLauncher = calPermLauncher,
                        hasLocPerm = hasLocPerm,
                        locPermLauncher = locPermLauncher,
                        canExactAlarm = canExactAlarm
                    )
                    ThemeCustomizerCard(
                        config = config,
                        onConfigChanged = {
                            config = it
                            prefs.saveConfig(it)
                            sendWidgetRefresh(context)
                        }
                    )
                    CalendarCustomizerCard(
                        config = config,
                        hasCalPerm = hasCalPerm,
                        calPermLauncher = calPermLauncher,
                        calendarRepo = calendarRepo,
                        onConfigChanged = {
                            config = it
                            prefs.saveConfig(it)
                            sendWidgetRefresh(context)
                        }
                    )
                    WeatherCustomizerCard(
                        config = config,
                        hasLocPerm = hasLocPerm,
                        locPermLauncher = locPermLauncher,
                        liveWeather = liveWeather,
                        isWeatherRefreshing = isWeatherRefreshing,
                        onRefreshWeather = refreshWeatherAction,
                        onConfigChanged = {
                            config = it
                            prefs.saveConfig(it)
                            sendWidgetRefresh(context)
                        }
                    )
                }
            }
        } else {
            // Phone, Tablet Portrait, Foldable, & Small Square Screens
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppHeader(context = context, onManualRefresh = onManualRefresh)
                    PreviewSizeSelector(
                        selectedHeight = previewHeight,
                        onHeightSelected = { previewHeight = it }
                    )
                    LivePreviewCard(
                        config = config,
                        bitmap = previewBitmap,
                        previewHeight = previewHeight
                    )
                    AddToHomeScreenButton(context)
                    PermissionsCard(
                        context = context,
                        hasCalPerm = hasCalPerm,
                        calPermLauncher = calPermLauncher,
                        hasLocPerm = hasLocPerm,
                        locPermLauncher = locPermLauncher,
                        canExactAlarm = canExactAlarm
                    )
                    ThemeCustomizerCard(
                        config = config,
                        onConfigChanged = {
                            config = it
                            prefs.saveConfig(it)
                            sendWidgetRefresh(context)
                        }
                    )
                    CalendarCustomizerCard(
                        config = config,
                        hasCalPerm = hasCalPerm,
                        calPermLauncher = calPermLauncher,
                        calendarRepo = calendarRepo,
                        onConfigChanged = {
                            config = it
                            prefs.saveConfig(it)
                            sendWidgetRefresh(context)
                        }
                    )
                    WeatherCustomizerCard(
                        config = config,
                        hasLocPerm = hasLocPerm,
                        locPermLauncher = locPermLauncher,
                        liveWeather = liveWeather,
                        isWeatherRefreshing = isWeatherRefreshing,
                        onRefreshWeather = refreshWeatherAction,
                        onConfigChanged = {
                            config = it
                            prefs.saveConfig(it)
                            sendWidgetRefresh(context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    context: Context,
    onManualRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(NothingRed)
            )
            Text(
                text = "MICRONOTHING",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        IconButton(onClick = onManualRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewSizeSelector(
    selectedHeight: Int,
    onHeightSelected: (Int) -> Unit
) {
    val sizes = listOf(
        "4x2 Standard" to 461,
        "4x3 Tall" to 700,
        "4x4 Square" to 1024,
        "5x2 Wide" to 360,
        "3x2 Compact" to 600
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sizes.forEach { (label, h) ->
            val isSelected = selectedHeight == h
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) NothingRed else Color.White.copy(alpha = 0.08f))
                    .clickable { onHeightSelected(h) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontFamily = GeistMonoFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun LivePreviewCard(
    config: WidgetConfig,
    bitmap: Bitmap,
    previewHeight: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                when (config.themePreset) {
                    ClockThemePreset.OS5_BLUEPRINT -> Color(0xFF92A6B8)
                    ClockThemePreset.MATERIAL_YOU -> Color(0xFF262C38)
                    ClockThemePreset.FROSTED_GLASS -> Color(0xFF384353)
                    ClockThemePreset.AMOLED_DARK -> Color(0xFF000000)
                    ClockThemePreset.NOTHING_RED -> Color(0xFF140506)
                    ClockThemePreset.TRANSPARENT_FLOATING -> Color(0xFF4A5568)
                }
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "MicroNothing Clock Face",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1024f / previewHeight.toFloat())
        )
    }
}

@Composable
private fun AddToHomeScreenButton(context: Context) {
    Button(
        onClick = { pinAppWidget(context) },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "ADD WIDGET TO HOMESCREEN",
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PermissionsCard(
    context: Context,
    hasCalPerm: Boolean,
    calPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    hasLocPerm: Boolean,
    locPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    canExactAlarm: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = NothingRed, modifier = Modifier.size(18.dp))
                Text(
                    text = "PERMISSIONS & SERVICES",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            PermissionStatusRow(
                title = "Calendar Access",
                desc = "Required to sync next meeting & dynamic hand countdown",
                isGranted = hasCalPerm,
                onGrantClick = { calPermLauncher.launch(Manifest.permission.READ_CALENDAR) }
            )

            PermissionStatusRow(
                title = "Location for Weather",
                desc = "Used to fetch local temperature & conditions via Open-Meteo",
                isGranted = hasLocPerm,
                onGrantClick = { locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionStatusRow(
                    title = "Exact Alarms (Minute Tick)",
                    desc = "Ensures the widget updates right on the minute boundary",
                    isGranted = canExactAlarm,
                    onGrantClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeCustomizerCard(
    config: WidgetConfig,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = NothingRed, modifier = Modifier.size(18.dp))
                Text(
                    text = "THEME & GLASS CUSTOMIZER",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClockThemePreset.entries.forEach { preset ->
                    val isSelected = config.themePreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NothingRed.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.35f))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NothingRed else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onConfigChanged(config.copy(themePreset = preset)) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName,
                            fontSize = 11.sp,
                            fontFamily = GeistMonoFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }

            // Opacity Slider
            Text(
                text = "Frosted Card Opacity: ${config.cardOpacityPercent}%",
                fontFamily = GeistMonoFamily,
                fontSize = 12.sp,
                color = TextMuted
            )
            Slider(
                value = config.cardOpacityPercent.toFloat(),
                onValueChange = { onConfigChanged(config.copy(cardOpacityPercent = it.toInt())) },
                valueRange = 20f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = NothingRed,
                    activeTrackColor = NothingRed,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            // Corner Radius Slider
            Text(
                text = "Corner Radius: ${config.cornerRadiusDp}dp",
                fontFamily = GeistMonoFamily,
                fontSize = 12.sp,
                color = TextMuted
            )
            Slider(
                value = config.cornerRadiusDp.toFloat(),
                onValueChange = { onConfigChanged(config.copy(cornerRadiusDp = it.toInt())) },
                valueRange = 16f..40f,
                colors = SliderDefaults.colors(
                    thumbColor = NothingRed,
                    activeTrackColor = NothingRed,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            // Cursor & 24h Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Terminal Cursor (█)", fontFamily = GeistMonoFamily, fontSize = 12.sp, color = Color.White)
                Switch(
                    checked = config.showCursor,
                    onCheckedChange = { onConfigChanged(config.copy(showCursor = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NothingRed)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24-Hour Time Format", fontFamily = GeistMonoFamily, fontSize = 12.sp, color = Color.White)
                Switch(
                    checked = config.is24Hour,
                    onCheckedChange = { onConfigChanged(config.copy(is24Hour = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NothingRed)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarCustomizerCard(
    config: WidgetConfig,
    hasCalPerm: Boolean,
    calPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    calendarRepo: CalendarRepository,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(18.dp))
                    Text(
                        text = "CALENDAR & GESTURE TALLY",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Switch(
                    checked = config.useLiveCalendar,
                    onCheckedChange = { enableLive ->
                        if (enableLive && !hasCalPerm) {
                            calPermLauncher.launch(Manifest.permission.READ_CALENDAR)
                        } else {
                            onConfigChanged(config.copy(useLiveCalendar = enableLive))
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NothingRed)
                )
            }

            Text(
                text = if (config.useLiveCalendar) "✓ Reading next upcoming event live from device Calendar" else "Using manual custom preview event",
                fontSize = 11.sp,
                fontFamily = GeistMonoFamily,
                color = TextMuted
            )

            if (!config.useLiveCalendar) {
                OutlinedTextField(
                    value = config.fallbackEventTitle,
                    onValueChange = { onConfigChanged(config.copy(fallbackEventTitle = it)) },
                    label = { Text("Event Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = config.fallbackMinutes.toString(),
                        onValueChange = {
                            val mins = it.toIntOrNull() ?: 0
                            onConfigChanged(config.copy(fallbackMinutes = mins))
                        },
                        label = { Text("Minutes Left") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Text(
                        text = "Hands: ${NothingClockRenderer.calculateHandGestures(config.fallbackMinutes.toLong(), isAllDay = config.fallbackMinutes == -2)}",
                        fontFamily = GeistMonoFamily,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onConfigChanged(config.copy(fallbackEventTitle = "WEEKLY DESIGN SYNC", fallbackMinutes = 38)) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("38MIN", fontSize = 10.sp, fontFamily = GeistMonoFamily, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { onConfigChanged(config.copy(fallbackEventTitle = "WEEKLY DESIGN SYNC", fallbackMinutes = 49)) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("49MIN", fontSize = 10.sp, fontFamily = GeistMonoFamily, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { onConfigChanged(config.copy(fallbackEventTitle = "MOM'S BIRTHDAY", fallbackMinutes = -2)) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("All Day", fontSize = 10.sp, fontFamily = GeistMonoFamily, color = Color(0xFF29B6F6))
                    }

                    OutlinedButton(
                        onClick = {
                            val quippy = calendarRepo.getRandomQuippyPlaceholder()
                            onConfigChanged(config.copy(fallbackEventTitle = quippy, fallbackMinutes = -1))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open", fontSize = 10.sp, fontFamily = GeistMonoFamily, color = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherCustomizerCard(
    config: WidgetConfig,
    hasLocPerm: Boolean,
    locPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    liveWeather: WeatherInfo?,
    isWeatherRefreshing: Boolean,
    onRefreshWeather: () -> Unit,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NothingCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                    Text(
                        text = "WEATHER & CONDITIONS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Switch(
                    checked = config.useLiveWeather,
                    onCheckedChange = { enableLive ->
                        if (enableLive && !hasLocPerm) {
                            locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        } else {
                            onConfigChanged(config.copy(useLiveWeather = enableLive))
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NothingRed)
                )
            }

            if (config.useLiveWeather) {
                // Live Weather Status Card
                if (hasLocPerm) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(15.dp))
                                Text(
                                    text = "LIVE WEATHER ACTIVE",
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${liveWeather?.temperature ?: config.tempCelsius}°${config.tempUnit} ${liveWeather?.condition ?: config.weatherCondition} • Open-Meteo",
                                fontFamily = GeistMonoFamily,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onRefreshWeather,
                            enabled = !isWeatherRefreshing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWeatherRefreshing) Color.Gray else NothingRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isWeatherRefreshing) "SYNCING" else "REFRESH",
                                fontFamily = GeistMonoFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Red.copy(alpha = 0.15f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LOCATION PERMISSION REQUIRED",
                                fontFamily = GeistMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NothingRed
                            )
                            Text(
                                text = "Allow location access to fetch live weather for your area",
                                fontFamily = GeistMonoFamily,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        Button(
                            onClick = { locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                            colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("ENABLE", fontFamily = GeistMonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Temperature Unit Selector (°F / °C)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Temperature Unit",
                        fontFamily = GeistMonoFamily,
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("°F", "°C").forEach { unit ->
                            val isSelected = config.tempUnit == unit
                            Box(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NothingRed else Color.Transparent)
                                    .clickable {
                                        if (unit != config.tempUnit) {
                                            val converted = if (unit == "°F") {
                                                (config.tempCelsius * 9 / 5 + 32)
                                            } else {
                                                ((config.tempCelsius - 32) * 5 / 9)
                                            }
                                            onConfigChanged(config.copy(tempUnit = unit, tempCelsius = converted))
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit,
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Refresh Interval Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "WEATHER REFRESH INTERVAL",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )

                    val intervals = listOf(
                        1 to "1 MIN",
                        5 to "5 MIN",
                        15 to "15 MIN",
                        30 to "30 MIN",
                        60 to "1 HOUR"
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.forEach { (mins, label) ->
                            val isSelected = config.weatherRefreshIntervalMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NothingRed else Color.White.copy(alpha = 0.08f))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NothingRed else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onConfigChanged(config.copy(weatherRefreshIntervalMinutes = mins)) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Text(
                        text = when (config.weatherRefreshIntervalMinutes) {
                            1 -> "Refreshes live on every minute tick (real-time precision)"
                            5 -> "Refreshes weather every 5 minutes"
                            15 -> "Refreshes weather every 15 minutes (recommended)"
                            30 -> "Refreshes weather every 30 minutes"
                            60 -> "Refreshes weather every 1 hour (maximum battery saving)"
                            else -> "Refreshes every ${config.weatherRefreshIntervalMinutes} minutes"
                        },
                        fontSize = 10.sp,
                        fontFamily = GeistMonoFamily,
                        color = TextMuted
                    )
                }
            } else {
                // Manual weather mode
                Text(
                    text = "Using manual preview weather (switch toggle on top-right for live)",
                    fontSize = 11.sp,
                    fontFamily = GeistMonoFamily,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = config.tempCelsius.toString(),
                        onValueChange = {
                            val temp = it.toIntOrNull() ?: 72
                            onConfigChanged(config.copy(tempCelsius = temp))
                        },
                        label = { Text("Temperature") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("°F", "°C").forEach { unit ->
                            val isSelected = config.tempUnit == unit
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NothingRed else Color.Transparent)
                                    .clickable {
                                        if (unit != config.tempUnit) {
                                            val converted = if (unit == "°F") {
                                                (config.tempCelsius * 9 / 5 + 32)
                                            } else {
                                                ((config.tempCelsius - 32) * 5 / 9)
                                            }
                                            onConfigChanged(config.copy(tempUnit = unit, tempCelsius = converted))
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit,
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = config.weatherCondition,
                    onValueChange = { onConfigChanged(config.copy(weatherCondition = it.uppercase())) },
                    label = { Text("Condition") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("SUNNY", "RAINY", "CLOUDY", "SNOW", "THUNDER", "BREEZY", "HAZE").forEach { cond ->
                        val isSelected = config.weatherCondition.contains(cond)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NothingRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NothingRed else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onConfigChanged(config.copy(weatherCondition = cond)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cond,
                                fontSize = 10.sp,
                                fontFamily = GeistMonoFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
            Text(
                text = desc,
                fontFamily = GeistMonoFamily,
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Text(text = "GRANTED", fontFamily = GeistMonoFamily, fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "ENABLE", fontFamily = GeistMonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun pinAppWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, NothingClockWidgetProvider::class.java)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
            Toast.makeText(context, "Adding MicroNothing Widget to Home Screen...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Pinning not supported by your launcher. Long-press your home screen to add manually.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Long-press your home screen and select 'MicroNothing'.", Toast.LENGTH_LONG).show()
    }
}

private fun sendWidgetRefresh(context: Context) {
    val intent = Intent(context, NothingClockWidgetProvider::class.java).apply {
        action = "com.micro.nothingclock.ACTION_REFRESH_WIDGET"
    }
    context.sendBroadcast(intent)
}
