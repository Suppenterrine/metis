# Configuration

Metis is configured by a single plain JSON file. There is no in-game UI and no
ModMenu/Cloth Config dependency — by design.

## File location

```
.minecraft/config/metis.json
```

It is created with defaults on first launch if absent. If the file is missing or
unreadable, Metis falls back to defaults and rewrites a clean file.

## Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `enabled` | `boolean` | `true` | Master switch. Starts/stops the HTTP server. |
| `port` | `integer` | `25566` | TCP port the local API listens on. |

```json
{
  "enabled": true,
  "port": 25566
}
```

## Behaviour

- **Live toggle.** `enabled` is re-checked every client tick. Flipping it (and
  reloading the file) starts or stops the server without restarting the game.
  While disabled, no snapshot is captured and the port is released.
- **Port default.** `25566` is one above the usual Minecraft server port
  (`25565`), so it does not clash with a local server. Change it freely.
- **Port sanitisation.** On load, a port outside `1–65535` is reset to the
  default and the file is rewritten.
- **Loopback only.** Regardless of port, the server binds to the loopback
  interface and only answers `127.0.0.1` / `::1`. This is not configurable — see
  [`conventions.md`](conventions.md).

## Source

`src/main/java/dev/suppenterrine/metis/config/MetisConfig.java`
