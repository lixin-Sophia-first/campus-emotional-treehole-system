CREATE TABLE account_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  token_type VARCHAR(30) NOT NULL CHECK (token_type IN ('VERIFY_EMAIL','RESET_PASSWORD')),
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX account_tokens_lookup_idx ON account_tokens(token_hash, token_type, expires_at);

