import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';

export function startMcp({ port, tools, log }) {
  const toolMap = new Map(tools.map(t => [t.name, t]));

  const http = createServer(async (req, res) => {
    if (req.method === 'GET' && req.url === '/health') {
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ ok: true, tools: tools.map(t => t.name) }));
      return;
    }

    if (req.method === 'GET' && req.url === '/sse') {
      res.writeHead(200, {
        'content-type': 'text/event-stream',
        'cache-control': 'no-cache',
        connection: 'keep-alive',
      });
      res.write(`event: endpoint\ndata: /messages?session=${randomUUID()}\n\n`);
      const ping = setInterval(() => res.write(': ping\n\n'), 20000);
      req.on('close', () => clearInterval(ping));
      return;
    }

    if (req.method !== 'POST') {
      res.writeHead(405); res.end(); return;
    }

    let body = '';
    for await (const chunk of req) body += chunk;
    let msg;
    try { msg = JSON.parse(body); } catch {
      res.writeHead(400); res.end('bad json'); return;
    }

    const reply = await handle(msg, toolMap, log);
    res.writeHead(200, { 'content-type': 'application/json' });
    res.end(JSON.stringify(reply));
  });

  http.listen(port, '0.0.0.0', () => log(`MCP HTTP listening on :${port}`));
  return http;
}

async function handle(msg, toolMap, log) {
  const id = msg.id ?? null;
  const method = msg.method;
  const params = msg.params ?? {};

  try {
    if (method === 'initialize') {
      return {
        jsonrpc: '2.0', id,
        result: {
          protocolVersion: '2024-11-05',
          serverInfo: { name: 'minecraft-mcp', version: '1.0.0' },
          capabilities: { tools: {} },
        },
      };
    }

    if (method === 'tools/list') {
      return {
        jsonrpc: '2.0', id,
        result: {
          tools: [...toolMap.values()].map(t => ({
            name: t.name,
            description: t.description,
            inputSchema: t.inputSchema,
          })),
        },
      };
    }

    if (method === 'tools/call') {
      const tool = toolMap.get(params.name);
      if (!tool) throw new Error(`unknown tool: ${params.name}`);
      const result = await tool.handler(params.arguments ?? {});
      return {
        jsonrpc: '2.0', id,
        result: {
          content: [{ type: 'text', text: JSON.stringify(result, replacer, 2) }],
        },
      };
    }

    if (method === 'notifications/initialized' || method?.startsWith('notifications/')) {
      return null;
    }

    return { jsonrpc: '2.0', id, error: { code: -32601, message: `method not found: ${method}` } };
  } catch (err) {
    log(`tool error: ${err.message}`);
    return {
      jsonrpc: '2.0', id,
      result: {
        isError: true,
        content: [{ type: 'text', text: err.message }],
      },
    };
  }
}

function replacer(_k, v) {
  if (v instanceof Map) return Object.fromEntries(v);
  if (v instanceof Set) return [...v];
  if (typeof v === 'bigint') return v.toString();
  return v;
}
