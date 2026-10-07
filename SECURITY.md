# OS4All Security & Compliance Model

## 1. Zero Trust & Defense in Depth

OS4All enforces a healthcare-grade security posture across the entire data lifecycle:

1. **Client Isolation**:
   - The Flutter frontend contains **ZERO API keys or cloud secrets**.
   - Communication occurs solely through authenticated Spring Boot REST endpoints using short-lived Bearer JWT tokens.

2. **Stateless JWT Authentication**:
   - Standard HS256 / SHA-256 HMAC digital signatures.
   - Configurable expiration (default 24 hours).
   - Strict subject verification and password hashing using BCrypt (cost factor 10).

3. **Granular Consent Management**:
   - `DATA_STORAGE`: Permission to persist sensor metrics and lab documents.
   - `AI_INFERENCE`: Permission to feed anonymized read-only context to the multi-agent AI pipeline.
   - `EVIDENCE_SEARCH`: Permission to formulate queries to Tavily.
   - Any AI evaluation or counselor interaction immediately aborts with HTTP 400 if user consent is not granted.

4. **Tamper-Evident Cryptographic Audit Logging**:
   - Every security-relevant event (`USER_REGISTERED`, `LOGIN`, `CONSENT_UPDATED`, `REPORT_OCR_UPLOADED`, `AI_INFERENCE_EXECUTED`, `COUNSELOR_CHAT`) writes to `AuditLog`.
   - Each audit log entry computes a cryptographic hash linking to the prior record (`previousHash`), forming an immutable chain:
     $$\text{integrityHash} = \text{SHA256}(\text{actor} + \text{action} + \text{resourceId} + \text{clientIp} + \text{timestamp} + \text{previousHash})$$

5. **OCR File Upload Security**:
   - Magic byte inspection: Verifies true MIME headers (PDF `%PDF-`, PNG `\x89PNG`, JPEG `\xFF\xD8\xFF`).
   - File size ceiling: Enforces maximum 15MB file upload limit.
   - Storage isolation: Uploaded files are written to isolated directories and never stored permanently in the git repository.

6. **Clinical Emergency Classifier**:
   - Rule-based safety gate inspects all user queries before AI generation.
   - Severe expressions (crushing chest pain, shortness of breath, slurred speech, suicidal ideation) trigger immediate safety redirection to emergency services (911/112), bypassing AI interpretation.
