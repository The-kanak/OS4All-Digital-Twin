# OS4All Evidence Retrieval Architecture (Tavily Integration)

## 1. Overview & Core Principles

The **OS4All Evidence Retrieval Engine** provides real-time, external scientific grounding for the AI Intelligence Layer. 

### Core Ethical & Technical Principles:
1. **Evidence is Selective, NOT Omnipresent**: Tavily search is **never** invoked for every routine AI request. It is strictly triggered only when active physiological anomalies, multi-signal autonomic strains, or out-of-range clinical biomarkers are detected.
2. **Never Treat Arbitrary Web Content as Medical Truth**: Search queries are strictly constrained to authoritative medical and scientific domains:
   - `nih.gov`, `ncbi.nlm.nih.gov`, `pubmed.ncbi.nlm.nih.gov`
   - `nature.com`, `nejm.org`, `thelancet.com`
   - `ahajournals.org`, `diabetesjournals.org`, `bmj.com`
   - `frontiersin.org`, `cdc.gov`, `who.int`
3. **Strict Separation of Evidence and Health Records**: External literature citations and web snippets are stored in volatile search results or dedicated audit logs; they **never contaminate the user's primary medical observation records**.
4. **Backend-Enforced Security**: Tavily API keys are maintained exclusively on the backend (`TAVILY_API_KEY`) and **never exposed to client frontends or Flutter mobile apps**.

---

## 2. Evidence Retrieval Workflow

```
[Personal Baseline & Anomaly Engine]
                 │
                 ▼
     [Detected Health Pattern]
  (e.g., Sleep ↓ + RHR ↑ + HRV ↓)
                 │
                 ▼
  [Determine Need for Evidence] ─── (State STABLE? No deviations? ──> Skip External Search)
                 │ (State ANOMALY / DRIFT / Lab Out-of-Bounds)
                 ▼
   [Generate Constrained Query]
     (Targets verified biomedical keywords)
                 │
                 ▼
  [EvidenceService: Tavily or Mock]
                 │
                 ▼
    [Filter & Relevance Ranking]
     (Min score >= 0.65, trusted domain check)
                 │
                 ▼
   [Evidence Agent → AI Orchestrator]
                 │
                 ▼
  [Structured AI Insight Response]
     (Observations, Explanations, Actions, and Grounded Literature Citations)
```

---

## 3. Data Models

### `EvidenceQuery`
- `queryText`: Targeted biomedical search query string.
- `topicKeywords`: Specific physiological keywords (e.g. `["HRV", "autonomic recovery"]`).
- `physiologicalContext`: State and direction of deviation.
- `includeMedicalDomainsOnly`: Boolean flag enforcing domain whitelisting.
- `maxResults`: Number of citations requested (default 3).

### `EvidenceSource`
- `title`: Article or publication title.
- `url`: Direct source link (e.g. PubMed or DOI link).
- `domain`: Authoritative publisher domain (e.g. `ncbi.nlm.nih.gov`).
- `snippet`: Extracted relevant scientific context.
- `relevanceScore`: Normalized relevance confidence score.
- `retrievedAt`: UTC timestamp of search execution.
- `isPeerReviewedOrGov`: Boolean validating trusted domain provenance.

### `EvidenceResult`
- `query`: The origin query parameters.
- `sources`: Ranked and filtered collection of `EvidenceSource` items.
- `retrievedFromExternalApi`: `true` for live Tavily calls, `false` for offline mock.
- `providerName`: Active provider (`"tavily"` or `"mock"`).
- `latencyMs`: Retrieval execution latency in milliseconds.

---

## 4. Configuration

Evidence retrieval is completely configurable and optional via environment variables:

| Variable | Default | Description |
|---|---|---|
| `EVIDENCE_ENABLED` | `true` | Globally toggle evidence retrieval |
| `EVIDENCE_PROVIDER` | `mock` | Active provider (`mock` or `tavily`) |
| `TAVILY_API_KEY` | *(empty)* | Live API key for Tavily Search |
| `TAVILY_API_URL` | `https://api.tavily.com/search` | Tavily REST endpoint |
| `EVIDENCE_MAX_RESULTS`| `3` | Maximum citations to supply to the LLM |
| `EVIDENCE_MIN_RELEVANCE`| `0.65` | Relevance score threshold |
| `EVIDENCE_TIMEOUT_MS` | `10000` | HTTP timeout (milliseconds) |

If `EVIDENCE_PROVIDER=tavily` is specified without an API key, the system automatically falls back to `MockEvidenceService`, ensuring zero downtime in offline or local developer environments.

---

## 5. Hackathon Verification & Test Summary

- Fully covered in `EvidenceRetrievalIntegrationTest.java`:
  - Constrained query validation and domain checking.
  - Verification of selective AI retrieval (skipped when stable, invoked on autonomic strain).
  - Standalone `GET /api/v1/evidence/search` endpoint verification.
- **Pass Rate**: 58 / 58 backend tests passing.
