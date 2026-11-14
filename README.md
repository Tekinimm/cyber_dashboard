# CYBER//DASH 🌆

Neon yüklü, retro-fütürist terminal dashboard. Saf ANSI escape kodlarıyla yazıldı — **sıfır bağımlılık**, sadece Node.js.

```
                       ███ █ █ ███  ████ ███    █    █  ███   ██   ███ █  █
                       █   █  █ █   █    █  █  █    █   █  █ █  █ █  █  █
                       █   █  ███  ███  ███  █    █    █  █ ████ ███ ████
                       █   █  █ █   █    █ █   █    █   █  █ █  █ █  █  █
                       ███ █  ███  ████ █  █   █    █  ███  █  █ ███ █  █

// 13:20:37 ▓ SYS.CLOCK                                                     DATE 2026-11-14

╔ SYSTEM ═════════════════════════════════════╗ ╔ CRYPTO ════════════════════════════════════╗
║HOST      8604fea1-3da1-479b                ║ ║BTC $  66,708 ▼  3.5% ▁░░░░░░░░░░░░░░░░      ║
║OS        linux 6.18.35                     ║ ║ETH $   1,891 ▼  3.2% ▁░░░░░░░░░░░░░░░░      ║
║CPU         4% █░░░░░░░░░░░░░░░░░░░░░░░ x5  ║ ║SOL $   75.49 ▼  6.3% ▁░░░░░░░░░░░░░░░░      ║
║MEM         5% █░░░░░░░░░░░░░░░░░░░░░░ 5.0G ║ ╚═════════════════════════════════════════════╝
║UPTIME    00:24:01                          ║
║LOAD 60s  ▁░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ ║ ╔ GITHUB ════════════════════════════════════╗
╚════════════════════════════════════════════╝ ║OPERATOR  @Tekinimm                         ║
                                               ║REPOS     18   FOLLOWERS 1   STARS ★1       ║
╔ WEATHER ════════════════════════════════════╗ ║LAST PUSH:                                 ║
║SECTOR    ISTANBUL [41.01,28.98]             ║ ║  cyber_dashboard   0h ago                ║
║STATE     SHOWERS                            ║ ║  user-zenlight      19h ago               ║
║TEMP      14.2°C (feels 11.2)                ║ ║  ecommerce-backend  19h ago               ║
║RANGE     ▲21° / ▼11°                        ║ ╚═══════════════════════════════════════════╝
║WIND      25.5 km/h                          ║
╚════════════════════════════════════════════╝

 UPLINK STABLE  ░░░░░░█▓░░░░░░░░░░░░░░                                     [q] QUIT
```

## Özellikler

- **SYSTEM** — CPU yükü, RAM kullanımı, uptime, 60 saniyelik yük sparkline'ı
- **CRYPTO** — BTC / ETH / SOL canlı fiyatları (CoinGecko), 24s değişim, fiyat geçmişi sparkline'ı
- **WEATHER** — Open-Meteo üzerinden anlık sıcaklık, hissedilen, rüzgar, gün aralığı
- **GITHUB** — repo / follower / yıldız sayısı, son push'lanan repolar
- **Glitch efekti** — banner her ~13 saniyede bir bozulur, CRT hissi
- **Tarama animasyonu** — footer'da kayan neon tarayıcı
- **Dayanıklılık** — bir API düşerse panel `OFFLINE // NO SIGNAL` gösterir, dashboard kapanmaz

## Çalıştırma

```bash
node src/main.js
```

Tek kare render (test / ekran görüntüsü için):

```bash
node src/main.js --once
```

Çıkmak için `q` veya Ctrl+C.

## Yapılandırma (ortam değişkenleri)

| Değişken | Varsayılan | Açıklama |
|---|---|---|
| `CYBER_LAT` | `41.01` | Hava durumu enlem |
| `CYBER_LON` | `28.98` | Hava durumu boylam |
| `CYBER_CITY` | `ISTANBUL` | Panelde gösterilecek şehir adı |
| `CYBER_GH_USER` | `Tekinimm` | GitHub panelindeki kullanıcı |
| `CYBER_GH_TOKEN` | — | Opsiyonel GitHub token (rate limit yükseltir) |

Örnek:

```bash
CYBER_CITY="ANKARA" CYBER_LAT=39.93 CYBER_LON=32.86 node src/main.js
```

## Mimari

```
src/
├── main.js          giriş noktası, render döngüsü, layout
├── ui.js            ANSI yardımcıları: kutular, sparkline, bar, glitch, ASCII font
└── panels/
    ├── system.js    os modülü üzerinden sistem telemetrisi
    ├── crypto.js    CoinGecko API
    ├── weather.js   Open-Meteo API
    └── github.js    GitHub public API
```

Her veri kaynağı `src/panels/` altında izole bir modül; yeni panel eklemek tek dosya + `main.js`'te iki satır.

## Gereksinimler

- Node.js >= 18 (dahili `fetch` için)
- `npm install` **gerekmez** — bağımlılık yok
