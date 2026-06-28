# `GET /api/environment`

The sensing body's **own** surroundings — time, sky, weather and light.

## At a glance

| | |
|---|---|
| Path | `/api/environment` |
| Methods | `GET`, `OPTIONS` |
| Perception class | `AMBIENT` |
| Source | `PerceptionSnapshot#environmentPayload()` |

## Why it exists

Lighting, time and weather are situational awareness. Night plus low light means
mob-spawn danger; rain changes outdoor work; the time of day frames everything.
A bot can sense this to coordinate — defensive posture at night, for instance.

The critical framing: this is **`AMBIENT`** — the observer's *own* environment.
It is read at the body's own position and is a statement about *here*, not a
claim about anyone else. A consumer wanting "is it night / is it raining" should
treat it as its own senses, never inherit another body's. Each Metis instance
reports its own sky.

## Request

```
GET /api/environment
```

## Response

`200 application/json`

| Field | Type | Null? | Description |
|---|---|---|---|
| `perception` | `string` | no | Always `"AMBIENT"`. |
| `world` | `string` | no | Dimension registry id, e.g. `minecraft:overworld`. |
| `biome` | `string` | no | Biome registry id, or `unknown`. |
| `timeOfDay` | `integer` | no | Time of day in ticks (`0`–`23999`, wraps daily). |
| `dayTime` | `integer` | no | Total world time in ticks (monotonic). |
| `isDay` | `boolean` | no | Daytime. |
| `isNight` | `boolean` | no | Nighttime. |
| `moonPhase` | `integer` | no | Moon phase `0`–`7`, derived from time of day. |
| `raining` | `boolean` | no | Rain active. |
| `thundering` | `boolean` | no | Thunderstorm active. |
| `rainGradient` | `number` | no | Rain strength `0.0`–`1.0` (3 decimals). |
| `thunderGradient` | `number` | no | Thunder strength `0.0`–`1.0` (3 decimals). |
| `light` | `object` | no | Light at the body's block position. |
| `light.block` | `integer` | no | Block-source light `0`–`15`. |
| `light.sky` | `integer` | no | Sky light `0`–`15`. |
| `light.effective` | `integer` | no | Effective light used for gameplay `0`–`15`. |

### Example

```json
{
  "perception": "AMBIENT",
  "world": "minecraft:overworld",
  "biome": "minecraft:plains",
  "timeOfDay": 13000,
  "dayTime": 145000,
  "isDay": false,
  "isNight": true,
  "moonPhase": 4,
  "raining": false,
  "thundering": false,
  "rainGradient": 0.0,
  "thunderGradient": 0.0,
  "light": { "block": 0, "sky": 4, "effective": 4 }
}
```

> **On `moonPhase`.** Minecraft `1.21.1` has no direct moon-phase accessor, so
> Metis derives it from time of day with the vanilla overworld formula
> (`timeOfDay / 24000 mod 8`).

## Errors

Standard — see [`../conventions.md`](../conventions.md).

## Perception & gating

`AMBIENT`. No gate — it is the observer's own senses, not a claim about another
subject. The only discipline is sourcing: read it from *your own* body's
position, never relay another body's environment as yours.

## Reads (Minecraft API)

Verified against Yarn `1.21.1`:

| Field | Source call |
|---|---|
| `world` | `World#getRegistryKey().getValue()` |
| `biome` | `World#getBiome(BlockPos)` → registry key |
| `timeOfDay` | `World#getTimeOfDay()` |
| `dayTime` | `World#getTime()` |
| `isDay` / `isNight` | `World#isDay()` / `World#isNight()` |
| `moonPhase` | derived from `getTimeOfDay()` |
| `raining` / `thundering` | `World#isRaining()` / `World#isThundering()` |
| `rainGradient` / `thunderGradient` | `World#getRainGradient(1.0)` / `getThunderGradient(1.0)` |
| `light.block` / `light.sky` | `World#getLightLevel(LightType.BLOCK/SKY, pos)` |
| `light.effective` | `World#getLightLevel(pos)` |

## Example call

```bash
curl http://localhost:25566/api/environment
```
