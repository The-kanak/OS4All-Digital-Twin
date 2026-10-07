package org.os4all.os4all_frontend

import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Android MainActivity with OS4All Health Connect MethodChannel Bridge.
 * Exposes read-only access to Android Health Connect records:
 * - Steps (StepsRecord)
 * - Heart Rate (HeartRateRecord)
 * - Heart Rate Variability (HeartRateVariabilityRmssdRecord)
 * - Resting Heart Rate (RestingHeartRateRecord)
 * - Sleep (SleepSessionRecord)
 * - Blood Glucose (BloodGlucoseRecord)
 *
 * Implements strict graceful degradation: returns NO_DATA or UNAVAILABLE on devices without Health Connect.
 */
class MainActivity : FlutterActivity() {

    companion object {
        private const val HEALTH_PERMISSIONS_REQUEST_CODE = 1001
    }

    private val CHANNEL = "os4all/health_connect"
    private var pendingPermissionResult: MethodChannel.Result? = null

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    )

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HEALTH_PERMISSIONS_REQUEST_CODE) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val permStatus = checkHasPermissions()
                    pendingPermissionResult?.success(mapOf(
                        "granted" to (permStatus["hasPermissions"] as Boolean),
                        "allGranted" to (permStatus["allGranted"] as Boolean),
                        "grantedCount" to (permStatus["grantedCount"] as Int),
                        "totalCount" to (permStatus["totalCount"] as Int),
                        "status" to (permStatus["status"] as String)
                    ))
                } catch (e: Exception) {
                    pendingPermissionResult?.success(mapOf(
                        "granted" to false,
                        "allGranted" to false,
                        "status" to "ERROR",
                        "errorMessage" to (e.message ?: "Failed to verify granted permissions")
                    ))
                } finally {
                    pendingPermissionResult = null
                }
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "checkAvailability", "isAvailable" -> {
                    result.success(checkAvailability())
                }
                "hasPermissions" -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val permStatus = checkHasPermissions()
                            result.success(permStatus)
                        } catch (e: Exception) {
                            result.success(mapOf(
                                "hasPermissions" to false,
                                "allGranted" to false,
                                "status" to "ERROR",
                                "errorMessage" to (e.message ?: "Failed to check permissions")
                            ))
                        }
                    }
                }
                "requestPermissions" -> {
                    if (checkAvailability() != "HEALTH_CONNECT_AVAILABLE") {
                        result.success(mapOf(
                            "granted" to false,
                            "allGranted" to false,
                            "status" to "HEALTH_CONNECT_UNAVAILABLE",
                            "errorMessage" to "Health Connect is not available on this device"
                        ))
                        return@setMethodCallHandler
                    }
                    pendingPermissionResult = result
                    try {
                        val intent = PermissionController.createRequestPermissionResultContract().createIntent(this, permissions)
                        startActivityForResult(intent, HEALTH_PERMISSIONS_REQUEST_CODE)
                    } catch (e: Exception) {
                        pendingPermissionResult = null
                        result.success(mapOf(
                            "granted" to false,
                            "allGranted" to false,
                            "status" to "ERROR",
                            "errorMessage" to (e.message ?: "Failed to launch Health Connect permission request")
                        ))
                    }
                }
                "readSteps" -> {
                    executeAsync(result) { readStepsInternal() }
                }
                "readHeartRate" -> {
                    executeAsync(result) { readHeartRateInternal() }
                }
                "readHeartRateVariability" -> {
                    executeAsync(result) { readHrvInternal() }
                }
                "readSleep" -> {
                    executeAsync(result) { readSleepInternal() }
                }
                "readRestingHeartRate" -> {
                    executeAsync(result) { readRestingHeartRateInternal() }
                }
                "readBloodGlucose" -> {
                    executeAsync(result) { readBloodGlucoseInternal() }
                }
                "readAllHealthData" -> {
                    executeAsync(result) { readAllHealthDataInternal() }
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun checkAvailability(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "HEALTH_CONNECT_UNAVAILABLE"
        }
        return try {
            val status = HealthConnectClient.getSdkStatus(this)
            if (status == HealthConnectClient.SDK_AVAILABLE) {
                "HEALTH_CONNECT_AVAILABLE"
            } else {
                "HEALTH_CONNECT_UNAVAILABLE"
            }
        } catch (_: Exception) {
            "HEALTH_CONNECT_UNAVAILABLE"
        }
    }

    private suspend fun checkHasPermissions(): Map<String, Any> {
        val avail = checkAvailability()
        if (avail != "HEALTH_CONNECT_AVAILABLE") {
            return mapOf(
                "hasPermissions" to false,
                "allGranted" to false,
                "status" to avail
            )
        }
        return withContext(Dispatchers.IO) {
            val client = HealthConnectClient.getOrCreate(this@MainActivity)
            val granted = client.permissionController.getGrantedPermissions()
            val grantedCount = permissions.count { granted.contains(it) }
            val allGranted = permissions.all { granted.contains(it) }
            mapOf(
                "hasPermissions" to (grantedCount > 0),
                "allGranted" to allGranted,
                "grantedCount" to grantedCount,
                "totalCount" to permissions.size,
                "status" to if (grantedCount > 0) "PERMISSION_GRANTED" else "PERMISSION_NOT_GRANTED"
            )
        }
    }

    private fun executeAsync(result: MethodChannel.Result, block: suspend () -> Map<String, Any?>) {
        if (checkAvailability() != "HEALTH_CONNECT_AVAILABLE") {
            result.success(mapOf(
                "status" to "HEALTH_CONNECT_UNAVAILABLE",
                "available" to false,
                "source" to "REAL_HEALTH_CONNECT",
                "errorMessage" to "Health Connect is not available on this device"
            ))
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val data = withContext(Dispatchers.IO) { block() }
                result.success(data)
            } catch (e: Exception) {
                result.success(mapOf(
                    "status" to "ERROR",
                    "available" to false,
                    "source" to "REAL_HEALTH_CONNECT",
                    "errorMessage" to (e.message ?: "Unknown Health Connect error")
                ))
            }
        }
    }

    private suspend fun readStepsInternal(): Map<String, Any?> {
        val client = HealthConnectClient.getOrCreate(this)
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
        val response = client.readRecords(
            ReadRecordsRequest(recordType = StepsRecord::class, timeRangeFilter = timeRange)
        )
        val totalSteps = if (response.records.isNotEmpty()) response.records.sumOf { it.count } else null
        val latestTime = response.records.maxByOrNull { it.endTime }?.endTime?.toString()
        return mapOf(
            "steps" to totalSteps,
            "recordedAt" to latestTime,
            "source" to "REAL_HEALTH_CONNECT",
            "status" to if (totalSteps != null) "SUCCESS" else "NO_DATA"
        )
    }

    private suspend fun readHeartRateInternal(): Map<String, Any?> {
        val client = HealthConnectClient.getOrCreate(this)
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
        val response = client.readRecords(
            ReadRecordsRequest(recordType = HeartRateRecord::class, timeRangeFilter = timeRange)
        )
        val latestSample = response.records.flatMap { it.samples }.maxByOrNull { it.time }
        val bpm = latestSample?.beatsPerMinute?.toDouble()
        return mapOf(
            "heartRateBpm" to bpm,
            "recordedAt" to latestSample?.time?.toString(),
            "source" to "REAL_HEALTH_CONNECT",
            "status" to if (bpm != null) "SUCCESS" else "NO_DATA"
        )
    }

    private suspend fun readHrvInternal(): Map<String, Any?> {
        val client = HealthConnectClient.getOrCreate(this)
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
        val response = client.readRecords(
            ReadRecordsRequest(recordType = HeartRateVariabilityRmssdRecord::class, timeRangeFilter = timeRange)
        )
        val latest = response.records.maxByOrNull { it.time }
        val hrv = latest?.heartRateVariabilityMillis
        return mapOf(
            "hrvMs" to hrv,
            "recordedAt" to latest?.time?.toString(),
            "source" to "REAL_HEALTH_CONNECT",
            "status" to if (hrv != null) "SUCCESS" else "NO_DATA"
        )
    }

    private suspend fun readRestingHeartRateInternal(): Map<String, Any?> {
        val client = HealthConnectClient.getOrCreate(this)
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
        val response = client.readRecords(
            ReadRecordsRequest(recordType = RestingHeartRateRecord::class, timeRangeFilter = timeRange)
        )
        val latest = response.records.maxByOrNull { it.time }
        val bpm = latest?.beatsPerMinute?.toDouble()
        return mapOf(
            "restingHeartRateBpm" to bpm,
            "recordedAt" to latest?.time?.toString(),
            "source" to "REAL_HEALTH_CONNECT",
            "status" to if (bpm != null) "SUCCESS" else "NO_DATA"
        )
    }

    private suspend fun readSleepInternal(): Map<String, Any?> {
        val client = HealthConnectClient.getOrCreate(this)
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
        val response = client.readRecords(
            ReadRecordsRequest(recordType = SleepSessionRecord::class, timeRangeFilter = timeRange)
        )
        val latest = response.records.maxByOrNull { it.endTime }
        val durationHours = latest?.let {
            Duration.between(it.startTime, it.endTime).toMinutes().toDouble() / 60.0
        }
        return mapOf(
            "sleepDurationHours" to durationHours,
            "sleepQuality" to null,
            "recordedAt" to latest?.endTime?.toString(),
            "source" to "REAL_HEALTH_CONNECT",
            "status" to if (durationHours != null) "SUCCESS" else "NO_DATA"
        )
    }

    private suspend fun readBloodGlucoseInternal(): Map<String, Any?> {
        val client = HealthConnectClient.getOrCreate(this)
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
        val response = client.readRecords(
            ReadRecordsRequest(recordType = BloodGlucoseRecord::class, timeRangeFilter = timeRange)
        )
        val latest = response.records.maxByOrNull { it.time }
        val glucoseMgDl = latest?.level?.inMilligramsPerDeciliter
        return mapOf(
            "bloodGlucoseMgDl" to glucoseMgDl,
            "recordedAt" to latest?.time?.toString(),
            "source" to "REAL_HEALTH_CONNECT",
            "status" to if (glucoseMgDl != null) "SUCCESS" else "NO_DATA"
        )
    }

    private suspend fun readAllHealthDataInternal(): Map<String, Any?> {
        val client = HealthConnectClient.getOrCreate(this)
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)

        var totalSteps: Long? = null
        var hrBpm: Double? = null
        var hrvMs: Double? = null
        var rhrBpm: Double? = null
        var sleepHours: Double? = null
        var glucoseMgDl: Double? = null
        var latestTimestamp: String? = null

        // 1. Steps
        try {
            val res = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange))
            if (res.records.isNotEmpty()) {
                totalSteps = res.records.sumOf { it.count }
                latestTimestamp = res.records.maxByOrNull { it.endTime }?.endTime?.toString()
            }
        } catch (_: Exception) {}

        // 2. Heart Rate
        try {
            val res = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRange))
            val s = res.records.flatMap { it.samples }.maxByOrNull { it.time }
            if (s != null) {
                hrBpm = s.beatsPerMinute.toDouble()
                if (latestTimestamp == null) latestTimestamp = s.time.toString()
            }
        } catch (_: Exception) {}

        // 3. HRV
        try {
            val res = client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRange))
            val h = res.records.maxByOrNull { it.time }
            if (h != null) {
                hrvMs = h.heartRateVariabilityMillis
                if (latestTimestamp == null) latestTimestamp = h.time.toString()
            }
        } catch (_: Exception) {}

        // 4. Resting Heart Rate
        try {
            val res = client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, timeRange))
            val r = res.records.maxByOrNull { it.time }
            if (r != null) {
                rhrBpm = r.beatsPerMinute.toDouble()
                if (latestTimestamp == null) latestTimestamp = r.time.toString()
            }
        } catch (_: Exception) {}

        // 5. Sleep
        try {
            val res = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRange))
            val sl = res.records.maxByOrNull { it.endTime }
            if (sl != null) {
                sleepHours = Duration.between(sl.startTime, sl.endTime).toMinutes().toDouble() / 60.0
                if (latestTimestamp == null) latestTimestamp = sl.endTime.toString()
            }
        } catch (_: Exception) {}

        // 6. Blood Glucose
        try {
            val res = client.readRecords(ReadRecordsRequest(BloodGlucoseRecord::class, timeRange))
            val g = res.records.maxByOrNull { it.time }
            if (g != null) {
                glucoseMgDl = g.level.inMilligramsPerDeciliter
                latestTimestamp = g.time.toString()
            }
        } catch (_: Exception) {}

        val hasAnyData = totalSteps != null || hrBpm != null || hrvMs != null ||
                rhrBpm != null || sleepHours != null || glucoseMgDl != null

        return mapOf(
            "status" to if (hasAnyData) "SUCCESS" else "NO_DATA",
            "available" to true,
            "steps" to totalSteps,
            "heartRateBpm" to hrBpm,
            "hrvMs" to hrvMs,
            "restingHeartRateBpm" to rhrBpm,
            "sleepDurationHours" to sleepHours,
            "sleepQuality" to null,
            "bloodGlucoseMgDl" to glucoseMgDl,
            "recordedAt" to (latestTimestamp ?: now.toString()),
            "source" to "REAL_HEALTH_CONNECT"
        )
    }
}
