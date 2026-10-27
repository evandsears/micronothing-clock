# MicroNothing Clock Proguard Rules

# Keep Widget, Renderer and Data classes from being obfuscated or stripped by R8
-keep class com.micro.nothingclock.widget.** { *; }
-keep class com.micro.nothingclock.data.** { *; }
-keep class com.micro.nothingclock.ui.** { *; }
-keep class com.micro.nothingclock.MainActivity { *; }

# Keep AppWidgetProvider and BroadcastReceiver implementations
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# AndroidX Startup (prevents reflection failure in InitializationProvider on app launch)
-keep class androidx.startup.** { *; }
-keep class * extends androidx.startup.Initializer { *; }
-keep class * implements androidx.startup.Initializer { *; }

# AndroidX WorkManager & Room (prevents fatal startup crash if WorkManager is pulled in transitively)
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.InputMerger { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-keep class androidx.room.** { *; }
-keep class androidx.room.paging.** { *; }
-dontwarn androidx.work.**
-dontwarn androidx.room.**

# AndroidX Compose & Lifecycle
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**

# Kotlin Reflection & Annotations
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepclassmembers class * {
    *** Companion;
}

# Preserve line numbers and original source file names for readable Play Console stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
