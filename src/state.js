import Vec3 from 'vec3';

export function makeState() {
  return {
    connected: false,
    username: null,
    uuid: null,
    position: null,
    velocity: null,
    yaw: 0,
    pitch: 0,
    onGround: true,
    health: 20,
    food: 20,
    saturation: 5,
    gamemode: 0,
    dimension: 'minecraft:overworld',
    held: 0,
    inventory: [],
    hotbar: [],
    players: new Map(),
    entities: new Map(),
    chat: [],
    serverBrand: null,
    serverPlugins: [],
    version: null,
    lastKick: null,
    tps: null,
    _lastTickTimes: [],
  };
}

export function applyServerPacket(state, name, data) {
  switch (name) {
    case 'login': {
      state.connected = true;
      state.uuid = data.entityId;
      state.gamemode = data.gameMode;
      state.dimension = data.worldName ?? data.dimension ?? state.dimension;
      state.version = data.levelType;
      break;
    }
    case 'position': {
      const p = Vec3(data.x, data.y, data.z);
      state.position = p;
      state.yaw = data.yaw;
      state.pitch = data.pitch;
      break;
    }
    case 'update_health': {
      state.health = data.health;
      state.food = data.food;
      state.saturation = data.foodSaturation;
      break;
    }
    case 'respawn': {
      state.dimension = data.worldName ?? data.dimension ?? state.dimension;
      state.gamemode = data.gamemode;
      break;
    }
    case 'held_item_slot': {
      state.held = data.slot;
      break;
    }
    case 'set_slot': {
      if (data.windowId === 0) {
        state.inventory[data.slot] = data.item;
        if (data.slot >= 36 && data.slot <= 44) {
          state.hotbar[data.slot - 36] = data.item;
        }
      }
      break;
    }
    case 'window_items': {
      if (data.windowId === 0) {
        state.inventory = data.items;
        state.hotbar = data.items.slice(36, 45);
      }
      break;
    }
    case 'player_info': {
      for (const entry of data.data ?? []) {
        if (entry.name) state.players.set(entry.UUID, entry);
      }
      break;
    }
    case 'named_entity_spawn': {
      state.entities.set(data.entityId, {
        id: data.entityId,
        uuid: data.playerUUID,
        type: 'player',
        position: Vec3(data.x, data.y, data.z),
      });
      break;
    }
    case 'spawn_entity_living':
    case 'spawn_entity': {
      state.entities.set(data.entityId, {
        id: data.entityId,
        type: data.type,
        position: Vec3(data.x, data.y, data.z),
      });
      break;
    }
    case 'entity_destroy': {
      for (const id of data.entityIds ?? []) state.entities.delete(id);
      break;
    }
    case 'kick_disconnect':
    case 'disconnect': {
      state.lastKick = { reason: data.reason, at: Date.now() };
      state.connected = false;
      break;
    }
    case 'chat':
    case 'system_chat': {
      state.chat.push({ at: Date.now(), raw: data.message ?? data.content });
      if (state.chat.length > 200) state.chat.shift();
      break;
    }
    case 'update_time': {
      const now = Date.now();
      state._lastTickTimes.push(now);
      if (state._lastTickTimes.length > 20) state._lastTickTimes.shift();
      if (state._lastTickTimes.length >= 2) {
        const span = now - state._lastTickTimes[0];
        const ticks = state._lastTickTimes.length - 1;
        state.tps = Math.min(20, (ticks * 1000) / span);
      }
      break;
    }
  }
}
