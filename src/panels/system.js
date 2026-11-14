import os from 'node:os';
import { F, BOLD, RESET, bar, sparkline } from '../ui.js';

function fmtUptime(sec) {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = Math.floor(sec % 60);
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

export function initSystem(state) {
  state.system = { ok: true, hist: [], tick: 0 };
}

export function tickSystem(state) {
  const s = state.system;
  s.tick++;
  const memPct = 1 - os.freemem() / os.totalmem();
  const cpus = os.cpus().length || 1;
  const loadPct = Math.min(1, os.loadavg()[0] / cpus);
  s.mem = memPct;
  s.load = loadPct;
  s.cpus = cpus;
  s.uptime = os.uptime();
  s.hist.push(loadPct);
  if (s.hist.length > 60) s.hist.shift();
}

export function systemLines(state, w) {
  const s = state.system;
  const inner = w - 4;
  if (!s || !s.ok) return offline(inner);
  const memBar = bar(s.mem, inner - 16);
  const loadBar = bar(s.load, inner - 16);
  return [
    `${F.gray}HOST${RESET}      ${F.white}${BOLD}${os.hostname().slice(0, 18)}${RESET}`,
    `${F.gray}OS${RESET}        ${F.cyan}${os.platform()} ${os.release()}${RESET}`,
    `${F.gray}CPU${RESET}       ${F.white}${(s.load * 100).toFixed(0).padStart(3)}%${RESET} ${loadBar} ${F.gray}x${s.cpus}${RESET}`,
    `${F.gray}MEM${RESET}       ${F.white}${(s.mem * 100).toFixed(0).padStart(3)}%${RESET} ${memBar} ${F.gray}${(os.totalmem() / 1024 ** 3).toFixed(1)}G${RESET}`,
    `${F.gray}UPTIME${RESET}    ${F.green}${fmtUptime(s.uptime)}${RESET}`,
    `${F.gray}LOAD 60s${RESET}  ${F.magenta}${sparkline(s.hist, Math.max(10, inner - 9))}${RESET}`
  ];
}

export function offline(inner) {
  return [
    `${F.red}▒ NO SIGNAL ▒${RESET}`,
    `${F.gray}${'░'.repeat(Math.max(0, inner - 4))}${RESET}`
  ];
}
