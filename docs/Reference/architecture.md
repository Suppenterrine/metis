# Architecture

How Metis works under the hood. This is the mental model you need before reading
any individual endpoint page.

## The shape of the thing

Metis is a **client-side Fabric mod**. There is no server-side component, no
companion process, no external dependency beyond the Fabric API. Inside your own
running Minecraft client it does exactly two things:

1. **Capture** — once per client tick, it reads what the player currently
   perceives and stores it as an immutable snapshot.
2. **Serve** — it runs a tiny HTTP server on `localhost` that hands slices of
   that snapshot to local callers as JSON.

```
  ┌─ Minecraft client (your machine) ──────────────────────────────┐
  │                                                                 │
  │   game thread                         HTTP executor thread      │
  │   ───────────                         ──────────────────        │
  │   every tick:                         on request:               │
  │     capture() ──► volatile snapshot ──► read snapshot ──► JSON   │
  │                                                                 │
  └──────────────────────────────┬──────────────────────────────────┘
                                  │  127.0.0.1 : <port>  (loopback only)
                                  ▼
                       local consumer (e.g. MCDS)
```

## Lifecycle

- **`Metis`** (`onInitialize`) is the common entrypoint. It loads the config
  (`metis.json`) once at startup.
- **`MetisClient`** (`onInitializeClient`) owns the HTTP server and the capture
  loop. On startup it starts the server if the config has `enabled: true`.
- A `ClientTickEvents.END_CLIENT_TICK` handler runs every client tick. It:
  - reconciles the server with the `enabled` flag (so toggling the config and
    reloading starts/stops the server live, no restart needed), and
  - captures a fresh `PerceptionSnapshot` while the server is running.

## The snapshot pattern (and why)

The HTTP server handles requests on its **own thread**, separate from the game
thread. Reading live Minecraft state directly from that thread is unsafe:
iterating collections (the tab list, equipment) while the game thread mutates
them can throw, and partial reads can tear.

Metis avoids this entirely:

- **`PerceptionSnapshot.capture(client)`** runs on the **game thread** (inside
  the tick handler) and extracts everything into **plain Java types** — numbers,
  strings, and small `record`s. No live Minecraft object is retained.
- The result is published into a **`volatile PerceptionSnapshot`** field. The
  volatile write/read establishes a happens-before relationship, so HTTP threads
  see a fully-built, consistent snapshot.
- HTTP handlers only ever touch that snapshot. They never call into Minecraft.

The snapshot is **immutable once published**: every request between two ticks
sees the same coherent picture. Freshness is bounded by one tick (~50 ms), which
is irrelevant for every consumer.

When the player is not in a world, the snapshot is `null` and every endpoint
returns `404`.

## Request handling

All endpoints share one path through `MetisClient`:

1. **CORS preflight** — an `OPTIONS` request returns `204` with CORS headers.
2. **Access guard** — non-loopback remote addresses get `403`. Only
   `127.0.0.1` / `::1` are served.
3. **Snapshot check** — if there is no snapshot (`null`), return `404`.
4. **Serialize** — build the endpoint's payload from the snapshot and write it
   as JSON.

`/api/coords` is special only in its serialization: it keeps a hand-formatted
string for byte-for-byte backward compatibility (see its page). Every other
endpoint is built from an ordered `Map` and serialized with Gson.

See [`conventions.md`](conventions.md) for the exact access, CORS, error, and
serialization rules.

## Threading summary

| Thread | Does | Touches Minecraft? |
|---|---|---|
| Game thread (client tick) | `PerceptionSnapshot.capture(...)`, publishes snapshot | Yes — this is the only place |
| HTTP executor thread | reads the volatile snapshot, serializes JSON | No |

This separation is the core architectural invariant. Keep capture on the game
thread; keep the HTTP side reading plain data only.

## Source

| Concern | File |
|---|---|
| Entrypoint + config load | `src/main/java/dev/suppenterrine/metis/Metis.java` |
| Server, tick loop, routing, guards | `src/client/java/dev/suppenterrine/metis/MetisClient.java` |
| Capture + payloads | `src/client/java/dev/suppenterrine/metis/perception/PerceptionSnapshot.java` |
