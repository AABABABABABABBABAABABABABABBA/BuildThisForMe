# minecraft-mcp

Two ways to let any AI drive Minecraft:

1. **`mod/`** — a Fabric client mod. Drop the .jar in your `mods/` folder. When Minecraft launches, a terminal window opens showing `Connected` / `Failed` / `Network error`, the sticky MCP port, config dir, and the GitHub URL. The MCP runs inside the JVM so it can read screens, decompile loaded classes, record+replay container clicks, drive movement with the real physics engine, and read the tab list / scoreboard / boss bars / lore.

2. **`src/`** — a Node.js transparent proxy. Point your vanilla client at `localhost:PORT`, it forwards to the real server, the MCP sees every packet both ways. Lower level — useful for raw packet research and anti-cheat forensics when you don't want to install a mod.

Both expose a JSON-RPC MCP server on a sticky port in `25600-25699`. Any AI (Claude, local LLM, custom) connects over HTTP POST.

## Honest scope note on "undetected"

Server-side anti-cheats that run server-side prediction (**Grim**, **Vulcan**) know physics. No client can beat that purely by being a client. What the mod ships is:

- Cadence limiter (click/rotation speed caps with jitter)
- Smoothed rotations that stay under per-tick yaw deltas most server checks care about
- Honest on-ground state, teleport-confirm handling, swing-before-attack packet order
- Passive fingerprinter that tells you which anti-cheat the server runs so you know the ceiling

You can tune the limiter at runtime with `anticheat_tune`. It defers to the server when the physics don't line up — that's the correct thing for a research tool.

## The mod (`mod/`)

### Build

```bash
cd mod
# One-time: generate gradle wrapper (needs gradle 8.10+)
gradle wrapper --gradle-version 8.10
./gradlew build
# jar lands in build/libs/mcpmc-1.0.0.jar
```

### Install

1. Install Minecraft **1.21.1** + Fabric Loader **0.16+**
2. Install **Fabric API** mod in your `mods/` folder
3. Drop `mcpmc-1.0.0.jar` next to it
4. Launch Minecraft

A terminal window opens with:
```
Minecraft MCP                                             Connected
[hh:mm:ss] Build mcpmc v1.0.0
[hh:mm:ss] MCP endpoint:    http://localhost:25600
[hh:mm:ss] Tools:           ~40
[hh:mm:ss] Strategies:      4
[hh:mm:ss] GitHub:          https://github.com/.../BuildThisForMe
[hh:mm:ss] Config dir:      .minecraft/config/mcpmc
```

### Tool groups (~40 tools)

| group | examples |
|---|---|
| core | `status`, `version` |
| chat | `chat_read`, `chat_send`, `chat_search`, `chat_clear` |
| movement | `move_to`, `look_at`, `jump`, `sneak`, `sprint`, `stop`, `walk_direction` |
| world | `block_at`, `find_blocks`, `raycast`, `dimension_info`, `chunk_info` |
| entity | `entities_nearby`, `attack_entity`, `interact_entity` |
| inventory | `inventory_list`, `hold_slot`, `drop_held`, `drop_slot`, `swap_to_offhand`, `use_item`, `find_item` |
| container | `container_read`, `container_click`, `container_close`, `container_record`, `container_stop_recording`, `container_replay`, `container_recordings` |
| gui | `tablist_read`, `tablist_header_footer`, `scoreboard_read`, `bossbars_read`, `screen_info`, `overlay_set`, `close_screen`, `open_inventory` |
| server | `server_join`, `server_disconnect`, `server_info`, `servers_saved` |
| registry | `registry_list`, `resource_read`, `class_dump`, `mods_list` |
| anticheat | `anticheat_read`, `anticheat_tune`, `anticheat_reset` |
| strategy | `strategy_list`, `strategy_run`, `strategy_status`, `strategy_cancel`, `strategy_runs` |

### Strategies (higher-level recipes)

| id | what it does |
|---|---|
| `base_finder` | spiral-scan for beacons, chests, shulker boxes, enchant tables, lodestones, barrels. Clusters hits into candidate base coords. Pass `server_hint: "donutsmp"` to enable server-specific heuristics when added. |
| `shop_replay` | replay a recorded container-click sequence N times with jitter. Pair with `container_record` to capture a shop purchase path. |
| `follow_player` | follow a named player at `distance` blocks, sprint at `sprint_at`. |
| `mine_block` | find nearest block matching, walk to it, face it. |

Add your own by implementing `Strategy` and registering it.

### Decompile / class dump

`class_dump` returns the raw .class bytes (base64) for any loaded class. Feed to CFR or Vineflower offline to see the source. `resource_read` returns asset files (lang jsons, model jsons, etc.) by `namespace:path`.

### GUI reading

`tablist_read` returns every player's name/uuid/ping/gamemode. `scoreboard_read` returns the sidebar lines (server balance readouts, team info). `container_read` returns every slot's item id, count, display name, and lore — that's how you read shop prices. `screen_info` tells you which screen class is open so you know if a custom shop GUI is up.

### Terminal statuses

- **Connected** — green. MCP is listening.
- **Network error** — red. Port couldn't bind. Close whatever took it or let the mod pick a new one.
- **Failed** — red. Something else broke. Check `logs/latest.log`.

## The node proxy (`src/`)

See the original scope doc. Still useful for raw-packet research when you can't install a mod. Keep this if your target server banned client mods — the proxy looks like a vanilla client to the server.

```
npm install
node src/index.js --remote-host play.example.com --version 1.20.4
```

## Any-AI usage

Standard JSON-RPC 2.0 over HTTP POST:

```bash
curl -s http://localhost:25600 -X POST \
  -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

```bash
curl -s http://localhost:25600 -X POST \
  -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"strategy_run","arguments":{"id":"base_finder","args":{"search_radius_chunks":8,"server_hint":"donutsmp"}}}}'
```

## Repo layout

```
mod/                     Fabric 1.21.1 mod
  build.gradle
  gradle.properties
  settings.gradle
  src/main/
    java/com/mcpmc/
      McpMod.java        entry
      terminal/          Swing status window
      server/            HTTP + sticky port + JSON
      tools/             ~40 tools in grouped files
      strategy/          Strategy registry + 4 defaults
      anticheat/         cadence limiter + fingerprint analyzer
      hooks/             chat / gui / packet capture
      mixin/             chat, playerlist, inv, hud overlay
    resources/
      fabric.mod.json
      mcpmc.mixins.json

src/                     Node transparent proxy (original)
  index.js  proxy.js  state.js  anticheat.js  tools.js  mcp-server.js  port.js
```
