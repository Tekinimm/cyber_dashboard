export const RESET = '\x1b[0m';
export const BOLD = '\x1b[1m';
export const DIM = '\x1b[2m';
export const INVERT = '\x1b[7m';

export const F = {
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  red: '\x1b[31m',
  blue: '\x1b[34m',
  white: '\x1b[97m',
  gray: '\x1b[90m'
};

const ANSI_RE = /\x1b\[[0-9;]*m/g;

export function visibleLen(s) {
  return s.replace(ANSI_RE, '').length;
}

export function clip(s, n) {
  let out = '';
  let vis = 0;
  let i = 0;
  while (i < s.length && vis < n) {
    if (s[i] === '\x1b') {
      const m = /^\x1b\[[0-9;]*m/.exec(s.slice(i));
      if (m) {
        out += m[0];
        i += m[0].length;
        continue;
      }
    }
    out += s[i];
    vis++;
    i++;
  }
  return out;
}

export function padEndVisible(s, n) {
  const v = visibleLen(s);
  if (v >= n) return clip(s, n);
  return s + ' '.repeat(n - v);
}

export function padStartVisible(s, n) {
  const v = visibleLen(s);
  if (v >= n) return clip(s, n);
  return ' '.repeat(n - v) + s;
}

export function centerVisible(s, n) {
  const v = visibleLen(s);
  if (v >= n) return clip(s, n);
  const left = Math.floor((n - v) / 2);
  return ' '.repeat(left) + s + ' '.repeat(n - v - left);
}

export function box(title, lines, w, color) {
  const inner = Math.max(10, w - 2);
  const t = ` ${title} `;
  const top =
    color + '╔' + RESET +
    F.cyan + BOLD + clip(t, inner) + RESET +
    color + '═'.repeat(Math.max(0, inner - visibleLen(t))) + '╗' + RESET;
  const body = lines.map((l) => {
    const cl = clip(l, inner);
    return (
      color + '║' + RESET +
      cl + ' '.repeat(Math.max(0, inner - visibleLen(cl))) +
      color + '║' + RESET
    );
  });
  const bottom = color + '╚' + '═'.repeat(inner) + '╝' + RESET;
  return [top, ...body, bottom];
}

const SPARK_CHARS = ['▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'];

export function sparkline(values, width) {
  if (!values || values.length === 0) return '░'.repeat(width);
  const slice = values.slice(-width);
  const min = Math.min(...slice);
  const max = Math.max(...slice);
  const span = max - min || 1;
  let out = '';
  for (const v of slice) {
    const idx = Math.min(7, Math.floor(((v - min) / span) * 8));
    out += SPARK_CHARS[idx];
  }
  return out + '░'.repeat(Math.max(0, width - slice.length));
}

export function bar(pct, width, fillColor) {
  const p = Math.max(0, Math.min(1, pct));
  const filled = Math.round(p * width);
  return (
    (fillColor || F.green) + '█'.repeat(filled) + RESET +
    F.gray + '░'.repeat(width - filled) + RESET
  );
}

const GLITCH_CHARS = ['▓', '▒', '░', '/', '\\', '<', '>'];

export function glitchify(line, prob) {
  const chars = [...line.replace(ANSI_RE, '')];
  let out = '';
  for (const ch of chars) {
    if (ch !== ' ' && Math.random() < prob) {
      out += F.red + GLITCH_CHARS[Math.floor(Math.random() * GLITCH_CHARS.length)] + RESET;
    } else {
      out += ch;
    }
  }
  return out;
}

const FONT = {
  C: ['███', '█  ', '█  ', '█  ', '███'],
  Y: ['█ █', ' █ ', ' █ ', ' █ ', ' █ '],
  B: ['███ ', '█  █', '███ ', '█  █', '███ '],
  E: ['████', '█   ', '███ ', '█   ', '████'],
  R: ['███ ', '█  █', '███ ', '█ █ ', '█  █'],
  D: ['███ ', '█  █', '█  █', '█  █', '███ '],
  A: [' ██ ', '█  █', '████', '█  █', '█  █'],
  S: [' ███', '█   ', '███ ', '   █', '███ '],
  H: ['█  █', '█  █', '████', '█  █', '█  █'],
  '/': ['  █ ', ' █  ', '█   ', ' █  ', '  █ '],
  ' ': ['  ', '  ', '  ', '  ', '  '],
  '_': ['   ', '   ', '   ', '   ', '███']
};

export function bigText(str, letterColor, accentColor) {
  const rows = ['', '', '', '', ''];
  const accented = new Set(['/', '_']);
  for (const ch of str.toUpperCase()) {
    const glyph = FONT[ch] || FONT[' '];
    for (let r = 0; r < 5; r++) {
      rows[r] += (accented.has(ch) ? accentColor : letterColor) + glyph[r] + RESET + ' ';
    }
  }
  return rows;
}
