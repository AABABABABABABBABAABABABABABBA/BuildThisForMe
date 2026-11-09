import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { createServer } from 'node:net';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const _here = dirname(fileURLToPath(import.meta.url));

const MIN = 25600;
const MAX = 25699;

function fileFor(role) {
  return join(_here, '..', `.port.${role}`);
}

function isFree(port) {
  return new Promise((resolve) => {
    const s = createServer();
    s.once('error', () => resolve(false));
    s.once('listening', () => s.close(() => resolve(true)));
    s.listen(port, '0.0.0.0');
  });
}

function readSaved(role) {
  const f = fileFor(role);
  if (!existsSync(f)) return null;
  const raw = readFileSync(f, 'utf8').trim();
  const n = Number(raw);
  return Number.isInteger(n) && n > 0 && n < 65536 ? n : null;
}

function save(role, port) {
  writeFileSync(fileFor(role), String(port));
}

export async function stickyPort(role, preferred, exclude = new Set()) {
  const saved = preferred ?? readSaved(role);
  if (saved && !exclude.has(saved) && await isFree(saved)) {
    save(role, saved);
    return saved;
  }

  for (let p = MIN; p <= MAX; p++) {
    if (exclude.has(p)) continue;
    if (await isFree(p)) {
      save(role, p);
      return p;
    }
  }

  throw new Error(`no free port in ${MIN}-${MAX}`);
}
