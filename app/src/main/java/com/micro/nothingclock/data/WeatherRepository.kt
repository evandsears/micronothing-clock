package com.micro.nothingclock.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class WeatherInfo(
    val temperature: Int,
    val condition: String,
    val isFahrenheit: Boolean = true
) {
    fun getDisplayString(): String = "$temperature°${if (isFahrenheit) "F" else "C"} $condition"
}

class WeatherRepository(private val context: Context? = null) {

    private val prefs = context?.getSharedPreferences("nothing_weather_cache", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "NothingWeatherRepo"
        private const val DEFAULT_LAT = 40.7128
        private const val DEFAULT_LON = -74.0060
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(): Pair<Double, Double> {
        if (context != null) {
            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    if (lm != null) {
                        val providers = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            providers.add(LocationManager.FUSED_PROVIDER)
                        }
                        providers.add(LocationManager.GPS_PROVIDER)
                        providers.add(LocationManager.NETWORK_PROVIDER)
                        providers.add(LocationManager.PASSIVE_PROVIDER)

                        var bestLocation: Location? = null
                        for (provider in providers) {
                            try {
                                val loc = lm.getLastKnownLocation(provider) ?: continue
                                if (bestLocation == null || loc.time > bestLocation.time) {
                                    bestLocation = loc
                                }
                            } catch (_: SecurityException) {
                            } catch (_: IllegalArgumentException) {
                            }
                        }

                        if (bestLocation != null) {
                            val lat = bestLocation.latitude
                            val lon = bestLocation.longitude
                            // Cache last known good coordinates
                            prefs?.edit()
                                ?.putFloat("last_known_lat", lat.toFloat())
                                ?.putFloat("last_known_lon", lon.toFloat())
                                ?.putLong("last_known_loc_time", bestLocation.time)
                                ?.apply()
                            return Pair(lat, lon)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error acquiring location from LocationManager", e)
                }
            }

            // If current fix is unavailable, check cached coordinates before defaulting to NYC
            val savedLat = prefs?.getFloat("last_known_lat", Float.NaN) ?: Float.NaN
            val savedLon = prefs?.getFloat("last_known_lon", Float.NaN) ?: Float.NaN
            if (!savedLat.isNaN() && !savedLon.isNaN()) {
                return Pair(savedLat.toDouble(), savedLon.toDouble())
            }
        }

        // Default coordinates if location permission is not granted and no cache exists
        return Pair(DEFAULT_LAT, DEFAULT_LON)
    }

    suspend fun fetchWeather(
        latitude: Double? = null,
        longitude: Double? = null,
        isFahrenheit: Boolean = true,
        refreshIntervalMinutes: Int = 15,
        forceRefresh: Boolean = false,
        fallbackTemp: Int = 72,
        fallbackCondition: String = "SUNNY"
    ): WeatherInfo = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cachedTime = prefs?.getLong("cached_time", 0L) ?: 0L
        val cachedIsF = prefs?.getBoolean("cached_is_f", isFahrenheit) ?: isFahrenheit
        val intervalMillis = refreshIntervalMinutes.coerceAtLeast(1) * 60 * 1000L

        val isCacheFresh = (now - cachedTime) < intervalMillis && cachedIsF == isFahrenheit

        // Return fresh cache if within refresh interval and matching unit
        if (!forceRefresh && isCacheFresh && hasValidCache()) {
            return@withContext getCachedOrDefault(fallbackTemp, fallbackCondition, isFahrenheit)
        }

        val (lat, lon) = if (latitude != null && longitude != null) {
            Pair(latitude, longitude)
        } else {
            getLastKnownCoordinates()
        }

        try {
            val tempUnitParam = if (isFahrenheit) "temperature_unit=fahrenheit" else "temperature_unit=celsius"
            val endpoint =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,wind_speed_10m,is_day&$tempUnitParam&wind_speed_unit=kmh"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MicroNothingClock/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val current = if (json.has("current")) {
                    json.getJSONObject("current")
                } else if (json.has("current_weather")) {
                    json.getJSONObject("current_weather")
                } else {
                    null
                }

                if (current != null) {
                    val tempDouble = current.optDouble("temperature_2m", current.optDouble("temperature", Double.NaN))
                    if (!tempDouble.isNaN()) {
                        val weatherCode = current.optInt("weather_code", current.optInt("weathercode", 0))
                        val windSpeed = current.optDouble("wind_speed_10m", current.optDouble("windspeed", 0.0))
                        val isDay = current.optInt("is_day", 1) == 1

                        val cond = mapWeatherCodeToNothingCondition(weatherCode, windSpeed, isDay)
                        val finalTemp = tempDouble.roundToInt()

                        // Cache successful weather with timestamp & coordinates
                        prefs?.edit()
                            ?.putInt("cached_temp", finalTemp)
                            ?.putString("cached_condition", cond)
                            ?.putBoolean("cached_is_f", isFahrenheit)
                            ?.putLong("cached_time", now)
                            ?.putFloat("cached_lat", lat.toFloat())
                            ?.putFloat("cached_lon", lon.toFloat())
                            ?.apply()

                        return@withContext WeatherInfo(finalTemp, cond, isFahrenheit)
                    }
                }
            } else {
                Log.w(TAG, "Open-Meteo HTTP response code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network failure while fetching Open-Meteo weather: ${e.message}")
        }

        // Network failure, timeout, or parsing issue: fallback to cached weather if available
        getCachedOrDefault(fallbackTemp, fallbackCondition, isFahrenheit)
    }

    private fun hasValidCache(): Boolean {
        if (prefs == null) return false
        val cachedTemp = prefs.getInt("cached_temp", -999)
        val cachedCond = prefs.getString("cached_condition", null)
        return cachedTemp != -999 && cachedCond != null
    }

    fun getCachedOrDefault(fallbackTemp: Int, fallbackCondition: String, isFahrenheit: Boolean): WeatherInfo {
        if (prefs != null) {
            val cachedTemp = prefs.getInt("cached_temp", -999)
            val cachedCond = prefs.getString("cached_condition", null)
            val cachedIsF = prefs.getBoolean("cached_is_f", isFahrenheit)

            if (cachedTemp != -999 && cachedCond != null) {
                if (cachedIsF == isFahrenheit) {
                    return WeatherInfo(cachedTemp, cachedCond, isFahrenheit)
                } else {
                    // Accurately convert cached temperature to the requested unit
                    val convertedTemp = if (isFahrenheit) {
                        (cachedTemp * 9.0 / 5.0 + 32.0).roundToInt()
                    } else {
                        ((cachedTemp - 32.0) * 5.0 / 9.0).roundToInt()
                    }
                    return WeatherInfo(convertedTemp, cachedCond, isFahrenheit)
                }
            }
        }
        return WeatherInfo(fallbackTemp, fallbackCondition, isFahrenheit)
    }

    /**
     * Maps WMO weather interpretation codes and wind conditions to MicroNothing glyph condition strings.
     * Open-Meteo uses WMO 4677 standard codes:
     * 0, 1: Clear sky / mainly clear
     * 2, 3: Partly cloudy, overcast
     * 45, 48: Fog / depositing rime fog
     * 51, 53, 55: Drizzle
     * 56, 57: Freezing Drizzle
     * 61, 63, 65: Rain
     * 66, 67: Freezing Rain
     * 71, 73, 75, 77: Snow fall / snow grains
     * 80, 81, 82: Rain showers
     * 85, 86: Snow showers
     * 95: Thunderstorm
     * 96, 99: Thunderstorm with hail
     */
    fun mapWeatherCodeToNothingCondition(code: Int, windSpeedKmH: Double = 0.0, isDay: Boolean = true): String {
        // Severe winds (> 85 km/h / 53 mph) override to STORM
        if (windSpeedKmH > 85.0) return "STORM"

        return when (code) {
            0, 1 -> if (windSpeedKmH > 35.0) "BREEZY" else if (isDay) "SUNNY" else "CLEAR NIGHT"
            2, 3 -> if (windSpeedKmH > 35.0) "BREEZY" else "CLOUDY"
            45, 48 -> "HAZE"
            51, 53, 55 -> "RAINY"
            56, 57 -> "HAIL"
            61, 63, 65 -> "RAINY"
            66, 67 -> "HAIL"
            71, 73, 75, 77 -> "SNOW"
            80, 81, 82 -> "RAINY"
            85, 86 -> "SNOW"
            95 -> "THUNDER"
            96, 99 -> "STORM"
            else -> if (windSpeedKmH > 35.0) "BREEZY" else if (isDay) "SUNNY" else "CLEAR NIGHT"
        }
    }
}
