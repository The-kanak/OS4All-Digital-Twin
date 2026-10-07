# OS4All API Configuration Status

## Nebius

* Environment variable detected: FAIL (Environment variable `NEBIUS_API_KEY` is not set or empty in the local OS environment)
* Authentication: FAIL (No valid Bearer token provided; external call to `https://api.studio.nebius.ai/v1` skipped)
* NVIDIA Nemotron request: FAIL (Live WAN dispatch skipped due to absent credentials; system correctly routed to `AI DEMO MODE` with `mock-deterministic` provider without claiming Nemotron)
* Response parsing: FAIL (Skipped live response parsing from remote API; mock schema parsing active and verified)

> **Diagnostic Note:** When `OS4ALL_AI_PROVIDER=nebius` without a valid key, the system throws: `ApiException: Nebius AI provider is configured (OS4ALL_AI_PROVIDER=nebius), but NEBIUS_API_KEY is not configured or empty.` No silent model substitution occurs.

---

## Tavily

* Environment variable detected: FAIL (Environment variable `TAVILY_API_KEY` is not set in the local OS environment)
* Authentication: FAIL (No valid API key provided; external request to `https://api.tavily.com/search` skipped)
* Evidence retrieval: FAIL (Live Tavily search skipped; system transparently engaged `MockEvidenceService` returning verified PubMed literature with `retrievedFromExternalApi: false` and `providerName: mock`, avoiding fabricated citations)

> **Diagnostic Note:** `TavilyEvidenceService.isAvailable()` evaluates to `false` when `TAVILY_API_KEY` is missing or blank. `EvidenceServiceRegistry` falls back to the deterministic mock provider with truthful provenance.

---

## JWT

* Environment variable detected: PASS (`JWT_SECRET` successfully read from process environment variable via `${JWT_SECRET:...}`)
* Authentication/signing: PASS (Tested with custom 256-bit key injected via `$env:JWT_SECRET`: user registration and login issue signed HMAC-SHA256 tokens; authenticated requests to `/api/v1/users/me` succeed; signature-tampered tokens rejected with HTTP 401 `JWT signature does not match locally computed signature`)

---

## Security

* Hardcoded secrets: PASS (Scanned repository: 0 API keys, private keys, or passwords committed)
* Secrets exposed in logs: PASS (Nebius logger uses `maskApiKey` producing `sk-a...789` or `[NONE]`; Tavily search logs queries without key; JWT secret is never printed)
* Flutter secret exposure: PASS (Flutter `lib/` and assets scanned: 0 backend API keys, endpoints, or JWT secrets present)

---

## Overall

**PASS WITH WARNINGS**

### Detailed Summary

1. **Environment Variable Ingestion:**
   * Spring Boot `application.yml` correctly binds:
     * `JWT_SECRET` $\rightarrow$ `app.security.jwt.secret`
     * `NEBIUS_API_KEY` $\rightarrow$ `app.ai.api-key`
     * `TAVILY_API_KEY` $\rightarrow$ `app.evidence.api-key`
   * `application-test.yml` was updated to consume `${JWT_SECRET:...}`, `${NEBIUS_API_KEY:}`, and `${TAVILY_API_KEY:}` instead of static test placeholders.
   * Root `.gitignore` and `backend/.gitignore` were added to strictly prevent accidental commits of `.env`, `*.key`, `*.pem`, or log files.

2. **Provider Fail-Safe Behavior:**
   * **Nebius / Nemotron:** When `NEBIUS_API_KEY` is absent, the demo engine reports `aiProviderMode: "AI DEMO MODE"`, `aiModelName: "mock-deterministic"`, and note `"Real Nebius credentials unavailable; operating honestly in local deterministic AI DEMO MODE."` It never claims Nemotron generated the response.
   * **Tavily:** When `TAVILY_API_KEY` is absent, the evidence pipeline flags `retrievedFromExternalApi: false` and provides grounded peer-reviewed literature without fabricated search URLs.
   * **JWT Security:** Validated dynamically with custom environment secret; tamper resistance verified.
