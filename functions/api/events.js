// POST /api/events: usage data from the app, from people who said yes (see app/.../stats/Stats.kt).
// Stores one row per event in the D1 database bound to this Pages project as DB (schema: stats/schema.sql).

// Which table each build's data goes to. Yoink Test sends "test"; the real app would send "release".
const TABLES = { test: "test_events", release: "events" };

const text = (value, max) => (typeof value === "string" && value.length > 0 ? value.slice(0, max) : null);

const MAX_EVENTS = 200;

export async function onRequestPost({ request, env }) {
  // Not set up yet: the app keeps the events and tries again later.
  if (!env.DB) return new Response("Database not connected", { status: 503 });

  let body;
  try {
    body = await request.json();
  } catch {
    return new Response("Bad request", { status: 400 });
  }
  const table = TABLES[body?.build];
  const installId = text(body?.id, 64);
  const events = Array.isArray(body?.events) ? body.events.slice(0, MAX_EVENTS) : [];
  if (!table || !installId || events.length === 0) return new Response("Bad request", { status: 400 });

  const country = request.cf?.country ?? null;
  const insert = env.DB.prepare(
    `INSERT INTO ${table} (install_id, at, name, app_version, android, model, language, country, video_id, title, channel, quality, data)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
  );
  const rows = events
    .filter((event) => text(event?.name, 40))
    .map((event) => {
      const { video_id, title, channel, quality, ...rest } = event.data ?? {};
      const at = Number.isFinite(event.at) ? new Date(event.at) : new Date();
      return insert.bind(
        installId,
        (isNaN(at) ? new Date() : at).toISOString(),
        text(event.name, 40),
        text(body.app, 40),
        Number.isInteger(body.android) ? body.android : null,
        text(body.model, 80),
        text(body.language, 20),
        country,
        text(video_id, 20),
        text(title, 300),
        text(channel, 200),
        text(quality, 20),
        JSON.stringify(rest).slice(0, 2000),
      );
    });
  if (rows.length > 0) await env.DB.batch(rows);
  return new Response(null, { status: 204 });
}
