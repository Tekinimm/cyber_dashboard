# CYBER//DASH 🌆

A neon-soaked, retro-futurist terminal dashboard, written in raw ANSI escape codes — **zero dependencies**. Two first-class implementations of the same dashboard:

- **Node.js** (`src/`) — requires Node.js >= 18, no `npm install` needed
- **Java** (`java/CyberDashboard.java`) — single-file source, requires JDK 11+

```
                       ███ █ █ ███  ████ ███    █    █  ███   ██   ███ █  █
                       █   █  █ █   █    █  █  █    █   █  █ █  █ █  █  █
                       █   █  ███  ███  ███  █    █    █  █ ████ ███ ████
                       █   █  █ █   █    █ █   █    █   █  █ █  █ █  █  █
                       ███ █  ███  ████ █  █   █    █  ███  █  █ ███ █  █

// 13:20:37 ▓ SYS.CLOCK                                                     DATE 2026-11-14

╔ SYSTEM ═══════════════════════════════════╗ ╔ CRYPTO ════════════════════════════════════╗
║HOST      8604fea1-3da1-479b               ║ ║BTC $  66,708 ▼  3.5% ▁░░░░░░░░░░░░░░░░      ║
║OS        linux 6.18.35                    ║ ║ETH $   1,891 ▼  3.2% ▁░░░░░░░░░░░░░░░░      ║
║CPU         4% █░░░░░░░░░░░░░░░░░░░░░░ x5  ║ ║SOL $   75.49 ▼  6.3% ▁░░░░░░░░░░░░░░░░      ║
║MEM         5% █░░░░░░░░░░░░░░░░░░░░░ 5.0G  ║ ╚═════════════════════════════════════════════╝
║UPTIME    00:24:01                         ║
║LOAD 60s  ▁░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ ║ ╔ GITHUB ════════════════════════════════════╗
╚═══════════════════════════════════════════╝ ║OPERATOR  @Tekinimm                         ║
                                              ║REPOS     18   FOLLOWERS 1   STARS ★1       ║
╔ WEATHER ══════════════════════════════════╗ ║LAST PUSH:                                 ║
║SECTOR    ISTANBUL [41.01,28.98]            ║ ║  cyber_dashboard   0h ago                ║
║STATE     SHOWERS                          ║ ║  user-zenlight      19h ago               ║
║TEMP      14.2°C (feels 11.2)              ║ ║  ecommerce-backend  19h ago               ║
║RANGE     ▲21° / ▼11°                      ║ ╚═══════════════════════════════════════════╝
║WIND      25.5 km/h                        ║
╚═══════════════════════════════════════════╝

 UPLINK STABLE  ░░░░░░█▓░░░░░░░░░░░░░░                                     [q] QUIT
```

## Features

- **SYSTEM** — CPU load, RAM usage, uptime, 60-second load sparkline
- **CRYPTO** — live BTC / ETH / SOL prices (CoinGecko), 24h change, price-history sparkline
- **WEATHER** — current temperature, apparent temperature, wind and daily range via Open-Meteo
- **GITHUB** — repo / follower / star counts plus most recently pushed repos
- **Glitch effect** — the banner corrupts itself every ~13 seconds for that CRT feel
- **Scanline animation** — a neon scanner sweeping across the footer
- **Resilience** — if an API goes down its panel shows `OFFLINE // NO SIGNAL`; the dashboard never crashes

## Running

### Node.js

```bash
node src/main.js
```

Single-frame render (for tests / screenshots):

```bash
node src/main.js --once
```

Quit with `q` or Ctrl+C.

### Java

```bash
java java/CyberDashboard.java
```

Or compile first for faster startup:

```bash
javac -d out java/CyberDashboard.java
java -cp out CyberDashboard
```

Single-frame render:

```bash
java java/CyberDashboard.java --once
```

Quit with Ctrl+C. Terminal width can be forced with `CYBER_WIDTH` (default `100`).

## Configuration (environment variables)

| Variable | Default | Description |
|---|---|---|
| `CYBER_LAT` | `41.01` | Weather latitude |
| `CYBER_LON` | `28.98` | Weather longitude |
| `CYBER_CITY` | `ISTANBUL` | City name shown in the panel |
| `CYBER_GH_USER` | `Tekinimm` | GitHub user shown in the panel |
| `CYBER_GH_TOKEN` | — | Optional GitHub token (raises rate limits) |
| `CYBER_WIDTH` | `100` | Java version only: render width |

Example:

```bash
CYBER_CITY="ANKARA" CYBER_LAT=39.93 CYBER_LON=32.86 node src/main.js
```

## Architecture

### Node.js (`src/`)

```
src/
├── main.js          entry point, render loop, layout
├── ui.js            ANSI helpers: boxes, sparklines, bars, glitch, ASCII font
└── panels/
    ├── system.js    system telemetry via the os module
    ├── crypto.js    CoinGecko API
    ├── weather.js   Open-Meteo API
    └── github.js    GitHub public API
```

### Java (`java/`)

```
java/
└── CyberDashboard.java   single-file dashboard: built-in mini JSON parser,
                          java.net.http.HttpClient, same panels as the Node version
```

Each data source is an isolated panel module; adding a new panel is one file (Node) or one nested class (Java) plus two lines in the layout.

## Requirements

- Node.js >= 18 (built-in `fetch`), **or** JDK >= 11
- No `npm install` / no dependencies in either implementation
