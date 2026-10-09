# Tree Hole

Tree Hole is a campus space where students can share what is on their mind or keep it in a private journal. The project started with a question beyond posting: if a student writes something concerning, how can the system bring a real person into the loop without treating the student as a rule-breaker or exposing more of their writing than necessary?

I built the student experience, the teacher care workflow, and the administrator tools around that question. This is a local demonstration project, not a counseling or emergency-response service.

## What I built

### One record, two ways to use it

Public treehole posts and private journal entries share the same editor and emotion-record model. Students choose the visibility of each entry, can look back through their own timeline and mood trends, and can interact with public posts through comments, likes, bookmarks, and reports. A public author can appear as **Stranger**; private writing remains visible only to its author, including when it triggers a risk case.

The community includes Latest, Recommended, Popular, search, and tag filtering. Posts have detail pages, while administrators have a separate review path for reported or held content.

### A path from risk detection to human care

The system treats emotional risk and content violations as separate decisions. Distress alone does not count as misconduct. Each entry receives a LOW, MEDIUM, HIGH, or CRITICAL assessment and an appropriate support card. HIGH and CRITICAL entries create risk cases. Public posts at those levels wait for administrator review before appearing in the community; private entries never enter the community.

Teachers can claim cases and speak with students in an anonymous care conversation. An administrator can assign or transfer a case, and an unclaimed case is automatically assigned after 24 hours if an active teacher is available. Teachers record follow-ups and recommend closure; only the administrator can close a case. Notifications and audit records track the important handoffs.

I made identity access depend on the case level and current ownership. For a HIGH case, the assigned teacher must request administrator approval to see the student's account. For a CRITICAL case, the current assigned teacher receives access automatically. Reassignment removes the previous teacher's access. Neither path reveals the body of a private journal entry.

### AI as a second assessment, not a counselor

Every new entry is assessed immediately by local, English-language rules. They look for signals such as negation, first-person statements, plans, timing, and means, and retain the reasons behind a risk result. This first pass works even when no external AI service is configured.

For **public posts only**, an optional OpenAI-compatible service can provide a second assessment in the background. A database-backed job records its state, attempts, errors, and structured result: emotion, intensity, risk level, plan/time/means signals, possible policy violations, confidence, and a support-card category. Administrators can inspect the analysis and retry a failed job. Private journal text is never sent to that service or copied into the job or risk-case records.

The model can raise a risk level, but it cannot override a stronger local warning with a lower one. Timeouts, missing credentials, and invalid model responses leave the local result in place. Students receive reviewed support-card templates rather than free-form model advice. This is assistance with triage, not a diagnosis or an AI chat companion.

When a model marks a public post HIGH or CRITICAL but the local rules did not, the admin console records the disagreement. An administrator can confirm the case, ask the model to draft a short candidate phrase, and separately activate it as a future HIGH-risk local rule. Candidates are checked against their source and a small set of benign examples, versioned, audited, and reversible. Editing the source text, deleting the post, or making it private withdraws its rule. This workflow is a review aid, not evidence that the new rule has been clinically validated.

### The application around it

Students register and verify an email address. Teachers apply for access, then manage their cases, conversations, follow-ups, and support resources. The administrator approves teacher applications, moderates content, manages accounts, reviews AI jobs, controls sensitive identity access, and makes final case decisions. The interface and current risk rules are designed for English input; Chinese risk recognition has not been validated.

## How it is built

- **Frontend:** Next.js 16, React 19, TypeScript
- **Backend:** Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA
- **Data and local services:** PostgreSQL 17, Flyway, Docker Compose, Mailpit
- **Checks:** JUnit and Testcontainers for the API; Vitest and a Playwright visitor/community smoke test for the web app

The Next.js app talks to the Spring Boot API, which stores records, risk cases, notifications, and analysis jobs in PostgreSQL. Mailpit captures verification emails during local development. The external AI endpoint is optional.

## Run locally

Start Docker Desktop and copy the example configuration from the repository root:

```bash
cp .env.example .env
```

Change `ADMIN_PASSWORD` and `JWT_SECRET` in `.env`, then run:

```bash
docker compose up -d --build
```

Open the app at <http://localhost:3000>, test email at <http://localhost:8025>, and API documentation at <http://localhost:8080/swagger-ui.html>. The administrator credentials come from `.env`; the optional AI settings can remain empty. Use invented accounts and posts for the demo, and never commit `.env`.

## Limits of this demo

Tree Hole does not automatically contact family, the school, or emergency services. Its rules are not clinically validated, and the 24-hour assignment fallback is not an emergency-response guarantee. A real campus rollout would require a staffed response policy, verified local support contacts, consent and data-retention decisions, production security and monitoring, and a review of any external AI provider. The current Playwright test is a smoke test, not full end-to-end coverage of all three roles.
