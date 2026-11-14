import { F, BOLD, DIM, RESET, box, bigText, glitchify, padEndVisible, padStartVisible, centerVisible } from './ui.js';
import { initSystem, tickSystem, systemLines } from './panels/system.js';
import { initCrypto, fetchCrypto, cryptoLines } from './panels/crypto.js';
import { initWeather, fetchWeather, weatherLines } from './panels/weather.js';
import { initGithub, fetchGithub, githubLines } from './panels/github.js';

const MIN_W = 100;
const MIN_H = 36;

const state = {};
initSystem(state);
initCrypto(state);
initWeather(state);
initGithub(state);

const once = process.argv.includes('--once');

function clockLine(W, tick) {
  const now = new Date();
  const time = now.toLocaleTimeString('en-GB');
  const date = now.toISOString().slice(0, 10);
  const blink = tick % 2 === 0 ? '▓' : '░';
  const left = `${F.gray}//${RESET} ${F.green}${BOLD}${time}${RESET} ${F.gray}${blink} SYS.CLOCK${RESET}`;
  const right = `${F.gray}DATE ${RESET}${F.cyan}${date}${RESET}`;
  return padEndVisible(left, W - visibleLenSafe(right) - 1) + right;
}

function visibleLenSafe(s) {
  return s.replace(/\x1b\[[0-9;]*m/g, '').length;
}

function footerLine(W, tick) {
  const scanW = 18;
  const pos = tick % scanW;
  let scan = '';
  for (let i = 0; i < scanW; i++) {
    scan += i === pos ? F.magenta + '█' + RESET : i === (pos + 1) % scanW ? F.magenta + '▓' + RESET : F.gray + '░' + RESET;
  }
  const left = `${F.green} UPLINK STABLE ${RESET} ${scan}`;
  const right = `${F.gray}[${RESET}${F.yellow}q${RESET}${F.gray}] QUIT${RESET}`;
  return padEndVisible(left, W - visibleLenSafe(right) - 1) + right;
}

function renderFrame(W, tick) {
  const lines = [];
  const banner = bigText('CYBER//DASH', F.magenta, F.yellow);
  for (const row of banner) {
    lines.push(centerVisible(tick % 13 === 0 ? glitchify(row, 0.06) : row, W));
  }
  lines.push('');
  lines.push(clockLine(W, tick));
  lines.push('');

  const colW = Math.floor((W - 1) / 2);
  const left = [
    ...box('SYSTEM', systemLines(state, colW), colW, F.magenta),
    '',
    ...box('WEATHER', weatherLines(state, colW), colW, F.blue)
  ];
  const right = [
    ...box('CRYPTO', cryptoLines(state, colW), colW, F.green),
    '',
    ...box('GITHUB', githubLines(state, colW), colW, F.yellow)
  ];
  const rows = Math.max(left.length, right.length);
  for (let i = 0; i < rows; i++) {
    const l = left[i] ?? '';
    const r = right[i] ?? '';
    lines.push(padEndVisible(l, colW) + ' ' + r);
  }
  lines.push('');
  lines.push(footerLine(W, tick));
  return lines;
}

async function refreshRemote() {
  const results = await Promise.allSettled([fetchCrypto(state), fetchWeather(state), fetchGithub(state)]);
  const [crypto, weather, github] = results;
  if (crypto.status === 'rejected') {
    state.crypto.ok = false;
    state.crypto.err = String(crypto.reason?.message ?? crypto.reason).slice(0, 24);
  }
  if (weather.status === 'rejected') {
    state.weather.ok = false;
    state.weather.err = String(weather.reason?.message ?? weather.reason).slice(0, 24);
  }
  if (github.status === 'rejected') {
    state.github.ok = false;
    state.github.err = String(github.reason?.message ?? github.reason).slice(0, 24);
  }
}

function termSize() {
  const cols = process.stdout.columns || MIN_W;
  const rows = process.stdout.rows || MIN_H;
  return { W: Math.max(MIN_W, cols), H: Math.max(MIN_H, rows) };
}

function writeFrame(tick) {
  const { W } = termSize();
  const frame = renderFrame(W, tick);
  process.stdout.write('\x1b[H' + frame.join('\n'));
}

async function main() {
  if (once) {
    tickSystem(state);
    await refreshRemote();
    const { W } = { W: MIN_W };
    console.log(renderFrame(W, 42).join('\n'));
    return;
  }

  process.stdout.write('\x1b[?1049h\x1b[2J\x1b[?25l');
  process.stdin.setRawMode?.(true);
  process.stdin.resume();
  process.stdin.on('data', (d) => {
    if (d.toString() === 'q' || d[0] === 3 || d[0] === 27) {
      process.stdout.write('\x1b[?25h\x1b[?1049l');
      process.exit(0);
    }
  });
  process.on('SIGINT', () => {
    process.stdout.write('\x1b[?25h\x1b[?1049l');
    process.exit(0);
  });

  let tick = 0;
  let remoteBusy = true;
  refreshRemote().finally(() => {
    remoteBusy = false;
  });
  setInterval(() => {
    if (!remoteBusy) {
      remoteBusy = true;
      refreshRemote().finally(() => {
        remoteBusy = false;
      });
    }
  }, 45000);

  const loop = setInterval(() => {
    tick++;
    tickSystem(state);
    writeFrame(tick);
  }, 1000);

  process.stdout.on('resize', () => {
    process.stdout.write('\x1b[2J');
    writeFrame(tick);
  });

  loop.unref?.();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
