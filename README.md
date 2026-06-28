<div align="center">

# Metis

**The perception sensor of the MCDS ecosystem.**
A lightweight, client-side Fabric mod that exposes the player's live coordinates and world context via a local HTTP API.

`Minecraft 1.21.1` · `Fabric` · `loopback-only` · `zero external dependencies beyond Fabric API`

</div>

## 📋 Overview

Metis runs a tiny HTTP server inside *your own* Minecraft client and serves your current position, view direction, and world/biome at a single localhost endpoint. External tools can read where you are without scraping the screen or the game's memory.

In the MCDS satellite family it is the **organ of perception**: where `blackbox` preserves memory, `thread` opens up knowledge, and `coda` carries information back out as sound, **Metis is what sees**. The player-distance use case (feeding bot↔player distance into MCDS radio comms) was just the first reason to build it — the mod is the sensory link to the live world, and is meant to grow (movement & pose, held item, targeted block, nearby entities, …) **within the limits of embodied perception**.

> The name: *Metis* is the Greek Titaness of cunning, perception, and situational intelligence — not raw power, but the ability to grasp reality and orient within it. The sailor's feel for the storm, the hunter's eye for a trail.

> **What Metis is allowed to sense is governed by one rule:** an agent must never know
> anything about a player that a second, co-located human couldn't also perceive or infer.
> Private state (health, hunger, …) is never exposed; everything else is exposed *raw and
> tagged with a perception class*, and the gating (distance, line of sight) happens in MCDS.
> See **[`docs/PERCEPTION_RULES.md`](docs/PERCEPTION_RULES.md)** — the Law of Embodied Perception.

## ✨ Features

- Lightweight HTTP server, **localhost-only** (binds to the loopback interface)
- **Client-side only** — no server-side components, works in singleplayer and multiplayer
- **Configurable port** via a plain JSON config file — no in-game UI, no ModMenu/Cloth dependency
- Single dependency: Fabric API

## 🚀 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1 and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Build the jar (`./gradlew build`) or grab one from releases, and drop it into `.minecraft/mods`
3. Launch Minecraft with the Fabric profile

## ⚙️ Configuration

Metis writes a config file on first launch at `.minecraft/config/metis.json`:

```json
{
  "enabled": true,
  "port": 25566
}
```

| Field     | Default | Description                                                    |
|-----------|---------|----------------------------------------------------------------|
| `enabled` | `true`  | Master switch. Toggling it starts/stops the server live.       |
| `port`    | `25566` | TCP port the local API listens on. **Change it freely.**       |

The default port is `25566` so it never clashes with a local Minecraft server on `25565`. The API is read-only and only accepts loopback connections (`127.0.0.1`, `::1`).

## 🔌 API Usage

All endpoints are `GET` (with `OPTIONS` for CORS preflight), read-only, and
loopback-only. Each perception endpoint carries a `perception` field naming its
class from the [Law of Embodied Perception](docs/PERCEPTION_RULES.md) — so a
consumer (MCDS) knows what gating, if any, the rule requires. Metis itself
applies no gating; it exposes raw, tagged data.

| Endpoint           | Class       | Description                                                        |
|--------------------|-------------|--------------------------------------------------------------------|
| `/api/coords`      | —           | Position, view direction, world/biome, identity (legacy contract)  |
| `/api/movement`    | `DISTANT`   | Pose & locomotion state, velocity, vehicle — visible at range      |
| `/api/look`        | `PROXIMATE` | Crosshair target (looked-at block or entity)                       |
| `/api/equipment`   | `PROXIMATE` | Held items, selected slot, visibly worn armor                      |
| `/api/environment` | `AMBIENT`   | The sensing body's own time, sky, weather and light                |
| `/api/players`     | `BROADCAST` | Tab-list common knowledge (online players, ping, game mode)        |

> **Never exposed.** Private internal state — health, hunger, breath, experience,
> status effects, the hidden inventory — has no endpoint by design. A co-located
> human couldn't read it, so neither can an agent. See
> [`docs/PERCEPTION_RULES.md`](docs/PERCEPTION_RULES.md).

### `/api/coords` Response Fields

| Field      | Type     | Description                   |
|------------|----------|-------------------------------|
| `x`        | `number` | East-West                     |
| `y`        | `number` | Height                        |
| `z`        | `number` | North-South                   |
| `yaw`      | `number` | Horizontal rotation (degrees) |
| `pitch`    | `number` | Vertical rotation (degrees)   |
| `world`    | `string` | Minecraft world registry ID   |
| `biome`    | `string` | Minecraft biome registry ID   |
| `uuid`     | `string` | Player UUID                   |
| `username` | `string` | Player username               |

### Error Responses

| Status | Message                      |
|--------|------------------------------|
| `403`  | Access denied (non-loopback) |
| `404`  | Player not in world          |

### Response Format Example

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

### `/api/movement` — `DISTANT`

How the player is moving — readable from a silhouette at range.

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

`pose` is the raw `EntityPose` name (`STANDING`, `CROUCHING`, `SWIMMING`,
`FALL_FLYING`, …). `vehicle` is the ridden entity's registry id, or `null`.

### `/api/look` — `PROXIMATE`

What the player is aiming at. Fine detail — only faithful up close and in line of
sight, so MCDS gates it on distance and sightline.

```json
{
  "perception": "PROXIMATE",
  "type": "BLOCK",
  "block": { "x": 12, "y": 63, "z": -40, "face": "UP", "id": "minecraft:stone", "distance": 3.41 },
  "entity": null
}
```

`type` is `BLOCK`, `ENTITY`, `MISS`, or `NONE`. For an entity target, `entity`
carries `{ id, uuid, name, distance }` and `block` is `null`.

### `/api/equipment` — `PROXIMATE`

Held and visibly worn gear. Item objects are `null` when the slot is empty;
`damage`/`maxDamage` are `null` for non-damageable items.

```json
{
  "perception": "PROXIMATE",
  "selectedSlot": 0,
  "mainHand": { "id": "minecraft:diamond_pickaxe", "count": 1, "name": "Pickaxe", "damage": 12, "maxDamage": 1561 },
  "offHand": { "id": "minecraft:torch", "count": 32, "name": "Torch", "damage": null, "maxDamage": null },
  "armor": {
    "head": null,
    "chest": { "id": "minecraft:iron_chestplate", "count": 1, "name": "Iron Chestplate", "damage": 0, "maxDamage": 240 },
    "legs": null,
    "feet": null
  }
}
```

### `/api/environment` — `AMBIENT`

The sensing body's *own* surroundings — read at its own position, never relayed
from another body.

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

### `/api/players` — `BROADCAST`

Tab-list common knowledge — what the game already shows every player. `server` is
the connected server address, or `null` in singleplayer.

```json
{
  "perception": "BROADCAST",
  "singleplayer": false,
  "server": "play.example.net",
  "players": [
    { "uuid": "550e8400-e29b-41d4-a716-446655440000", "name": "PlayerName", "latency": 42, "gameMode": "survival" }
  ]
}
```

## 🛠️ Examples

Replace `25566` with your configured port if you changed it.

### cURL
```bash
curl http://localhost:25566/api/coords
```

### Python
```python
import requests

data = requests.get("http://localhost:25566/api/coords").json()
print(f"Player {data['username']} at X: {data['x']}, Y: {data['y']}, Z: {data['z']}")
```

### JavaScript
```javascript
fetch("http://localhost:25566/api/coords")
    .then(r => r.json())
    .then(d => console.log(`Player ${d.username} at X: ${d.x}, Y: ${d.y}, Z: ${d.z}`));
```

## 🧱 Building

Requires JDK 21.

```bash
./gradlew build
```

The built jar lands in `build/libs/`.

---

## Credits & License

Metis is a stand-alone project by **Suppenterrine**. It started as a backport of
[**PlayerCoordsAPI** by Sukikui](https://github.com/Sukikui/PlayerCoordsAPI) to Minecraft 1.21.1
and has since diverged: the ModMenu/Cloth Config UI was removed in favour of a plain JSON config
file, the package and mod identity were rebuilt around Metis, and the mod is scoped as the
perception sensor of the MCDS ecosystem rather than a general coordinates utility.

Full credit to Sukikui for the original idea and HTTP API design. This project retains the original
MIT licence and copyright notice (see [`LICENSE`](LICENSE)); it is **not** affiliated with or
endorsed by the upstream project.

Licensed under the [MIT License](LICENSE).
