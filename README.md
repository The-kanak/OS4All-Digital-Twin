# OS4All Digital Twin: From Patient Data to a Living Health Model

> **Happiest Health Digital Twin Challenge 2026 Proof-of-Concept**
>
> *"Research / Hackathon Prototype — Not a Medical Diagnosis."*

---

## 1. Executive Summary

**OS4All Digital Twin** transforms static and dynamic patient health data into a continuously evolving, individualized virtual patient representation. 

The Digital Twin dynamically unites:
1. **Static / Historical EHR Data**: Synthetic longitudinal medical history, family conditions, previous diagnoses, ICD-10 codes, active medications, lab records (HbA1c, fasting glucose, lipid panels, creatinine, ALT).
2. **Dynamic IoT Time-Series Streams**: Continuous simulated wearable streams for CGM blood glucose, Heart Rate, HRV, Sleep Duration, Steps, SpO2, and Core Body Temperature.
3. **Personalized Baseline Engine**: Derived rolling statistical baselines (mean, variance, normal tolerance envelopes, Z-scores), honoring that *“normal for the population is not necessarily normal for the individual.”*
4. **Digital Twin Virtual State**: State-machine representation (`STABLE`, `PRE_SYMPTOMATIC_DRIFT`, `ELEVATED_RISK`, `ACTIVE_ANOMALY`) driven algorithmically with clear drivers.
5. **Primary Clinical Prediction Engine**: Explainable, transparent algorithmic prediction layer for **Early Prediction of Glucose Spike / Metabolic Risk Horizon** within a 2-hour window.
6. **Doctor Workstation & Interactive Simulation Engine**: A professional healthcare dashboard with controllable scenarios, timeline visualization, and grounded virtual patient inquiry.

---

## 2. Core Architecture

```
Static EHR & Lab Data   +   Dynamic IoT Wearable Stream (CGM, HR, HRV, Sleep, Steps)
                     │
                     ▼
         PERSONAL BASELINE ENGINE
   (Patient-specific mean, std dev, z-scores, delta %)
                     │
                     ▼
       FEATURE ENGINEERING PIPELINE
 (Glucose slope, variability CV, HRV deviation, sleep deficit, BMI)
                     │
                     ▼
    METABOLIC PREDICTION & STATE ENGINE
 (Early Glucose Spike Probability & Twin State Classification)
                     │
                     ▼
     DOCTOR DASHBOARD & TWIN WORKSTATION
   - Virtual Patient Cohort (Alex, Sarah, Marcus)
   - Live Wearable Telemetry & CGM Charts
   - Algorithmic Feature Contribution & Explanations
   - Baseline Comparison Grid (Current vs Personal Mean)
   - Controllable Simulation Scenarios (1–5)
   - Grounded Virtual Patient Q&A Interface
```

---

## 3. Supported Competition Scenarios

The system includes a reproducible simulation engine with 5 controllable scenarios:

1. **Scenario 1: Stable Patient Homeostasis**
   - 45 days of homeostatic equilibrium (Glucose ~92 mg/dL, HRV ~55 ms, Resting HR ~60 bpm, Sleep ~7.8 hrs).
   - Twin State: `STABLE` | Spike Probability: ~12%.

2. **Scenario 2: Poor Sleep → Metabolic Drift**
   - 36 days stable baseline followed by 9 days of cumulative sleep reduction (7.8h down to 4.7h).
   - Triggers sympathetic autonomic tone dominance (HRV drops to 33 ms, RHR rises to 76.5 bpm, glucose creeps upward).
   - Twin State: `PRE_SYMPTOMATIC_DRIFT` | Spike Probability: 48–62%.

3. **Scenario 3: High Physical Activity (GLUT4)**
   - High volume daily activity (14,500 daily steps) stimulating non-insulin-mediated glucose clearance.
   - Twin State: `STABLE` | Spike Probability: <10%.

4. **Scenario 4: Glucose Spike Risk**
   - High glycemic carbohydrate intake on a background of sleep debt and suppressed HRV.
   - Rapid upward velocity (+22 mg/dL/hr).
   - Twin State: `ELEVATED_RISK` / `ACTIVE_ANOMALY` | Spike Probability: ~78.5% within 2 Hours.

5. **Scenario 5: Recovery After Lifestyle Intervention**
   - Application of lifestyle intervention: 8.5 hours restorative sleep, 20-min post-meal walk.
   - Autonomic parasympathetic recovery restores glycemic control.
   - Twin State: `STABLE` | Risk normalized.

---

## 4. Digital Twin API Endpoints

All endpoints are available under `/api/v1` (and documented in Swagger UI at `/swagger-ui.html`):

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/patients` | List all synthetic virtual patients with twin state summary. |
| `GET` | `/api/v1/patients/{id}` | Patient demographics, clinical history, and laboratory biomarkers. |
| `GET` | `/api/v1/patients/{id}/history` | Historical medical records, conditions, and medications. |
| `GET` | `/api/v1/patients/{id}/wearables` | Simulated wearable stream (glucose, heart rate, HRV, sleep, steps). |
| `GET` | `/api/v1/patients/{id}/baseline` | Personalized baseline profile, tolerances, and deviations. |
| `GET` | `/api/v1/patients/{id}/digital-twin` | Unified Digital Twin state, vitals snapshot, and risk drivers. |
| `GET` | `/api/v1/patients/{id}/prediction` | Explainable glucose spike prediction & feature contributions. |
| `GET` | `/api/v1/patients/{id}/timeline` | Unified chronological health event timeline. |
| `POST` | `/api/v1/patients/{id}/interact` | Grounded clinical Q&A with the Virtual Patient Digital Twin. |
| `POST` | `/api/v1/simulation/start` | Start live telemetry stream. |
| `POST` | `/api/v1/simulation/pause` | Pause telemetry stream. |
| `POST` | `/api/v1/simulation/reset` | Reset patient to stable homeostatic baseline. |
| `POST` | `/api/v1/simulation/next-reading` | Advance simulation by one reading tick. |
| `POST` | `/api/v1/simulation/scenario/{scenario}` | Inject one of the 5 reproducible scenarios. |
| `POST` | `/api/v1/digital-twin/update` | Trigger digital twin recomputation. |

---

## 5. Judge Demo Walkthrough

1. **Launch Backend**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
2. **Launch Frontend (Web or Desktop)**:
   ```bash
   cd frontend
   flutter run -d chrome
   ```
3. **Open Doctor Workstation**:
   - In the top navigation bar, click the blue **DOCTOR TWIN** button.
4. **Select a Patient**:
   - Choose *Alex Rivera (Virtual Twin)* or *Sarah Chen (Virtual Twin)* in the left sidebar.
5. **Inspect Historical EHR & Baseline**:
   - Review prediabetes diagnosis, family history, and 45-day personalized baseline envelopes.
6. **Trigger Simulation Scenario**:
   - Click **2. Poor Sleep → Drift** on the simulation toolbar.
   - Observe the Digital Twin state update to `PRE_SYMPTOMATIC_DRIFT`.
7. **Simulate Acute Spike**:
   - Click **4. Glucose Spike Risk**.
   - Notice the predicted glucose spike probability jump to **78.5% within the Next 2 Hours**.
   - Review the feature contributions table showing exact weights and drivers.
8. **Interact with the Twin**:
   - Click any preset question: *"What is driving the predicted glucose spike?"* or type a custom question.
   - The virtual twin responds with answers strictly grounded in the patient's verified telemetry.
9. **Observe Recovery**:
   - Click **5. Recovery After Walk** to watch autonomic metrics stabilize and the twin transition back to `STABLE`.

---

## 6. Disclaimer

> **Research / Hackathon Prototype — Not a Medical Diagnosis.**
> OS4All Digital Twin is designed for clinical research, physiological trend interpretation, and algorithmic demonstration. It does not replace professional medical judgment.
