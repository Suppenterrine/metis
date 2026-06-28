# Conventions

Rules that hold for **every** endpoint. Each endpoint page assumes these and
only documents what is specific to it.

## Access model

- **Loopback only.** The server binds to the loopback interface. Requests from
  any non-loopback address are rejected with `403`. The API is therefore only
  reachable by software running on the same machine. This is a hard invariant,
  not a setting.
- **Read-only.** Every endpoint is a `GET`. Metis never mutates game state.
- **No authentication.** The loopback boundary *is* the trust boundary. Any
  local process can read the API.

## Methods

| Method | Meaning |
|---|---|
| `GET` | Fetch the resource. |
| `OPTIONS` | CORS preflight. Returns `204` with CORS headers and no body. |

Any other method simply isn't routed to a meaningful handler.

## CORS

Every response carries:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

This lets a browser-based local tool call the API. The `*` origin is acceptable
because the loopback bind already restricts *who* can connect.

## Responses

- **Content type:** `application/json; charset=utf-8`.
- **Success:** `200` with the endpoint's JSON body.
- **Preflight:** `204` with no body.

### Error responses

| Status | Body | When |
|---|---|---|
| `403` | `{"error": "Access denied"}` | Remote address is not loopback. |
| `404` | `{"error": "Player not in world"}` | No snapshot — player is not in a world (menus, loading, disconnected). |

A `404` is normal and transient: it just means there is nothing to perceive yet.
Consumers should treat it as "not in world right now," not as a failure.

## The `perception` tag

Every perception endpoint (everything except the legacy `/api/coords`) includes
a top-level field:

```json
"perception": "DISTANT"
```

It names the datum's **perception class** from the
[Law of Embodied Perception](../PERCEPTION_RULES.md). It tells a consumer what
gating the rule implies before the data should inform a bot:

| Class | Meaning | Gate the consumer should apply |
|---|---|---|
| `AMBIENT` | The sensing body's own surroundings | None — read it as your own senses |
| `BROADCAST` | Common knowledge the game shows every player | None (treat as meta, low-embodiment) |
| `DISTANT` | Silhouette / gross behaviour, visible at range | Subject plausibly in view |
| `PROXIMATE` | Fine detail, only readable up close + line of sight | Distance + line-of-sight, accuracy falls off with range |

Metis **applies none of these gates itself** — it only labels. See the rule
system for the full definitions.

## Types

JSON types used across the API:

| Type | Notes |
|---|---|
| `integer` | Whole numbers — counts, light levels, block coordinates, latency, ticks. |
| `number` | Decimal — player coordinates, angles, velocity, distances, gradients. |
| `boolean` | `true` / `false`. |
| `string` | Includes **registry ids** like `minecraft:stone`, `minecraft:plains`. |
| `object` | Nested structure; may be `null` (see below). |
| `array` | Ordered list. |

A **registry id** string is always `namespace:path`. Dimensions, biomes, blocks,
items and entity types are all reported as registry ids.

## Nulls

Responses are serialized with nulls **kept**, not omitted. Every documented key
is always present; a `null` value means "empty" or "not applicable" (an empty
equipment slot, no vehicle, durability on a non-damageable item). Consumers can
rely on key presence and only need to null-check values.

## Number formatting

- `/api/coords` formats `x, y, z, yaw, pitch` with **exactly two decimals**
  (e.g. `64.00`). This is a frozen legacy format — see its page.
- All other endpoints use natural JSON numbers. To keep payloads tidy, decimals
  are rounded at capture time: velocity to 4 places, distances and weather
  gradients to 3 places. Integers (light, counts, coordinates of a looked-at
  block, latency, ticks) are exact.

## Source

Routing, guards, CORS and serialization all live in
`src/client/java/dev/suppenterrine/metis/MetisClient.java`.
