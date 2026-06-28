# `GET /api/players`

The tab-list common knowledge — who is online, their ping and game mode.

## At a glance

| | |
|---|---|
| Path | `/api/players` |
| Methods | `GET`, `OPTIONS` |
| Perception class | `BROADCAST` |
| Source | `PerceptionSnapshot#playersPayload()` |

## Why it exists

The tab/player list is information the game already shows every player through a
UI affordance, regardless of proximity. A player can open tab and see who is
online and their latency, so a bot may know the same.

This is `BROADCAST` — out-of-band common knowledge. It is allowed, but it is
**meta, low-embodiment** data: it is not the kind of real sensing Metis exists
for. Prefer the perception endpoints where a choice exists. Reported here is what
the tab list actually lists (`getListedPlayerListEntries`).

## Request

```
GET /api/players
```

## Response

`200 application/json`

| Field | Type | Null? | Description |
|---|---|---|---|
| `perception` | `string` | no | Always `"BROADCAST"`. |
| `singleplayer` | `boolean` | no | Whether this is a singleplayer/integrated-server session. |
| `server` | `string` | yes | Connected server address, or `null` in singleplayer. |
| `players` | `array` | no | Listed players (may be empty). |

### Player object

| Field | Type | Null? | Description |
|---|---|---|---|
| `uuid` | `string` | no | Player UUID (from the game profile). |
| `name` | `string` | no | Player name (from the game profile). |
| `latency` | `integer` | no | Ping in milliseconds, as shown by the connection bars. |
| `gameMode` | `string` | yes | `survival`, `creative`, `adventure`, `spectator`, or `null` if unknown. |

### Example

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

## Errors

Standard — see [`../conventions.md`](../conventions.md).

## Perception & gating

`BROADCAST`. No gate is required by the rule — every player has the tab list —
but treat it as meta knowledge, not embodied sensing. Metis applies no gating.

## Reads (Minecraft API)

Verified against Yarn `1.21.1`:

| Field | Source call |
|---|---|
| `singleplayer` | `MinecraftClient#isInSingleplayer()` |
| `server` | `MinecraftClient#getCurrentServerEntry().address` |
| players | `ClientPlayNetworkHandler#getListedPlayerListEntries()` |
| `uuid` / `name` | `PlayerListEntry#getProfile().getId()` / `getName()` |
| `latency` | `PlayerListEntry#getLatency()` |
| `gameMode` | `PlayerListEntry#getGameMode().getName()` |

## Example call

```bash
curl http://localhost:25566/api/players
```
