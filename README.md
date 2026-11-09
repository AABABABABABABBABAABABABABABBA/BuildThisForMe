# minecraft-mcp

MCP server that bridges any AI assistant to a live Minecraft session. Your Minecraft client points at a local proxy, the proxy forwards to the real server, and the MCP exposes tools the AI can call: read state, send chat, move, attack, place, inspect the anti-cheat.

Works with any MCP-capable AI (Claude, local LLMs, custom clients) over HTTP JSON-RPC. No mod, no injection, no second account.

## How it works

```
Minecraft client ──► localhost:PROXY_PORT ──► real.server.net:25565
                            ▲
                            │ reads/writes packets
                            │
                       MCP server on :MCP_PORT ──► your AI
```

The proxy sits in the middle so the MCP sees every inbound/outbound packet. Ports are sticky — the one chosen on first run is saved to `.port` and reused on every later run, unless it's already taken, in which case the next free port in 25600-25699 is picked.

## Setup

```bash
npm install
node src/index.js --remote-host play.example.com --version 1.20.4
```

Then in Minecraft, open Multiplayer → Direct Connect → `localhost:<PROXY_PORT>` (logged on stderr).

### Flags

| flag | default | meaning |
|---|---|---|
| `--remote-host` | required | the real server you'd normally join |
| `--remote-port` | 25565 | upstream port |
| `--version` | 1.20.4 | protocol version — must match the server |
| `--proxy-port` | auto | pin a specific proxy port instead of sticky |
| `--mcp-port` | auto | pin a specific MCP HTTP port |

## Tools

| tool | what it does |
|---|---|
| `status` | position, health, dimension, gamemode, detected anti-cheat |
| `read_chat` | recent chat + system messages |
| `send_chat` | send a chat line or slash command as the player |
| `move_to` | walk to x,y,z under the vanilla speed cap |
| `look_at` | aim yaw/pitch at a world coordinate |
| `attack_entity` | swing + attack a specific entity id |
| `use_item` | right-click the held item |
| `hold_slot` | switch hotbar slot 0-8 |
| `list_inventory` | inventory + hotbar |
| `list_entities` | nearby tracked entities within N blocks |
| `list_players` | tab list |
| `read_anticheat` | detected anti-cheat, flags seen, position corrections, mitigation tips |
| `raw_packet` | send any named client→server packet (research only) |
| `disconnect` | close the session |

## Anti-cheat detection

`read_anticheat` passively fingerprints which plugin is running by watching:

- kick message text (`Grim`, `Vulcan`, `Matrix`, `Verus`, `NCP`, `AAC`, `Spartan`, `Themis`, `Horizon`, `Hawk`, `Karhu`, `Witherac`, `Intave`, `Polar`)
- plugin channel names (`grim:...`, `vulcan:...`, `aac:...`)
- in-chat flag strings (`flagged`, `violation`, `reach`, `killaura`, etc.)
- rate of server-forced position corrections (rubber-banding bursts)

It also logs the raw correction stream so you can tell whether the server is doing client-authoritative movement or server-side prediction (Grim/Vulcan style).

The mitigation tips are human-cadence hints (speed caps, on-ground truthfulness, swing-before-attack, teleport confirms). Nothing in this project tries to forge packets or evade correctly-functioning checks — it reports what's observable so you can analyze the server honestly.

## Any AI client

The MCP speaks standard JSON-RPC 2.0 over HTTP POST:

```bash
curl -s http://localhost:MCP_PORT -X POST \
  -H 'content-type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

SSE is served on `/sse` for clients that prefer it.

## Files

```
src/
  index.js         entry — parses args, picks ports, wires everything
  port.js          sticky-port picker
  proxy.js         minecraft-protocol proxy
  state.js         player + world state from server packets
  anticheat.js     fingerprinting + mitigation tips
  tools.js         MCP tool definitions
  mcp-server.js    HTTP JSON-RPC transport
```
