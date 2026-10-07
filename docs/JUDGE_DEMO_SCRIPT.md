# 5-Minute Judge Demo Script

**Project:** OS4All Digital Twin  
**Target:** Live Judging / Video Recording (Under 5 Minutes)  
**Preparation:** Start backend (`mvn spring-boot:run`) and Flutter frontend (`flutter run -d chrome`).

---

### Timing Breakdown & Script

| Timestamp | Phase & Screen | Actions & Visual Highlights | Spoken Talking Points |
| :--- | :--- | :--- | :--- |
| **0:00 – 0:30** | **Problem & Mission**<br>*(Top Bar & Workstation)* | Point to `CLINICAL TWIN WORKSTATION` and `HACKATHON PROTOTYPE` disclaimer badge. | *"Hello judges. Healthcare monitoring today is largely reactive—doctors review historical labs weeks late, while wearable trackers display isolated raw numbers. We built OS4All Digital Twin: a living, stateful virtual patient model that fuses static EHR history with continuous dynamic telemetry and individual baselines to predict subclinical metabolic risks hours before they occur."* |
| **0:30 – 1:00** | **Patient Cohort Selection**<br>*(Left Sidebar)* | Click on **Alex Rivera (38M)** in the left sidebar. | *"We present 3 synthetic longitudinal patient cohorts. Let's look at Alex Rivera, a 38-year-old desk worker with prediabetic risk indicators. Notice his baseline state is currently STABLE with a low prototype risk score."* |
| **1:00 – 1:30** | **Historical EHR Stream**<br>*(Historical Records Panel)* | Scroll to `HISTORICAL MEDICAL RECORDS` and `DUAL-STREAM DATA FUSION` card. | *"Here is Stream 1: Static Historical EHR. We see Alex's 45-day history: prediabetic diagnosis, baseline HbA1c 5.4%, fasting glucose 88 mg/dL, and family history. This establishes his individual biological foundation."* |
| **1:30 – 2:00** | **Dynamic Wearable Telemetry**<br>*(Live Wearable Section)* | Toggle tabs: `Glucose (CGM)`, `Resting HR`, `HRV`, `Sleep`. Show 7-day stream chart with amber baseline line. | *"Here is Stream 2: Dynamic Wearables and Continuous CGM. Real-time readings show current glucose at 92 mg/dL, resting heart rate at 60 bpm, nocturnal HRV at 55 ms, and restorative sleep at 7.8 hours. The bar chart compares daily stream values directly against his personal baseline average."* |
| **2:00 – 2:30** | **Personal Baseline & Fusion**<br>*(Data Fusion Card & Comparison Engine)* | Highlight the `DUAL-STREAM DATA FUSION` card and the `PERSONALIZED BASELINE COMPARISON ENGINE` table. | *"Crucially, our system does not rely on generic population tables. In this table, Z-scores ($Z = \frac{x-\mu}{\sigma}$) track exact statistical departures from Alex's personal 45-day normal envelope. The Data Fusion card visually integrates Stream 1 EHR plus Stream 2 IoT into the Fused Digital Twin State."* |
| **2:30 – 3:15** | **Inject POOR_SLEEP Scenario**<br>*(Simulation Toolbar)* | Click the button: **`2. Poor Sleep → Drift`**. | *"Now let's simulate real-world stress. I'll inject Scenario 2: Cumulative Sleep Deprivation. Over several days, Alex gets only 4.5 hours of sleep. Watch how the twin responds..."* |
| **3:15 – 3:45** | **State Transition Tracking**<br>*(Hero & Transition Banner)* | Point to the new blue transition banner: `STATE TRANSITION: STABLE → ELEVATED_RISK` and `Triggered by: Sleep 3.1 hours below baseline`. | *"Look at the top hero card: The Digital Twin transitioned from STABLE to ELEVATED RISK. The transition banner explicitly identifies the root causality: 3.1 hours of sleep debt suppressing autonomic HRV down to 33 ms, triggering sympathetic tone elevation."* |
| **3:45 – 4:15** | **Metabolic Prediction Layer**<br>*(Primary Prediction Card)* | Scroll to `PRIMARY CLINICAL USE CASE`. Point out `PROTOTYPE RISK SCORE: 76.5 / 100` and feature weights (+18 pts sleep deficit, +15 pts HRV drop). | *"Here is our explainable prediction engine. The 2-hour glucose spike prototype risk score surged to 76.5/100. Every single point is inspectable: Sleep deficit contributed +18 points, autonomic HRV suppression contributed +15 points. It also suggests immediate non-pharmacological interventions like a post-meal walk."* |
| **4:15 – 4:40** | **Virtual Patient Interaction**<br>*(Interaction Engine Card)* | Click preset chip: `Why is this patient currently elevated risk?` | *"Doctors can interact directly with the Virtual Patient. We ask: 'Why is this patient currently elevated risk?' The engine synthesizes a response strictly grounded in verified telemetry, baseline deviations, and literature on sleep-mediated insulin resistance, with zero hallucination."* |
| **4:40 – 5:00** | **Recovery Intervention**<br>*(Simulation Toolbar)* | Click **`5. Recovery After Walk`**. | *"Finally, we apply a clinical intervention: Scenario 5 Recovery. Alex gets 8.5 hours of restorative sleep and does a light walk. The telemetry normalizes, the risk score drops back to baseline equilibrium, and the twin safely returns to STABLE. This demonstrates a complete closed-loop, explainable healthcare Digital Twin."* |

---

### Backup Troubleshooting Tips:
- If running offline, Gemini seamlessly switches to the deterministic rule engine without throwing any errors or popup alerts.
- Resetting any patient to baseline takes one click on `Reset` in the Simulation toolbar.
