# `GET /api/look`

What the player is aiming at — the crosshair target, block or entity.

## At a glance

| | |
|---|---|
| Path | `/api/look` |
| Methods | `GET`, `OPTIONS` |
| Perception class | `PROXIMATE` |
| Source | `PerceptionSnapshot#lookPayload()` |

## Why it exists

This is the bridge from perception to *intent*. Where the player is looking is
how a teammate reads "go **there**," "mine **this**," "it's **that** one" —
without anyone typing coordinates. Up close, a bot can anticipate and pre-walk
toward where you're aiming.

It is `PROXIMATE`: fine detail that only a nearby observer with a clear sightline
can make out, and that should blur with distance. Metis reports it raw; the
gating is the consumer's.

## Request

```
GET /api/look
```

## Response

`200 application/json`

| Field | Type | Null? | Description |
|---|---|---|---|
| `perception` | `string` | no | Always `"PROXIMATE"`. |
| `type` | `string` | no | `BLOCK`, `ENTITY`, `MISS` (looking at nothing in range), or `NONE` (no target computed). |
| `block` | `object` | yes | Present (non-null) when `type` is `BLOCK`. |
| `entity` | `object` | yes | Present (non-null) when `type` is `ENTITY`. |

When `type` is `BLOCK`, `entity` is `null`; when `ENTITY`, `block` is `null`;
for `MISS`/`NONE`, both are `null`.

### `block` object

| Field | Type | Description |
|---|---|---|
| `x` | `integer` | Block X coordinate. |
| `y` | `integer` | Block Y coordinate. |
| `z` | `integer` | Block Z coordinate. |
| `face` | `string` | Which face is targeted: `UP`, `DOWN`, `NORTH`, `SOUTH`, `EAST`, `WEST`. |
| `id` | `string` | Block registry id, e.g. `minecraft:stone`. |
| `distance` | `number` | Distance from the player's eyes to the hit point (3 decimals). |

### `entity` object

| Field | Type | Description |
|---|---|---|
| `id` | `string` | Entity type registry id, e.g. `minecraft:zombie`. |
| `uuid` | `string` | Entity UUID. |
| `name` | `string` | Display name (custom name or default). |
| `distance` | `number` | Distance from the player's eyes to the hit point (3 decimals). |

### Example — looking at a block

```json
{
  "perception": "PROXIMATE",
  "type": "BLOCK",
  "block": { "x": 12, "y": 63, "z": -40, "face": "UP", "id": "minecraft:stone", "distance": 3.41 },
  "entity": null
}
```

### Example — looking at an entity

```json
{
  "perception": "PROXIMATE",
  "type": "ENTITY",
  "block": null,
  "entity": { "id": "minecraft:zombie", "uuid": "…", "name": "Zombie", "distance": 2.18 }
}
```

## Errors

Standard — see [`../conventions.md`](../conventions.md).

## Perception & gating

`PROXIMATE`. The consumer should gate on **distance + line of sight** and let
accuracy fall off with range: full fidelity up close and in sight; fading to
nothing far away or through terrain. Metis applies none of this — it reports the
raw crosshair result.

## Reads (Minecraft API)

Verified against Yarn `1.21.1`:

| Field | Source call |
|---|---|
| target | `MinecraftClient.crosshairTarget` (`HitResult`) |
| `type` | `HitResult#getType()` |
| `block.x/y/z` | `BlockHitResult#getBlockPos()` |
| `block.face` | `BlockHitResult#getSide().name()` |
| `block.id` | `World#getBlockState(pos).getBlock()` → `Registries.BLOCK.getId` |
| `entity` | `EntityHitResult#getEntity()` → `Registries.ENTITY_TYPE.getId`, `getUuid`, `getName` |
| `distance` | `Entity#getEyePos().distanceTo(HitResult#getPos())` |

## Example call

```bash
curl http://localhost:25566/api/look
```
