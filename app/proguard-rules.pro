# MicroNothing Clock Proguard Rules

# Keep AndroidManifest entrypoints (Activity, AppWidgetProvider, BroadcastReceiver)
-keep class com.micro.nothingclock.MainActivity {
    public <init>();
    protected void onCreate(android.os.Bundle);
}
-keep class com.micro.nothingclock.widget.NothingClockWidgetProvider {
    public <init>();
}
-keep class com.micro.nothingclock.widget.WidgetAlarmReceiver {
    public <init>();
}
-keep class * extends android.appwidget.AppWidgetProvider {
    public <init>();
    public void onReceive(android.content.Context, android.content.Intent);
    public void onUpdate(android.content.Context, android.appwidget.AppWidgetManager, int[]);
    public void onAppWidgetOptionsChanged(android.content.Context, android.appwidget.AppWidgetManager, int, android.os.Bundle);
    public void onDeleted(android.content.Context, int[]);
    public void onEnabled(android.content.Context);
    public void onDisabled(android.content.Context);
    public void onRestored(android.content.Context, int[], int[]);
}
-keep class * extends android.content.BroadcastReceiver {
    public <init>();
    public void onReceive(android.content.Context, android.content.Intent);
}

# Keep AndroidX EdgeToEdge classes & methods un-obfuscated and un-inlined so Google Play Console
# static analysis detects EdgeToEdge.enable() and recognizes AndroidX backward-compatibility calls
-keep class androidx.activity.EdgeToEdge* { *; }
-keep class androidx.activity.SystemBarStyle* { *; }

# AndroidX Startup (prevents reflection failure in InitializationProvider on app launch)
-keep class androidx.startup.** { *; }
-keep class * extends androidx.startup.Initializer { *; }
-keep class * implements androidx.startup.Initializer { *; }

# Keep enum methods for safe enum lookups
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve annotations and line numbers for readable Play Console crash stack traces
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
