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
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.micro.nothingclock.data.AppThemeMode
import com.micro.nothingclock.data.CalendarRepository
import com.micro.nothingclock.data.ClockThemePreset
import com.micro.nothingclock.data.UpcomingCalendarEvent
import com.micro.nothingclock.data.WeatherInfo
import com.micro.nothingclock.data.WeatherRepository
import com.micro.nothingclock.data.WidgetConfig
import com.micro.nothingclock.data.WidgetFontWeight
import com.micro.nothingclock.data.WidgetPreferences
import com.micro.nothingclock.ui.theme.GeistMonoFamily
import com.micro.nothingclock.widget.NothingClockRenderer
import com.micro.nothingclock.widget.NothingClockWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClockPreviewScreen(
    initialConfig: WidgetConfig? = null,
    onAppConfigChanged: ((WidgetConfig) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { WidgetPreferences(context) }
    var config by remember { mutableStateOf(initialConfig ?: prefs.loadConfig()) }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var previewHeight by remember { mutableIntStateOf(461) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val weatherRepo = remember { WeatherRepository(context) }
    var isWeatherRefreshing by remember { mutableStateOf(false) }
    var showWalkthrough by remember { mutableStateOf(!prefs.hasCompletedWalkthrough()) }

    val onGrantExactAlarm: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Please enable exact alarms in system settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateConfig(newConfig: WidgetConfig) {
        config = newConfig
        prefs.saveConfig(newConfig)
        onAppConfigChanged?.invoke(newConfig)
        sendWidgetRefresh(context)
    }

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

                if (calGranted && !wasCalGranted && !config.useLiveCalendar && config.showCalendar) {
                    updateConfig(config.copy(useLiveCalendar = true))
                }

                if (locGranted && !wasLocGranted && !config.useLiveWeather) {
                    updateConfig(config.copy(useLiveWeather = true))
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
            updateConfig(config.copy(showCalendar = true, useLiveCalendar = true))
            Toast.makeText(context, "Calendar access granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please enable Calendar in App Settings", Toast.LENGTH_SHORT).show()
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocPerm = granted
        if (granted) {
            updateConfig(config.copy(useLiveWeather = true))
            Toast.makeText(context, "Location access granted for weather!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please enable Location in App Settings", Toast.LENGTH_SHORT).show()
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    val calendarRepo = remember { CalendarRepository(context) }
    val event = remember(currentTimeMillis, config.showCalendar, config.useLiveCalendar, hasCalPerm, config.fallbackEventTitle, config.fallbackMinutes) {
        if (!config.showCalendar) {
            UpcomingCalendarEvent("", -1L, hasEvent = false, isAllDay = false)
        } else if (config.useLiveCalendar && hasCalPerm) {
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
            updateConfig(config.copy(tempCelsius = result.temperature, weatherCondition = result.condition))
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isTabletLandscape = maxWidth >= 840.dp
        val isCompactWidth = maxWidth < 400.dp
        val horizontalPadding = if (isCompactWidth) 12.dp else 20.dp

        if (isTabletLandscape) {
            // Tablet Landscape & Desktop: 2-pane dashboard
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Pane: Sticky Preview, Header, and Add Button
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppHeader(
                        config = config,
                        onConfigChanged = { updateConfig(it) },
                        onManualRefresh = onManualRefresh,
                        onLaunchWalkthrough = { showWalkthrough = true }
                    )
                    PreviewSizeDropdown(
                        selectedHeight = previewHeight,
                        onHeightSelected = { previewHeight = it }
                    )
                    LivePreviewCard(
                        config = config,
                        bitmap = previewBitmap,
                        previewHeight = previewHeight
                    )
                    AddToHomeScreenButton(
                        context = context,
                        config = config,
                        previewHeight = previewHeight
                    )
                }

                // Right Pane: Categorized Tab Panels
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                ) {
                    CategoryTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedTab) {
                            0 -> StyleAndFaceTab(
                                config = config,
                                onConfigChanged = { updateConfig(it) }
                            )
                            1 -> CalendarTab(
                                config = config,
                                hasCalPerm = hasCalPerm,
                                calPermLauncher = calPermLauncher,
                                calendarRepo = calendarRepo,
                                onConfigChanged = { updateConfig(it) }
                            )
                            2 -> WeatherTab(
                                config = config,
                                hasLocPerm = hasLocPerm,
                                locPermLauncher = locPermLauncher,
                                liveWeather = liveWeather,
                                isWeatherRefreshing = isWeatherRefreshing,
                                onRefreshWeather = refreshWeatherAction,
                                onConfigChanged = { updateConfig(it) }
                            )
                            3 -> SystemAndThemeTab(
                                context = context,
                                config = config,
                                hasCalPerm = hasCalPerm,
                                calPermLauncher = calPermLauncher,
                                hasLocPerm = hasLocPerm,
                                locPermLauncher = locPermLauncher,
                                canExactAlarm = canExactAlarm,
                                onConfigChanged = { updateConfig(it) },
                                onManualRefresh = onManualRefresh,
                                onLaunchWalkthrough = { showWalkthrough = true }
                            )
                        }
                    }
                }
            }
        } else {
            // Phone, Tablet Portrait, & Foldable
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 6.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppHeader(
                        config = config,
                        onConfigChanged = { updateConfig(it) },
                        onManualRefresh = onManualRefresh,
                        onLaunchWalkthrough = { showWalkthrough = true }
                    )

                    LivePreviewCard(
                        config = config,
                        bitmap = previewBitmap,
                        previewHeight = previewHeight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PreviewSizeDropdown(
                                selectedHeight = previewHeight,
                                onHeightSelected = { previewHeight = it }
                            )
                        }
                        AddToHomeScreenButton(
                            context = context,
                            config = config,
                            previewHeight = previewHeight,
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    CategoryTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    when (selectedTab) {
                        0 -> StyleAndFaceTab(
                            config = config,
                            onConfigChanged = { updateConfig(it) }
                        )
                        1 -> CalendarTab(
                            config = config,
                            hasCalPerm = hasCalPerm,
                            calPermLauncher = calPermLauncher,
                            calendarRepo = calendarRepo,
                            onConfigChanged = { updateConfig(it) }
                        )
                        2 -> WeatherTab(
                            config = config,
                            hasLocPerm = hasLocPerm,
                            locPermLauncher = locPermLauncher,
                            liveWeather = liveWeather,
                            isWeatherRefreshing = isWeatherRefreshing,
                            onRefreshWeather = refreshWeatherAction,
                            onConfigChanged = { updateConfig(it) }
                        )
                        3 -> SystemAndThemeTab(
                            context = context,
                            config = config,
                            hasCalPerm = hasCalPerm,
                            calPermLauncher = calPermLauncher,
                            hasLocPerm = hasLocPerm,
                            locPermLauncher = locPermLauncher,
                            canExactAlarm = canExactAlarm,
                            onConfigChanged = { updateConfig(it) },
                            onManualRefresh = onManualRefresh,
                            onLaunchWalkthrough = { showWalkthrough = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = showWalkthrough,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            PermissionWalkthrough(
                hasCalPerm = hasCalPerm,
                onGrantCalPerm = { calPermLauncher.launch(Manifest.permission.READ_CALENDAR) },
                hasLocPerm = hasLocPerm,
                onGrantLocPerm = { locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                canExactAlarm = canExactAlarm,
                onGrantExactAlarm = onGrantExactAlarm,
                onPinWidget = { pinAppWidget(context, config, previewHeight) },
                onDismiss = {
                    prefs.setWalkthroughCompleted(true)
                    showWalkthrough = false
                }
            )
        }
    }
}

@Composable
private fun AppHeader(
    config: WidgetConfig,
    onConfigChanged: (WidgetConfig) -> Unit,
    onManualRefresh: () -> Unit,
    onLaunchWalkthrough: () -> Unit
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column {
                Text(
                    text = "MICRONOTHING",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "CLOCK COMPANION & STUDIO",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Theme Mode Selector Button & Dropdown
            Box {
                FilledTonalIconButton(
                    onClick = { themeMenuExpanded = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    val icon = when (config.appThemeMode) {
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    }
                    Icon(
                        icon,
                        contentDescription = "Switch Theme Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Follow System", fontFamily = GeistMonoFamily, fontSize = 12.sp)
                                if (config.appThemeMode == AppThemeMode.SYSTEM) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            onConfigChanged(config.copy(appThemeMode = AppThemeMode.SYSTEM))
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Light Mode", fontFamily = GeistMonoFamily, fontSize = 12.sp)
                                if (config.appThemeMode == AppThemeMode.LIGHT) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            onConfigChanged(config.copy(appThemeMode = AppThemeMode.LIGHT))
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Dark Mode", fontFamily = GeistMonoFamily, fontSize = 12.sp)
                                if (config.appThemeMode == AppThemeMode.DARK) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            onConfigChanged(config.copy(appThemeMode = AppThemeMode.DARK))
                            themeMenuExpanded = false
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Dynamic Color", fontFamily = GeistMonoFamily, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = config.useDynamicColor,
                                    onCheckedChange = {
                                        onConfigChanged(config.copy(useDynamicColor = it))
                                    },
                                    modifier = Modifier.size(width = 36.dp, height = 24.dp)
                                )
                            }
                        },
                        onClick = {
                            onConfigChanged(config.copy(useDynamicColor = !config.useDynamicColor))
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Setup Walkthrough", fontFamily = GeistMonoFamily, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            themeMenuExpanded = false
                            onLaunchWalkthrough()
                        }
                    )
                }
            }

            // Sync / Refresh Action Button
            FilledTonalIconButton(
                onClick = onManualRefresh,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Sync Widgets & Data",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
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
    val wallpaperBg = when (config.themePreset) {
        ClockThemePreset.OS5_BLUEPRINT -> Color(0xFF8B9FB0)
        ClockThemePreset.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceContainerHighest
        ClockThemePreset.FROSTED_GLASS -> Color(0xFF2C3545)
        ClockThemePreset.AMOLED_DARK -> Color(0xFF0A0A0C)
        ClockThemePreset.NOTHING_RED -> Color(0xFF1B0709)
        ClockThemePreset.TRANSPARENT_FLOATING -> Color(0xFF3E4857)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(wallpaperBg)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "MicroNothing Clock Face Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1024f / previewHeight.toFloat())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 6.dp, end = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "LIVE CLOCK CANVAS",
                        fontFamily = GeistMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${config.themePreset.displayName} • ${if (config.is24Hour) "24H" else "12H"}",
                    fontFamily = GeistMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewSizeDropdown(
    selectedHeight: Int,
    onHeightSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val sizes = listOf(
        "4x2 Standard (Recommended)" to 461,
        "4x1 Slim Ribbon" to 260,
        "4x3 Tall Grid" to 700,
        "4x4 Full Square" to 1024,
        "5x2 Wide Landscape" to 360,
        "3x2 Compact Card" to 600
    )

    val currentLabel = sizes.find { it.second == selectedHeight }?.first ?: "4x2 Standard"

    Box {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = currentLabel.substringBefore(" ("),
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Sizes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            sizes.forEach { (label, height) ->
                val isSelected = selectedHeight == height
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontFamily = GeistMonoFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onHeightSelected(height)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddToHomeScreenButton(
    context: Context,
    config: WidgetConfig,
    previewHeight: Int,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { pinAppWidget(context, config, previewHeight) },
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "ADD WIDGET",
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CategoryTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Style" to Icons.Default.Palette,
        "Calendar" to Icons.Default.CalendarMonth,
        "Weather" to Icons.Default.Cloud,
        "System" to Icons.Default.Settings
    )

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                height = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, (title, icon) ->
            val isSelected = selectedTab == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontFamily = GeistMonoFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                },
                icon = {
                    Icon(
                        icon,
                        contentDescription = title,
                        modifier = Modifier.size(18.dp)
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleAndFaceTab(
    config: WidgetConfig,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Presets Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "THEME PRESETS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockThemePreset.entries.forEach { preset ->
                        val isSelected = config.themePreset == preset
                        val animatedBg by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            label = "presetBg"
                        )
                        val animatedBorder by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            label = "presetBorder"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(animatedBg)
                                .border(width = if (isSelected) 1.5.dp else 1.dp, color = animatedBorder, shape = RoundedCornerShape(12.dp))
                                .clickable { onConfigChanged(config.copy(themePreset = preset)) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = preset.displayName,
                                    fontSize = 11.sp,
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Geometry & Glassmorphism Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "GEOMETRY & GLASSMORPHISM",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Opacity Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Card Translucency",
                            fontFamily = GeistMonoFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${config.cardOpacityPercent}%",
                            fontFamily = GeistMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = config.cardOpacityPercent.toFloat(),
                        onValueChange = { onConfigChanged(config.copy(cardOpacityPercent = it.toInt())) },
                        valueRange = 20f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }

                // Corner Radius Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Corner Curvature",
                            fontFamily = GeistMonoFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${config.cornerRadiusDp} dp",
                            fontFamily = GeistMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = config.cornerRadiusDp.toFloat(),
                        onValueChange = { onConfigChanged(config.copy(cornerRadiusDp = it.toInt())) },
                        valueRange = 16f..40f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }

        // Widget Font Weight Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "WIDGET FONT WEIGHT",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Adjust the glyph thickness of the widget text. Nothing OS 5.0 micrographics widgets feature a slightly thinner weight (Medium or Regular). Bold is the classic default.",
                    fontFamily = GeistMonoFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetFontWeight.entries.forEach { weightOption ->
                        val isSelected = config.fontWeight == weightOption
                        val animatedBg by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            label = "weightBg"
                        )
                        val animatedBorder by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            label = "weightBorder"
                        )

                        val composeWeight = when (weightOption) {
                            WidgetFontWeight.LIGHT -> FontWeight.Light
                            WidgetFontWeight.REGULAR -> FontWeight.Normal
                            WidgetFontWeight.MEDIUM -> FontWeight.Medium
                            WidgetFontWeight.SEMI_BOLD -> FontWeight.SemiBold
                            WidgetFontWeight.BOLD -> FontWeight.Bold
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(animatedBg)
                                .border(width = if (isSelected) 1.5.dp else 1.dp, color = animatedBorder, shape = RoundedCornerShape(12.dp))
                                .clickable { onConfigChanged(config.copy(fontWeight = weightOption)) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = weightOption.displayName,
                                    fontSize = 12.sp,
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = composeWeight,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (config.fontWeight.description.isNotEmpty()) {
                    Text(
                        text = "• ${config.fontWeight.displayName}: ${config.fontWeight.description}",
                        fontFamily = GeistMonoFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Typography & Format Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "CLOCK DISPLAY FORMAT",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text("24-Hour Time Format", fontFamily = GeistMonoFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Military 00:00 - 23:59 display", fontFamily = GeistMonoFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.is24Hour,
                        onCheckedChange = { onConfigChanged(config.copy(is24Hour = it)) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text("Terminal Cursor (█)", fontFamily = GeistMonoFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Retro command-line cursor on time block", fontFamily = GeistMonoFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.showCursor,
                        onCheckedChange = { onConfigChanged(config.copy(showCursor = it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarTab(
    config: WidgetConfig,
    hasCalPerm: Boolean,
    calPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    calendarRepo: CalendarRepository,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Master switch card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "CALENDAR & AGENDA",
                            fontFamily = GeistMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = config.showCalendar,
                        onCheckedChange = { onConfigChanged(config.copy(showCalendar = it)) }
                    )
                }

                if (!config.showCalendar) {
                    Text(
                        text = "Calendar row is hidden on the widget. Displays time & weather only.",
                        fontSize = 11.sp,
                        fontFamily = GeistMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (config.showCalendar) {
            // Live Calendar Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Live Device Calendar",
                                fontFamily = GeistMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reads next upcoming event automatically",
                                fontFamily = GeistMonoFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            }
                        )
                    }

                    if (config.useLiveCalendar && !hasCalPerm) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Permission Required for Calendar Sync",
                                    fontFamily = GeistMonoFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Button(
                                    onClick = { calPermLauncher.launch(Manifest.permission.READ_CALENDAR) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("GRANT", fontFamily = GeistMonoFamily, fontSize = 10.sp)
                                }
                            }
                        }
                    } else if (config.useLiveCalendar) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(15.dp))
                            Text(
                                text = "Connected to Android Calendar provider",
                                fontFamily = GeistMonoFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Manual Event & Hand Gesture Simulator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (config.useLiveCalendar) "FALLBACK / SIMULATOR EVENT" else "MANUAL PREVIEW EVENT",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = config.fallbackEventTitle,
                        onValueChange = { onConfigChanged(config.copy(fallbackEventTitle = it)) },
                        label = { Text("Event Name / Headline") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "HAND TALLY",
                                    fontFamily = GeistMonoFamily,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = NothingClockRenderer.calculateHandGestures(config.fallbackMinutes.toLong(), isAllDay = config.fallbackMinutes == -2),
                                    fontFamily = GeistMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Text(
                        text = "QUICK SCENARIOS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onConfigChanged(config.copy(fallbackEventTitle = "WEEKLY DESIGN SYNC", fallbackMinutes = 38)) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("38 MIN", fontSize = 10.sp, fontFamily = GeistMonoFamily)
                        }

                        OutlinedButton(
                            onClick = { onConfigChanged(config.copy(fallbackEventTitle = "STANDUP REVIEW", fallbackMinutes = 49)) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("49 MIN", fontSize = 10.sp, fontFamily = GeistMonoFamily)
                        }

                        OutlinedButton(
                            onClick = { onConfigChanged(config.copy(fallbackEventTitle = "FLIGHT DEPARTURE", fallbackMinutes = 300)) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("5 HOURS", fontSize = 10.sp, fontFamily = GeistMonoFamily)
                        }

                        OutlinedButton(
                            onClick = { onConfigChanged(config.copy(fallbackEventTitle = "MOM'S BIRTHDAY", fallbackMinutes = -2)) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ALL DAY", fontSize = 10.sp, fontFamily = GeistMonoFamily)
                        }

                        OutlinedButton(
                            onClick = {
                                val quippy = calendarRepo.getRandomQuippyPlaceholder()
                                onConfigChanged(config.copy(fallbackEventTitle = quippy, fallbackMinutes = -1))
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("OPEN / QUIPPY", fontSize = 10.sp, fontFamily = GeistMonoFamily)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherTab(
    config: WidgetConfig,
    hasLocPerm: Boolean,
    locPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    liveWeather: WeatherInfo?,
    isWeatherRefreshing: Boolean,
    onRefreshWeather: () -> Unit,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Master Live Weather Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = "LIVE OPEN-METEO WEATHER",
                                fontFamily = GeistMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatic location forecast & condition icons",
                                fontFamily = GeistMonoFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = config.useLiveWeather,
                        onCheckedChange = { enableLive ->
                            if (enableLive && !hasLocPerm) {
                                locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            } else {
                                onConfigChanged(config.copy(useLiveWeather = enableLive))
                            }
                        }
                    )
                }

                if (config.useLiveWeather) {
                    if (hasLocPerm) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                            text = "FORECAST ACTIVE",
                                            fontFamily = GeistMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${liveWeather?.temperature ?: config.tempCelsius}°${config.tempUnit} ${liveWeather?.condition ?: config.weatherCondition} • Open-Meteo API",
                                        fontFamily = GeistMonoFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Button(
                                    onClick = onRefreshWeather,
                                    enabled = !isWeatherRefreshing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isWeatherRefreshing) "SYNCING" else "SYNC",
                                        fontFamily = GeistMonoFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Location Permission Needed",
                                        fontFamily = GeistMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Allow approximate location to query local weather",
                                        fontFamily = GeistMonoFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Button(
                                    onClick = { locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("ENABLE", fontFamily = GeistMonoFamily, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Units and Refresh Interval Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Temperature Unit Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Temperature Unit",
                            fontFamily = GeistMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fahrenheit (°F) or Celsius (°C)",
                            fontFamily = GeistMonoFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("°F", "°C").forEach { unit ->
                            val isSelected = config.tempUnit == unit
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (config.useLiveWeather) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Refresh Interval
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "BACKGROUND WEATHER SYNC INTERVAL",
                            fontFamily = GeistMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val intervals = listOf(
                            1 to "1 MIN",
                            5 to "5 MIN",
                            15 to "15 MIN (REC)",
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
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual Forecast Override Card (when live weather is disabled)
        if (!config.useLiveWeather) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "MANUAL PREVIEW CONDITIONS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = config.tempCelsius.toString(),
                        onValueChange = {
                            val temp = it.toIntOrNull() ?: 72
                            onConfigChanged(config.copy(tempCelsius = temp))
                        },
                        label = { Text("Temperature (${config.tempUnit})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Text(
                        text = "SELECT WEATHER CONDITION",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SUNNY", "CLEAR NIGHT", "RAINY", "CLOUDY", "SNOW", "THUNDER", "BREEZY", "HAZE").forEach { cond ->
                            val isSelected = config.weatherCondition.contains(cond)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemAndThemeTab(
    context: Context,
    config: WidgetConfig,
    hasCalPerm: Boolean,
    calPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    hasLocPerm: Boolean,
    locPermLauncher: ManagedActivityResultLauncher<String, Boolean>,
    canExactAlarm: Boolean,
    onConfigChanged: (WidgetConfig) -> Unit,
    onManualRefresh: () -> Unit,
    onLaunchWalkthrough: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // App Theme & Dynamic Color Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "APP APPEARANCE & THEME",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Theme Mode Segmented Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        AppThemeMode.SYSTEM to "System",
                        AppThemeMode.LIGHT to "Light",
                        AppThemeMode.DARK to "Dark"
                    ).forEach { (mode, label) ->
                        val isSelected = config.appThemeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onConfigChanged(config.copy(appThemeMode = mode)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = GeistMonoFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Material 3 Dynamic Color Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Material 3 Dynamic Color",
                                fontFamily = GeistMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                "Dynamically tints app surfaces from your system wallpaper"
                            else
                                "Requires Android 12+ (Using Nothing Red signature theme)",
                            fontFamily = GeistMonoFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = config.useDynamicColor,
                        onCheckedChange = { onConfigChanged(config.copy(useDynamicColor = it)) }
                    )
                }
            }
        }

        // Permissions & Background Services Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "PERMISSIONS & SERVICES",
                            fontFamily = GeistMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = onLaunchWalkthrough,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WALKTHROUGH",
                            fontFamily = GeistMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Interactive Setup Walkthrough Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Walkthrough",
                                fontFamily = GeistMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Step-by-step setup guide for all permissions",
                                fontFamily = GeistMonoFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onLaunchWalkthrough,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "REPLAY", fontFamily = GeistMonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                PermissionStatusRow(
                    title = "Calendar Access",
                    desc = "Syncs upcoming meetings & hand gestures",
                    isGranted = hasCalPerm,
                    onGrantClick = { calPermLauncher.launch(Manifest.permission.READ_CALENDAR) }
                )

                PermissionStatusRow(
                    title = "Location Access",
                    desc = "Queries local weather via Open-Meteo",
                    isGranted = hasLocPerm,
                    onGrantClick = { locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PermissionStatusRow(
                        title = "Exact Alarms (Minute Boundary)",
                        desc = "Powers the precise zero-second clock tick",
                        isGranted = canExactAlarm,
                        onGrantClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(fallbackIntent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Please enable exact alarms in system settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // Actions & Maintenance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "WIDGET ACTIONS",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = onManualRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FORCE REFRESH ALL HOMESCREEN WIDGETS", fontFamily = GeistMonoFamily, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { pinAppWidget(context, config, 461) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PIN WIDGET TO HOME SCREEN", fontFamily = GeistMonoFamily, fontSize = 11.sp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontFamily = GeistMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Text(text = "ACTIVE", fontFamily = GeistMonoFamily, fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = "ENABLE", fontFamily = GeistMonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun pinAppWidget(
    context: Context,
    config: WidgetConfig,
    previewHeight: Int = 461
) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, NothingClockWidgetProvider::class.java)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val previewViews = NothingClockWidgetProvider.buildPreviewRemoteViews(
                context = context,
                config = config,
                canvasWidth = 1024,
                canvasHeight = previewHeight
            )
            val extras = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
            }
            appWidgetManager.requestPinAppWidget(provider, extras, null)
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
