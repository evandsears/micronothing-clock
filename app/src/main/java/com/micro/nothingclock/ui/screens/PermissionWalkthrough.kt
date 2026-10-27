package com.micro.nothingclock.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micro.nothingclock.ui.theme.GeistMonoFamily

private const val TOTAL_STEPS = 4

private val SuccessGreen = Color(0xFF4CAF50)
private val SuccessGreenBg = Color(0x1F4CAF50)
private val WarningAmber = Color(0xFFFFB300)
private val WarningAmberBg = Color(0x1FFFB300)

@Composable
fun PermissionWalkthrough(
    hasCalPerm: Boolean,
    onGrantCalPerm: () -> Unit,
    hasLocPerm: Boolean,
    onGrantLocPerm: () -> Unit,
    canExactAlarm: Boolean,
    onGrantExactAlarm: () -> Unit,
    onPinWidget: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top Navigation Bar
                WalkthroughHeader(
                    currentStep = currentStep,
                    totalSteps = TOTAL_STEPS,
                    onSkip = onDismiss
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Indicator Pills
                WalkthroughProgressIndicators(
                    currentStep = currentStep,
                    totalSteps = TOTAL_STEPS,
                    onStepSelected = { currentStep = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Scrollable Step Content with horizontal slide transition
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(tween(260)) { width -> width / 3 } + fadeIn(tween(260)))
                                    .togetherWith(slideOutHorizontally(tween(200)) { width -> -width / 3 } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally(tween(260)) { width -> -width / 3 } + fadeIn(tween(260)))
                                    .togetherWith(slideOutHorizontally(tween(200)) { width -> width / 3 } + fadeOut(tween(200)))
                            }
                        },
                        label = "walkthrough_step_animation"
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (step) {
                                0 -> ExactAlarmsStep(
                                    canExactAlarm = canExactAlarm,
                                    onGrantExactAlarm = onGrantExactAlarm
                                )
                                1 -> CalendarStep(
                                    hasCalPerm = hasCalPerm,
                                    onGrantCalPerm = onGrantCalPerm
                                )
                                2 -> LocationWeatherStep(
                                    hasLocPerm = hasLocPerm,
                                    onGrantLocPerm = onGrantLocPerm
                                )
                                3 -> AllSetAndPinStep(
                                    canExactAlarm = canExactAlarm,
                                    hasCalPerm = hasCalPerm,
                                    hasLocPerm = hasLocPerm,
                                    onPinWidget = onPinWidget,
                                    onFinish = onDismiss
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Bar (Back / Next / Finish)
                WalkthroughBottomBar(
                    currentStep = currentStep,
                    totalSteps = TOTAL_STEPS,
                    onBack = { if (currentStep > 0) currentStep-- },
                    onNext = { if (currentStep < TOTAL_STEPS - 1) currentStep++ },
                    onFinish = onDismiss
                )
            }
        }
    }
}

@Composable
private fun WalkthroughHeader(
    currentStep: Int,
    totalSteps: Int,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
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
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "SETUP WALKTHROUGH // ${currentStep + 1} OF $totalSteps",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }
        }

        TextButton(
            onClick = onSkip,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (currentStep == totalSteps - 1) "CLOSE" else "SKIP",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun WalkthroughProgressIndicators(
    currentStep: Int,
    totalSteps: Int,
    onStepSelected: (Int) -> Unit
) {
    val stepLabels = listOf("TIMING", "CALENDAR", "WEATHER", "HOMESCREEN")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isSelected = currentStep == i
            val isCompleted = currentStep > i
            val barColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }

            Box(
                modifier = Modifier
                    .weight(if (isSelected) 2.2f else 1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
                    .clickable { onStepSelected(i) }
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "STEP 0${currentStep + 1} — ${stepLabels.getOrElse(currentStep) { "" }}",
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
        Text(
            text = "${((currentStep + 1) * 100) / totalSteps}% COMPLETE",
            fontFamily = GeistMonoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// STEP 1: PRECISE ALARMS
// -------------------------------------------------------------
@Composable
private fun ExactAlarmsStep(
    canExactAlarm: Boolean,
    onGrantExactAlarm: () -> Unit
) {
    StepHeroBadge(
        icon = Icons.Default.Timer,
        isGranted = canExactAlarm,
        tag = "ESSENTIAL // ZERO-LAG TIMING"
    )

    Text(
        text = "PRECISE MINUTE TICKS",
        fontFamily = GeistMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        letterSpacing = 0.8.sp
    )

    Text(
        text = "Android battery savers put apps to sleep. Grant exact alarm access to ensure the MicroNothing clock flips at the exact 00-second mark on your home screen without freezing or lagging.",
        fontFamily = GeistMonoFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )

    // Interactive Status & Action Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EXACT ALARM PERMISSION",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            "Android 12+ Alarms & Reminders"
                        else
                            "Standard Android Exact Alarm (Active)",
                        fontFamily = GeistMonoFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(isGranted = canExactAlarm)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (canExactAlarm) {
                ConfirmedStateBanner(
                    title = "PRECISION TIMING ACTIVE",
                    description = "Clock receiver is scheduled to trigger at the top of every minute."
                )
            } else {
                ActionPromptBanner(
                    title = "ACTION REQUIRED",
                    description = "Tap below to open system settings and enable 'Allow setting alarms and reminders' for MicroNothing Clock."
                )

                Button(
                    onClick = onGrantExactAlarm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENABLE EXACT ALARMS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }

    // Technical Insight Card
    TechnicalInfoCard(
        title = "HOW IT WORKS",
        text = "MicroNothing calculates the exact milliseconds until the next minute boundary (:00.000) and schedules a high-precision wake alarm. This guarantees your homescreen time is always pixel-perfect without draining battery."
    )
}

// -------------------------------------------------------------
// STEP 2: CALENDAR SYNC
// -------------------------------------------------------------
@Composable
private fun CalendarStep(
    hasCalPerm: Boolean,
    onGrantCalPerm: () -> Unit
) {
    StepHeroBadge(
        icon = Icons.Default.CalendarMonth,
        isGranted = hasCalPerm,
        tag = "MODULE // EVENT GLYPHS"
    )

    Text(
        text = "CALENDAR EVENT SYNC",
        fontFamily = GeistMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        letterSpacing = 0.8.sp
    )

    Text(
        text = "Sync your upcoming schedule to display live meeting titles, minutes countdowns, and Nothing OS gesture icons directly under the clock digits.",
        fontFamily = GeistMonoFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )

    // Interactive Status & Action Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "READ CALENDAR ACCESS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Local Device & Google Calendar",
                        fontFamily = GeistMonoFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(isGranted = hasCalPerm)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (hasCalPerm) {
                ConfirmedStateBanner(
                    title = "CALENDAR SYNC ACTIVE",
                    description = "Upcoming events and meeting countdowns will display automatically on widget."
                )
            } else {
                ActionPromptBanner(
                    title = "PERMISSION OPTIONAL BUT RECOMMENDED",
                    description = "Allow calendar access to see your real meetings. If disabled, a customizable fallback placeholder will be shown."
                )

                Button(
                    onClick = onGrantCalPerm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRANT CALENDAR ACCESS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }

    // Privacy Notice Card
    TechnicalInfoCard(
        title = "PRIVACY & SECURITY",
        text = "Your calendar data is processed 100% on-device and never leaves your phone. MicroNothing only queries the next event title and start timestamp to calculate the minute countdown."
    )
}

// -------------------------------------------------------------
// STEP 3: LOCATION & WEATHER
// -------------------------------------------------------------
@Composable
private fun LocationWeatherStep(
    hasLocPerm: Boolean,
    onGrantLocPerm: () -> Unit
) {
    StepHeroBadge(
        icon = Icons.Default.Cloud,
        isGranted = hasLocPerm,
        tag = "MODULE // WEATHER CAPSULE"
    )

    Text(
        text = "HYPERLOCAL WEATHER",
        fontFamily = GeistMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        letterSpacing = 0.8.sp
    )

    Text(
        text = "Power the right-hand weather pill with live temperature readings and condition glyphs (Clear, Rain, Cloud, Snow) updated periodically from Open-Meteo.",
        fontFamily = GeistMonoFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )

    // Interactive Status & Action Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COARSE LOCATION ACCESS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "City/Region level (Low Battery Impact)",
                        fontFamily = GeistMonoFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(isGranted = hasLocPerm)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (hasLocPerm) {
                ConfirmedStateBanner(
                    title = "LIVE WEATHER READY",
                    description = "Weather pill will query local forecast and refresh in the background."
                )
            } else {
                ActionPromptBanner(
                    title = "LOCATION ACCESS NEEDED",
                    description = "Approximate location is required to look up your local weather forecast. If disabled, a manual temperature can be configured in the studio."
                )

                Button(
                    onClick = onGrantLocPerm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRANT LOCATION ACCESS",
                        fontFamily = GeistMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }

    // Technical Info Card
    TechnicalInfoCard(
        title = "OPEN-SOURCE WEATHER",
        text = "Weather is powered by Open-Meteo without proprietary tracking or API keys. Your coordinates are used solely to fetch weather data and are cached locally to conserve battery."
    )
}

// -------------------------------------------------------------
// STEP 4: ALL SET & HOMESCREEN
// -------------------------------------------------------------
@Composable
private fun AllSetAndPinStep(
    canExactAlarm: Boolean,
    hasCalPerm: Boolean,
    hasLocPerm: Boolean,
    onPinWidget: () -> Unit,
    onFinish: () -> Unit
) {
    val allConfigured = canExactAlarm && hasCalPerm && hasLocPerm

    StepHeroBadge(
        icon = if (allConfigured) Icons.Default.CheckCircle else Icons.Default.Widgets,
        isGranted = allConfigured,
        tag = "HOMESCREEN // READY TO LAUNCH"
    )

    Text(
        text = if (allConfigured) "ALL SYSTEMS OPERATIONAL" else "SETUP SUMMARY",
        fontFamily = GeistMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        letterSpacing = 0.8.sp
    )

    Text(
        text = if (allConfigured)
            "Great job! All essential permissions are active. Your widget will tick accurately at :00, display upcoming meetings, and show real-time weather."
        else
            "Review your permissions configuration below. You can update any permission now or adjust them later under the System tab.",
        fontFamily = GeistMonoFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )

    // Summary Checklist Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PERMISSIONS STATUS",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )

            SummaryItemRow(
                icon = Icons.Default.Timer,
                title = "Exact Alarms (Minute Ticks)",
                isGranted = canExactAlarm,
                grantedDesc = "Zero-second synchronization active",
                missingDesc = "Standard fallback ticks (may lag)"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SummaryItemRow(
                icon = Icons.Default.CalendarMonth,
                title = "Live Calendar Events",
                isGranted = hasCalPerm,
                grantedDesc = "Connected to device calendar",
                missingDesc = "Showing fallback/quippy events"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SummaryItemRow(
                icon = Icons.Default.Cloud,
                title = "Local Weather Capsule",
                isGranted = hasLocPerm,
                grantedDesc = "Live Open-Meteo forecasts active",
                missingDesc = "Manual temperature in studio"
            )
        }
    }

    // Quick Actions
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onPinWidget,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PIN WIDGET TO HOME SCREEN",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp
            )
        }

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ENTER STUDIO & CUSTOMIZE",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp
            )
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPONENTS
// -------------------------------------------------------------
@Composable
private fun StepHeroBadge(
    icon: ImageVector,
    isGranted: Boolean,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        // Tag Capsule
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = tag,
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        // Circular Icon Badge with Status Ring
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) SuccessGreenBg
                    else MaterialTheme.colorScheme.primaryContainer
                )
                .border(
                    width = 2.dp,
                    color = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(isGranted: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isGranted) SuccessGreenBg else WarningAmberBg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (isGranted) SuccessGreen else WarningAmber,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isGranted) "ACTIVE" else "NOT SET",
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (isGranted) SuccessGreen else WarningAmber,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ConfirmedStateBanner(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SuccessGreenBg)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = SuccessGreen
                )
                Text(
                    text = description,
                    fontFamily = GeistMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionPromptBanner(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontFamily = GeistMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TechnicalInfoCard(
    title: String,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = text,
                    fontFamily = GeistMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SummaryItemRow(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    grantedDesc: String,
    missingDesc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) SuccessGreenBg else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGranted) grantedDesc else missingDesc,
                    fontFamily = GeistMonoFamily,
                    fontSize = 9.sp,
                    color = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StatusBadge(isGranted = isGranted)
    }
}

@Composable
private fun WalkthroughBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BACK",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (currentStep < totalSteps - 1) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "CONTINUE",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        } else {
            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "DONE",
                    fontFamily = GeistMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}
