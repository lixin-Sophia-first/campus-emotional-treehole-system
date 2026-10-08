# Tree Hole

Tree Hole is an English-language campus anonymous community and emotional-support platform. A public treehole post and a private journal entry are both represented as an **emotion record**; visibility determines whether the record enters the community. Risk detection and content moderation are deliberately separate.

> This application supports care workflows. It does not provide medical diagnosis and does not automatically contact a school, family member or emergency service.

## Technology

- Web: Next.js 16, React 19, TypeScript and CSS
- API: Java 21, Spring Boot 4.1, Spring Security and Spring Data JPA
- Data: PostgreSQL 17 with Flyway migrations
- Local email: Mailpit
- Delivery: Docker Compose
- AI: deterministic English rules plus an optional OpenAI-compatible JSON endpoint

## Start the complete application

1. Install and open Docker Desktop.
2. Copy `.env.example` to `.env`.
3. Change at least `ADMIN_PASSWORD` and `JWT_SECRET` in `.env`.
4. From the project directory, run `docker compose up --build`.
5. Wait until `postgres`, `api` and `web` report healthy.

Open:

- Website: <http://localhost:3000>
- API documentation: <http://localhost:8080/swagger-ui.html>
- Local test mailbox: <http://localhost:8025>
- API health: <http://localhost:8080/actuator/health>

The administrator signs in with `ADMIN_EMAIL` and `ADMIN_PASSWORD`. A student registration email appears in Mailpit; open that message and follow its verification link. A teacher first submits a teacher application, then the administrator approves it.

Stop the application with `docker compose down`. Data remains in the named PostgreSQL volume. Use `docker compose down -v` only when you intentionally want to delete local application data.

## Architecture

```text
Browser
  │
  ├── Next.js web :3000
  │      └── /api proxy
  │
  └── Spring Boot API :8080
          ├── PostgreSQL :5432
          ├── Mailpit SMTP :1025
          └── Optional OpenAI-compatible service
```

Flyway owns the database schema. Hibernate validates it at startup but does not create or silently change production tables. AI work is stored in a database task queue so failures and retries are visible to the administrator.

## Role and privacy matrix

| Capability | Student | Teacher | Administrator |
|---|---:|---:|---:|
| Read public community records | Yes | Yes | Yes |
| Create and interact with records | Yes | No | No |
| Read a student's private record body | Owner only | Never | Never |
| View the anonymous risk queue | Own cases | Yes | Yes |
| Claim a case and provide anonymous care | No | Yes | Assign/transfer only |
| View identity for a HIGH case | Own identity | With approval | Approval authority |
| View identity for a CRITICAL case | Own identity | Current assignee only | Yes |
| Submit/confirm case closure | No | Submit proposal | Confirm, return or reassign |
| Publish support resources | No | Yes | May take down/restore |
| Moderate community content/accounts | No | No | Yes |

Private record text is never copied into a risk case, AI task row, analysis result or audit log. A private event exposes only classification metadata. Reassignment immediately removes the previous teacher's case and identity access because access is always checked against the current assignee. Identity views and sensitive case changes are audited.

Private entries are always analyzed by local English rules only. Even when `AI_BASE_URL`, `AI_API_KEY` and `AI_MODEL` are configured, only public record bodies may be sent to the optional external AI classifier. Existing private tasks and administrator retries cannot send private text to that service.

Teachers may claim an unassigned risk case. The administrator may assign or transfer it at any time. A case still unclaimed 24 hours after creation is automatically assigned to an active teacher with the fewest active cases; the background job checks every minute. If no active teacher exists, it remains in the queue for administrator attention.

## Risk levels

| Level | Automated handling | Student experience | Teacher workflow |
|---|---|---|---|
| `LOW` | Normal distribution | Encouragement | No case |
| `MEDIUM` | Normal distribution | Grounding and support card | No case |
| `HIGH` | Removed from recommendation/popular ranking | Strong support and optional care | Anonymous case; identity needs admin approval |
| `CRITICAL` | Removed from recommendation/popular ranking | Persistent urgent-support options | Highest-priority case; identity only for current assignee |

Explicit plan, imminent timing or means cannot be downgraded by a model response. If the model is missing, slow or invalid, local English rules remain active. Content violations such as harassment, sexual content, illegal content and spam use a separate moderation result.

## Local development and tests

Requirements: Node.js 24, Java 21, Maven 3.9 and PostgreSQL, or Docker Desktop.

```bash
cd web
npm install
npm run build
npm test

cd ../api
mvn test
```

Backend integration tests use Testcontainers and therefore require Docker. The repository must never contain API keys, passwords, database exports, logs, Mailpit messages or personal data.

## Before any real-campus deployment

- Review the optional external AI provider before sending public record bodies to it. Private entries remain local-only.
- Replace local test email and example support contacts with verified campus services, opening hours and an agreed human response/escalation procedure. The software does not contact emergency services automatically.
- Anyone with a working email address may register as a student. Define how teacher staff identity is verified and who handles cases when the administrator is unavailable and no active teacher can accept automatic assignment.
- Approve retention, deletion and access-review periods for private records, risk-case metadata, messages and audit logs. Soft-deleting a record does not erase its historical case/audit data.
- Review deployment security before exposing the site publicly: HTTPS, production email, backups, secrets, monitoring and access-token revocation. Logging out or resetting a password revokes refresh tokens, but an already-issued access token can remain usable until its 15-minute expiry.

## Demonstration flow

1. Register a student and verify the email in Mailpit.
2. Create one public record and one private record; confirm only the public record appears in Community.
3. Open the student's timeline and switch between weekly and monthly trends.
4. Create an English HIGH or CRITICAL example and show that it is excluded from Recommended and Popular.
5. Sign in as an approved teacher, claim the case and open the anonymous care workspace. An unclaimed case is assigned automatically after 24 hours.
6. For HIGH, request identity access and approve it as the administrator. For CRITICAL, show that only the current assignee can view identity.
7. Transfer the case and confirm the former teacher loses access.
8. Add a follow-up and propose closure; confirm or return it as the administrator.
9. Demonstrate reports, the moderation queue, AI-task retry, account actions, notifications and the audit log.
10. Show that neither the teacher nor administrator can retrieve a private emotion-record body.

## Git workflow

`main` should remain runnable. Use short-lived branches prefixed with `codex/` and Conventional Commits such as `feat: add risk case transfer`. The original prototype is tagged `v1.0.0`; the completed platform is released as `v1.1.0`. A private GitHub remote can be added when repository credentials are available.
