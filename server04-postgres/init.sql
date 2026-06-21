-- ============================================================
--  Event Announcement System — PostgreSQL Schema
--  Server 04
-- ============================================================

-- Database and user are created by Docker env vars (POSTGRES_DB, POSTGRES_USER)

-- ─── Tables ──────────────────────────────────────────────

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE events (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255)  NOT NULL,
    description  TEXT,
    location     VARCHAR(255),
    event_date   TIMESTAMP     NOT NULL,
    category_id  BIGINT        NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    image_url    VARCHAR(500),
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ─── Indexes ─────────────────────────────────────────────
-- Used for pgBadger performance comparison before/after

-- Most common query: list active upcoming events
CREATE INDEX idx_events_event_date     ON events(event_date);
CREATE INDEX idx_events_is_active      ON events(is_active);
CREATE INDEX idx_events_category_id    ON events(category_id);
CREATE INDEX idx_events_created_at     ON events(created_at);

-- Text search on title (homepage search feature)
CREATE INDEX idx_events_title          ON events(title);

-- Composite: the homepage query pattern (active events ordered by date)
CREATE INDEX idx_events_active_date    ON events(is_active, event_date DESC);

-- Grant table access to eventuser
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO eventuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO eventuser;

-- ─── Seed Data ───────────────────────────────────────────

INSERT INTO categories (name, description) VALUES
  ('Conference',  'Professional and academic conferences'),
  ('Workshop',    'Hands-on skill building sessions'),
  ('Seminar',     'Educational talks and presentations'),
  ('Networking',  'Professional networking events'),
  ('Cultural',    'Arts, music, and cultural events');

INSERT INTO events (title, description, location, event_date, category_id, is_active) VALUES
  ('Tech Summit 2025',
   'Annual technology conference covering AI, cloud, and cybersecurity trends.',
   'Convention Center, Hall A', '2025-09-15 09:00:00', 1, TRUE),

  ('Spring Boot Masterclass',
   'Deep dive into Spring Boot 3, JPA, and microservices architecture.',
   'Room 204, Engineering Building', '2025-08-20 14:00:00', 2, TRUE),

  ('Database Performance Workshop',
   'Learn PostgreSQL indexing, query optimization, and pgBadger monitoring.',
   'Computer Lab 3', '2025-08-10 10:00:00', 2, TRUE),

  ('Startup Networking Night',
   'Connect with founders, investors, and developers in the local tech scene.',
   'The Hub Co-working Space', '2025-07-30 18:00:00', 4, TRUE),

  ('AI in Healthcare Seminar',
   'Exploring the applications of machine learning in modern medical practice.',
   'Auditorium B', '2025-09-05 13:00:00', 3, TRUE),

  ('Cultural Arts Festival',
   'A celebration of local and international arts, music, and performance.',
   'City Park Main Stage', '2025-10-01 10:00:00', 5, TRUE);

-- ─── PostgreSQL Logging Configuration ────────────────────
-- Add these to postgresql.conf on the DB server for pgBadger:
--
-- log_destination = 'stderr'
-- logging_collector = on
-- log_directory = '/var/log/postgresql'
-- log_filename = 'postgresql-%Y-%m-%d.log'
-- log_min_duration_statement = 0        (log ALL statements — good for demo)
-- log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
-- log_checkpoints = on
-- log_connections = on
-- log_disconnections = on
-- log_lock_waits = on
-- log_temp_files = 0
-- log_autovacuum_min_duration = 0
-- lc_messages = 'C'                     (required by pgBadger)