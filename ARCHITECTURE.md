# OS4All System Architecture

```
                                  OS4All ARCHITECTURE
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 PRESENTATION TIER                                      │
│                                                                                        │
│   Flutter Mobile & Web App (19 Screen Polished Healthcare UI / Dark Navy & Cyan)      │
│   • Dashboard & Overview       • Vitals Trackers         • Personal Baseline Visualizer│
│   • Multi-Signal Anomaly View  • Report Upload & OCR     • Evidence & Citations Explorer│
│   • AI Health Counselor Chat   • Unified Timeline View   • Flagship 9-Step Demo Runner │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ HTTP / REST / Bearer JWT
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                  BACKEND API GATEWAY                                   │
│                                                                                        │
│   Spring Boot 3.3.4 REST Controllers (OpenAPI / Swagger 3.0 Documentation)             │
│   • Security Filter Chain: Stateless Bearer JWT Authentication                         │
│   • User Consent Enforcer: Granular verification (AI_INFERENCE, EVIDENCE_SEARCH)       │
│   • Rate Limiting & Input Validation: JSR-380 / Hibernate Validator                    │
└───────┬──────────────────────┬──────────────────────┬──────────────────────────┬───────┘
        │                      │                      │                          │
        ▼                      ▼                      ▼                          ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐           ┌──────────────┐
│  INGESTION   │       │   BASELINE   │       │   ANOMALY    │           │     OCR      │
│    ENGINE    │       │    ENGINE    │       │    ENGINE    │           │   PIPELINE   │
│              │       │              │       │              │           │              │
│ • Health Obs │       │ • Mean/Median│       │ • Rule-based │           │ • PDF/PNG/JPG│
│ • Vitals     │──────►│ • Std Dev    │──────►│ • Persistent │           │ • Magic bytes│
│ • Normalizer │       │ • Confidence │       │   Drift      │           │ • Confidence │
│   (mg/dL,bpm)│       │ • Threshold  │       │ • Multi-sig  │           │   Extraction │
└──────────────┘       └──────────────┘       └──────┬───────┘           └──────────────┘
                                                     │
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               AI ORCHESTRATION LAYER                                   │
│                                                                                        │
│   Curated Read-Only StructuredHealthContext Snapshot (Zero Direct DB Access for LLM)   │
│                                                                                        │
│   Logical Agent Sequence:                                                              │
│   1. Health Context Agent    ──► Curates baselines, deviations & confirmed labs        │
│   2. Trend Interp Agent      ──► Distinguishes verified facts from hypotheses          │
│   3. Evidence Agent          ──► Formulates constrained scientific search queries      │
│   4. Explanation Agent       ──► Synthesizes plain-language non-diagnostic summary     │
│   5. Action Agent            ──► Formulates lifestyle recovery & physician triage rules│
│                                                                                        │
│   Provider Abstraction (ModelProvider):                                                │
│   • Nebius AI Studio (NVIDIA Nemotron-4-340B-Instruct)                                 │
│   • MockModelProvider (Deterministic offline evaluation & testing)                     │
│                                                                                        │
│   Evidence Retrieval (EvidenceService):                                                │
│   • Tavily Search API (Constrained to PubMed, Nature, Frontiers, Circulation)          │
│   • MockEvidenceService (Deterministic offline citations)                              │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              PERSISTENCE & AUDIT TIER                                  │
│                                                                                        │
│   • Relational Persistence: MySQL / Embedded H2 (Hibernate Dynamic DDL)                │
│   • AI Inference Logs: Timestamped record with prompt, output, latency & model provider│
│   • Cryptographic Audit Trail: SHA-256 chained hash logs with client IP verification   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

## Architectural Tenets

1. **Individual Baselines Over Rigid Population Averages**:
   Continuous vitals are interpreted relative to personal historical variance rather than one-size-fits-all clinical cutoffs.

2. **Transparent Multi-Signal Cross-Correlation**:
   No black-box calculation for core anomaly detection. The system pairs resting heart rate, nocturnal HRV, sleep duration, and temperature into a deterministic, verifiable rule engine.

3. **Isolated AI Read-Only Context**:
   The LLM receives strictly read-only, structured JSON health snapshots. It has no direct database access and cannot mutate records.

4. **Selective Evidence Grounding**:
   External medical literature is retrieved conditionally via Tavily only when significant deviations exist, and is strictly isolated from raw patient charts.

5. **Tamper-Evident Accountability**:
   All user actions, OCR ingestions, consent toggles, and AI inferences write to a SHA-256 chained audit log.
