export const DB = [
  { id: 'MCP-GRIM-001', ac: 'Grim', affects: '< 2.3.60', fixed: '2.3.60', sev: 'medium',
    title: 'Vehicle setback bypass', category: 'movement',
    desc: 'Mounting/dismounting a vehicle during a setback stalled the predictor and let a cheat skip a correction round. Fixed via vehicle-state resync in the position predictor.',
    ref: 'https://github.com/GrimAnticheat/Grim/releases' },
  { id: 'MCP-GRIM-002', ac: 'Grim', affects: '< 2.3.57', fixed: '2.3.57', sev: 'medium',
    title: 'Elytra prediction gap during riptide', category: 'movement',
    desc: 'Riptide-into-elytra transitions under specific latency caused one-tick false-negatives on speed checks.',
    ref: 'https://github.com/GrimAnticheat/Grim/releases' },
  { id: 'MCP-GRIM-003', ac: 'Grim', affects: '< 2.3.50', fixed: '2.3.50', sev: 'low',
    title: 'SetbackTeleportUtil frame-drop', category: 'movement',
    desc: 'High-latency join raced with chunk-load confirmation and dropped the first setback packet.',
    ref: 'https://github.com/GrimAnticheat/Grim/commits/2.x' },

  { id: 'MCP-VULCAN-001', ac: 'Vulcan', affects: '< 2.9.0', fixed: '2.9.0', sev: 'medium',
    title: 'Reach check latency compensation', category: 'combat',
    desc: 'Reach check used client-reported latency rather than a server-smoothed RTT; high-ping sessions had wider reach windows than intended.',
    ref: 'https://www.spigotmc.org/resources/vulcan-anticheat.83626/' },
  { id: 'MCP-VULCAN-002', ac: 'Vulcan', affects: '< 2.8.0', fixed: '2.8.0', sev: 'low',
    title: 'KillAura rotation smoothing gap', category: 'combat',
    desc: 'Rotation-based killaura detector had a 1-tick observation gap at the end of a swing.',
    ref: 'https://www.spigotmc.org/resources/vulcan-anticheat.83626/' },

  { id: 'MCP-MATRIX-001', ac: 'Matrix', affects: '6.x', fixed: '7.0', sev: 'high',
    title: 'Matrix 6 legacy prediction model', category: 'general',
    desc: 'Matrix 6 is weaker than Matrix 7 across movement, combat, and scaffold. Many public bypasses. Upgrade recommended.',
    ref: 'https://www.spigotmc.org/resources/matrix-anticheat.27269/' },

  { id: 'MCP-NCP-001', ac: 'NCP', affects: 'all', fixed: null, sev: 'high',
    title: 'NoCheatPlus is legacy', category: 'general',
    desc: 'No active movement-model updates matching modern MC physics; many public bypasses. Migrate to Grim/Vulcan for 1.19+.',
    ref: 'https://github.com/Updated-NoCheatPlus/NoCheatPlus' },
  { id: 'MCP-NCP-002', ac: 'NCP', affects: '< 1.17-SNAPSHOT', fixed: '1.17-SNAPSHOT', sev: 'medium',
    title: 'SurvivalFly fall-distance desync', category: 'movement',
    desc: 'Fall distance reset on edge-step transitions one tick late, enabling NoFall-style damage avoidance.',
    ref: 'https://github.com/Updated-NoCheatPlus/Issues' },

  { id: 'MCP-AAC-001', ac: 'AAC', affects: '<= 5.x', fixed: null, sev: 'high',
    title: 'AAC 5 EOL', category: 'general',
    desc: 'End-of-life; public bypass scripts exist. Free line no longer receives fixes.',
    ref: 'https://www.spigotmc.org/resources/aac-advanced-anti-cheat.3245/' },

  { id: 'MCP-SPARTAN-001', ac: 'Spartan', affects: 'all', fixed: null, sev: 'medium',
    title: 'Heuristic-heavy design', category: 'general',
    desc: 'Heuristic-forward rather than physics-prediction. Timing-aware clients can tune under thresholds; false positives on latency also documented.',
    ref: 'https://www.spigotmc.org/resources/spartan.29216/' },

  { id: 'MCP-POLAR-001', ac: 'Polar', affects: '< 1.0.5', fixed: '1.0.5', sev: 'low',
    title: 'Early version prediction holes', category: 'movement',
    desc: 'Early releases shipped before the predictor caught up to 1.20 step-climb physics.',
    ref: null },

  { id: 'MCP-GENERIC-001', ac: '*', affects: 'any', fixed: null, sev: 'info',
    title: '/plugins leaks the anti-cheat identity', category: 'disclosure',
    desc: 'If `/pl` or `/plugins` is unrestricted, any player can see which anti-cheat and version. Most servers forget to deny `bukkit.command.plugins`.',
    ref: null },
  { id: 'MCP-GENERIC-002', ac: '*', affects: 'any', fixed: null, sev: 'info',
    title: 'Plugin channel disclosure', category: 'disclosure',
    desc: 'Anti-cheats that register plugin channels (`grim:*`, `vulcan:*`) leak identity to the client via channel registration, no permissions required.',
    ref: null },
];

function cmpVer(a, b) {
  const as = a.replace(/[^0-9.]/g, '').split('.').map(Number);
  const bs = b.replace(/[^0-9.]/g, '').split('.').map(Number);
  const n = Math.max(as.length, bs.length);
  for (let i = 0; i < n; i++) {
    const ai = as[i] || 0, bi = bs[i] || 0;
    if (ai !== bi) return ai - bi;
  }
  return 0;
}

export function isLikelyAffected(entry, detectedVersion) {
  if (!detectedVersion) return true;
  const spec = entry.affects;
  if (!spec || spec === 'any' || spec === 'all') return true;
  if (spec.startsWith('<=')) return cmpVer(detectedVersion, spec.slice(2).trim()) <= 0;
  if (spec.startsWith('<'))  return cmpVer(detectedVersion, spec.slice(1).trim()) <  0;
  return spec.toLowerCase() === detectedVersion.toLowerCase();
}

export function findFor(ac) {
  if (!ac) return DB.filter(e => e.ac === '*');
  const low = ac.toLowerCase();
  return DB.filter(e => e.ac.toLowerCase() === low || e.ac === '*');
}
