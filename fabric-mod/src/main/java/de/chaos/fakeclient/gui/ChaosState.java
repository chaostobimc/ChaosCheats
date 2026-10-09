package de.chaos.fakeclient.gui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gemeinsamer Zustand für Menü und HUD. Rein kosmetisch.
 */
public final class ChaosState {

	public static final int S_TOASTS = 0;
	public static final int S_RAINBOW = 1;
	public static final int S_CONSOLE = 2;
	public static final int S_PARTICLES = 3;
	public static final int S_ANTISS = 4;
	public static final int S_HUD = 5;

	public static final String[] SETTING_LABELS = {
		"Toast-Benachrichtigungen", "Rainbow-Akzent", "Konsole anzeigen",
		"Hintergrund-Effekte", "Anti-Screenshot-Schutz", "Modulliste im HUD"
	};
	public static final String[] SETTING_SUBS = {
		"Kleine Einblendungen bei Änderungen",
		"Animiert den Akzent durch den Farbkreis",
		"Zeigt die Konsole unten im Fenster",
		"Animierte Sterne und Glanz im Hintergrund",
		"Versteckt das Fenster bei Screenshots",
		"Zeigt aktive Module oben rechts im Spiel"
	};
	public static final boolean[] settings = {true, false, true, true, false, true};

	/** Akzent-Presets: [Start, Ende] */
	public static final int[][] ACCENTS = {
		{0xFFA855F7, 0xFF22D3EE},
		{0xFF22D3EE, 0xFF34D399},
		{0xFFF43F5E, 0xFFF97316},
		{0xFF34D399, 0xFF22D3EE},
		{0xFFEC4899, 0xFFA855F7}
	};
	public static int accentIdx = 0;

	public static final int FPS_SAMPLES = 48;

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final String[] LOGS = {
		"[render] Frame-Cache neu aufgebaut (3.1 ms)",
		"[net] Paketverlust 0.0% · stabil",
		"[module] Registry synchronisiert",
		"[sec] Integritätsprüfung bestanden",
		"[cfg] Autosave abgeschlossen",
		"[gfx] VRAM-Auslastung 41%",
		"[net] Heartbeat empfangen · 23 ms",
		"[input] Mausrate 1000 Hz erkannt",
		"[thread] Worker #3 idle",
		"[update] Keine neuen Updates verfügbar",
		"[scan] Ressourcen-Scan übersprungen (manuell)"
	};

	private static final List<FakeModule> MODULES = ModuleCatalog.create();
	private static final List<String[]> CONFIGS = new ArrayList<>();
	private static final List<LogLine> CONSOLE = new ArrayList<>();
	private static final List<Toast> TOASTS = new ArrayList<>();
	private static final int[] FPS_HISTORY = new int[FPS_SAMPLES];
	private static final Random RND = new Random();
	private static final long SESSION_START = System.currentTimeMillis();

	private static int fpsHead = 0;
	private static int fps = 144;
	private static int ping = 23;
	private static long lastStatMs = 0L;
	private static long nextLogAt = 0L;
	private static long lastFrameNs = 0L;
	private static float dt = 0f;
	private static long nowMs = 0L;

	public static final class LogLine {
		public final String time;
		public final String text;
		public final int color;
		public final long at;

		LogLine(String time, String text, int color, long at) {
			this.time = time;
			this.text = text;
			this.color = color;
			this.at = at;
		}
	}

	public static final class Toast {
		public final String text;
		public final long created;

		Toast(String text, long created) {
			this.text = text;
			this.created = created;
		}
	}

	static {
		CONFIGS.add(new String[] {"legit_v3", "vor 2 Tagen"});
		CONFIGS.add(new String[] {"rage_mode", "vor 5 Tagen"});
		CONFIGS.add(new String[] {"pvp_default", "vor 1 Woche"});
		CONFIGS.add(new String[] {"stream_safe", "vor 3 Wochen"});
		CONFIGS.add(new String[] {"clean_setup", "vor 1 Monat"});
		CONFIGS.add(new String[] {"tryhard_2026", "gestern"});
		log("Kern gestartet", 0xFF22D3EE);
		log("Module geladen: " + MODULES.size(), 0xFF7D7896);
	}

	private ChaosState() {
	}

	// ------------------------------------------------------------------
	// Zugriff
	// ------------------------------------------------------------------

	public static List<FakeModule> modules() {
		return MODULES;
	}

	public static List<String[]> configs() {
		return CONFIGS;
	}

	public static List<LogLine> console() {
		return CONSOLE;
	}

	public static List<Toast> toasts() {
		return TOASTS;
	}

	public static int fps() {
		return fps;
	}

	public static int ping() {
		return ping;
	}

	/** Probe i in zeitlicher Reihenfolge (0 = ältester). */
	public static int fpsSample(int i) {
		return FPS_HISTORY[(fpsHead + i) % FPS_SAMPLES];
	}

	public static long sessionStart() {
		return SESSION_START;
	}

	public static float dt() {
		return dt;
	}

	public static long nowMs() {
		return nowMs;
	}

	public static int accent() {
		if (settings[S_RAINBOW]) {
			return Ui.hsv(((nowMs / 25L) % 360L) / 360f, 0.8f, 1.0f);
		}
		return ACCENTS[accentIdx][0];
	}

	public static int accent2() {
		if (settings[S_RAINBOW]) {
			return Ui.hsv((((nowMs / 25L) + 120L) % 360L) / 360f, 0.8f, 1.0f);
		}
		return ACCENTS[accentIdx][1];
	}

	public static List<FakeModule> inCategory(String cat) {
		List<FakeModule> out = new ArrayList<>();
		for (FakeModule m : MODULES) {
			if (m.category.equals(cat)) {
				out.add(m);
			}
		}
		return out;
	}

	public static List<FakeModule> search(String q) {
		List<FakeModule> out = new ArrayList<>();
		String needle = q.toLowerCase(java.util.Locale.ROOT);
		for (FakeModule m : MODULES) {
			if ((m.name + " " + m.desc).toLowerCase(java.util.Locale.ROOT).contains(needle)) {
				out.add(m);
			}
		}
		return out;
	}

	public static int activeTotal() {
		int n = 0;
		for (FakeModule m : MODULES) {
			if (m.enabled) {
				n++;
			}
		}
		return n;
	}

	public static int activeIn(String cat) {
		int n = 0;
		for (FakeModule m : MODULES) {
			if (m.enabled && m.category.equals(cat)) {
				n++;
			}
		}
		return n;
	}

	public static int keyCount() {
		int n = 0;
		for (FakeModule m : MODULES) {
			if (m.key != 0) {
				n++;
			}
		}
		return n;
	}

	// ------------------------------------------------------------------
	// Aktionen
	// ------------------------------------------------------------------

	public static void log(String text, int color) {
		long now = System.currentTimeMillis();
		CONSOLE.add(new LogLine(LocalTime.now().format(TIME), text, color, now));
		while (CONSOLE.size() > 120) {
			CONSOLE.remove(0);
		}
	}

	public static void toast(String text) {
		if (!settings[S_TOASTS]) {
			return;
		}
		TOASTS.add(new Toast(text, System.currentTimeMillis()));
		while (TOASTS.size() > 4) {
			TOASTS.remove(0);
		}
	}

	public static void toggle(FakeModule m) {
		m.enabled = !m.enabled;
		log(m.name + " -> " + (m.enabled ? "aktiviert" : "deaktiviert"), m.enabled ? 0xFF34D399 : 0xFFFBBF24);
		toast(m.name + (m.enabled ? " aktiviert" : " deaktiviert"));
	}

	public static void applyConfig(String name) {
		java.util.Random r = new java.util.Random(name.hashCode());
		for (FakeModule m : MODULES) {
			m.enabled = r.nextFloat() < 0.35f;
		}
		log("Config '" + name + "' geladen", 0xFF34D399);
		toast("Config '" + name + "' geladen");
	}

	public static void deleteConfig(String name) {
		CONFIGS.removeIf(cfg -> cfg[0].equals(name));
		log("Config '" + name + "' gelöscht", 0xFFFBBF24);
		toast("Config '" + name + "' gelöscht");
	}

	/** Pro Frame aufrufen. Mehrfache Aufrufe pro Frame sind harmlos. */
	public static void frame() {
		long ns = System.nanoTime();
		if (lastFrameNs == 0L) {
			lastFrameNs = ns;
		}
		float d = (ns - lastFrameNs) / 1_000_000_000f;
		lastFrameNs = ns;
		dt = Math.min(0.1f, Math.max(0f, d));
		nowMs = System.currentTimeMillis();

		if (nowMs - lastStatMs >= 600L) {
			lastStatMs = nowMs;
			fps = 138 + RND.nextInt(31);
			ping = 18 + RND.nextInt(14);
			FPS_HISTORY[fpsHead] = fps;
			fpsHead = (fpsHead + 1) % FPS_SAMPLES;
		}

		for (FakeModule m : MODULES) {
			m.toggle.to(m.enabled ? 1f : 0f);
			m.hud.to(m.enabled ? 1f : 0f);
			m.toggle.update(dt, 14f);
			m.hud.update(dt, 9f);
			m.hover.update(dt, 12f);
			for (FakeModule.Check c : m.checks) {
				c.anim.to(c.value ? 1f : 0f);
				c.anim.update(dt, 16f);
			}
		}
		TOASTS.removeIf(t -> nowMs - t.created > 2800L);
	}

	/** Zufällige Konsolen-Zeilen, solange das Menü offen ist. */
	public static void streamLog() {
		if (!settings[S_CONSOLE]) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now >= nextLogAt) {
			log(LOGS[RND.nextInt(LOGS.length)], 0xFF7D7896);
			nextLogAt = now + 2200L + RND.nextInt(2000);
		}
	}
}
