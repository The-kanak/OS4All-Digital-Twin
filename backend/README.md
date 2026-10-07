# OS4All Backend — Preventive Health Intelligence Operating System

## Overview

The OS4All backend is a **Java 21 / Spring Boot 3.3.x** application that implements the foundational layer of a preventive health intelligence operating system. This service handles:

- **Authentication & JWT security** — Registration, login, and stateless bearer token validation.
- **User profiles** — Biological demographics and clinical lifestyle attributes.
- **Granular consent management** — Explicit per-category privacy opt-ins for AI inference, evidence search, and research.
- **Tamper-evident audit logging** — SHA-256 hash-chained ledger of all health data access and AI operations.
- **Flyway-managed database migrations** — Reproducible MySQL schema evolution.
- **OpenAPI 3 / Swagger documentation** — Interactive API explorer at `/swagger-ui.html`.
- **Spring Boot Actuator** — System health and telemetry at `/actuator/health`.

> ⚠️ **OS4All never provides medical diagnoses.** All AI-generated outputs clearly distinguish observation (data facts) from interpretation (physiological context), and always recommend professional medical evaluation when appropriate.

---

## Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java JDK | 17 LTS (or 21 LTS) | [Eclipse Adoptium Temurin](https://adoptium.net/) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org/) |
| MySQL | 8.0+ | [mysql.com](https://dev.mysql.com/downloads/) |
| Git | Any | [git-scm.com](https://git-scm.com/) |

---

## 1. Running MySQL

### Option A: Using Docker (Recommended for local dev)

```bash
docker run -d \
  --name os4all-mysql \
  -e MYSQL_DATABASE=os4all \
  -e MYSQL_ROOT_PASSWORD=your_mysql_password \
  -p 3306:3306 \
  mysql:8.0
```

### Option B: Using Local MySQL Installation

1. Open `mysql` client as root:
```bash
mysql -u root -p
```
2. Create database:
```sql
CREATE DATABASE IF NOT EXISTS os4all;
```

---

## 2. Environment Variables

Copy the provided template to a `.env` file (never commit this file):

```bash
cp .env.example .env
```

Then set the following environment variables. You can set them in your shell profile, a `.env` file loaded with a tool like `direnv`, or directly in your IDE run configuration:

| Variable | Description | Default (dev only) |
|---|---|---|
| `SERVER_PORT` | HTTP port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Spring profile (`dev`, `test`, `prod`) | `dev` |
| `DB_HOST` | MySQL host | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database name | `os4all` |
| `DB_USERNAME` | Database user | `root` |
| `DB_PASSWORD` | Database password | `your_mysql_password` |
| `JWT_SECRET` | **REQUIRED in prod**: Hex-encoded 256-bit HMAC secret | Dev fallback only |
| `JWT_EXPIRATION_MS` | Access token lifetime (ms) | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token lifetime (ms) | `604800000` (7 days) |
| `AUDIT_LOG_ENABLED` | Enable tamper-evident audit logs | `true` |
| `NEBIUS_API_KEY` | (Phase 3) Nebius AI Cloud API key | — |
| `TAVILY_API_KEY` | (Phase 3) Tavily evidence search key | — |

> **Security:** Never hardcode `JWT_SECRET`, `DB_PASSWORD`, or API keys. Use environment variables or a secrets manager (HashiCorp Vault, AWS Secrets Manager) in production.

### Generating a Secure JWT Secret

```bash
# Generate a cryptographically random 256-bit hex key
openssl rand -hex 32
```

---

## 3. Starting the Backend

### Windows (PowerShell — set env vars first)

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="os4all"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:SPRING_PROFILES_ACTIVE="dev"
$env:JWT_SECRET="your_generated_secret_here"

cd backend
mvn spring-boot:run
```

### Windows (using build.bat)

```batch
cd backend
build.bat
```

### Linux / macOS

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=$(openssl rand -hex 32)

cd backend
mvn spring-boot:run
```

### Expected Startup Output

```
  .   ____          _
 /\\ / ___'_ __ _ _(_)_ __  __ _
( ( )\___ | '_ | '_| | '_ \/ _` |
 \\/  ___)| |_)| | | | | || (_| |
  '  |____| .__|_| |_|_| |_\__, |
 =========|_|==============|___/=====|_|
 :: Spring Boot :: (v3.3.4)

...
[INFO] OS4All backend started on port 8080
```

---

## 4. Running Tests

Tests use an **in-memory H2 database** (in MySQL mode) — no MySQL connection needed.

```bash
cd backend
mvn test
```

Or on Windows:

```batch
cd backend
test.bat
```

### Test Coverage

| Test Class | Type | What it covers |
|---|---|---|
| `JwtServiceTest` | Unit | Token generation, extraction, expiry validation |
| `AuthServiceTest` | Unit (Mockito) | Registration, login, bad credentials, audit calls |
| `AuditServiceTest` | Unit (Mockito) | Hash chain integrity, previous hash linking |
| `AuthIntegrationTest` | Integration | Full Spring Boot context; register/login/duplicate/bad-creds |
| `UserControllerTest` | Integration | Profile GET/PUT, consent POST/GET with JWT |
| `HealthControllerTest` | Integration | Public health check endpoint |

---

## 5. API Documentation

When the backend is running, browse interactive API documentation at:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 6. Endpoint Reference (Phase 1)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | None | Register new user account |
| `POST` | `/api/v1/auth/login` | None | Login and receive JWT tokens |
| `GET` | `/api/v1/users/me` | JWT | Fetch current user profile |
| `PUT` | `/api/v1/users/me` | JWT | Update profile demographics |
| `POST` | `/api/v1/users/consent` | JWT | Grant or revoke a specific consent |
| `GET` | `/api/v1/users/consent` | JWT | List all active/revoked consents |
| `GET` | `/api/v1/health` | None | Application health status |
| `GET` | `/actuator/health` | None | Spring Boot Actuator system check |

### Quick Test with curl

**Register:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"sarah@example.com","password":"Password123!","fullName":"Sarah Connor"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sarah@example.com","password":"Password123!"}'
```

**Get Profile (replace TOKEN):**
```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer TOKEN"
```

**Update Consent:**
```bash
curl -X POST http://localhost:8080/api/v1/users/consent \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"consentType":"AI_INFERENCE","granted":true}'
```

---

## 7. Project Structure

```
backend/
├── src/
│   ├── main/java/org/os4all/
│   │   ├── Os4AllApplication.java       # Entry point
│   │   ├── core/
│   │   │   ├── common/ApiResponse.java  # Standard API response wrapper
│   │   │   ├── config/                  # OpenAPI, CORS
│   │   │   ├── security/               # JWT, UserDetails, SecurityConfig
│   │   │   └── exception/              # GlobalExceptionHandler, custom exceptions
│   │   └── modules/
│   │       ├── auth/                   # Registration, login, DTOs
│   │       ├── user/                   # User entity, profile, UserService
│   │       ├── consent/                # ConsentType, UserConsent, ConsentService
│   │       ├── audit/                  # AuditLog entity, AuditService
│   │       └── observability/          # Health check controller
│   ├── main/resources/
│   │   ├── application.yml             # Main config (env-var driven)
│   │   ├── application-test.yml        # H2 test config
│   │   └── db/migration/              # Flyway SQL scripts
│   └── test/java/org/os4all/
│       ├── core/security/              # JwtServiceTest
│       ├── modules/auth/               # AuthServiceTest, AuthIntegrationTest
│       ├── modules/user/               # UserControllerTest
│       ├── modules/audit/              # AuditServiceTest
│       └── modules/observability/      # HealthControllerTest
├── build.bat                           # Windows compile helper
├── test.bat                            # Windows test helper
├── pom.xml                             # Maven build
└── .env.example                        # Environment variable template
```

---

## 8. Security Architecture

- **Passwords:** BCrypt (cost factor 12) — never stored or logged in plaintext.
- **JWT:** HMAC-SHA256, 15-minute access tokens, 7-day rotating refresh tokens.
- **Consent enforcement:** All health data queries validate user consent before dispatching to external AI or search providers.
- **Audit trail:** Every authentication event, profile update, and consent change is recorded in a SHA-256 hash-chained tamper-evident ledger.
- **Zero secrets in code:** All credentials, API keys, and JWT secrets come from environment variables.

---

## 9. What's Next — Implementation Phases

| Phase | Status | Description |
|---|---|---|
| **Phase 1: Foundation** | ✅ Complete | Auth, profiles, consents, audit logging |
| **Phase 2: Baselines** | Pending | Biometric ingestion, personal baseline (Welford), trend & anomaly state machine |
| **Phase 3: AI & Evidence** | Pending | Nebius/Nemotron LLM orchestration, Tavily search, structured insight generation |
| **Phase 4: Labs & OCR** | Pending | PDF/image upload, OCR extraction, LOINC normalization |
| **Phase 5: Timeline & Counselor** | Pending | Unified health timeline, guardrailed AI counselor |
| **Phase 6: Flutter Client** | Pending | Healthcare UI, charts, AI counselor chat interface |
