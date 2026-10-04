ALTER TABLE emotion_records ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE emotion_records SET anonymous_alias='Stranger', is_anonymous=TRUE;
