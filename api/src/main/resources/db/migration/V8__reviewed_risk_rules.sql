CREATE TABLE risk_rule_candidates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  version BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
  task_id UUID NOT NULL UNIQUE REFERENCES ai_analysis_tasks(id) ON DELETE CASCADE,
  record_id UUID NOT NULL REFERENCES emotion_records(id) ON DELETE CASCADE,
  local_risk VARCHAR(20) NOT NULL,
  model_risk VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DETECTED' CHECK (status IN ('DETECTED','CONFIRMED','DRAFTED','ACTIVE','REJECTED','DISABLED','WITHDRAWN')),
  phrase VARCHAR(120),
  rationale VARCHAR(500),
  reviewed_by UUID REFERENCES users(id),
  reviewed_at TIMESTAMPTZ,
  activated_at TIMESTAMPTZ,
  disabled_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX risk_rule_candidates_status_idx ON risk_rule_candidates(status, created_at DESC);
CREATE UNIQUE INDEX risk_rule_active_phrase_idx ON risk_rule_candidates(lower(phrase)) WHERE status = 'ACTIVE';
