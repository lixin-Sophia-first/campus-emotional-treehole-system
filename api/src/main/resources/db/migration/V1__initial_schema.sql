CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','TEACHER','STUDENT')),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  department VARCHAR(120),
  employee_number VARCHAR(80),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX only_one_admin ON users ((role)) WHERE role = 'ADMIN';

CREATE TABLE teacher_applications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(100) NOT NULL,
  employee_number VARCHAR(80) NOT NULL,
  department VARCHAR(120) NOT NULL,
  statement TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewed_by UUID REFERENCES users(id),
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE emotion_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_id UUID NOT NULL REFERENCES users(id),
  content TEXT NOT NULL,
  visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PUBLIC','PRIVATE')),
  emotion VARCHAR(40) NOT NULL,
  intensity INTEGER NOT NULL CHECK (intensity BETWEEN 1 AND 10),
  tags TEXT NOT NULL DEFAULT '',
  anonymous_alias VARCHAR(60) NOT NULL,
  risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
  risk_reasons TEXT NOT NULL DEFAULT '',
  support_card VARCHAR(40) NOT NULL DEFAULT 'ENCOURAGEMENT',
  recommendation_eligible BOOLEAN NOT NULL DEFAULT TRUE,
  moderation_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  deleted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX emotion_records_feed_idx ON emotion_records (visibility, recommendation_eligible, created_at DESC);
CREATE INDEX emotion_records_author_idx ON emotion_records (author_id, created_at DESC);

CREATE TABLE comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  record_id UUID NOT NULL REFERENCES emotion_records(id),
  author_id UUID NOT NULL REFERENCES users(id),
  content TEXT NOT NULL,
  anonymous_alias VARCHAR(60) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at TIMESTAMPTZ
);
CREATE TABLE likes (record_id UUID REFERENCES emotion_records(id), user_id UUID REFERENCES users(id), created_at TIMESTAMPTZ DEFAULT now(), PRIMARY KEY(record_id,user_id));
CREATE TABLE bookmarks (record_id UUID REFERENCES emotion_records(id), user_id UUID REFERENCES users(id), created_at TIMESTAMPTZ DEFAULT now(), PRIMARY KEY(record_id,user_id));
CREATE TABLE reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), record_id UUID NOT NULL REFERENCES emotion_records(id), reporter_id UUID NOT NULL REFERENCES users(id),
  reason VARCHAR(80) NOT NULL, details TEXT NOT NULL DEFAULT '', status VARCHAR(20) NOT NULL DEFAULT 'PENDING', created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE risk_cases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  record_id UUID NOT NULL UNIQUE REFERENCES emotion_records(id),
  student_id UUID NOT NULL REFERENCES users(id),
  risk_level VARCHAR(20) NOT NULL,
  source_visibility VARCHAR(20) NOT NULL,
  public_content_snapshot TEXT,
  status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  assignee_id UUID REFERENCES users(id),
  closure_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX risk_cases_queue_idx ON risk_cases(status, risk_level, created_at DESC);

CREATE TABLE case_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), case_id UUID NOT NULL REFERENCES risk_cases(id), sender_id UUID NOT NULL REFERENCES users(id),
  content TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE identity_access_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), case_id UUID NOT NULL REFERENCES risk_cases(id), teacher_id UUID NOT NULL REFERENCES users(id),
  reason TEXT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', reviewed_by UUID REFERENCES users(id), reviewed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE support_resources (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), author_id UUID NOT NULL REFERENCES users(id), title VARCHAR(180) NOT NULL,
  summary TEXT NOT NULL, content TEXT NOT NULL, resource_type VARCHAR(30) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
  version INTEGER NOT NULL DEFAULT 1, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id), title VARCHAR(180) NOT NULL,
  content TEXT NOT NULL, read_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), actor_id UUID REFERENCES users(id), action VARCHAR(100) NOT NULL,
  target_type VARCHAR(60) NOT NULL, target_id UUID, details TEXT NOT NULL DEFAULT '', created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

