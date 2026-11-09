import { suggestMitigations } from './anticheat.js';

export function defineTools({ state, ac, proxy, log }) {
  return [
    {
      name: 'status',
      description: 'Get current player + proxy status: username, position, health, dimension, whether a client is connected through the proxy, and the anti-cheat fingerprint so far.',
      inputSchema: { type: 'object', properties: {} },
      handler: () => ({
        connected: state.connected,
        username: state.username,
        position: state.position && { x: state.position.x, y: state.position.y, z: state.position.z },
        yaw: state.yaw,
        pitch: state.pitch,
        health: state.health,
        food: state.food,
        dimension: state.dimension,
        gamemode: state.gamemode,
        heldSlot: state.held,
        tps: state.tps,
        anticheat: { name: ac.name, confidence: ac.confidence, rubberBands: ac.rubberBands },
      }),
    },

    {
      name: 'read_chat',
      description: 'Return the most recent chat and system messages the server has sent. Pass limit for how many to return (default 30).',
      inputSchema: {
        type: 'object',
        properties: { limit: { type: 'number', default: 30 } },
      },
      handler: ({ limit = 30 }) => ({ messages: state.chat.slice(-limit) }),
    },

    {
      name: 'send_chat',
      description: 'Send a chat message or run a slash command as the player. Pass exactly what would be typed in the chat box.',
      inputSchema: {
        type: 'object',
        properties: { message: { type: 'string' } },
        required: ['message'],
      },
      handler: ({ message }) => {
        if (!state.connected) throw new Error('not connected to a server');
        proxy.send('chat', { message });
        return { sent: message };
      },
    },

    {
      name: 'move_to',
      description: 'Move the player toward absolute coordinates by sending position_look packets. Takes x,y,z and optional yaw/pitch. Pass per_tick_blocks to cap speed (default 0.4 blocks per tick — stays under vanilla walk cap of 0.6).',
      inputSchema: {
        type: 'object',
        properties: {
          x: { type: 'number' },
          y: { type: 'number' },
          z: { type: 'number' },
          yaw: { type: 'number' },
          pitch: { type: 'number' },
          per_tick_blocks: { type: 'number', default: 0.4 },
        },
        required: ['x', 'y', 'z'],
      },
      handler: async ({ x, y, z, yaw, pitch, per_tick_blocks = 0.4 }) => {
        if (!state.position) throw new Error('no position yet — not in a world');
        const target = { x, y, z };
        return await walk(state, proxy, target, { yaw, pitch, speed: per_tick_blocks });
      },
    },

    {
      name: 'look_at',
      description: 'Set yaw/pitch so the player is facing a world coordinate. Useful before attack/interact.',
      inputSchema: {
        type: 'object',
        properties: {
          x: { type: 'number' }, y: { type: 'number' }, z: { type: 'number' },
        },
        required: ['x', 'y', 'z'],
      },
      handler: ({ x, y, z }) => {
        if (!state.position) throw new Error('no position');
        const dx = x - state.position.x;
        const dy = y - (state.position.y + 1.62);
        const dz = z - state.position.z;
        const horiz = Math.sqrt(dx * dx + dz * dz);
        const yaw = -Math.atan2(dx, dz) * 180 / Math.PI;
        const pitch = -Math.atan2(dy, horiz) * 180 / Math.PI;
        state.yaw = yaw; state.pitch = pitch;
        proxy.send('look', { yaw, pitch, onGround: state.onGround });
        return { yaw, pitch };
      },
    },

    {
      name: 'attack_entity',
      description: 'Attack a specific entity id. Sends swing_arm first, then use_entity with action 1 (attack). Pair with look_at to stay within reach for anti-cheat.',
      inputSchema: {
        type: 'object',
        properties: { entity_id: { type: 'number' } },
        required: ['entity_id'],
      },
      handler: ({ entity_id }) => {
        proxy.send('arm_animation', { hand: 0 });
        proxy.send('use_entity', { target: entity_id, mouse: 1, sneaking: false });
        return { attacked: entity_id };
      },
    },

    {
      name: 'use_item',
      description: 'Right-click with the currently held item (eat, drink, throw, place block on air).',
      inputSchema: {
        type: 'object',
        properties: { hand: { type: 'number', default: 0 } },
      },
      handler: ({ hand = 0 }) => {
        proxy.send('use_item', { hand });
        return { used: true };
      },
    },

    {
      name: 'hold_slot',
      description: 'Switch the hotbar slot (0-8).',
      inputSchema: {
        type: 'object',
        properties: { slot: { type: 'number' } },
        required: ['slot'],
      },
      handler: ({ slot }) => {
        if (slot < 0 || slot > 8) throw new Error('slot must be 0-8');
        proxy.send('held_item_slot', { slotId: slot });
        state.held = slot;
        return { held: slot };
      },
    },

    {
      name: 'list_inventory',
      description: 'List the player inventory and hotbar with slot ids and item ids.',
      inputSchema: { type: 'object', properties: {} },
      handler: () => ({
        hotbar: state.hotbar,
        inventory: state.inventory,
      }),
    },

    {
      name: 'list_entities',
      description: 'List nearby tracked entities with id, type, and position. Pass max_distance in blocks.',
      inputSchema: {
        type: 'object',
        properties: { max_distance: { type: 'number', default: 32 } },
      },
      handler: ({ max_distance = 32 }) => {
        const p = state.position;
        if (!p) return { entities: [] };
        const out = [];
        for (const e of state.entities.values()) {
          const dx = e.position.x - p.x, dy = e.position.y - p.y, dz = e.position.z - p.z;
          const d = Math.sqrt(dx * dx + dy * dy + dz * dz);
          if (d <= max_distance) out.push({ id: e.id, type: e.type, position: e.position, distance: d });
        }
        out.sort((a, b) => a.distance - b.distance);
        return { entities: out };
      },
    },

    {
      name: 'list_players',
      description: 'Return the server tab list: players currently online with name + uuid + latency.',
      inputSchema: { type: 'object', properties: {} },
      handler: () => ({ players: [...state.players.values()] }),
    },

    {
      name: 'read_anticheat',
      description: 'Return the detected anti-cheat fingerprint, confidence, observed signals, recent server-forced position corrections, and mitigation tips to reduce kick risk. Pentest-style read, not a bypass toggle.',
      inputSchema: { type: 'object', properties: {} },
      handler: () => ({
        detected: ac.name,
        confidence: ac.confidence,
        rubber_bands: ac.rubberBands,
        last_rubber_band_at: ac.lastRubberBandAt,
        signals: ac.signals.slice(-30),
        recent_corrections: ac.corrections.slice(-10),
        last_kick: state.lastKick,
        mitigations: suggestMitigations(ac),
      }),
    },

    {
      name: 'raw_packet',
      description: 'Send a raw client->server packet by name. Dangerous: malformed packets cause disconnects. For protocol research only.',
      inputSchema: {
        type: 'object',
        properties: {
          name: { type: 'string' },
          data: { type: 'object' },
        },
        required: ['name', 'data'],
      },
      handler: ({ name, data }) => {
        proxy.send(name, data);
        return { sent: name };
      },
    },

    {
      name: 'disconnect',
      description: 'Close the proxy session (both client and upstream).',
      inputSchema: { type: 'object', properties: {} },
      handler: () => {
        if (proxy.session.client) try { proxy.session.client.end('disconnected by MCP'); } catch {}
        if (proxy.session.target) try { proxy.session.target.end('disconnected by MCP'); } catch {}
        return { ok: true };
      },
    },
  ];
}

async function walk(state, proxy, target, { yaw, pitch, speed }) {
  const sent = [];
  const safety = 400;
  let steps = 0;
  while (steps < safety) {
    const p = state.position;
    const dx = target.x - p.x, dy = target.y - p.y, dz = target.z - p.z;
    const dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (dist < 0.1) break;
    const step = Math.min(speed, dist);
    const nx = p.x + (dx / dist) * step;
    const ny = p.y + (dy / dist) * step;
    const nz = p.z + (dz / dist) * step;
    state.position = { ...p, x: nx, y: ny, z: nz };
    proxy.send('position_look', {
      x: nx, y: ny, z: nz,
      yaw: yaw ?? state.yaw,
      pitch: pitch ?? state.pitch,
      onGround: true,
    });
    sent.push({ x: nx, y: ny, z: nz });
    steps += 1;
    await new Promise(r => setTimeout(r, 50));
  }
  return { steps, final: state.position };
}
