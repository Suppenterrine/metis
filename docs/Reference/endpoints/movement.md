# `GET /api/movement`

How the player is moving — pose, locomotion state, velocity, and vehicle.

## At a glance

| | |
|---|---|
| Path | `/api/movement` |
| Methods | `GET`, `OPTIONS` |
| Perception class | `DISTANT` |
| Source | `PerceptionSnapshot#movementPayload()` |

## Why it exists

Movement is the tactical goldmine. A teammate doesn't need to read your mind to
see *how* you're travelling — walking, sprinting, sneaking, swimming, gliding on
an elytra, riding a boat. A bot that can see the same can adapt its own
navigation automatically: match a sprint, follow onto water, hang back when you
sneak, predict where you're heading from your velocity.

All of it is `DISTANT` — readable from a silhouette at range — so it is honest
to expose and cheap to act on.

## Request

```
GET /api/movement
```

## Response

`200 application/json`

| Field | Type | Null? | Description |
|---|---|---|---|
| `perception` | `string` | no | Always `"DISTANT"`. |
| `pose` | `string` | no | Raw `EntityPose` name: `STANDING`, `CROUCHING`, `SWIMMING`, `FALL_FLYING`, `SLEEPING`, … |
| `onGround` | `boolean` | no | Standing on the ground. |
| `sprinting` | `boolean` | no | Sprinting. |
| `sneaking` | `boolean` | no | Holding sneak. |
| `swimming` | `boolean` | no | Actively swimming. |
| `crawling` | `boolean` | no | In the crawling (1-block) pose. |
| `climbing` | `boolean` | no | On a ladder/vine/climbable. |
| `gliding` | `boolean` | no | Gliding with an elytra (`isFallFlying`). |
| `onFire` | `boolean` | no | Visibly on fire. |
| `inWater` | `boolean` | no | Touching water. |
| `submerged` | `boolean` | no | Eyes below the water surface. |
| `inLava` | `boolean` | no | In lava. |
| `velocity` | `object` | no | Velocity vector — see below. |
| `velocity.x` | `number` | no | East–west component (4 decimals). |
| `velocity.y` | `number` | no | Vertical component (4 decimals). |
| `velocity.z` | `number` | no | North–south component (4 decimals). |
| `horizontalSpeed` | `number` | no | `sqrt(x² + z²)` of velocity (4 decimals). |
| `vehicle` | `string` | yes | Registry id of the ridden entity, or `null` if on foot. |

### Example

```json
{
  "perception": "DISTANT",
  "pose": "STANDING",
  "onGround": true,
  "sprinting": false,
  "sneaking": false,
  "swimming": false,
  "crawling": false,
  "climbing": false,
  "gliding": false,
  "onFire": false,
  "inWater": false,
  "submerged": false,
  "inLava": false,
  "velocity": { "x": 0.0, "y": -0.0784, "z": 0.0 },
  "horizontalSpeed": 0.0,
  "vehicle": null
}
```

## Errors

Standard — see [`../conventions.md`](../conventions.md).

## Perception & gating

`DISTANT`. A consumer should treat this as visible only when the subject is
plausibly **in view** — you cannot read a silhouette through terrain or out of
render range. The data is coarse, so the gate is lenient. Metis applies none of
it.

## Reads (Minecraft API)

Verified against Yarn `1.21.1`:

| Field | Source call |
|---|---|
| `pose` | `Entity#getPose().name()` |
| `onGround` | `Entity#isOnGround` |
| `sprinting` | `Entity#isSprinting` |
| `sneaking` | `Entity#isSneaking` |
| `swimming` | `Entity#isSwimming` |
| `crawling` | `Entity#isCrawling` |
| `climbing` | `LivingEntity#isClimbing` |
| `gliding` | `LivingEntity#isFallFlying` |
| `onFire` | `Entity#isOnFire` |
| `inWater` | `Entity#isTouchingWater` |
| `submerged` | `Entity#isSubmergedInWater` |
| `inLava` | `Entity#isInLava` |
| `velocity` | `Entity#getVelocity()` (`Vec3d`) |
| `vehicle` | `Entity#getVehicle()` → `Registries.ENTITY_TYPE.getId(type)` |

## Example call

```bash
curl http://localhost:25566/api/movement
```
