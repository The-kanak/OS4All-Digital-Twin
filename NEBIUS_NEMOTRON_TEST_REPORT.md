# OS4All — Nebius AI & NVIDIA Nemotron Integration Test Report

## 1. Executive Summary

This report documents the configuration, integration, and verification of the **NVIDIA Nemotron** model hosted on **Nebius AI Studio / Token Factory** (`https://api.studio.nebius.ai/v1`) for the OS4All health platform.

The system enforces:
- **No Silent Model Substitution:** If the configured model is unavailable or throws an error, the system reports the exact error and terminates the workflow rather than silently substituting another model or falling back to mock synthesis.
- **Strict Non-Diagnostic Grounding:** Verified physiological observations are separated from non-diagnostic interpretations.
- **Zero Credential Leakage:** API keys are never exposed in logs, API responses, or frontend assets.
- **Dual Operating Modes:** Selectable between `mock` mode and `nebius` mode via standard environment variables.

---

## 2. Configuration Matrix

The integration is driven by standard environment variables defined in `.env` and `backend/src/main/resources/application.yml`:

```env
# AI Layer Configuration
OS4ALL_AI_PROVIDER=nebius
NEBIUS_API_KEY=your_nebius_api_key_here
NEBIUS_BASE_URL=https://api.studio.nebius.ai/v1
NEBIUS_MODEL=nvidia/Llama-3_1-Nemotron-70B-Instruct
AI_TEMPERATURE=0.2
AI_MAX_TOKENS=1500
AI_TIMEOUT_MS=30000
```

### Configured Model
- **Model Identifier:** `nvidia/Llama-3_1-Nemotron-70B-Instruct`
- **Provider:** Nebius AI Studio (`api.studio.nebius.ai`)
- **Protocol:** OpenAI-compatible Chat Completions REST API (`POST /v1/chat/completions`)
- **Output Schema:** Enforced structured JSON output (`response_format: {"type": "json_object"}`)

---

## 3. End-to-End Request Flow

The execution workflow follows a sequential multi-agent architecture across 5 logical stages:

```
[Health Context (35-day wearable perturbation)]
                  ↓
       [Trend Interpretation Agent]
                  ↓
        [Evidence Agent (Tavily)]
                  ↓
           [Explanation Agent]
                  ↓
             [Action Agent]
                  ↓
       [NebiusModelProvider (NVIDIA Nemotron)]
                  ↓
     [JSON Extraction & Fence Stripping]
                  ↓
    [OS4All Backend Inference & Audit Log]
                  ↓
      [Flutter App: InsightsScreen / DemoScreen]
```

### Step-by-Step Flow Description:
1. **Health Context Agent:**
   Curates a read-only, anonymized physiological snapshot (`StructuredHealthContext`). For the synthetic demonstration scenario:
   - **Days 1–30 Baseline:** Resting HR: 60.0 bpm (±2.5), Nocturnal HRV: 55.0 ms (±3.8), Sleep: 8.0 hrs/night (±0.5).
   - **Days 31–35 Perturbation:** Resting HR: 74.0 bpm (+14 bpm), HRV: 36.0 ms (-19 ms), Sleep: 5.2 hrs (-2.8h).
2. **Trend Interpretation Agent:**
   Identifies the directional autonomic departure, separating verified data facts from interpretations.
3. **Evidence Agent:**
   Retrieves targeted literature citations (e.g. PubMed studies on short-term sleep restriction and autonomic recovery deficits).
4. **Explanation Agent:**
   Drafts a clear, non-diagnostic synthesis adhering to clinical guidelines (no medical disease diagnosis).
5. **Action Agent:**
   Generates pragmatic lifestyle steps (sleep recovery window, cardiovascular load deloading) and assigns an appropriate urgency level (`MONITOR`).
6. **Nebius Model Provider Invocation:**
   Dispatches prompt to `https://api.studio.nebius.ai/v1/chat/completions` with masked key logging.
7. **Response Ingestion & Parsing:**
   Extracts `choices[0].message.content`, removes markdown code fences (` ```json ... ``` `), validates JSON schema, and maps to `AiHealthInsightResponse`.
8. **Audit & Timeline Logging:**
   Persists record into `AiInferenceLog` with execution latency, confidence, urgency, and tamper-evident audit record.
9. **Frontend Rendering:**
   Displayed in the Flutter application via `InsightsScreen` (multi-agent synthesis, confidence badge, citations) and `FlagshipDemoScreen` (9-step visual chain).

---

## 4. Verification Test Results

Tests were run against both mocked HTTP clients and the Spring Boot application container without requiring real external API keys in automated suites:

### A. Nebius Model Provider Unit Tests (`NebiusModelProviderTest`)
| Test Case | Scenario | Expected Behavior | Status |
| :--- | :--- | :--- | :--- |
| `testProviderNameAndAvailability` | Provider contract | Returns `"nebius"`, checks key presence | **PASSED** |
| `shouldThrowIfApiKeyMissing` | Missing API key | Throws `ApiException` with clear message | **PASSED** |
| `shouldSuccessfullySendAndParseCompletion` | HTTP 200 Success | Sends structured prompt, parses JSON | **PASSED** |
| `shouldStripMarkdownFencesFromModelContent` | Markdown fences | Strips ` ```json ` fences cleanly | **PASSED** |
| `shouldReportExactErrorWhenModelUnavailable` | HTTP 404 Model Not Found | Reports exact model name & 404 error, halts | **PASSED** |
| `shouldHandleAuthenticationErrors` | HTTP 401 / 403 Auth Error | Reports invalid/expired credentials | **PASSED** |
| `shouldHandleRateLimitErrors` | HTTP 429 Rate Limit | Reports rate limit exceeded | **PASSED** |
| `shouldHandleTimeoutGracefully` | Network timeout | Catches `HttpTimeoutException` | **PASSED** |
| `shouldHandleMalformedResponseWithoutChoices` | Malformed body | Throws `ApiException` for invalid structure | **PASSED** |

### B. Agent Workflow Integration Tests (`AgentWorkflowNebiusIntegrationTest`)
| Test Case | Scenario | Result |
| :--- | :--- | :--- |
| `shouldExecuteCompleteWorkflowWithNebiusNemotron` | Synthetic scenario → Trend → Evidence → Explanation → Action → Nebius Nemotron | **PASSED** (Urgency: MONITOR, Confidence: 0.94, Latency: 4ms) |
| `shouldReportExactErrorAndStopWhenModelUnavailable` | Configured model unavailable (HTTP 404) on Nebius Token Factory | **PASSED** (Exact error reported, execution stopped without substitution) |
| `shouldReportExactErrorAndStopWhenAuthenticationFails` | Nebius authentication failure (HTTP 401) | **PASSED** (Exact error reported, execution stopped without substitution) |

### Test Suite Execution Summary
- **Total Backend Tests:** 76
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Build Status:** SUCCESS

---

## 5. Error & Failure Handling Diagnostics

| Failure Scenario | Error Code / Behavior | System Diagnostic Output |
| :--- | :--- | :--- |
| **Model Unavailable** | HTTP 404 | `Nebius AI Studio model 'nvidia/Llama-3_1-Nemotron-70B-Instruct' is unavailable or not found (HTTP 404): <detail>` |
| **Invalid/Expired Key** | HTTP 401 / 403 | `Nebius AI Studio authentication failed (HTTP 401): <detail>` |
| **Rate Limited** | HTTP 429 | `Nebius AI rate limit exceeded (HTTP 429): Please retry after backoff interval.` |
| **Network Timeout** | Timeout > 30s | `Nebius AI request timed out after 30000ms. Consider increasing AI_TIMEOUT_MS.` |
| **Missing API Key** | Pre-flight check | `Nebius AI provider is configured (OS4ALL_AI_PROVIDER=nebius), but NEBIUS_API_KEY is not configured or empty.` |

---

## 6. How to Switch Modes

### To Run with Real Nebius AI & NVIDIA Nemotron:
In `.env`:
```env
OS4ALL_AI_PROVIDER=nebius
NEBIUS_API_KEY=sk-your-actual-nebius-api-key
NEBIUS_BASE_URL=https://api.studio.nebius.ai/v1
NEBIUS_MODEL=nvidia/Llama-3_1-Nemotron-70B-Instruct
```

### To Run in Local Offline Mock Mode:
In `.env`:
```env
OS4ALL_AI_PROVIDER=mock
```
