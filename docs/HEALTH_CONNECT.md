# Android Health Connect Integration Guide

> **Happiest Health Digital Twin Challenge 2026**  
> **Status:** Integrated, Tested, and Verified  
> **Architecture Role:** Optional, Read-Only Real-Device Health Telemetry Ingestion Source

---

## 1. Executive Summary

OS4All Digital Twin features a dual-stream fusion architecture combining:
1. **Static / Historical Clinical EHR:** 15 Synthea FHIR longitudinal patient records.
2. **Dynamic / Real-Time Telemetry:** Simulated 5-minute IoT and CGM time-series data with 5 interactive clinical perturbation scenarios.

To bridge synthetic physiological simulation with real personal health hardware, **Android Health Connect** has been integrated as an **OPTIONAL, READ-ONLY** data ingestion channel. When deployed on an Android device (physical or emulated API 26+) with Health Connect installed and authorized, OS4All can ingest real biometric readings—including steps, resting heart rate, heart rate variability, sleep sessions, and blood glucose—and map them directly into the Digital Twin state machine and personalized baseline engine.

### Strict Non-Interference Guarantees
- **Never a hard dependency:** The application boots, executes, tests, and demonstrates all features completely without Health Connect.
- **Strictly read-only:** Zero write permissions are requested or declared. Personal device data is never modified, deleted, or overwritten.
- **Clean fallback:** On Web, Windows desktop, iOS, or unsupported Android devices, the service cleanly reports `HEALTH_CONNECT_UNAVAILABLE` without unhandled exceptions or crashes.
- **No data fabrication:** Sparse Health Connect metrics are ingested as-is; unmeasured continuous variables (such as derivative CGM velocity or qualitative activity level) are explicitly left `null` rather than fabricated.

---

## 2. System Architecture & Data Flow

```
+---------------------------------------------------------------------------------------------------+
|                                       ANDROID DEVICE HARDWARE                                      |
|  [Wearables / Smartwatches / CGMs] ---> [Android Health Connect Store (com.google.android.apps...)] |
+---------------------------------------------------------------------------------------------------+
                                                      │
                                                      │ androidx.health.connect:connect-client:1.1.0-alpha11
                                                      ▼
+---------------------------------------------------------------------------------------------------+
|                                    NATIVE ANDROID RUNTIME                                         |
|  MainActivity.kt                                                                                  |
|  - HealthConnectClient.getOrCreate(context)                                                       |
|  - PermissionController: 6 READ permissions (Steps, HR, HRV, Sleep, RHR, Glucose)               |
|  - ReadRecordsRequest across last 24h window                                                      |
|  - MethodChannel: 'os4all/health_connect'                                                          |
+---------------------------------------------------------------------------------------------------+
                                                      │
                                                      │ MethodChannel IPC
                                                      ▼
+---------------------------------------------------------------------------------------------------+
|                                      FLUTTER UI & SERVICE                                         |
|  - HealthConnectService (Platform detection, graceful fallback, async sync)                       |
|  - HealthConnectSnapshot (Null-safe model, no fabrication)                                        |
|  - DoctorDashboardScreen:                                                                         |
|      * Health Data Ingestion Sources Panel                                                        |
|      * "Connect Health Connect" & "Sync Telemetry" actions                                        |
|      * Status badges & telemetry preview                                                          |
+---------------------------------------------------------------------------------------------------+
                                                      │
                                                      │ POST /api/telemetry/health-connect
                                                      ▼
+---------------------------------------------------------------------------------------------------+
|                                     SPRING BOOT BACKEND                                           |
|  - TelemetryController.ingestHealthConnectTelemetry(...)                                          |
|  - Tagged source: 'REAL_HEALTH_CONNECT', scenario: 'REAL_DEVICE_DATA'                              |
|  - DigitalTwinStateService & BaselineEngine:                                                      |
|      * Calculates physiological deviations against Synthea FHIR baseline                          |
|      * Evaluates state machine (STABLE, ELEVATED_METABOLIC_STRAIN, etc.)                           |
|      * Projects 2-hour metabolic risk & trajectory                                                |
|  - Grounded Clinical Explanation Layer (Offline deterministic fallback + Gemini explainability)   |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Supported Health Connect Data Types & Permissions

All permissions are strictly **read-only**:

| Biometric Metric | Health Connect Record Type | Android Permission | Ingestion Mapping |
| :--- | :--- | :--- | :--- |
| **Cumulative Steps** | `StepsRecord` | `android.permission.health.READ_STEPS` | `steps` (integer) |
| **Heart Rate** | `HeartRateRecord` | `android.permission.health.READ_HEART_RATE` | `heartRate` (bpm, double) |
| **Heart Rate Variability** | `HeartRateVariabilityRmssdRecord` | `android.permission.health.READ_HEART_RATE_VARIABILITY` | `hrv` (RMSSD ms, double) |
| **Resting Heart Rate** | `RestingHeartRateRecord` | `android.permission.health.READ_RESTING_HEART_RATE` | `restingHeartRate` (bpm, double) |
| **Sleep Duration & Quality** | `SleepSessionRecord` | `android.permission.health.READ_SLEEP` | `sleepDurationHours` (hours) & `sleepQualityScore` |
| **Blood Glucose** | `BloodGlucoseRecord` | `android.permission.health.READ_BLOOD_GLUCOSE` | `glucose` (mg/dL, double) |

### Manifest Configuration (`AndroidManifest.xml`)
```xml
<!-- Health Connect Read-Only Permissions -->
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE_VARIABILITY" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
<uses-permission android:name="android.permission.health.READ_RESTING_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_BLOOD_GLUCOSE" />

<!-- Health Connect App Package Query -->
<queries>
    <package android:name="com.google.android.apps.healthdata" />
</queries>

<!-- Health Connect Permissions Rationale Intent -->
<intent-filter>
    <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
</intent-filter>

<!-- View Permission Usage Activity Alias -->
<activity-alias
    android:name="ViewPermissionUsageActivity"
    android:exported="true"
    android:targetActivity=".MainActivity"
    android:permission="android.permission.START_VIEW_PERMISSION_USAGE">
    <intent-filter>
        <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
        <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
    </intent-filter>
</activity-alias>
```

---

## 4. Backend Telemetry Integration Endpoint

### `POST /api/telemetry/health-connect`

Ingests raw device health metrics without requiring synthetic scenario metadata.

#### Request Payload
```json
{
  "patientId": "synthea-t2d-001",
  "timestamp": "2026-10-06T14:30:00Z",
  "steps": 6540,
  "heartRate": 74.0,
  "hrv": 48.0,
  "restingHeartRate": 61.0,
  "sleepDurationHours": 7.2,
  "sleepQualityScore": 0.85,
  "glucose": 108.0,
  "source": "REAL_HEALTH_CONNECT",
  "scenario": "REAL_DEVICE_DATA"
}
```

#### Response Payload (`200 OK`)
```json
{
  "id": "e6f4773c-6cb4-48f1-a185-30fa1db4e6e0",
  "patientId": "synthea-t2d-001",
  "timestamp": "2026-10-06T14:30:00Z",
  "cgmGlucoseMgDl": 108.0,
  "glucoseVelocityMgDlPerMin": null,
  "heartRateBpm": 74.0,
  "hrvMs": 48.0,
  "restingHeartRateBpm": 61.0,
  "sleepDurationHours": 7.2,
  "sleepQuality": "GOOD",
  "steps": 6540,
  "activityLevel": null,
  "scenario": "REAL_DEVICE_DATA",
  "source": "REAL_HEALTH_CONNECT",
  "confidenceScore": 1.0
}
```

---

## 5. Non-Fabrication & Physiological Grounding Rules

Health Connect records captured from personal fitness trackers or CGMs often have gaps or missing dimensions:
1. **No CGM Velocity:** Consumer CGMs reporting via Health Connect typically transmit discrete point-in-time glucose measurements rather than continuous first-order differentials. OS4All sets `glucoseVelocityMgDlPerMin = null`. The backend prediction engine calculates velocity only when consecutive timestamps allow rigorous computation, without fabricating arbitrary slopes.
2. **No Qualitative Activity Inferences:** Step counts are exact integers. Qualitative categories (`LIGHT`, `MODERATE`, `VIGOROUS`) are not assumed unless steps/HR data clearly satisfy baseline criteria.
3. **Traceability:** The record retains `source = 'REAL_HEALTH_CONNECT'` to clearly inform doctors that this data originated from a real personal device, separating it from the simulated stress test streams.

---

## 6. Doctor Dashboard User Interface

The Clinical Twin Workstation (`DoctorDashboardScreen`) includes a dedicated **HEALTH DATA INGESTION SOURCES** section:
- **Card 1: Synthea FHIR EHR** (Status: Active / Loaded) – Shows baseline demographics, BMI, historical HbA1c, and lab history.
- **Card 2: Simulated Dynamic Stream** (Status: Standby or Streaming Active) – Displays 5-minute physiological stream status with scenario injection buttons.
- **Card 3: Android Health Connect** (Status: Dynamic) –
  - On non-Android platforms: `NOT AVAILABLE ON THIS PLATFORM/DEVICE`
  - On Android before grant: `PERMISSION NEEDED` with `[Connect]` button
  - Once granted: `CONNECTED (READ-ONLY)` with `[Sync Telemetry]` button
  - Displays real-time sync status and latest sensor values (Steps, HR, Glucose).

---

## 7. Verification & Test Suite Results

| Test Suite | Executed Command | Tests Run | Failures | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Android Kotlin Compilation** | `.\gradlew compileDebugKotlin` | N/A | 0 | **BUILD SUCCESS** |
| **Backend Integration Suite** | `mvn test` | 127 | 0 | **BUILD SUCCESS** |
| **Frontend Unit & Widget Suite** | `flutter test` | 11 | 0 | **ALL PASSED** |
| **Static Code Analysis** | `flutter analyze` | Clean | 0 errors | **VERIFIED** |

---

## 8. Hackathon Prototype & Regulatory Disclaimer

> **IMPORTANT CLINICAL & HACKATHON NOTICE:**  
> The Android Health Connect integration in OS4All Digital Twin is developed strictly as a technical proof-of-concept for the **Happiest Health Digital Twin Challenge 2026**.  
> - It is **NOT** a certified medical diagnostic system, software as a medical device (SaMD), or clinical telemetry monitor.  
> - All risk scores and 2-hour trajectory projections are experimental algorithmic demonstrations.  
> - Real personal health data read from Health Connect is processed only for the local demonstration session and must never be used to make actual diagnostic or treatment decisions.
