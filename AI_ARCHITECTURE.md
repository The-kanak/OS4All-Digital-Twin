# OS4All AI Intelligence Layer Architecture

## 1. Multi-Agent Design Pattern

OS4All employs a sequential, 5-agent pipeline orchestrated before invocation of the foundational model:

```
               ┌───────────────────────────────┐
               │    StructuredHealthContext    │
               │   (Read-Only Data Snapshot)   │
               └───────────────┬───────────────┘
                               │
                               ▼
               ┌───────────────────────────────┐
               │   1. Health Context Agent     │
               │ Curates baseline deviations,  │
               │ active anomalies & labs       │
               └───────────────┬───────────────┘
                               │
                               ▼
               ┌───────────────────────────────┐
               │ 2. Trend Interpretation Agent │
               │ Disaggregates factual signals │
               │ from non-diagnostic hypotheses│
               └───────────────┬───────────────┘
                               │
                               ▼
               ┌───────────────────────────────┐
               │      3. Evidence Agent        │
               │ Conditionally queries Tavily  │
               │ for peer-reviewed literature  │
               └───────────────┬───────────────┘
                               │
                               ▼
               ┌───────────────────────────────┐
               │    4. Explanation Agent       │
               │ Synthesizes accessible,       │
               │ baseline-grounded narrative   │
               └───────────────┬───────────────┘
                               │
                               ▼
               ┌───────────────────────────────┐
               │       5. Action Agent         │
               │ Formulates non-invasive steps │
               │ & clinical evaluation criteria│
               └───────────────┬───────────────┘
                               │
                               ▼
               ┌───────────────────────────────┐
               │    NVIDIA Nemotron-4-340B     │
               │    via Nebius AI Studio       │
               └───────────────────────────────┘
```

---

## 2. NVIDIA Nemotron-4-340B-Instruct Integration via Nebius AI Studio

- **Endpoint**: `https://api.studio.nebius.ai/v1/chat/completions`
- **Model**: `nvidia/nemotron-4-340b-instruct`
- **Authentication**: Bearer Token via environment variable `APP_AI_API_KEY`.
- **System Prompt Directives**:
  1. Strictly distinguish verified physiological observations from interpretations.
  2. Never assign clinical diagnoses or assert that personal deviation equals pathology.
  3. Avoid hallucinations: never fabricate lab values, vitals, or medical history.
  4. Always specify non-invasive lifestyle interventions and criteria for professional evaluation.
  5. Enforce strict JSON schema compliance.

---

## 3. Evidence Retrieval via Tavily

- **Selective Invocation**: Only executed when active multi-signal departures or confirmed abnormal lab biomarkers are detected.
- **Constrained Query Generation**: Formulates targeted medical queries (e.g. `"heart rate variability and resting heart rate elevation during acute sleep restriction recovery"`).
- **Domain Constraints**: Constrained to scientific repositories:
  - `pubmed.ncbi.nlm.nih.gov`
  - `nature.com`
  - `frontiersin.org`
  - `ahajournals.org`
- **Data Segregation**: Retrieved articles are attached as external citations; they are never merged into the patient's medical record.

---

## 4. Structured Output Schema

All AI inferences conform strictly to the validated schema:

```json
{
  "summary": "Concise non-diagnostic physiological assessment",
  "observations": [
    "Resting HR measured at 74 bpm (+14 bpm above personal baseline)"
  ],
  "possibleInterpretations": [
    "Autonomic strain pattern consistent with acute sleep debt and sympathetic dominance"
  ],
  "evidence": [
    {
      "title": "Heart rate variability as a marker of autonomic recovery and physical stress",
      "source": "Frontiers in Physiology / PubMed",
      "urlOrDoi": "https://doi.org/10.3389/fphys.2018.00532",
      "relevance": "Suppressed HRV and elevated resting pulse reliably correlate with physiological recovery debt"
    }
  ],
  "recommendedActions": [
    "Prioritize 8 hours of restorative sleep over next 48-72 hours",
    "Deload strenuous training to active recovery"
  ],
  "urgency": "MONITOR",
  "confidence": 0.94,
  "disclaimer": "OS4All AI insights are informational health interpretations based strictly on personal historical baselines. They DO NOT constitute a medical diagnosis."
}
```

---

## 5. Offline Fallback & Reliability

If remote Nebius API or Tavily connections are unavailable:
- The system gracefully transitions to `MockModelProvider` and `MockEvidenceService`.
- Guarantees 100% test reproducibility and uninterrupted hackathon demonstrations.
