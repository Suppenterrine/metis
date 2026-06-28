# `GET /api/coords`

Position, view direction, world context and identity. The original Metis
endpoint and the stable contract MCDS consumes for radio-distance audio.

## At a glance

| | |
|---|---|
| Path | `/api/coords` |
| Methods | `GET`, `OPTIONS` |
| Perception class | — (legacy; predates the class system) |
| Source | `MetisClient#handleCoordsRequest` |
| Reads from | `PerceptionSnapshot` (identity + coordinate fields) |

## Why it exists

This was the first reason Metis was built: feeding the real distance between the
player and the bots into the MCDS radio / walkie-talkie audio model, where
distance drives signal strength. That use consumes the **raw** coordinates
directly and on purpose — see the rule system's note on legitimate raw use.

It also carries the world/biome and player identity, making it a useful single
"where am I, who am I" call.

> **Compatibility.** This endpoint's response is frozen byte-for-byte: same
> fields, same order, same two-decimal number formatting. New perception data
> lives in the other endpoints so this contract never has to change.

## Request

```
GET /api/coords
```

No parameters, no headers required.

## Response

`200 application/json`

| Field | Type | Null? | Description |
|---|---|---|---|
| `x` | `number` | no | East–west position (2 decimals). |
| `y` | `number` | no | Height (2 decimals). |
| `z` | `number` | no | North–south position (2 decimals). |
| `yaw` | `number` | no | Horizontal rotation in degrees (2 decimals). |
| `pitch` | `number` | no | Vertical rotation in degrees (2 decimals). |
| `world` | `string` | no | Dimension registry id, e.g. `minecraft:overworld`. |
| `biome` | `string` | no | Biome registry id, or `unknown`. |
| `uuid` | `string` | no | Player UUID. |
| `username` | `string` | no | Player username. |

### Example

```json
{
  "x": 123.45,
  "y": 64.00,
  "z": -789.12,
  "yaw": 180.00,
  "pitch": 12.50,
  "world": "minecraft:overworld",
  "biome": "minecraft:plains",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "username": "PlayerName"
}
```

## Errors

Standard — see [`../conventions.md`](../conventions.md). `403` off-loopback,
`404` when not in a world.

## Perception & gating

Predates the perception-class system, so it carries no `perception` tag. Its
fields span what would today be `DISTANT` (position), `AMBIENT` (world/biome) and
`BROADCAST` (identity). Because it is consumed raw by design (radio distance),
how a consumer uses it is the consumer's responsibility — see
[the rule system](../../PERCEPTION_RULES.md).

## Reads (Minecraft API)

Verified against Yarn `1.21.1`:

| Field | Source call |
|---|---|
| `x`/`y`/`z` | `PlayerEntity#getX/getY/getZ` |
| `yaw`/`pitch` | `Entity#getYaw/getPitch` |
| `world` | `World#getRegistryKey().getValue()` |
| `biome` | `World#getBiome(BlockPos)` → registry key |
| `uuid` | `Entity#getUuid()` |
| `username` | `Entity#getName().getString()` |

## Example call

```bash
curl http://localhost:25566/api/coords
```
