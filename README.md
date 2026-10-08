# Tree Hole

Tree Hole is a campus app built around a simple choice: share a thought with the community or keep it to yourself. Students can write a public post, choose whether to appear as “Stranger”, or save the same kind of entry as a private journal. They can also look back at their mood over time.

The harder question is what happens when a post suggests someone may need help. Tree Hole gives that signal a path to a person: a teacher can take on the case and offer support through an anonymous conversation, while an administrator handles review and sensitive access decisions. It keeps emotional risk separate from rule-breaking, because asking for help should not be treated as misconduct.

This is a local demo project, not a live counseling service. Please use invented accounts and posts when exploring it.

## Take a look

- **As a student:** verify an email address, write public or private entries, browse and interact with public posts, see mood trends, and read support resources.
- **As a teacher:** apply for access, claim a risk case, talk with the student anonymously, record follow-ups, and suggest when a case can close. Teachers can also publish support resources.
- **As an administrator:** approve teacher applications, review held posts and reports, assign or transfer cases, decide identity-access requests, and make the final closure decision.

Private journal text is visible only to its author. A teacher or administrator may see that a private entry triggered a risk case, but not what the student wrote.

## Run it locally

You need Docker Desktop running. From the repository root, create a local config file:

```bash
cp .env.example .env
```

Change `ADMIN_PASSWORD` and `JWT_SECRET` in `.env`, then start the app:

```bash
docker compose up -d --build
```

The example file is for development; never commit your real `.env` or reuse its example passwords in a deployed system. The external AI settings can stay empty.

Once the containers are healthy, open:

| | Local address |
|---|---|
| Tree Hole | <http://localhost:3000> |
| Test inbox | <http://localhost:8025> |
| API documentation | <http://localhost:8080/swagger-ui.html> |

The administrator signs in with the `ADMIN_EMAIL` and `ADMIN_PASSWORD` values from `.env`. Student verification emails appear in the test inbox. A teacher submits an application first; the administrator must approve it before the teacher workspace opens.

To stop the app, run `docker compose down`. Your local PostgreSQL data stays in its Docker volume. `docker compose down -v` also deletes that data, so only use it when you intend to start over.

## A short walkthrough

1. Register a student and open the verification email in Mailpit.
2. Write one public post and one private entry. The community shows only the public one; both appear in the student's own timeline and trends.
3. Post a **synthetic English** example that triggers HIGH risk. The post is saved but held from the community until an administrator reviews it. The student gets a notification.
4. Approve a teacher application, then claim the risk case. The student and teacher can talk without the teacher seeing the student's account by default.
5. For a HIGH case, the teacher can request identity access and the administrator decides. The teacher can suggest closure, but only the administrator can close the case.

You can also try comments, likes, bookmarks, reports, teacher-written resources and notifications. The demo works without an external AI key.

## How the risk flow works

The app checks each new entry right away using local rules written for English text. LOW and MEDIUM entries are saved normally, with different support cards for the student. HIGH and CRITICAL entries create a risk case. If the entry is public, it waits for administrator review before anyone else can see it. An approved high-risk post can appear in Latest and search, but not in Recommended or Popular. A private entry never enters the community.

Teachers can claim unassigned cases; an administrator can assign or transfer them. If no teacher claims a case within 24 hours, a background job assigns it to an active teacher when one is available. For HIGH cases, a teacher needs administrator approval to see the student's account. For CRITICAL cases, only the current assigned teacher gets that access automatically. A transfer removes the previous teacher's access. Identity views and case changes are audited.

Risk is **not** the same as a content violation. Harassment, sexual content, illegal content and spam follow a separate moderation path. The system does not ban someone merely for expressing distress.

## Where AI fits

The local rules are always available. If `AI_BASE_URL`, `AI_API_KEY` and `AI_MODEL` are configured, **public** posts can also be analyzed asynchronously by an OpenAI-compatible `/chat/completions` service. The base URL includes the provider's API prefix, such as `/v1`, but not `/chat/completions` itself. Private entries are never sent to that service.

Analysis jobs live in PostgreSQL, so the administrator can see their status, inspect results and retry failed jobs. A missing, slow or invalid model response leaves the local assessment in place. The model may raise a risk level but cannot lower one already found by the local rules. Students see support cards from fixed templates, not free-form model advice.

Neither the rules nor the model provide a diagnosis. The current rules are designed for English input; Chinese risk recognition has not been validated.

## Under the hood

- **Web:** Next.js 16, React 19 and TypeScript
- **API:** Java 21, Spring Boot 4.1, Spring Security and Spring Data JPA
- **Data:** PostgreSQL 17, with schema changes managed by Flyway
- **Local services:** Docker Compose and Mailpit for test email

The browser talks to the Next.js app, which proxies API requests to Spring Boot. The API stores records, cases and analysis jobs in PostgreSQL. Mailpit catches development emails; an external AI endpoint is optional.

To run the checks locally, use Node.js 24 for the web app and Java 21/Maven 3.9 for the API. Docker must be running for the backend's Testcontainers tests.

```bash
cd web
npm ci
npm test
npm run build
```

```bash
cd ../api
mvn test
```

With the application running, `npm run test:e2e` from `web` runs the current Playwright visitor/community smoke test. It does not yet cover the full three-role workflow.

## Before using this beyond a demo

Tree Hole is not clinically validated and does not automatically contact a family member, school or emergency service. The 24-hour assignment fallback is not an emergency-response promise. A real campus rollout would need verified support contacts, a staffed response and escalation plan, consent and retention rules, and a review of any external AI provider before public posts are sent to it.

It would also need production email, HTTPS, backups, secret management, monitoring and more security testing. Anyone with a working email address can currently register as a student; teacher applications are reviewed manually by the administrator. Deleting an entry is a soft delete and does not erase the associated case or audit history. Access tokens may remain valid for up to 15 minutes after logout or password reset.

The Git tags mark development milestones, starting with the original `v1.0.0` prototype. The current code is on `main`.
