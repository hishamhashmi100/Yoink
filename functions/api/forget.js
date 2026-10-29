// POST /api/forget: "Delete my data" in the app's Settings. Removes every event from that install.

// Which table each build's data goes to. Yoink Test sends "test"; the real app would send "release".
const TABLES = { test: "test_events", release: "events" };

const text = (value, max) => (typeof value === "string" && value.length > 0 ? value.slice(0, max) : null);

export async function onRequestPost({ request, env }) {
  if (!env.DB) return new Response("Database not connected", { status: 503 });

  let body;
  try {
    body = await request.json();
  } catch {
    return new Response("Bad request", { status: 400 });
  }
  const table = TABLES[body?.build];
  const installId = text(body?.id, 64);
  if (!table || !installId) return new Response("Bad request", { status: 400 });

  await env.DB.prepare(`DELETE FROM ${table} WHERE install_id = ?`).bind(installId).run();
  return new Response(null, { status: 204 });
}
