# Metis Reference

Technical reference for the **current state** of Metis — what exists, what each
piece does, and how to use it. This is the "as-built" documentation: descriptive,
not aspirational.

If you are looking for the *philosophy* — the rule that decides what Metis is
allowed to sense at all — read [`../PERCEPTION_RULES.md`](../PERCEPTION_RULES.md)
first. This reference describes the implementation that lives under that rule.

## How this reference is organized

One topic per file, so you can read exactly the part you need.

### Cross-cutting

| Document | What it covers |
|---|---|
| [`architecture.md`](architecture.md) | How Metis works: the HTTP server, the per-tick snapshot, threading, JSON serialization |
| [`configuration.md`](configuration.md) | The `metis.json` config file — port, enable toggle, live reload |
| [`conventions.md`](conventions.md) | Access model, CORS, error responses, the `perception` tag, types & number formatting |

### Endpoints

Each endpoint has its own page with the same structure: an at-a-glance table,
why it exists, the request/response shape, an example, its perception class and
the gating it implies, and the exact Minecraft API it reads.

| Endpoint | Class | Page |
|---|---|---|
| `GET /api/coords` | — | [`endpoints/coords.md`](endpoints/coords.md) |
| `GET /api/movement` | `DISTANT` | [`endpoints/movement.md`](endpoints/movement.md) |
| `GET /api/look` | `PROXIMATE` | [`endpoints/look.md`](endpoints/look.md) |
| `GET /api/equipment` | `PROXIMATE` | [`endpoints/equipment.md`](endpoints/equipment.md) |
| `GET /api/environment` | `AMBIENT` | [`endpoints/environment.md`](endpoints/environment.md) |
| `GET /api/players` | `BROADCAST` | [`endpoints/players.md`](endpoints/players.md) |

## Orientation in 60 seconds

- Metis is a **client-side Fabric mod** for Minecraft `1.21.1`. It runs a tiny
  HTTP server inside *your own* client, bound to **loopback only**.
- Every tick it captures an immutable **snapshot** of what the player perceives,
  and the HTTP endpoints serve slices of that snapshot as JSON.
- It exposes **only embodied-perceivable data**, each datum **tagged** with a
  perception class. It applies **no gating** — that is the consumer's job
  (MCDS). Private internal state is never captured.
- Target version: `Minecraft 1.21.1`, Yarn mappings, Java 21.

## Source map

| Concern | File |
|---|---|
| Common entrypoint, config loading | `src/main/java/dev/suppenterrine/metis/Metis.java` |
| HTTP server, routing, guards, serialization | `src/client/java/dev/suppenterrine/metis/MetisClient.java` |
| Per-tick capture + endpoint payloads | `src/client/java/dev/suppenterrine/metis/perception/PerceptionSnapshot.java` |
| Config file model | `src/main/java/dev/suppenterrine/metis/config/MetisConfig.java` |
