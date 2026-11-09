const FINGERPRINTS = [
  { name: 'Grim', keywords: ['grim', '[grim]', 'gac-'] },
  { name: 'Vulcan', keywords: ['vulcan', '[vulcan]'] },
  { name: 'Matrix', keywords: ['matrix', '[matrix]'] },
  { name: 'Verus', keywords: ['verus', '[verus]'] },
  { name: 'NCP', keywords: ['nocheatplus', 'ncp'] },
  { name: 'AAC', keywords: ['advanced anti cheat', 'aac'] },
  { name: 'Spartan', keywords: ['spartan'] },
  { name: 'Themis', keywords: ['themis'] },
  { name: 'Horizon', keywords: ['horizon'] },
  { name: 'Hawk', keywords: ['hawk'] },
  { name: 'Karhu', keywords: ['karhu'] },
  { name: 'Witherac', keywords: ['witherac'] },
  { name: 'Intave', keywords: ['intave'] },
  { name: 'Polar', keywords: ['polar'] },
];

const FLAG_HINTS = [
  'flagged', 'violation', 'suspicious', 'moving too fast',
  'you are hacking', 'cheating is not allowed', 'illegal action',
  'reach', 'killaura', 'fly', 'speed', 'scaffold', 'aimbot',
  'anti knockback', 'nofall', 'phase', 'autoclick',
];

export function makeAnticheat() {
  return {
    name: null,
    confidence: 0,
    signals: [],
    rubberBands: 0,
    lastRubberBandAt: 0,
    corrections: [],
  };
}

function textFromChat(msg) {
  if (!msg) return '';
  if (typeof msg === 'string') {
    try {
      const j = JSON.parse(msg);
      return extractText(j).toLowerCase();
    } catch {
      return msg.toLowerCase();
    }
  }
  return extractText(msg).toLowerCase();
}

function extractText(node) {
  if (!node) return '';
  if (typeof node === 'string') return node;
  let out = node.text ?? '';
  if (node.translate) out += ' ' + node.translate;
  if (node.with) for (const w of node.with) out += ' ' + extractText(w);
  if (node.extra) for (const e of node.extra) out += ' ' + extractText(e);
  return out;
}

export function observe(ac, name, data) {
  switch (name) {
    case 'kick_disconnect':
    case 'disconnect': {
      const t = textFromChat(data.reason);
      matchFingerprint(ac, t, 'kick');
      scanFlags(ac, t, 'kick');
      break;
    }
    case 'chat':
    case 'system_chat': {
      const t = textFromChat(data.message ?? data.content);
      matchFingerprint(ac, t, 'chat');
      scanFlags(ac, t, 'chat');
      break;
    }
    case 'position': {
      ac.rubberBands += 1;
      ac.lastRubberBandAt = Date.now();
      ac.corrections.push({ at: Date.now(), x: data.x, y: data.y, z: data.z });
      if (ac.corrections.length > 50) ac.corrections.shift();
      if (ac.rubberBands > 5 && !ac.name) {
        ac.signals.push({ kind: 'rubberband-burst', at: Date.now() });
      }
      break;
    }
    case 'plugin_message': {
      const ch = (data.channel ?? '').toLowerCase();
      if (ch.includes('grim')) markHit(ac, 'Grim', ch, 0.9);
      if (ch.includes('vulcan')) markHit(ac, 'Vulcan', ch, 0.8);
      if (ch.includes('aac')) markHit(ac, 'AAC', ch, 0.7);
      break;
    }
  }
}

function matchFingerprint(ac, text, source) {
  if (!text) return;
  for (const fp of FINGERPRINTS) {
    for (const kw of fp.keywords) {
      if (text.includes(kw)) {
        markHit(ac, fp.name, `${source}:${kw}`, source === 'kick' ? 0.95 : 0.7);
        return;
      }
    }
  }
}

function scanFlags(ac, text, source) {
  if (!text) return;
  for (const hint of FLAG_HINTS) {
    if (text.includes(hint)) {
      ac.signals.push({ kind: 'flag', hint, source, at: Date.now() });
      return;
    }
  }
}

function markHit(ac, name, source, confidence) {
  if (ac.confidence >= confidence && ac.name === name) return;
  ac.name = name;
  ac.confidence = Math.max(ac.confidence, confidence);
  ac.signals.push({ kind: 'fingerprint', name, source, at: Date.now() });
}

export function suggestMitigations(ac) {
  const tips = [];
  if (!ac.name) {
    tips.push('no anti-cheat fingerprint yet; use read_anticheat after a few minutes of play');
  }
  if (ac.rubberBands > 0) {
    tips.push('server is correcting your position — keep movement deltas under the vanilla cap (~0.6 blocks/tick walk, ~0.28 air)');
  }
  tips.push('always send on-ground flag truthfully; false ground states flag fly/nofall checks');
  tips.push('send swing_arm before use_entity for attack packets; some checks require it');
  tips.push('throttle actions to human cadence: max ~20 CPS attacks, ~15 CPS block place');
  tips.push('respect server teleport_confirm packets; silent drops flag timer checks');
  if (ac.name === 'Grim' || ac.name === 'Vulcan') {
    tips.push(`${ac.name} runs server-side prediction — any desync (sprint+jump+attack timing) flags fast`);
  }
  return tips;
}
