# Dynamic Real-Time Telemetry Stream Architecture
**OS4All Digital Twin — Happiest Health Digital Twin Challenge 2026**

---

## 1. Executive Summary & Purpose

The **OS4All Digital Twin** combines two orthogonal physiological data streams to establish a continuous, dynamic virtual patient:

1. **Static / Historical Stream**: Synthea FHIR Longitudinal EHR (patient demographics, historical diagnoses, baseline HbA1c, fasting glucose, medication history, and retrospective lab panels).
2. **Dynamic / Real-Time Stream**: High-frequency simulated wearable & Continuous Glucose Monitor (CGM) telemetry packets sampled at 5-minute intervals (CGM glucose, rate-of-change velocity, heart rate, autonomic HRV, resting heart rate, sleep architecture, step counts, and physical activity intensity).

By fusing static longitudinal vulnerability with dynamic real-time autonomic and metabolic stress, the Digital Twin computes an explainable **Prototype Risk Score (0–100)** and projects metabolic trajectories across a **2-Hour Early Horizon Window**.

```
+-----------------------------------------------------------------------------------------+
|                                DUAL-STREAM DATA FUSION                                  |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|   STREAM 1: STATIC EHR (Synthea FHIR)          STREAM 2: DYNAMIC TELEMETRY (IoT / CGM)  |
|   - Confirmed Diagnoses (T2D, HTN)             - CGM Glucose (mg/dL) & Velocity         |
|   - Baseline HbA1c & Fasting Glucose           - Heart Rate & Autonomic HRV (ms)        |
|   - Longitudinal Lab Panels                    - Sleep Duration & Sleep Quality         |
|   - Demographics & BMI                         - Step Count & Activity Level            |
|                   \                                           /                         |
|                    \                                         /                          |
|                     +---------------------------------------+                           |
|                                         |                                               |
|                                         v                                               |
|                     +---------------------------------------+                           |
|                     |     DIGITAL TWIN FUSION ENGINE        |                           |
|                     | - Baseline Normalization & Z-scores   |                           |
|                     | - Metabolic State Machine             |                           |
|                     | - 2-Hour Trajectory Projection        |                           |
|                     | - Feature Contribution Explainability |                           |
|                     +---------------------------------------+                           |
|                                         |                                               |
|                     +-------------------+-------------------+                           |
|                     |                                       |                           |
|                     v                                       v                           |
|         DOCTOR WORKSTATION DASHBOARD           GEMINI EXPLANATION SERVICE               |
|         - Live Stream Controller               - Grounded Clinical Narrative            |
|         - 5 Scenario Switchers                 - Zero Hallucination Guardrails          |
|         - Dual-Stream Fusion Visualizer        - Deterministic Offline Fallback         |
+-----------------------------------------------------------------------------------------+
```

---

## 2. Telemetry Packet Schema

Every 5-minute dynamic telemetry packet conforms to strict physiological validation boundaries. Out-of-bounds or physically impossible values are rejected by the backend with HTTP `400 Bad Request`.

| Field Name | Type | Unit / Enum | Valid Range | Description |
| :--- | :--- | :--- | :--- | :--- |
| `patientId` | `String` | Identifier | Non-null | Maps directly to imported Synthea patient (e.g. `patientA`) |
| `timestamp` | `Instant` | ISO-8601 UTC | Valid timestamp | Time of sample capture |
| `cgmGlucoseMgDl` | `Double` | mg/dL | `30.0` – `500.0` | Continuous interstitial glucose reading |
| `glucoseVelocityMgDlPerMin` | `Double` | mg/dL/min | Computed / Optional | Rate of change $(\frac{\Delta \text{Glucose}}{\Delta t})$; positive = rising |
| `heartRateBpm` | `Double` | bpm | `30.0` – `220.0` | Instantaneous heart rate |
| `hrvMs` | `Double` | ms (RMSSD) | `5.0` – `250.0` | Heart rate variability; parasympathetic tone proxy |
| `restingHeartRateBpm` | `Double` | bpm | `30.0` – `150.0` | Baseline resting cardiovascular rate |
| `sleepDurationHours` | `Double` | hours | `0.0` – `16.0` | Sleep duration over prior night |
| `sleepQuality` | `String` | Enum | `POOR`, `FAIR`, `GOOD`, `EXCELLENT` | Sleep architecture score |
| `steps` | `Integer` | count | $\ge 0$ | Cumulative daily or interval steps |
| `activityLevel` | `String` | Enum | `SEDENTARY`, `LIGHT`, `MODERATE`, `VIGOROUS` | Metabolic activity intensity (GLUT4 clearance proxy) |
| `activeScenario` | `String` | Enum | `STABLE`, `POOR_SLEEP`, `HIGH_ACTIVITY`, `GLUCOSE_RISE`, `RECOVERY` | Underlying physiological scenario tag |
| `confidenceScore` | `Double` | Ratio | `0.0` – `1.0` | Sensor reliability & noise confidence index |

---

## 3. First-Class Backend REST APIs

All telemetry endpoints are exposed under `/api/telemetry/**` on the Spring Boot backend (`http://localhost:8080/api/telemetry`).

### 3.1 Ingestion & Retrieval Endpoints

#### Ingest Single Telemetry Packet
- **Endpoint**: `POST /api/telemetry`
- **Request Body**:
  ```json
  {
    "patientId": "patientA",
    "cgmGlucoseMgDl": 142.5,
    "heartRateBpm": 78.0,
    "hrvMs": 42.0,
    "restingHeartRateBpm": 66.0,
    "sleepDurationHours": 6.8,
    "sleepQuality": "FAIR",
    "steps": 4200,
    "activityLevel": "LIGHT",
    "activeScenario": "STABLE",
    "confidenceScore": 0.95
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Telemetry packet ingested successfully",
    "data": {
      "id": 104,
      "patientId": "patientA",
      "timestamp": "2026-10-06T10:15:00Z",
      "cgmGlucoseMgDl": 142.5,
      "glucoseVelocityMgDlPerMin": 0.12,
      "heartRateBpm": 78.0,
      "hrvMs": 42.0,
      "restingHeartRateBpm": 66.0,
      "sleepDurationHours": 6.8,
      "sleepQuality": "FAIR",
      "steps": 4200,
      "activityLevel": "LIGHT",
      "activeScenario": "STABLE",
      "confidenceScore": 0.95
    }
  }
  ```

#### Fetch Latest Telemetry Packet
- **Endpoint**: `GET /api/telemetry/{patientId}/latest`
- **Response** (`200 OK`): Returns the most recent `TelemetryPacketDto` for the patient.

#### Fetch Historical Chronological Packets
- **Endpoint**: `GET /api/telemetry/{patientId}/history?limit=30`
- **Response** (`200 OK`): Returns up to `limit` packets in chronological order for charting.

#### Fetch Time-Series Stream for Specific Metric
- **Endpoint**: `GET /api/telemetry/{patientId}/stream?metric=glucose&days=7`
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "data": {
      "patientId": "patientA",
      "metric": "glucose",
      "unit": "mg/dL",
      "baselineMean": 98.4,
      "points": [
        {"timestampIso": "2026-10-06T09:55:00Z", "value": 138.0},
        {"timestampIso": "2026-10-06T10:00:00Z", "value": 142.5}
      ]
    }
  }
  ```

### 3.2 Live Simulation Control Endpoints

| Method | Endpoint | Query Parameters | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/telemetry/simulation/start` | `patientId` (required), `intervalSeconds` (default: 5), `scenario` (optional) | Starts background simulation task generating 5-min packets |
| `POST` | `/api/telemetry/simulation/stop` | `patientId` (required) | Stops the active simulation background worker |
| `POST` | `/api/telemetry/simulation/scenario` | `patientId` (required), `scenario` (required) | Modifies the active trajectory scenario dynamically |
| `GET` | `/api/telemetry/simulation/status` | `patientId` (required) | Returns running state, active scenario, tick count, last tick timestamp |

---

## 4. The 5 Deterministic Simulation Scenarios

Each scenario simulates reproducible, medically rational physiology:

| Scenario | Physiological Dynamics | Telemetry Impact | Digital Twin State Impact |
| :--- | :--- | :--- | :--- |
| **1. STABLE** | Homeostatic equilibrium | Glucose near patient baseline ($\pm 4\text{ mg/dL}$), velocity $\approx 0$, HRV normal, sleep 7.5h | Patient remains in `STABLE` or baseline drift; risk score stable. |
| **2. POOR_SLEEP** | Sympathetic overdrive & insulin resistance | Sleep 4.2h, sleep quality `POOR`, HRV drops $-15$ ms, resting HR rises $+8$ bpm | Baseline autonomic deviation triggers `PRE_SYMPTOMATIC_DRIFT`. |
| **3. HIGH_ACTIVITY** | Exercise-induced GLUT4 translocation | Steps $+3,500$, `VIGOROUS` activity, HR elevated ($125$ bpm), glucose trending downward | Digital Twin features grant $-6$ GLUT4 clearance bonus; accelerates recovery. |
| **4. GLUCOSE_RISE** | Postprandial acute glycemic surge | Glucose $+22\text{ mg/dL}$ per tick, velocity rises to $+0.8\text{–}1.4\text{ mg/dL/min}$, HRV drops | Triggers `ACUTE_SPIKE_DETECTED` or `HIGH_RISK_METABOLIC_STRAIN` ($>65$ risk). |
| **5. RECOVERY** | Post-exercise or post-insulin clearance | Glucose returns toward baseline ($-12\text{ mg/dL}$ per tick), velocity negative ($-0.6\text{ mg/dL/min}$), HRV normalizes | Transitions to `METABOLIC_RECOVERY` $\to$ `STABLE`. |

---

## 5. Patient Linking & Multi-Tenant Isolation

1. **Deterministic Cohort Ingestion**:
   Dynamic telemetry streams are strictly anchored to imported Synthea patients:
   - `patientA` (Synthea T2D / Prediabetes cohort)
   - `patientB` (Synthea Non-Diabetic Control cohort)
   - `patientC` (Synthea Metabolic Syndrome cohort)
2. **Data Isolation**:
   Every repository query filters by `patientId`. Ingesting telemetry packets for `patientA` does not modify, corrupt, or alter `patientB`'s timeline, baseline, or current state.

---

## 6. Doctor Workstation UI Integration

The Flutter Clinical Workstation (`doctor_dashboard_screen.dart`) exposes:
- **Dual-Stream Architecture Visualizer**:
  Displays Stream 1 (Synthea FHIR EHR baseline) alongside Stream 2 (Dynamic IoT / CGM stream) and the resulting fused Digital Twin state.
- **Simulation Control Panel**:
  - Live Stream status indicator (pulsing green dot when active, paused indicator when idle).
  - One-click buttons for the 5 scenarios (`1. Stable Homeostasis`, `2. Poor Sleep → Drift`, `3. High Activity (GLUT4)`, `4. Glucose Spike Risk`, `5. Recovery After Walk`).
  - Next Reading Tick (manual 5-minute single-step advance).
  - Reset to Stable Homeostasis.
- **Dynamic Vitals Pills & Chart**:
  Real-time CGM Glucose, glucose velocity ($\text{mg/dL/min}$), resting HR, HRV, sleep, and steps updated live with 2-hour projected trajectory horizon.

---

## 7. Research Prototype Disclaimer

> [!WARNING]
> **COMPETITION PROTOTYPE DISCLAIMER**
> The telemetry data stream, simulation scenarios, and early spike predictions in OS4All Digital Twin are generated for the Happiest Health Digital Twin Challenge 2026 proof-of-concept. The system demonstrates algorithmic multi-stream fusion and is **NOT** a medical diagnostic device or FDA/CE-cleared clinical software.
