# Hackathon Submission Screenshot Checklist

**Project:** OS4All Digital Twin  
**Target:** Competition Submission Portal / Slides / Readme Artifacts  
**Privacy & Security Rule:** Ensure zero API keys, zero local filesystem paths (`C:\Users\...`), and zero personal data are visible in any screenshot.

---

### Recommended Screenshots (10 Key Panels)

| # | Panel Name | Purpose / What to Capture | Best Scenario State |
| :-: | :--- | :--- | :--- |
| **1** | **Main Digital Twin Workstation** | Full widescreen capture showing sidebar patient selector, header badges, hero state card, and top controls. | `STABLE_PATIENT` (Alex Rivera) |
| **2** | **Dual-Stream Data Fusion Card** | Detailed crop of the `DUAL-STREAM DATA FUSION ARCHITECTURE` card showing: `[Static EHR]` + `[Dynamic Wearables]` $\rightarrow$ `[Fused Digital Twin State]`. | `POOR_SLEEP` or `GLUCOSE_SPIKE` |
| **3** | **State Transition Banner** | Highlight the dynamic banner showing: `STATE TRANSITION: STABLE → ELEVATED_RISK` with `Triggered by: Sleep 3.1 hours below baseline`. | Immediately after injecting `POOR_SLEEP` |
| **4** | **Metabolic Prediction Layer** | Highlight `PRIMARY CLINICAL USE CASE` showing `PROTOTYPE RISK SCORE: 78.5 / 100` and the transparent notice. | `GLUCOSE_SPIKE` |
| **5** | **Feature-Level Explainability** | Close-up of the `FEATURE CONTRIBUTIONS & EXPLAINABILITY` table showing green/red arrows, feature names, weights (+18 pts, +15 pts), and descriptions. | `GLUCOSE_SPIKE` |
| **6** | **Personalized Baseline Comparison Engine** | The data table showing `Metric`, `Baseline Mean`, `Current Value`, `Deviation (Δ)`, `Z-score (e.g. -2.40σ)`, and `Trend Direction`. | `POOR_SLEEP` |
| **7** | **Dynamic Wearable Telemetry & CGM Chart** | The 7-day bar chart with baseline horizontal threshold and metric selector tabs (`Glucose`, `Resting HR`, `HRV`, `Sleep`). | `GLUCOSE_SPIKE` (showing spike bars) |
| **8** | **Patient Historical Records & Timeline** | Two-column panel showing ICD-10 medical records, medications, and unified chronological health event timeline. | Default / Loaded |
| **9** | **Virtual Patient Interaction (Grounded Q&A)** | Conversation history showing the clinician asking *"Why is this patient currently elevated risk?"* and the grounded reply. | Answer rendered |
| **10** | **Simulation Controller in Action** | The 5 scenario buttons with one active (highlighted in orange/red) and the `Next Reading Tick` button. | Any active simulation |

---

### Submission Image Hygiene Verification:
- [x] No `AIza...` or token strings visible.
- [x] No terminal or IDE window frames in the crop.
- [x] Standard browser 1080p / 1440p clean aspect ratio.
- [x] Medical disclaimer banner clearly visible at the top right.
