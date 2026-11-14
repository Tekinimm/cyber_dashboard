import { F, BOLD, RESET, sparkline } from '../ui.js';

const COINS = [
  { id: 'bitcoin', sym: 'BTC' },
  { id: 'ethereum', sym: 'ETH' },
  { id: 'solana', sym: 'SOL' }
];

export function initCrypto(state) {
  state.crypto = { ok: false, err: 'NO DATA YET' };
  state.hist = {};
  for (const c of COINS) state.hist[c.sym] = [];
}

export async function fetchCrypto(state) {
  const ids = COINS.map((c) => c.id).join(',');
  const url = `https://api.coingecko.com/api/v3/simple/price?ids=${ids}&vs_currencies=usd&include_24hr_change=true`;
  const res = await fetch(url, { signal: AbortSignal.timeout(15000) });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  for (const c of COINS) {
    const p = data[c.id];
    if (!p) continue;
    state.crypto[c.sym] = { price: p.usd, change: p.usd_24h_change ?? 0 };
    state.hist[c.sym].push(p.usd);
    if (state.hist[c.sym].length > 60) state.hist[c.sym].shift();
  }
  state.crypto.ok = true;
  state.crypto.err = null;
}

function fmtPrice(p) {
  if (p >= 1000) return p.toLocaleString('en-US', { maximumFractionDigits: 0 });
  if (p >= 1) return p.toFixed(2);
  return p.toFixed(4);
}

export function cryptoLines(state, w) {
  const inner = w - 4;
  const c = state.crypto;
  if (!c || !c.ok) {
    return [
      `${F.red}▒ OFFLINE // ${c?.err ?? 'NO SIGNAL'} ▒${RESET}`,
      `${F.gray}${'░'.repeat(Math.max(0, inner - 4))}${RESET}`
    ];
  }
  const sparkW = Math.max(10, inner - 26);
  return COINS.map((coin) => {
    const d = c[coin.sym];
    if (!d) return `${F.gray}${coin.sym.padEnd(5)} ...${RESET}`;
    const up = d.change >= 0;
    const col = up ? F.green : F.red;
    const arrow = up ? '▲' : '▼';
    return (
      `${F.yellow}${BOLD}${coin.sym}${RESET} ` +
      `${F.white}$${fmtPrice(d.price).padStart(8)}${RESET} ` +
      `${col}${arrow}${Math.abs(d.change).toFixed(1).padStart(5)}%${RESET} ` +
      `${F.magenta}${sparkline(state.hist[coin.sym], sparkW)}${RESET}`
    );
  });
}
