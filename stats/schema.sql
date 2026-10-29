-- Usage data from the app (see app/.../stats/Stats.kt and functions/api/). One row per event.
-- Run this once in the Cloudflare dashboard: Storage & databases → D1 → yoink-data → Console.
-- Yoink Test's events go to test_events, the real app's (once it collects) to events, so they never mix.

CREATE TABLE IF NOT EXISTS test_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  install_id TEXT NOT NULL,     -- random per install, not tied to a person
  at TEXT NOT NULL,             -- when it happened (UTC)
  name TEXT NOT NULL,           -- app_open, download_done, download_failed, playlist_queued, opted_in
  app_version TEXT,
  android INTEGER,              -- Android API level (36 = Android 16)
  model TEXT,
  language TEXT,
  country TEXT,                 -- from Cloudflare; the internet address isn't stored
  video_id TEXT,
  title TEXT,
  channel TEXT,
  quality TEXT,
  data TEXT                     -- everything else about the event, as JSON
);
CREATE INDEX IF NOT EXISTS test_events_install ON test_events (install_id);
CREATE INDEX IF NOT EXISTS test_events_at ON test_events (at);

CREATE TABLE IF NOT EXISTS events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  install_id TEXT NOT NULL,
  at TEXT NOT NULL,
  name TEXT NOT NULL,
  app_version TEXT,
  android INTEGER,
  model TEXT,
  language TEXT,
  country TEXT,
  video_id TEXT,
  title TEXT,
  channel TEXT,
  quality TEXT,
  data TEXT
);
CREATE INDEX IF NOT EXISTS events_install ON events (install_id);
CREATE INDEX IF NOT EXISTS events_at ON events (at);
