# OS4All REST API Reference

Base URL: `http://localhost:8080/api/v1`  
OpenAPI / Swagger UI: `http://localhost:8080/swagger-ui/index.html`  
Interactive OpenAPI Spec: `http://localhost:8080/v3/api-docs`

---

## 1. Authentication & User Profile

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user with email & password | No |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain Bearer JWT token | No |
| `GET` | `/api/v1/user/profile` | Retrieve user profile & demographics | Yes |
| `PUT` | `/api/v1/user/profile` | Update profile demographics | Yes |

---

## 2. Consent Management

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/v1/consent` | List current user consent status | Yes |
| `POST` | `/api/v1/consent` | Grant or revoke consent (`AI_INFERENCE`, `DATA_STORAGE`, `EVIDENCE_SEARCH`) | Yes |

---

## 3. Health Observations & Lab Ingestion

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/health/observations` | Ingest normalized continuous observation | Yes |
| `GET` | `/api/v1/health/observations` | List filtered observations (by type, date range) | Yes |
| `POST` | `/api/v1/reports/upload` | Upload PDF/PNG/JPG lab report for OCR extraction | Yes |
| `GET` | `/api/v1/reports` | List uploaded lab reports & review flags | Yes |
| `GET` | `/api/v1/reports/{id}` | Get report details with extracted biomarkers | Yes |
| `POST` | `/api/v1/reports/{id}/confirm` | Confirm extracted biomarkers into timeline | Yes |
| `GET` | `/api/v1/health/timeline` | Unified chronological feed of observations, labs & insights | Yes |

---

## 4. Personal Baseline & Anomaly Engine

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/v1/health/baseline` | Get complete individual baseline profile | Yes |
| `GET` | `/api/v1/health/baseline/{metric}` | Get detailed baseline for a specific continuous metric | Yes |
| `GET` | `/api/v1/health/signals` | Evaluate current signals against personal baselines | Yes |
| `GET` | `/api/v1/health/anomalies` | List active anomaly events & compound patterns | Yes |
| `GET` | `/api/v1/health/status` | Complete aggregate health status (`STABLE`, `DRIFT`, `ANOMALY`, `FOLLOW_UP`) | Yes |

---

## 5. AI Orchestration & Evidence

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/ai/insights/evaluate` | Trigger Nemotron multi-agent health evaluation | Yes (Consent) |
| `GET` | `/api/v1/ai/insights/logs` | Paginated AI inference audit logs | Yes |
| `POST` | `/api/v1/evidence/retrieve` | Constrained search query execution via Tavily | Yes (Consent) |

---

## 6. AI Health Counselor

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/counselor/chat` | Send conversational query grounded in personal health context | Yes (Consent) |
| `GET` | `/api/v1/counselor/conversations` | List conversation threads | Yes |
| `GET` | `/api/v1/counselor/conversations/{id}` | Get conversation thread & message history | Yes |
| `DELETE` | `/api/v1/counselor/conversations/{id}` | Delete conversation thread | Yes |

---

## 7. Flagship Hackathon Demonstration

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/v1/demo/run-flagship-scenario` | Run complete 9-step reproducible demonstration chain | No (Permitted for judging) |
