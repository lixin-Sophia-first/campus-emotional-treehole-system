CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  replaced_by UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens(user_id, expires_at DESC);

CREATE TABLE login_attempts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL,
  ip_address VARCHAR(80) NOT NULL DEFAULT '',
  successful BOOLEAN NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX login_attempts_lookup_idx ON login_attempts(lower(email), created_at DESC);

CREATE TABLE ai_analysis_tasks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  record_id UUID NOT NULL REFERENCES emotion_records(id) ON DELETE CASCADE,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_error TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ai_tasks_queue_idx ON ai_analysis_tasks(status, next_attempt_at);

CREATE TABLE ai_analysis_results (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id UUID REFERENCES ai_analysis_tasks(id) ON DELETE SET NULL,
  record_id UUID NOT NULL REFERENCES emotion_records(id) ON DELETE CASCADE,
  source VARCHAR(20) NOT NULL,
  primary_emotion VARCHAR(80) NOT NULL DEFAULT '',
  intensity INTEGER NOT NULL CHECK (intensity BETWEEN 1 AND 10),
  risk_level VARCHAR(20) NOT NULL,
  explicit_plan BOOLEAN NOT NULL DEFAULT FALSE,
  imminent_timing BOOLEAN NOT NULL DEFAULT FALSE,
  means_present BOOLEAN NOT NULL DEFAULT FALSE,
  violation_category VARCHAR(60) NOT NULL DEFAULT 'NONE',
  confidence NUMERIC(5,4) NOT NULL DEFAULT 0,
  support_card VARCHAR(40) NOT NULL,
  reason TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ai_results_record_idx ON ai_analysis_results(record_id, created_at DESC);

CREATE TABLE risk_case_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  case_id UUID NOT NULL REFERENCES risk_cases(id) ON DELETE CASCADE,
  actor_id UUID REFERENCES users(id),
  action VARCHAR(80) NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30),
  details TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX risk_case_history_case_idx ON risk_case_history(case_id, created_at);

CREATE TABLE case_followups (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  case_id UUID NOT NULL REFERENCES risk_cases(id) ON DELETE CASCADE,
  teacher_id UUID NOT NULL REFERENCES users(id),
  note TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE resource_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  resource_id UUID NOT NULL REFERENCES support_resources(id) ON DELETE CASCADE,
  version INTEGER NOT NULL,
  title VARCHAR(180) NOT NULL,
  summary TEXT NOT NULL,
  content TEXT NOT NULL,
  resource_type VARCHAR(30) NOT NULL,
  editor_id UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(resource_id, version)
);

CREATE TABLE user_warnings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  admin_id UUID NOT NULL REFERENCES users(id),
  reason TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE emotion_records ADD COLUMN violation_category VARCHAR(60) NOT NULL DEFAULT 'NONE';
ALTER TABLE risk_cases ADD COLUMN student_declined BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE risk_cases ADD COLUMN false_positive BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX comments_record_idx ON comments(record_id, created_at);
CREATE INDEX reports_status_idx ON reports(status, created_at DESC);
CREATE INDEX notifications_user_idx ON notifications(user_id, read_at, created_at DESC);
