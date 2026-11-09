import mc from 'minecraft-protocol';
import { applyServerPacket } from './state.js';
import { observe } from './anticheat.js';

export function startProxy({ port, remoteHost, remotePort, version, state, ac, log }) {
  const server = mc.createServer({
    'online-mode': false,
    host: '0.0.0.0',
    port,
    keepAlive: false,
    version,
    maxPlayers: 1,
    motd: 'Minecraft MCP Proxy — connect here instead of the real server',
  });

  const session = {
    client: null,
    target: null,
    outbound: [],
  };

  server.on('login', (client) => {
    if (session.client) {
      client.end('Proxy already in use by another session.');
      return;
    }
    session.client = client;
    state.username = client.username;
    log(`client connected: ${client.username}`);

    const target = mc.createClient({
      host: remoteHost,
      port: remotePort,
      username: client.username,
      version,
      auth: 'offline',
      keepAlive: true,
    });
    session.target = target;

    client.on('packet', (data, meta) => {
      if (meta.state !== 'play') return;
      try {
        target.write(meta.name, data);
      } catch (err) {
        log(`client->server write failed on ${meta.name}: ${err.message}`);
      }
    });

    target.on('packet', (data, meta) => {
      if (meta.state !== 'play') return;
      try {
        client.write(meta.name, data);
        applyServerPacket(state, meta.name, data);
        observe(ac, meta.name, data);
      } catch (err) {
        log(`server->client write failed on ${meta.name}: ${err.message}`);
      }
    });

    client.on('end', () => {
      log('client disconnected');
      try { target.end('proxy client ended'); } catch {}
      session.client = null;
      session.target = null;
      state.connected = false;
    });
    client.on('error', (e) => log(`client error: ${e.message}`));

    target.on('end', (reason) => {
      log(`target ended: ${reason ?? ''}`);
      try { client.end(reason || 'target ended'); } catch {}
      state.connected = false;
    });
    target.on('error', (e) => log(`target error: ${e.message}`));
    target.on('kick_disconnect', (data) => {
      applyServerPacket(state, 'kick_disconnect', data);
      observe(ac, 'kick_disconnect', data);
    });

    while (session.outbound.length) {
      const [name, data] = session.outbound.shift();
      try { target.write(name, data); } catch {}
    }
  });

  server.on('error', (e) => log(`proxy error: ${e.message}`));

  function send(name, data) {
    if (!session.target) {
      session.outbound.push([name, data]);
      return false;
    }
    session.target.write(name, data);
    return true;
  }

  return { server, session, send };
}
