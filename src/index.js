#!/usr/bin/env node
import { parseArgs } from 'node:util';
import { stickyPort } from './port.js';
import { startProxy } from './proxy.js';
import { startMcp } from './mcp-server.js';
import { makeState } from './state.js';
import { makeAnticheat } from './anticheat.js';
import { defineTools } from './tools.js';

const { values } = parseArgs({
  options: {
    'remote-host': { type: 'string' },
    'remote-port': { type: 'string', default: '25565' },
    'version': { type: 'string', default: '1.20.4' },
    'proxy-port': { type: 'string' },
    'mcp-port': { type: 'string' },
    'config': { type: 'string' },
  },
});

const remoteHost = values['remote-host'] ?? process.env.MC_HOST;
if (!remoteHost) {
  console.error('missing --remote-host (the real Minecraft server to proxy to)');
  console.error('usage: minecraft-mcp --remote-host play.example.com [--remote-port 25565] [--version 1.20.4]');
  process.exit(1);
}
const remotePort = Number(values['remote-port']);
const version = values['version'];

const proxyPortPref = values['proxy-port'] ? Number(values['proxy-port']) : null;
const mcpPortPref = values['mcp-port'] ? Number(values['mcp-port']) : null;

function log(msg) {
  process.stderr.write(`[${new Date().toISOString()}] ${msg}\n`);
}

const state = makeState();
const ac = makeAnticheat();

const proxyPort = await stickyPort('proxy', proxyPortPref);
const mcpPort = await stickyPort('mcp', mcpPortPref, new Set([proxyPort]));

const proxy = startProxy({
  port: proxyPort,
  remoteHost,
  remotePort,
  version,
  state,
  ac,
  log,
});

const tools = defineTools({ state, ac, proxy, log });
startMcp({ port: mcpPort, tools, log });

log(`minecraft proxy: point your client at localhost:${proxyPort}`);
log(`upstream target: ${remoteHost}:${remotePort} (${version})`);
log(`MCP endpoint:    http://localhost:${mcpPort}  (POST JSON-RPC, SSE on /sse)`);
log(`tools:           ${tools.map(t => t.name).join(', ')}`);

process.on('SIGINT', () => { log('shutting down'); process.exit(0); });
process.on('SIGTERM', () => { log('shutting down'); process.exit(0); });
