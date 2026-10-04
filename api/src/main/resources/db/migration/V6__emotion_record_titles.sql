ALTER TABLE emotion_records ADD COLUMN title VARCHAR(160) NOT NULL DEFAULT 'Untitled entry';

UPDATE emotion_records
SET title = LEFT(regexp_replace(content, E'\\s+', ' ', 'g'), 80)
WHERE title = 'Untitled entry';
