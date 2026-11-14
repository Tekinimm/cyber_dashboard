import { F, BOLD, RESET } from '../ui.js';

const WMO = {
  0: 'CLEAR SKY',
  1: 'MOSTLY CLEAR',
  2: 'PARTLY CLOUDY',
  3: 'OVERCAST',
  45: 'FOG',
  48: 'RIME FOG',
  51: 'LIGHT DRIZZLE',
  53: 'DRIZZLE',
  55: 'HEAVY DRIZZLE',
  61: 'LIGHT RAIN',
  63: 'RAIN',
  65: 'HEAVY RAIN',
  71: 'LIGHT SNOW',
  73: 'SNOW',
  75: 'HEAVY SNOW',
  77: 'SNOW GRAINS',
  80: 'SHOWERS',
  81: 'SHOWERS',
  82: 'VIOLENT SHOWERS',
  95: 'THUNDERSTORM',
  96: 'THUNDERSTORM + HAIL',
  99: 'THUNDERSTORM + HAIL'
};

export function initWeather(state) {
  state.weather = {
    ok: false,
    err: 'NO DATA YET',
    lat: process.env.CYBER_LAT || '41.01',
    lon: process.env.CYBER_LON || '28.98',
    city: process.env.CYBER_CITY || 'ISTANBUL'
  };
}

export async function fetchWeather(state) {
  const s = state.weather;
  const url =
    `https://api.open-meteo.com/v1/forecast?latitude=${s.lat}&longitude=${s.lon}` +
    `&current=temperature_2m,apparent_temperature,wind_speed_10m,weather_code` +
    `&daily=temperature_2m_max,temperature_2m_min&forecast_days=1`;
  const res = await fetch(url, { signal: AbortSignal.timeout(15000) });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  s.temp = data.current.temperature_2m;
  s.feels = data.current.apparent_temperature;
  s.wind = data.current.wind_speed_10m;
  s.code = data.current.weather_code;
  s.cond = WMO[s.code] ?? 'UNKNOWN';
  s.max = data.daily.temperature_2m_max[0];
  s.min = data.daily.temperature_2m_min[0];
  s.ok = true;
  s.err = null;
}

function tempColor(t) {
  if (t <= 0) return F.cyan;
  if (t < 15) return F.blue;
  if (t < 25) return F.green;
  if (t < 33) return F.yellow;
  return F.red;
}

export function weatherLines(state, w) {
  const inner = w - 4;
  const s = state.weather;
  if (!s || !s.ok) {
    return [
      `${F.red}▒ OFFLINE // ${s?.err ?? 'NO SIGNAL'} ▒${RESET}`,
      `${F.gray}${'░'.repeat(Math.max(0, inner - 4))}${RESET}`
    ];
  }
  return [
    `${F.gray}SECTOR${RESET}    ${F.magenta}${BOLD}${s.city}${RESET} ${F.gray}[${s.lat},${s.lon}]${RESET}`,
    `${F.gray}STATE${RESET}     ${F.white}${BOLD}${s.cond}${RESET}`,
    `${F.gray}TEMP${RESET}      ${tempColor(s.temp)}${BOLD}${s.temp.toFixed(1)}°C${RESET} ${F.gray}(feels ${s.feels.toFixed(1)})${RESET}`,
    `${F.gray}RANGE${RESET}     ${F.red}▲${s.max.toFixed(0)}°${RESET} / ${F.cyan}▼${s.min.toFixed(0)}°${RESET}`,
    `${F.gray}WIND${RESET}      ${F.cyan}${s.wind.toFixed(1)} km/h${RESET}`
  ];
}
