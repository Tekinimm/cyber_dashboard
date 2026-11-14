import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CyberDashboard {

    private static final String RESET = "\u001b[0m";
    private static final String BOLD = "\u001b[1m";
    private static final String MAG = "\u001b[35m";
    private static final String CYN = "\u001b[36m";
    private static final String GRN = "\u001b[32m";
    private static final String YLW = "\u001b[33m";
    private static final String RED = "\u001b[31m";
    private static final String BLU = "\u001b[34m";
    private static final String WHT = "\u001b[97m";
    private static final String GRY = "\u001b[90m";

    private static final String ANSI_RE = "\u001b\\[[0-9;]*m";
    private static final String[] SPARK = {"\u2581", "\u2582", "\u2583", "\u2584", "\u2585", "\u2586", "\u2587", "\u2588"};
    private static final String[] GLITCH_CHARS = {"\u2593", "\u2592", "\u2591", "/", "\\", "<", ">"};

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) throws Exception {
        boolean once = false;
        for (String a : args) if (a.equals("--once")) once = true;

        SystemPanel system = new SystemPanel();
        CryptoPanel crypto = new CryptoPanel();
        WeatherPanel weather = new WeatherPanel();
        GithubPanel github = new GithubPanel();
        List<Panel> panels = List.of(system, crypto, weather, github);

        if (once) {
            system.tick();
            for (Panel p : panels) if (p != system) safeFetch(p);
            System.out.println(render(panels, 42));
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print("\u001b[?25h\u001b[?1049l");
        }));
        Thread fetcher = new Thread(() -> {
            while (true) {
                for (Panel p : panels) if (p != system) safeFetch(p);
                try { Thread.sleep(45000); } catch (InterruptedException e) { return; }
            }
        });
        fetcher.setDaemon(true);
        fetcher.start();

        System.out.print("\u001b[?1049h\u001b[2J\u001b[?25l");
        long tick = 0;
        while (true) {
            tick++;
            system.tick();
            System.out.print("\u001b[H" + render(panels, (int) (tick % 1000)));
            Thread.sleep(1000);
        }
    }

    private interface Panel {
        List<String> lines(int width);
    }

    private static void safeFetch(Panel p) {
        try {
            if (p instanceof CryptoPanel) ((CryptoPanel) p).fetch();
            else if (p instanceof WeatherPanel) ((WeatherPanel) p).fetch();
            else if (p instanceof GithubPanel) ((GithubPanel) p).fetch();
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());
            if (msg.length() > 24) msg = msg.substring(0, 24);
            if (p instanceof CryptoPanel) ((CryptoPanel) p).err = msg;
            else if (p instanceof WeatherPanel) ((WeatherPanel) p).err = msg;
            else if (p instanceof GithubPanel) ((GithubPanel) p).err = msg;
        }
    }

    private static String get(String url, String accept, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "cyber-dashboard")
                .GET();
        if (accept != null) b.header("Accept", accept);
        if (token != null && !token.isEmpty()) b.header("Authorization", "Bearer " + token);
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new RuntimeException("HTTP " + r.statusCode());
        return r.body();
    }

    private static List<String> offline(String err, int inner) {
        List<String> out = new ArrayList<>();
        out.add(RED + "\u2592 OFFLINE // " + (err == null ? "NO SIGNAL" : err) + " \u2592" + RESET);
        int fill = Math.max(0, inner - 4);
        out.add(GRY + repeat('\u2591', fill) + RESET);
        return out;
    }

    static class SystemPanel implements Panel {
        double mem;
        double load;
        int cpus = Runtime.getRuntime().availableProcessors();
        final List<Double> hist = new ArrayList<>();
        long start = System.currentTimeMillis();

        void tick() {
            Runtime rt = Runtime.getRuntime();
            mem = 1.0 - (double) rt.freeMemory() / rt.maxMemory();
            double la = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            load = la < 0 ? 0 : Math.min(1.0, la / cpus);
            hist.add(load);
            if (hist.size() > 60) hist.remove(0);
        }

        public List<String> lines(int w) {
            int inner = w - 4;
            List<String> out = new ArrayList<>();
            out.add(GRY + "HOST" + RESET + "      " + WHT + BOLD + clip(hostname(), 18) + RESET);
            out.add(GRY + "OS" + RESET + "        " + CYN + System.getProperty("os.name") + " " + System.getProperty("os.version") + RESET);
            out.add(GRY + "CPU" + RESET + "       " + WHT + String.format("%3d", (int) (load * 100)) + "%" + RESET
                    + " " + bar(load, inner - 16) + " " + GRY + "x" + cpus + RESET);
            out.add(GRY + "MEM" + RESET + "       " + WHT + String.format("%3d", (int) (mem * 100)) + "%" + RESET
                    + " " + bar(mem, inner - 16) + " " + GRY + String.format("%.1fG", Runtime.getRuntime().maxMemory() / 1073741824.0) + RESET);
            out.add(GRY + "UPTIME" + RESET + "    " + GRN + fmtUptime((System.currentTimeMillis() - start) / 1000) + RESET);
            out.add(GRY + "LOAD 60s" + RESET + "  " + MAG + sparkline(hist, Math.max(10, inner - 9)) + RESET);
            return out;
        }

        private String hostname() {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                return "localhost";
            }
        }

        private String fmtUptime(long sec) {
            return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
        }
    }

    static class CryptoPanel implements Panel {
        String err = "NO DATA YET";
        final Map<String, Double> price = new LinkedHashMap<>();
        final Map<String, Double> change = new LinkedHashMap<>();
        final Map<String, List<Double>> hist = new LinkedHashMap<>();
        final String[][] coins = {
            {"bitcoin", "BTC"}, {"ethereum", "ETH"}, {"solana", "SOL"}
        };

        CryptoPanel() {
            for (String[] c : coins) hist.put(c[1], new ArrayList<>());
        }

        void fetch() throws Exception {
            StringBuilder ids = new StringBuilder();
            for (int i = 0; i < coins.length; i++) {
                if (i > 0) ids.append(',');
                ids.append(coins[i][0]);
            }
            String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + ids
                    + "&vs_currencies=usd&include_24hr_change=true";
            Map<String, Object> data = Json.obj(Json.parse(get(url, null, null)));
            for (String[] c : coins) {
                Map<String, Object> p = Json.obj(data.get(c[0]));
                if (p == null) continue;
                double pr = Json.num(p.get("usd"));
                double ch = Json.num(p.get("usd_24h_change"));
                price.put(c[1], pr);
                change.put(c[1], ch);
                List<Double> h = hist.get(c[1]);
                h.add(pr);
                if (h.size() > 60) h.remove(0);
            }
            err = null;
        }

        public List<String> lines(int w) {
            int inner = w - 4;
            if (err != null) return offline(err, inner);
            List<String> out = new ArrayList<>();
            int sparkW = Math.max(10, inner - 26);
            for (String[] c : coins) {
                String sym = c[1];
                Double pr = price.get(sym);
                if (pr == null) {
                    out.add(GRY + sym + "     ..." + RESET);
                    continue;
                }
                double ch = change.getOrDefault(sym, 0.0);
                boolean up = ch >= 0;
                String col = up ? GRN : RED;
                String arrow = up ? "\u25b2" : "\u25bc";
                out.add(YLW + BOLD + sym + RESET + " "
                        + WHT + "$" + String.format("%8s", fmtPrice(pr)) + RESET + " "
                        + col + arrow + String.format("%5.1f", Math.abs(ch)) + "%" + RESET + " "
                        + MAG + sparkline(hist.get(sym), sparkW) + RESET);
            }
            return out;
        }

        private String fmtPrice(double p) {
            if (p >= 1000) return String.format("%,.0f", p);
            if (p >= 1) return String.format("%.2f", p);
            return String.format("%.4f", p);
        }
    }

    static class WeatherPanel implements Panel {
        String err = "NO DATA YET";
        final String lat = env("CYBER_LAT", "41.01");
        final String lon = env("CYBER_LON", "28.98");
        final String city = env("CYBER_CITY", "ISTANBUL");
        double temp, feels, wind, max, min;
        String cond = "";

        static final Map<Long, String> WMO = Map.ofEntries(
                Map.entry(0L, "CLEAR SKY"), Map.entry(1L, "MOSTLY CLEAR"),
                Map.entry(2L, "PARTLY CLOUDY"), Map.entry(3L, "OVERCAST"),
                Map.entry(45L, "FOG"), Map.entry(48L, "RIME FOG"),
                Map.entry(51L, "LIGHT DRIZZLE"), Map.entry(53L, "DRIZZLE"), Map.entry(55L, "HEAVY DRIZZLE"),
                Map.entry(61L, "LIGHT RAIN"), Map.entry(63L, "RAIN"), Map.entry(65L, "HEAVY RAIN"),
                Map.entry(71L, "LIGHT SNOW"), Map.entry(73L, "SNOW"), Map.entry(75L, "HEAVY SNOW"), Map.entry(77L, "SNOW GRAINS"),
                Map.entry(80L, "SHOWERS"), Map.entry(81L, "SHOWERS"), Map.entry(82L, "VIOLENT SHOWERS"),
                Map.entry(95L, "THUNDERSTORM"), Map.entry(96L, "THUNDERSTORM + HAIL"), Map.entry(99L, "THUNDERSTORM + HAIL"));

        void fetch() throws Exception {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,apparent_temperature,wind_speed_10m,weather_code"
                    + "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1";
            Map<String, Object> data = Json.obj(Json.parse(get(url, null, null)));
            Map<String, Object> cur = Json.obj(data.get("current"));
            Map<String, Object> day = Json.obj(data.get("daily"));
            temp = Json.num(cur.get("temperature_2m"));
            feels = Json.num(cur.get("apparent_temperature"));
            wind = Json.num(cur.get("wind_speed_10m"));
            long code = Math.round(Json.num(cur.get("weather_code")));
            cond = WMO.getOrDefault(code, "UNKNOWN");
            List<Object> mx = Json.arr(day.get("temperature_2m_max"));
            List<Object> mn = Json.arr(day.get("temperature_2m_min"));
            max = mx.isEmpty() ? 0 : Json.num(mx.get(0));
            min = mn.isEmpty() ? 0 : Json.num(mn.get(0));
            err = null;
        }

        public List<String> lines(int w) {
            int inner = w - 4;
            if (err != null) return offline(err, inner);
            List<String> out = new ArrayList<>();
            out.add(GRY + "SECTOR" + RESET + "    " + MAG + BOLD + city + RESET + " " + GRY + "[" + lat + "," + lon + "]" + RESET);
            out.add(GRY + "STATE" + RESET + "     " + WHT + BOLD + cond + RESET);
            out.add(GRY + "TEMP" + RESET + "      " + tempColor(temp) + BOLD + String.format("%.1f°C", temp) + RESET
                    + " " + GRY + "(feels " + String.format("%.1f", feels) + ")" + RESET);
            out.add(GRY + "RANGE" + RESET + "     " + RED + "\u25b2" + String.format("%.0f°", max) + RESET
                    + " / " + CYN + "\u25bc" + String.format("%.0f°", min) + RESET);
            out.add(GRY + "WIND" + RESET + "      " + CYN + String.format("%.1f km/h", wind) + RESET);
            return out;
        }

        private String tempColor(double t) {
            if (t <= 0) return CYN;
            if (t < 15) return BLU;
            if (t < 25) return GRN;
            if (t < 33) return YLW;
            return RED;
        }
    }

    static class GithubPanel implements Panel {
        String err = "NO DATA YET";
        final String user = env("CYBER_GH_USER", "Tekinimm");
        String name = "";
        int repos, followers, stars;
        final List<String[]> recent = new ArrayList<>();

        void fetch() throws Exception {
            String token = env("CYBER_GH_TOKEN", "");
            Map<String, Object> u = Json.obj(Json.parse(get(
                    "https://api.github.com/users/" + user, "application/vnd.github+json", token)));
            List<Object> rs = Json.arr(Json.parse(get(
                    "https://api.github.com/users/" + user + "/repos?per_page=100&sort=pushed",
                    "application/vnd.github+json", token)));
            name = Json.str(u.get("login"));
            repos = (int) Json.num(u.get("public_repos"));
            followers = (int) Json.num(u.get("followers"));
            stars = 0;
            recent.clear();
            for (Object o : rs) {
                Map<String, Object> r = Json.obj(o);
                stars += (int) Json.num(r.get("stargazers_count"));
                boolean fork = Boolean.TRUE.equals(r.get("fork"));
                if (!fork && recent.size() < 3) {
                    recent.add(new String[]{Json.str(r.get("name")), Json.str(r.get("pushed_at"))});
                }
            }
            err = null;
        }

        public List<String> lines(int w) {
            int inner = w - 4;
            if (err != null) return offline(err, inner);
            List<String> out = new ArrayList<>();
            out.add(GRY + "OPERATOR" + RESET + "  " + MAG + BOLD + "@" + name + RESET);
            out.add(GRY + "REPOS" + RESET + "     " + WHT + repos + RESET + "   " + GRY + "FOLLOWERS" + RESET
                    + " " + WHT + followers + RESET + "   " + GRY + "STARS" + RESET + " " + YLW + "\u2605" + stars + RESET);
            out.add(GRY + "LAST PUSH:" + RESET);
            for (String[] r : recent) {
                String nm = r[0].length() > 22 ? r[0].substring(0, 22) : r[0];
                out.add("  " + CYN + nm + RESET + " " + GRY + String.format("%4s", ago(r[1])) + " ago" + RESET);
            }
            return out;
        }

        private String ago(String iso) {
            long h = (System.currentTimeMillis() - java.time.Instant.parse(iso).toEpochMilli()) / 3600000;
            return h < 24 ? h + "h" : (h / 24) + "d";
        }
    }

    private static String render(List<Panel> panels, int tick) {
        int W = Integer.parseInt(env("CYBER_WIDTH", "100"));
        StringBuilder sb = new StringBuilder();
        String[] banner = bigText("CYBER//DASH");
        for (String row : banner) {
            sb.append(center(tick % 13 == 0 ? glitchify(row, 0.06) : row, W)).append("\n");
        }
        sb.append("\n");
        sb.append(clockLine(W)).append("\n\n");

        int colW = (W - 1) / 2;
        List<String> left = new ArrayList<>();
        List<String> right = new ArrayList<>();
        left.addAll(box("SYSTEM", panels.get(0).lines(colW), colW, MAG));
        left.add("");
        left.addAll(box("WEATHER", panels.get(2).lines(colW), colW, BLU));
        right.addAll(box("CRYPTO", panels.get(1).lines(colW), colW, GRN));
        right.add("");
        right.addAll(box("GITHUB", panels.get(3).lines(colW), colW, YLW));

        int rows = Math.max(left.size(), right.size());
        for (int i = 0; i < rows; i++) {
            String l = i < left.size() ? left.get(i) : "";
            String r = i < right.size() ? right.get(i) : "";
            sb.append(padEnd(l, colW)).append(' ').append(r).append("\n");
        }
        sb.append("\n");
        sb.append(footerLine(W, tick));
        return sb.toString();
    }

    private static String clockLine(int W) {
        String time = LocalDateTime.now().format(TIME_FMT);
        String date = LocalDate.now().toString();
        String blink = (System.currentTimeMillis() / 1000 % 2 == 0) ? "\u2593" : "\u2592";
        String left = GRY + "//" + RESET + " " + GRN + BOLD + time + RESET + " " + GRY + blink + " SYS.CLOCK" + RESET;
        String right = GRY + "DATE " + RESET + CYN + date + RESET;
        return padEnd(left, W - visibleLen(right) - 1) + right;
    }

    private static String footerLine(int W, int tick) {
        int scanW = 18;
        int pos = tick % scanW;
        StringBuilder scan = new StringBuilder();
        for (int i = 0; i < scanW; i++) {
            if (i == pos) scan.append(MAG).append('\u2588').append(RESET);
            else if (i == (pos + 1) % scanW) scan.append(MAG).append('\u2593').append(RESET);
            else scan.append(GRY).append('\u2591').append(RESET);
        }
        String left = GRN + " UPLINK STABLE " + RESET + " " + scan;
        String right = GRY + "[" + RESET + YLW + "CTRL+C" + RESET + GRY + "] QUIT" + RESET;
        return padEnd(left, W - visibleLen(right) - 1) + right;
    }

    static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }

    static int visibleLen(String s) {
        return s.replaceAll(ANSI_RE, "").length();
    }

    static String clip(String s, int n) {
        StringBuilder out = new StringBuilder();
        int vis = 0;
        int i = 0;
        while (i < s.length() && vis < n) {
            char c = s.charAt(i);
            if (c == '\u001b' && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int j = s.indexOf('m', i);
                if (j > 0) {
                    out.append(s, i, j + 1);
                    i = j + 1;
                    continue;
                }
            }
            out.append(c);
            vis++;
            i++;
        }
        return out.toString();
    }

    static String padEnd(String s, int n) {
        int v = visibleLen(s);
        if (v >= n) return clip(s, n);
        return s + repeat(' ', n - v);
    }

    static String center(String s, int n) {
        int v = visibleLen(s);
        if (v >= n) return clip(s, n);
        int left = (n - v) / 2;
        return repeat(' ', left) + s + repeat(' ', n - v - left);
    }

    static String repeat(char c, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    static List<String> box(String title, List<String> lines, int w, String color) {
        int inner = Math.max(10, w - 2);
        String t = " " + title + " ";
        List<String> out = new ArrayList<>();
        out.add(color + "\u2554" + RESET + CYN + BOLD + clip(t, inner) + RESET + color
                + repeat('\u2550', Math.max(0, inner - visibleLen(t))) + "\u2557" + RESET);
        for (String l : lines) {
            String cl = clip(l, inner);
            out.add(color + "\u2551" + RESET + cl + repeat(' ', Math.max(0, inner - visibleLen(cl))) + color + "\u2551" + RESET);
        }
        out.add(color + "\u255a" + repeat('\u2550', inner) + "\u255d" + RESET);
        return out;
    }

    static String sparkline(List<Double> values, int width) {
        if (values == null || values.isEmpty()) return repeat('\u2591', width);
        int start = Math.max(0, values.size() - width);
        List<Double> slice = values.subList(start, values.size());
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : slice) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double span = (max - min) == 0 ? 1 : max - min;
        StringBuilder sb = new StringBuilder();
        for (double v : slice) {
            int idx = (int) Math.min(7, Math.floor(((v - min) / span) * 8));
            sb.append(SPARK[idx]);
        }
        for (int i = slice.size(); i < width; i++) sb.append('\u2591');
        return sb.toString();
    }

    static String bar(double pct, int width) {
        double p = Math.max(0, Math.min(1, pct));
        int filled = (int) Math.round(p * width);
        return GRN + repeat('\u2588', filled) + RESET + GRY + repeat('\u2591', width - filled) + RESET;
    }

    static String glitchify(String line, double prob) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\u001b' && i + 1 < line.length() && line.charAt(i + 1) == '[') {
                int j = line.indexOf('m', i);
                if (j > 0) {
                    out.append(line, i, j + 1);
                    i = j + 1;
                    continue;
                }
            }
            if (c != ' ' && ThreadLocalRandom.current().nextDouble() < prob) {
                out.append(RED).append(GLITCH_CHARS[ThreadLocalRandom.current().nextInt(GLITCH_CHARS.length)]).append(RESET);
            } else {
                out.append(c);
            }
            i++;
        }
        return out.toString();
    }

    private static final Map<Character, String[]> FONT = new LinkedHashMap<>();

    static {
        FONT.put('C', new String[]{"\u2588\u2588\u2588", "\u2588  ", "\u2588  ", "\u2588  ", "\u2588\u2588\u2588"});
        FONT.put('Y', new String[]{"\u2588 \u2588", " \u2588 ", " \u2588 ", " \u2588 ", " \u2588 "});
        FONT.put('B', new String[]{"\u2588\u2588\u2588 ", "\u2588  \u2588", "\u2588\u2588\u2588 ", "\u2588  \u2588", "\u2588\u2588\u2588 "});
        FONT.put('E', new String[]{"\u2588\u2588\u2588\u2588", "\u2588   ", "\u2588\u2588\u2588 ", "\u2588   ", "\u2588\u2588\u2588\u2588"});
        FONT.put('R', new String[]{"\u2588\u2588\u2588 ", "\u2588  \u2588", "\u2588\u2588\u2588 ", "\u2588 \u2588 ", "\u2588  \u2588"});
        FONT.put('D', new String[]{"\u2588\u2588\u2588 ", "\u2588  \u2588", "\u2588  \u2588", "\u2588  \u2588", "\u2588\u2588\u2588 "});
        FONT.put('A', new String[]{" \u2588\u2588 ", "\u2588  \u2588", "\u2588\u2588\u2588\u2588", "\u2588  \u2588", "\u2588  \u2588"});
        FONT.put('S', new String[]{" \u2588\u2588\u2588", "\u2588   ", "\u2588\u2588\u2588 ", "   \u2588", "\u2588\u2588\u2588 "});
        FONT.put('H', new String[]{"\u2588  \u2588", "\u2588  \u2588", "\u2588\u2588\u2588\u2588", "\u2588  \u2588", "\u2588  \u2588"});
        FONT.put('/', new String[]{"  \u2588 ", " \u2588  ", "\u2588   ", " \u2588  ", "  \u2588 "});
        FONT.put('_', new String[]{"   ", "   ", "   ", "   ", "\u2588\u2588\u2588"});
        FONT.put(' ', new String[]{"  ", "  ", "  ", "  ", "  "});
    }

    static String[] bigText(String text) {
        String[] rows = {"", "", "", "", ""};
        for (char ch : text.toUpperCase().toCharArray()) {
            String[] glyph = FONT.getOrDefault(ch, FONT.get(' '));
            String col = (ch == '/' || ch == '_') ? YLW : MAG;
            for (int r = 0; r < 5; r++) rows[r] += col + glyph[r] + RESET + " ";
        }
        return rows;
    }

    static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        static Object parse(String text) {
            Json p = new Json(text);
            p.ws();
            return p.value();
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> obj(Object o) {
            return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
        }

        static List<Object> arr(Object o) {
            return o instanceof List ? (List<Object>) o : new ArrayList<>();
        }

        static double num(Object o) {
            return o instanceof Double ? (Double) o : 0;
        }

        static String str(Object o) {
            return o == null ? "" : o.toString();
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private Object value() {
            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (c == 't') { i += 4; return Boolean.TRUE; }
            if (c == 'f') { i += 5; return Boolean.FALSE; }
            if (c == 'n') { i += 4; return null; }
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                String key = string();
                ws();
                i++;
                ws();
                m.put(key, value());
                ws();
                if (s.charAt(i) == ',') { i++; ws(); continue; }
                i++;
                return m;
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(value());
                ws();
                if (s.charAt(i) == ',') { i++; ws(); continue; }
                i++;
                return l;
            }
        }

        private String string() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (s.charAt(i) != '"') {
                char c = s.charAt(i);
                if (c == '\\') {
                    char e = s.charAt(i + 1);
                    if (e == 'u') {
                        sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                        i += 6;
                    } else {
                        sb.append(e == 'n' ? '\n' : e == 't' ? '\t' : e);
                        i += 2;
                    }
                } else {
                    sb.append(c);
                    i++;
                }
            }
            i++;
            return sb.toString();
        }

        private Double number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(start, i));
        }
    }
}
