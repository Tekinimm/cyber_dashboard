import { F, BOLD, RESET } from '../ui.js';

export function initGithub(state) {
  state.github = {
    ok: false,
    err: 'NO DATA YET',
    user: process.env.CYBER_GH_USER || 'Tekinimm'
  };
}

export async function fetchGithub(state) {
  const g = state.github;
  const headers = { Accept: 'application/vnd.github+json', 'User-Agent': 'cyber-dashboard' };
  const token = process.env.CYBER_GH_TOKEN;
  if (token) headers.Authorization = `Bearer ${token}`;
  const [uRes, rRes] = await Promise.all([
    fetch(`https://api.github.com/users/${g.user}`, {
      headers,
      signal: AbortSignal.timeout(15000)
    }),
    fetch(`https://api.github.com/users/${g.user}/repos?per_page=100&sort=pushed`, {
      headers,
      signal: AbortSignal.timeout(15000)
    })
  ]);
  if (!uRes.ok) throw new Error(`USER HTTP ${uRes.status}`);
  if (!rRes.ok) throw new Error(`REPOS HTTP ${rRes.status}`);
  const u = await uRes.json();
  const repos = await rRes.json();
  g.name = u.login;
  g.repos = u.public_repos;
  g.followers = u.followers;
  g.stars = repos.reduce((a, r) => a + (r.stargazers_count || 0), 0);
  g.recent = repos
    .filter((r) => !r.fork)
    .slice(0, 3)
    .map((r) => ({ name: r.name, pushed: r.pushed_at }));
  g.ok = true;
  g.err = null;
}

function ago(iso) {
  const d = Date.now() - new Date(iso).getTime();
  const h = Math.floor(d / 3600000);
  if (h < 24) return `${h}h`;
  return `${Math.floor(h / 24)}d`;
}

export function githubLines(state, w) {
  const inner = w - 4;
  const g = state.github;
  if (!g || !g.ok) {
    return [
      `${F.red}▒ OFFLINE // ${g?.err ?? 'NO SIGNAL'} ▒${RESET}`,
      `${F.gray}${'░'.repeat(Math.max(0, inner - 4))}${RESET}`
    ];
  }
  const lines = [
    `${F.gray}OPERATOR${RESET}  ${F.magenta}${BOLD}@${g.name}${RESET}`,
    `${F.gray}REPOS${RESET}     ${F.white}${g.repos}${RESET}   ${F.gray}FOLLOWERS${RESET} ${F.white}${g.followers}${RESET}   ${F.gray}STARS${RESET} ${F.yellow}★${g.stars}${RESET}`,
    `${F.gray}LAST PUSH:${RESET}`
  ];
  for (const r of g.recent || []) {
    lines.push(`  ${F.cyan}${r.name.slice(0, 22)}${RESET} ${F.gray}${ago(r.pushed).padStart(4)} ago${RESET}`);
  }
  return lines;
}
