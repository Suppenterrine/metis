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

| Endpoint      | Method           | Description                                              |
|---------------|------------------|----------------------------------------------------------|
| `/api/coords` | `GET`, `OPTIONS` | Returns the player's current coordinates and world infos |

### Response Fields

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
