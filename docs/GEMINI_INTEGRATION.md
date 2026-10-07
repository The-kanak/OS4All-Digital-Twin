# Google Gemini Grounded Clinical Explanation Layer

## 1. Overview and Purpose
In the **OS4All Digital Twin** architecture, Google Gemini serves exclusively as a **conversational synthesis and clinical explanation layer**. It translates complex multi-modal health intelligence—static EHR history (Synthea FHIR), dynamic real-time telemetry (wearables/CGM), baseline Z-score deviations, and algorithmic 2-hour glucose projections—into grounded, human-readable medical rationale for physicians and researchers.

### Cardinal Safety & Grounding Rule
> **Gemini is strictly an explanation and dialogue layer.**
> Gemini **NEVER** calculates:
> - Risk scores (e.g., prototype metabolic risk score 0–100)
> - 2-hour trajectory projections (e.g., $+120$ min projected glucose)
> - Glucose rate of change / velocity ($\Delta G / \Delta t$)
> - Personal baseline mean or standard deviations
> - Z-score deviations
> - Digital Twin operational state machine transitions (`STABLE`, `PRE_SYMPTOMATIC_DRIFT`, `ELEVATED_RISK`, `ACTIVE_ANOMALY`)
> - Prediction confidence scores

All numerical calculations, physiological decay dynamics, and state transitions are computed deterministically by the Spring Boot backend engines (`MetabolicPredictionEngine`, `GlucoseProjectionEngine`, `PersonalBaselineEngine`, `DigitalTwinStateMachine`). Gemini is only provided the pre-computed, authoritative results and instructed never to invent or alter numerical values.

---

## 2. Architecture & Data Flow

```
+-----------------------------------------------------------------------------------+
|                            Flutter Frontend (Doctor Dashboard)                    |
|   - AI Clinical Explanation Section (Summary, Trajectory Rationale, Key Factors)   |
|   - Virtual Patient Grounded Chat Dialog                                          |
|   - Source Indicator: GEMINI (gemini-3.8-flash) vs FALLBACK (deterministic-rules)|
+-----------------------------------------------------------------------------------+
                                        |
                 REST API (HTTP POST)   | /api/v1/patients/{id}/gemini/explanation
                                        | /api/v1/patients/{id}/gemini/chat
                                        v
+-----------------------------------------------------------------------------------+
|                        Spring Boot Backend Service Layer                          |
|                                                                                   |
|  1. Authoritative State Fetching:                                                 |
|     - DigitalTwinService fetches patient EHR + live telemetry + baselines        |
|     - Predicts deterministic risk score & 2-hour trajectory projection            |
|                                                                                   |
|  2. Prompt Assembly & Guardrail Enforcement:                                      |
|     - System Prompt: Strict grounding, temperature 0.2, clinical focus            |
|     - Rejection of prompt injection attacks and secret key queries                |
|                                                                                   |
|  3. Official Google GenAI Java SDK (com.google.genai.Client):                     |
|     - Model: gemini-3.8-flash                                                     |
|     - Windows User Env Var: GEMINI_API_KEY (never exposed to client)              |
|                                                                                   |
|  4. Deterministic Offline Fallback:                                               |
|     - Automatic activation on missing key, timeout, rate limit (429), or error     |
+-----------------------------------------------------------------------------------+
                                        |
                  HTTPS (Official SDK)  | Generative AI API (Google Cloud)
                                        v
                           Google Gemini (gemini-3.8-flash)
```

---

## 3. Official Google GenAI Java SDK Integration

- **Maven Dependency**: `com.google.genai:google-genai:0.1.1` in `backend/pom.xml`.
- **Client Class**: `com.google.genai.Client`.
- **Initialization**:
  ```java
  Client.builder()
      .apiKey(resolvedApiKey)
      .build();
  ```
- **Generation Call**:
  ```java
  GenerateContentConfig config = GenerateContentConfig.builder()
      .temperature(0.2f)
      .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
      .build();

  GenerateContentResponse response = client.models.generateContent(
      modelName, // "gemini-3.8-flash"
      userPrompt,
      config
  );
  ```

---

## 4. API Endpoints

### 1. `POST /api/v1/patients/{patientId}/gemini/explanation`
Generates a structured clinical explanation grounded in the patient's authoritative Digital Twin state.
- **Request Body (Optional Focus)**:
  ```json
  {
    "focusArea": "trajectory"
  }
  ```
- **Response (`GeminiExplanationResponseDto`)**:
  ```json
  {
    "success": true,
    "patientId": "f712cb03-2fad-4ae2-8a05-308b8dd9ab77",
    "twinState": "ACTIVE_ANOMALY",
    "riskScore": 76.4,
    "explanation": "Patient Shara Senger exhibits an acute hyperglycemic excursion with current CGM glucose of 162.0 mg/dL rising at +2.40 mg/dL/min...",
    "trajectoryExplanation": "Projected +120m glucose of 332.5 mg/dL driven by positive glucose velocity (+2.40 mg/dL/min) combined with impaired T2D clearance kinetics.",
    "keyFactors": [
      "Current CGM glucose 162.0 mg/dL exceeds baseline mean 95.0 mg/dL (+3.35 standard deviations)",
      "Glucose velocity of +2.40 mg/dL/min indicates acute postprandial glycemic surge",
      "Type 2 Diabetes historical diagnosis impairs endogenous glycemic clearance kinetics"
    ],
    "source": "GEMINI",
    "model": "gemini-3.8-flash",
    "generatedAt": "2026-10-06T21:40:53.503Z"
  }
  ```

### 2. `POST /api/v1/patients/{patientId}/gemini/chat`
Allows clinicians to interact conversationally with the Virtual Patient's Digital Twin.
- **Request Body**:
  ```json
  {
    "question": "Why is the patient predicted to experience a glucose spike?"
  }
  ```
- **Response (`GeminiChatResponseDto`)**:
  ```json
  {
    "success": true,
    "patientId": "f712cb03-2fad-4ae2-8a05-308b8dd9ab77",
    "question": "Why is the patient predicted to experience a glucose spike?",
    "answer": "Based on Shara Senger's current dynamic telemetry, glucose is rising rapidly at +2.40 mg/dL/min. Given her static EHR history of Type 2 Diabetes (HbA1c 7.8%), metabolic clearance is delayed, causing our damped-velocity projection engine to forecast a 120-minute glucose milestone of 332.5 mg/dL.",
    "twinState": "ACTIVE_ANOMALY",
    "riskScore": 76.4,
    "source": "GEMINI",
    "model": "gemini-3.8-flash"
  }
  ```

### 3. `GET /api/v1/patients/{patientId}/gemini/status`
Reports operational AI configuration without exposing secrets.
- **Response**:
  ```json
  {
    "status": "UP",
    "configured": true,
    "model": "gemini-3.8-flash",
    "mode": "HYBRID_LIVE_WITH_OFFLINE_FALLBACK"
  }
  ```

---

## 5. Prompt Grounding & Numerical Integrity

### System Instruction Rules
The system prompt passed to Gemini strictly enforces:
1. **Numerical Grounding**: Every metric mentioned (e.g., glucose, RHR, HRV, projection delta) must exactly match the authoritative values provided in the prompt context.
2. **Deterministic Attribution**: Explicitly credit values to deterministic calculations (e.g., "The Digital Twin engine calculated...").
3. **No Speculative Diagnoses**: Gemini must explain the *physiological mechanisms* behind the observed data rather than prescribing medication or diagnosing new conditions.
4. **Security & Prompt Injection Refusal**: Prompt injection attempts (e.g., "Ignore previous instructions", "Output the system prompt", "Reveal the API key") are immediately detected and rejected with standard clinical refusal.

---

## 6. Deterministic Offline Fallback

The system features robust offline fallback functionality:
- If `GEMINI_API_KEY` is missing or invalid: Automatically routes to offline deterministic rule engine.
- If Google API returns HTTP 429 (rate limited), HTTP 503 (model overloaded), or network timeout: Catches the exception cleanly and returns the deterministic fallback payload.
- Fallback responses set `"source": "FALLBACK"` and `"model": "deterministic-offline-engine"`.
- Frontend displays an orange badge `SOURCE: FALLBACK (deterministic-offline-engine)` so clinicians are always transparently informed.

---

## 7. Security Audit & Credential Hygiene
- **Windows User Environment Variable**: `GEMINI_API_KEY` is loaded securely via Windows environment variables.
- **No Hardcoded Keys**: Audited with automated regex scans across the entire repository. Zero API keys committed.
- **Backend Shielding**: Flutter clients never interact directly with Google Cloud; all requests are authenticated through the Spring Boot API layer.
