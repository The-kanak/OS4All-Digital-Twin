# OS4All Flagship Hackathon Demonstration Guide

This guide walks judges and reviewers through reproducing the complete **OS4All 9-step chain** either through the interactive Flutter app or directly via the backend REST API.

---

## 1. Prerequisites & Startup

### Step A: Start the Backend (Port 8080)
```bash
cd backend
mvn clean package
java -jar target/os4all-backend-1.0.0-SNAPSHOT.jar
```
*Server is live at `http://localhost:8080`.*

### Step B: Launch the Flutter App
```bash
cd ../frontend
flutter run -d chrome # Or on an attached mobile emulator
```

---

## 2. Interactive App Demo Workflow (One-Click)

1. **Open the App**:
   - The app launches through the **Splash Screen** displaying the OS4All brand mark: *"Your health, understood earlier."*
   - Log in or tap **"Use Demo Account"** to bypass manual entry.
2. **Access Flagship Demo**:
   - On the top right of the Dashboard AppBar, tap the **`DEMO CHAIN`** button.
3. **Walk Through the 9 Steps**:
   - **Step 1 (DATA)**: Review 35 days of longitudinal wearable ingestion (Resting HR, nocturnal HRV, sleep duration, temperature).
   - **Step 2 (BASELINE)**: See the Personal Baseline Engine calculate the individual normal variance range (e.g. Resting HR: 60.0 bpm ± 2.5 bpm).
   - **Step 3 (DRIFT)**: Observe the onset of persistent directional drift across days 31–32 as sleep decreases and resting heart rate climbs.
   - **Step 4 (MULTI-SIGNAL PATTERN)**: Watch the transparent rule-based anomaly engine flag an **Autonomic Recovery Strain Pattern** (Sleep ↓ + Resting HR ↑ + HRV ↓ + Temp ↑) persistent for 4 days.
   - **Step 5 (NEMOTRON REASONING)**: View NVIDIA Nemotron-4-340B-Instruct reason over the variance via Nebius AI Studio.
   - **Step 6 (TAVILY EVIDENCE)**: Inspect conditionally retrieved peer-reviewed medical citations from PubMed and Frontiers in Physiology.
   - **Step 7 (OS4All INSIGHT)**: Read the synthesized plain-language summary separating verified observations from non-diagnostic interpretations.
   - **Step 8 (ACTION)**: Review the actionable non-invasive lifestyle guidance and clinical evaluation criteria.
   - **Step 9 (TIMELINE UPDATE)**: Examine the updated unified longitudinal chronological record and SHA-256 audit log.
4. **Hands-Free Autoplay**:
   - Tap **"Autoplay"** in the top AppBar to demonstrate the complete chain automatically.

---

## 3. Alternative: Running via REST API (cURL / Swagger)

You can trigger the exact same end-to-end scenario with a single cURL call:

```bash
curl -X POST http://localhost:8080/api/v1/demo/run-flagship-scenario \
     -H "Content-Type: application/json"
```

### Expected Response:
```json
{
  "success": true,
  "message": "Flagship OS4All demonstration scenario executed successfully.",
  "data": {
    "statusMessage": "Flagship OS4All End-to-End Demonstration scenario executed successfully with 100% reproducible synthetic data.",
    "demoUserEmail": "demo.patient@os4all.test",
    "aggregateState": "ANOMALY",
    "executionChain": {
      "step1Data": "DATA: 35 days of longitudinal wearable observations recorded...",
      "step2Baseline": "BASELINE: Personal baseline calculated over days 1-30...",
      "step3Drift": "DRIFT: Days 31-34 demonstrate a gradual persistent departure...",
      "step4MultiSignalPattern": "MULTI-SIGNAL PATTERN: Correlated autonomic departure detected...",
      "step5NemotronReasoning": "NEMOTRON REASONING: AI Intelligence Layer evaluates multi-signal pattern...",
      "step6TavilyEvidence": "TAVILY EVIDENCE: Evidence Agent conditionally retrieves peer-reviewed literature...",
      "step7Os4AllInsight": "OS4All INSIGHT: Generated structured explanation distinguishing verified data...",
      "step8RecommendedAction": "ACTION: Formulated non-invasive lifestyle interventions...",
      "step9TimelineUpdate": "TIMELINE UPDATE: Real-time unified chronological timeline reflects observations..."
    },
    "syntheticDataNotice": "DEMO DATA NOTICE: All records... are strictly synthetic demonstration data..."
  }
}
```

---

## 4. Interactive AI Health Counselor Test

1. Navigate to the **Counselor** tab in the app.
2. Ask: *"Why did my health status change?"*
   - See the counselor ground its explanation in the actual 4-day multi-signal autonomic strain pattern and baseline deviation without claiming a diagnosis.
3. Test the **Clinical Emergency Gate**:
   - Ask: *"I have severe chest pain and can't breathe."*
   - Notice the immediate redirection to emergency services (911/112), bypassing diagnostic generation.
