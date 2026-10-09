package de.chaos.fakeclient.gui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Das Fake-Client-Menue. Alles hier ist Optik: Module lassen sich umschalten,
 * Slider verschieben und Tasten belegen, bewirkt wird aber nichts.
 */
public final class ChaosScreen extends Screen {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final long BOOT_MS = 2600L;
	private static boolean bootedOnce = false;

	// Farben (ARGB)
	private static final int DIM = 0xB8030207;
	private static final int WIN = 0xFF0D0B16;
	private static final int HEADER_TOP = 0xFF2B1456;
	private static final int TEXT = 0xFFE6E1F5;
	private static final int MUTED = 0xFF7D7896;
	private static final int ON = 0xFF34D399;
	private static final int WARN = 0xFFFBBF24;
	private static final int INFO = 0xFF22D3EE;
	private static final int ROW = 0x14FFFFFF;
	private static final int ROW_HOVER = 0x26A855F7;
	private static final int ROW_SEL = 0x40A855F7;
	private static final int LINE = 0x66A855F7;
	private static final int OFF_TRACK = 0xFF2A2539;
	private static final int KNOB_OFF = 0xFF8A84A3;
	private static final int DANGER = 0xFFF43F5E;

	/** Akzent-Presets: [Start, Ende] */
	private static final int[][] ACCENTS = {
		{0xFFA855F7, 0xFF22D3EE},
		{0xFF22D3EE, 0xFF34D399},
		{0xFFF43F5E, 0xFFF97316},
		{0xFF34D399, 0xFF22D3EE},
		{0xFFEC4899, 0xFFA855F7}
	};

	private static final String[] SETTING_LABELS = {
		"Toast-Benachrichtigungen", "Rainbow-Akzent", "Konsole anzeigen", "Hintergrund-Effekte", "Anti-Screenshot-Schutz"
	};
	private static final String[] SETTING_SUBS = {
		"Kleine Einblendungen bei Änderungen",
		"Animiert den Akzent durch den Farbkreis",
		"Zeigt die Konsole unten im Fenster",
		"Animierte Sterne im Hintergrund",
		"Versteckt das Fenster bei Screenshots"
	};

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

	private static final String[] BOOT_LINES = {
		"Initialisiere CHAOS-Kern...",
		"Lade Modul-Register (1247 Einträge)",
		"Prüfe Lizenz ... Founder Tier",
		"Starte Render-Pipeline",
		"Synchronisiere Config legit_v3",
		"Fertig."
	};

	/** Config-Liste: [Name, Zeitangabe] */
	private static final List<String[]> CONFIGS = new ArrayList<>();
	static {
		CONFIGS.add(new String[] {"legit_v3", "vor 2 Tagen"});
		CONFIGS.add(new String[] {"rage_mode", "vor 5 Tagen"});
		CONFIGS.add(new String[] {"pvp_default", "vor 1 Woche"});
		CONFIGS.add(new String[] {"stream_safe", "vor 3 Wochen"});
		CONFIGS.add(new String[] {"clean_setup", "vor 1 Monat"});
		CONFIGS.add(new String[] {"tryhard_2026", "gestern"});
	}

	private static final int HEADER = 30;
	private static final int FOOTER = 92;
	private static final int ROW_H = 26;
	private static final int ROW_PITCH = 28;
	private static final int CONFIG_PITCH = 40;
	private static final int SETTING_PITCH = 32;

	private static final class LogLine {
		final String time;
		final String text;
		final int color;

		LogLine(String time, String text, int color) {
			this.time = time;
			this.text = text;
			this.color = color;
		}
	}

	private static final class Toast {
		final String text;
		final long created;

		Toast(String text, long created) {
			this.text = text;
			this.created = created;
		}
	}

	private final Random rnd = new Random();
	private final List<FakeModule> modules = ModuleCatalog.create();
	private final List<LogLine> console = new ArrayList<>();
	private final List<Toast> toasts = new ArrayList<>();
	private final boolean[] settings = {true, false, true, true, false};

	private boolean booting;
	private long bootEndAt;
	private long nextLogAt;
	private long nextStatAt;
	private int fps = 144;
	private int ping = 23;

	private int accentIdx = 0;
	private int tab = 0;
	private int scrollOff = 0;
	private FakeModule selected = null;
	private FakeModule binding = null;
	private FakeModule.Slider dragSlider = null;

	// Layout, wird pro Frame/Klick berechnet
	private int wx, wy, ww, wh;
	private int bodyTop, bodyH, footTop, sideW, mainX, mainW, detX, detW;

	public ChaosScreen() {
		super(Component.literal("CHAOS Client"));
		long now = System.currentTimeMillis();
		this.booting = !bootedOnce;
		bootedOnce = true;
		this.bootEndAt = now + BOOT_MS;
		this.nextLogAt = now + 1500L;
		this.nextStatAt = now;
		log("Kern gestartet", INFO);
		log("Module geladen: " + modules.size(), MUTED);
	}

	// ------------------------------------------------------------------
	// Zustand
	// ------------------------------------------------------------------

	private void log(String text, int color) {
		console.add(new LogLine(LocalTime.now().format(TIME), text, color));
		while (console.size() > 120) {
			console.remove(0);
		}
	}

	private void toast(String text) {
		if (!settings[0]) {
			return;
		}
		toasts.add(new Toast(text, System.currentTimeMillis()));
		while (toasts.size() > 4) {
			toasts.remove(0);
		}
	}

	private void toggle(FakeModule m) {
		m.enabled = !m.enabled;
		log(m.name + " -> " + (m.enabled ? "aktiviert" : "deaktiviert"), m.enabled ? ON : WARN);
		toast(m.name + (m.enabled ? " aktiviert" : " deaktiviert"));
	}

	private void finishBoot(long now) {
		if (!booting) {
			return;
		}
		booting = false;
		log("CHAOS Client geladen", ON);
		toast("Willkommen, xX_Founder_Xx");
		nextLogAt = now + 800L;
	}

	private void simulate(long now) {
		if (booting && now >= bootEndAt) {
			finishBoot(now);
		}
		if (now >= nextStatAt) {
			nextStatAt = now + 700L;
			fps = 138 + rnd.nextInt(31);
			ping = 18 + rnd.nextInt(14);
		}
		if (!booting && settings[2] && now >= nextLogAt) {
			log(LOGS[rnd.nextInt(LOGS.length)], MUTED);
			nextLogAt = now + 2200L + rnd.nextInt(2000);
		}
		toasts.removeIf(t -> now - t.created > 2800L);
	}

	// ------------------------------------------------------------------
	// Layout
	// ------------------------------------------------------------------

	private void computeLayout() {
		ww = Math.min(width - 8, Math.max(540, Math.min(width - 24, 780)));
		wh = Math.min(height - 8, Math.max(400, Math.min(height - 24, 440)));
		wx = (width - ww) / 2;
		wy = (height - wh) / 2;
		boolean narrow = ww < 620;
		sideW = narrow ? 112 : 136;
		detW = narrow ? 180 : 216;
		bodyTop = wy + HEADER;
		bodyH = wh - HEADER - FOOTER;
		footTop = wy + wh - FOOTER;
		mainX = wx + sideW;
		mainW = ww - sideW - detW;
		detX = wx + ww - detW;
	}

	private int visibleRows() {
		return Math.max(1, (bodyH - 30) / ROW_PITCH);
	}

	private int rowY(int k) {
		return bodyTop + 26 + k * ROW_PITCH;
	}

	private int[] tabRect(int i) {
		int y = bodyTop + 6 + i * 22 + (i >= ModuleCatalog.MODULE_TABS ? 8 : 0);
		return new int[] {wx + 6, y, sideW - 12, 20};
	}

	private int sliderY(int i) {
		return bodyTop + 86 + i * 30;
	}

	private int checkY(int i) {
		return bodyTop + 86 + 3 * 30 + 6 + i * 14;
	}

	private int accentY() {
		return bodyTop + 26 + SETTING_LABELS.length * SETTING_PITCH + 26;
	}

	private int[] configButton(int rowX, int rowW, int rowTop, int b) {
		int right = rowX + rowW - 8;
		int delX = right - 60;
		int saveX = delX - 6 - 70;
		int loadX = saveX - 6 - 52;
		if (b == 0) {
			return new int[] {loadX, rowTop + 8, 52, 18};
		}
		if (b == 1) {
			return new int[] {saveX, rowTop + 8, 70, 18};
		}
		return new int[] {delX, rowTop + 8, 60, 18};
	}

	private List<FakeModule> modulesIn(String cat) {
		List<FakeModule> out = new ArrayList<>();
		for (FakeModule m : modules) {
			if (m.category.equals(cat)) {
				out.add(m);
			}
		}
		return out;
	}

	private int activeIn(String cat) {
		int n = 0;
		for (FakeModule m : modules) {
			if (m.category.equals(cat) && m.enabled) {
				n++;
			}
		}
		return n;
	}

	private int accent1(long now) {
		if (settings[1]) {
			return hsv(((now / 25L) % 360L) / 360f, 0.8f, 1.0f);
		}
		return ACCENTS[accentIdx][0];
	}

	private int accent2(long now) {
		if (settings[1]) {
			return hsv((((now / 25L) + 120L) % 360L) / 360f, 0.8f, 1.0f);
		}
		return ACCENTS[accentIdx][1];
	}

	// ------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.currentTimeMillis();
		simulate(now);
		computeLayout();

		g.fill(0, 0, width, height, DIM);
		drawStars(g, now);

		if (booting) {
			drawBoot(g, now);
			return;
		}

		g.fill(wx, wy, wx + ww, wy + wh, WIN);
		drawHeader(g, now, mouseX, mouseY);
		drawSidebar(g, now, mouseX, mouseY);
		drawMain(g, now, mouseX, mouseY);
		drawDetails(g, now, mouseX, mouseY);
		drawFooter(g, now);
		drawToasts(g, now);
		outline(g, wx, wy, ww, wh, LINE);
	}

	private void drawStars(GuiGraphics g, long now) {
		if (!settings[3]) {
			return;
		}
		double t = now / 1000.0;
		int col = accent2(now);
		for (int i = 0; i < 70; i++) {
			double fx = ((i * 0.6180339) + t * (0.004 + (i % 5) * 0.002)) % 1.0;
			double fy = ((i * 0.3819660) + t * 0.01 * (1 + i % 3)) % 1.0;
			int x = (int) (fx * width);
			int y = (int) (fy * height);
			int a = 0x30 + (i % 4) * 0x18;
			int size = (i % 7 == 0) ? 2 : 1;
			g.fill(x, y, x + size, y + size, argb(a, col));
		}
	}

	private void drawBoot(GuiGraphics g, long now) {
		float p = (now - (bootEndAt - BOOT_MS)) / (float) BOOT_MS;
		p = Math.max(0f, Math.min(1f, p));
		int a1 = accent1(now);
		int cx = width / 2;
		int cy = height / 2 - 70;

		String title = "CHAOS CLIENT";
		g.drawString(font, title, cx - font.width(title) / 2, cy, a1);
		String sub = "v4.7.2 · Injection";
		g.drawString(font, sub, cx - font.width(sub) / 2, cy + 12, MUTED);

		int bw = 240;
		int bx = cx - bw / 2;
		int by = cy + 34;
		g.fill(bx, by, bx + bw, by + 4, OFF_TRACK);
		g.fill(bx, by, bx + (int) (bw * p), by + 4, a1);

		for (int i = 0; i < BOOT_LINES.length; i++) {
			float thr = (i + 1) / (float) (BOOT_LINES.length + 1);
			if (p >= thr) {
				int color = (p - thr > 0.12f) ? MUTED : TEXT;
				g.drawString(font, "> " + BOOT_LINES[i], bx, by + 14 + i * 11, color);
			}
		}

		String hint = "Klicken zum Überspringen";
		g.drawString(font, hint, cx - font.width(hint) / 2, height / 2 + 90, 0xFF4D4766);
	}

	private void drawHeader(GuiGraphics g, long now, int mx, int my) {
		for (int i = 0; i < HEADER; i++) {
			g.fill(wx, wy + i, wx + ww, wy + i + 1, lerp(HEADER_TOP, WIN, i / (float) HEADER));
		}
		int a1 = accent1(now);
		int ty = wy + 11;
		g.drawString(font, "CHAOS", wx + 12, ty, a1);
		int lx = wx + 12 + font.width("CHAOS ");
		g.drawString(font, "CLIENT", lx, ty, TEXT);
		g.drawString(font, "v4.7.2", lx + font.width("CLIENT "), ty, MUTED);

		String right = "FPS " + fps + "   PING " + ping + "ms   xX_Founder_Xx";
		g.drawString(font, right, wx + ww - 30 - font.width(right) - 8, ty, MUTED);

		int cx1 = wx + ww - 22;
		int cy1 = wy + 7;
		boolean hov = inside(mx, my, cx1, cy1, 16, 16);
		g.fill(cx1, cy1, cx1 + 16, cy1 + 16, hov ? 0x66F43F5E : 0x22FFFFFF);
		g.drawString(font, "x", cx1 + 5, cy1 + 4, TEXT);

		g.fill(wx, wy + HEADER - 1, wx + ww, wy + HEADER, LINE);
	}

	private void drawSidebar(GuiGraphics g, long now, int mx, int my) {
		int a1 = accent1(now);
		g.fill(wx, bodyTop, mainX, bodyTop + bodyH, 0x40000000);
		g.fill(mainX - 1, bodyTop, mainX, bodyTop + bodyH, LINE);
		for (int i = 0; i < ModuleCatalog.TAB_IDS.length; i++) {
			int[] r = tabRect(i);
			boolean act = tab == i;
			boolean hov = inside(mx, my, r);
			if (act) {
				g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], argb(0x55, a1));
				g.fill(r[0], r[1], r[0] + 2, r[1] + r[3], a1);
			} else if (hov) {
				g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x22FFFFFF);
			}
			g.drawString(font, ModuleCatalog.TAB_LABELS[i], r[0] + 10, r[1] + 6, act ? TEXT : MUTED);
			if (i < ModuleCatalog.MODULE_TABS) {
				String cnt = activeIn(ModuleCatalog.TAB_IDS[i]) + "/" + modulesIn(ModuleCatalog.TAB_IDS[i]).size();
				g.drawString(font, cnt, r[0] + r[2] - 8 - font.width(cnt), r[1] + 6, MUTED);
			}
		}
	}

	private void drawMain(GuiGraphics g, long now, int mx, int my) {
		int a1 = accent1(now);
		if (tab < ModuleCatalog.MODULE_TABS) {
			String cat = ModuleCatalog.TAB_IDS[tab];
			List<FakeModule> list = modulesIn(cat);
			g.drawString(font, ModuleCatalog.TAB_LABELS[tab], mainX + 10, bodyTop + 8, a1);
			String info = activeIn(cat) + " von " + list.size() + " aktiv";
			g.drawString(font, info, mainX + mainW - 10 - font.width(info), bodyTop + 8, MUTED);

			int vis = visibleRows();
			clampScroll(list.size(), vis);
			int rowX = mainX + 6;
			int rowW = mainW - 12;
			for (int k = 0; k < vis; k++) {
				int idx = scrollOff + k;
				if (idx >= list.size()) {
					break;
				}
				FakeModule m = list.get(idx);
				int ry = rowY(k);
				boolean hov = inside(mx, my, rowX, ry, rowW, ROW_H);
				drawModuleRow(g, m, rowX, ry, rowW, hov, m == selected, a1);
			}
			if (list.size() > vis) {
				int listTop = rowY(0);
				int listH = vis * ROW_PITCH - (ROW_PITCH - ROW_H);
				int trackX = mainX + mainW - 5;
				g.fill(trackX, listTop, trackX + 3, listTop + listH, 0x33FFFFFF);
				int thumbH = Math.max(20, listH * vis / list.size());
				int range = list.size() - vis;
				int thumbY = listTop + (listH - thumbH) * scrollOff / Math.max(1, range);
				g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, a1);
			}
		} else if (tab == 9) {
			g.drawString(font, "Configs", mainX + 10, bodyTop + 8, a1);
			int rowX = mainX + 6;
			int rowW = mainW - 12;
			for (int i = 0; i < CONFIGS.size(); i++) {
				int ry = bodyTop + 26 + i * CONFIG_PITCH;
				if (ry + 34 > bodyTop + bodyH) {
					break;
				}
				String[] c = CONFIGS.get(i);
				g.fill(rowX, ry, rowX + rowW, ry + 34, ROW);
				g.drawString(font, c[0], rowX + 8, ry + 6, TEXT);
				g.drawString(font, c[1], rowX + 8, ry + 18, MUTED);
				String[] labels = {"Laden", "Speichern", "Löschen"};
				for (int b = 0; b < 3; b++) {
					int[] r = configButton(rowX, rowW, ry, b);
					boolean hov = inside(mx, my, r);
					int bg;
					if (b == 2) {
						bg = hov ? 0x66F43F5E : 0x33F43F5E;
					} else {
						bg = hov ? 0x66A855F7 : 0x33A855F7;
					}
					g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
					outline(g, r[0], r[1], r[2], r[3], b == 2 ? DANGER : LINE);
					int tx = r[0] + (r[2] - font.width(labels[b])) / 2;
					g.drawString(font, labels[b], tx, r[1] + 5, TEXT);
				}
			}
		} else {
			g.drawString(font, "Settings", mainX + 10, bodyTop + 8, a1);
			int rowX = mainX + 6;
			int rowW = mainW - 12;
			for (int i = 0; i < SETTING_LABELS.length; i++) {
				int ry = bodyTop + 26 + i * SETTING_PITCH;
				boolean hov = inside(mx, my, rowX, ry, rowW, 28);
				g.fill(rowX, ry, rowX + rowW, ry + 28, hov ? ROW_HOVER : ROW);
				g.drawString(font, SETTING_LABELS[i], rowX + 8, ry + 5, TEXT);
				g.drawString(font, SETTING_SUBS[i], rowX + 8, ry + 16, MUTED);
				drawSwitch(g, rowX + rowW - 40, ry + 8, 30, 12, settings[i], a1);
			}
			int ay = accentY();
			g.drawString(font, "Akzentfarbe", mainX + 10, ay - 14, MUTED);
			for (int i = 0; i < ACCENTS.length; i++) {
				int sx = rowX + 8 + i * 26;
				g.fill(sx, ay, sx + 9, ay + 18, ACCENTS[i][0]);
				g.fill(sx + 9, ay, sx + 18, ay + 18, ACCENTS[i][1]);
				boolean sel = accentIdx == i && !settings[1];
				outline(g, sx - 1, ay - 1, 20, 20, sel ? TEXT : 0x44FFFFFF);
			}
		}
	}

	private void clampScroll(int count, int vis) {
		int max = Math.max(0, count - vis);
		scrollOff = Math.max(0, Math.min(max, scrollOff));
	}

	private void drawModuleRow(GuiGraphics g, FakeModule m, int x, int y, int w, boolean hov, boolean sel, int a1) {
		int bg = sel ? ROW_SEL : (hov ? ROW_HOVER : ROW);
		g.fill(x, y, x + w, y + ROW_H, bg);
		if (m.enabled) {
			outline(g, x, y, w, ROW_H, argb(0x99, ON));
		}
		int textMax = w - 150;
		g.drawString(font, trim(m.name, textMax), x + 8, y + 4, m.enabled ? 0xFFD8B4FE : TEXT);
		g.drawString(font, trim(m.desc, textMax), x + 8, y + 15, MUTED);

		drawSwitch(g, x + w - 86, y + 7, 30, 12, m.enabled, a1);

		boolean bindingThis = binding == m;
		String keyTxt = bindingThis ? "..." : FakeModule.keyName(m.key);
		drawChip(g, x + w - 46, y + 6, 38, 14, keyTxt, bindingThis, a1);
	}

	private void drawSwitch(GuiGraphics g, int x, int y, int w, int h, boolean on, int a1) {
		g.fill(x, y, x + w, y + h, on ? a1 : OFF_TRACK);
		int kx = on ? x + w - h + 2 : x + 2;
		g.fill(kx, y + 2, kx + h - 4, y + h - 2, on ? 0xFFFFFFFF : KNOB_OFF);
	}

	private void drawChip(GuiGraphics g, int x, int y, int w, int h, String text, boolean active, int a1) {
		g.fill(x, y, x + w, y + h, active ? argb(0x55, a1) : 0x66000000);
		outline(g, x, y, w, h, active ? a1 : LINE);
		int tx = x + (w - font.width(text)) / 2;
		g.drawString(font, text, tx, y + 3, active ? TEXT : MUTED);
	}

	private void drawDetails(GuiGraphics g, long now, int mx, int my) {
		int a1 = accent1(now);
		g.fill(detX, bodyTop, wx + ww, bodyTop + bodyH, 0x33000000);
		g.fill(detX, bodyTop, detX + 1, bodyTop + bodyH, LINE);
		int x = detX + 10;
		int w = detW - 20;
		int y = bodyTop + 8;

		if (selected == null) {
			g.drawString(font, "Kein Modul gewählt", x, y, MUTED);
			for (int i = 0; i < 2; i++) {
				String hint = i == 0 ? "Klicke ein Modul an, um" : "Einstellungen zu sehen.";
				g.drawString(font, hint, x, y + 14 + i * 11, MUTED);
			}
			return;
		}

		FakeModule m = selected;
		g.drawString(font, trim(m.name, w), x, y, TEXT);
		int dy = y + 16;
		for (String line : wrap(m.desc, w, 3)) {
			g.drawString(font, line, x, dy, MUTED);
			dy += 11;
		}

		int ky = bodyTop + 60;
		g.drawString(font, "Keybind", x, ky + 3, MUTED);
		boolean bindingThis = binding == m;
		drawChip(g, x + 56, ky, 50, 14, bindingThis ? "..." : FakeModule.keyName(m.key), bindingThis, a1);
		String status = m.enabled ? "AKTIV" : "INAKTIV";
		g.drawString(font, status, x + w - font.width(status), ky + 3, m.enabled ? ON : MUTED);

		for (int i = 0; i < m.sliders.size(); i++) {
			FakeModule.Slider s = m.sliders.get(i);
			int sy = sliderY(i);
			g.drawString(font, s.label, x, sy, MUTED);
			String val = s.display();
			g.drawString(font, val, x + w - font.width(val), sy, TEXT);
			g.fill(x, sy + 15, x + w, sy + 19, OFF_TRACK);
			int fillW = (int) Math.round(s.fraction() * w);
			g.fill(x, sy + 15, x + fillW, sy + 19, a1);
			int knobX = Math.max(x, Math.min(x + w - 4, x + fillW - 2));
			g.fill(knobX, sy + 13, knobX + 4, sy + 21, 0xFFFFFFFF);
		}

		for (int i = 0; i < m.checks.size(); i++) {
			FakeModule.Check c = m.checks.get(i);
			int cy = checkY(i);
			g.fill(x, cy, x + 9, cy + 9, 0x66000000);
			outline(g, x, cy, 9, 9, LINE);
			if (c.value) {
				g.fill(x + 2, cy + 2, x + 7, cy + 7, a1);
			}
			g.drawString(font, c.label, x + 16, cy, c.value ? TEXT : MUTED);
		}
	}

	private void drawFooter(GuiGraphics g, long now) {
		g.fill(wx, footTop, wx + ww, wy + wh, 0x66000000);
		g.fill(wx, footTop, wx + ww, footTop + 1, LINE);
		g.drawString(font, "KONSOLE", wx + 10, footTop + 6, accent1(now));
		String stat = "fps " + fps + " · ping " + ping + "ms";
		g.drawString(font, stat, wx + ww - 10 - font.width(stat), footTop + 6, MUTED);
		if (!settings[2]) {
			g.drawString(font, "Konsole ausgeblendet (Settings)", wx + 10, footTop + 20, MUTED);
			return;
		}
		int lineH = 10;
		int maxLines = Math.max(1, (wy + wh - (footTop + 18)) / lineH);
		int start = Math.max(0, console.size() - maxLines);
		int ly = footTop + 18;
		for (int i = start; i < console.size(); i++) {
			LogLine l = console.get(i);
			g.drawString(font, l.time, wx + 10, ly, 0xFF5D5778);
			g.drawString(font, trim(l.text, ww - 90), wx + 62, ly, l.color);
			ly += lineH;
		}
	}

	private void drawToasts(GuiGraphics g, long now) {
		int a1 = accent1(now);
		int idx = 0;
		for (int i = toasts.size() - 1; i >= 0; i--) {
			Toast t = toasts.get(i);
			long age = now - t.created;
			float alpha = age > 2300L ? Math.max(0f, (2800L - age) / 500f) : 1f;
			int a = (int) (alpha * 255);
			int tw = font.width(t.text) + 16;
			int tx = wx + ww - tw - 12;
			int ty = footTop - 22 - idx * 20;
			g.fill(tx, ty, tx + tw, ty + 16, argb((int) (alpha * 230), 0x0D0B16));
			g.fill(tx, ty, tx + 2, ty + 16, argb(a, a1));
			g.drawString(font, t.text, tx + 8, ty + 4, argb(a, TEXT));
			idx++;
		}
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		long now = System.currentTimeMillis();
		if (booting) {
			finishBoot(now);
			return true;
		}
		computeLayout();
		int mx = (int) event.x();
		int my = (int) event.y();
		binding = null;

		if (inside(mx, my, wx + ww - 22, wy + 7, 16, 16)) {
			this.onClose();
			return true;
		}
		for (int i = 0; i < ModuleCatalog.TAB_IDS.length; i++) {
			if (inside(mx, my, tabRect(i))) {
				tab = i;
				scrollOff = 0;
				return true;
			}
		}
		if (!inside(mx, my, wx, bodyTop, ww, bodyH)) {
			return true;
		}

		if (mx >= mainX && mx < mainX + mainW) {
			boolean handled;
			if (tab < ModuleCatalog.MODULE_TABS) {
				handled = handleModuleClick(mx, my);
			} else if (tab == 9) {
				handled = handleConfigClick(mx, my);
			} else {
				handled = handleSettingsClick(mx, my, now);
			}
			if (handled) {
				return true;
			}
		}

		if (selected != null && mx >= detX) {
			FakeModule m = selected;
			int x = detX + 10;
			int w = detW - 20;
			if (inside(mx, my, x + 56, bodyTop + 60, 50, 14)) {
				binding = m;
				log("Warte auf Taste für " + m.name, INFO);
				return true;
			}
			for (int i = 0; i < m.sliders.size(); i++) {
				if (inside(mx, my, x, sliderY(i) + 8, w, 14)) {
					dragSlider = m.sliders.get(i);
					dragSlider.setFraction((mx - x) / (double) w);
					return true;
				}
			}
			for (int i = 0; i < m.checks.size(); i++) {
				if (inside(mx, my, x, checkY(i) - 1, w, 12)) {
					FakeModule.Check c = m.checks.get(i);
					c.value = !c.value;
					log(m.name + ": " + c.label + " = " + (c.value ? "an" : "aus"), c.value ? ON : WARN);
					return true;
				}
			}
		}
		return true;
	}

	private boolean handleModuleClick(int mx, int my) {
		List<FakeModule> list = modulesIn(ModuleCatalog.TAB_IDS[tab]);
		int vis = visibleRows();
		int x = mainX + 6;
		int w = mainW - 12;
		for (int k = 0; k < vis; k++) {
			int idx = scrollOff + k;
			if (idx >= list.size()) {
				break;
			}
			FakeModule m = list.get(idx);
			int y = rowY(k);
			if (inside(mx, my, x + w - 86, y + 7, 30, 12)) {
				toggle(m);
				return true;
			}
			if (inside(mx, my, x + w - 46, y + 6, 38, 14)) {
				binding = m;
				log("Warte auf Taste für " + m.name, INFO);
				return true;
			}
			if (inside(mx, my, x, y, w, ROW_H)) {
				selected = m;
				return true;
			}
		}
		return false;
	}

	private boolean handleConfigClick(int mx, int my) {
		int rowX = mainX + 6;
		int rowW = mainW - 12;
		for (int i = 0; i < CONFIGS.size(); i++) {
			int ry = bodyTop + 26 + i * CONFIG_PITCH;
			for (int b = 0; b < 3; b++) {
				if (!inside(mx, my, configButton(rowX, rowW, ry, b))) {
					continue;
				}
				String name = CONFIGS.get(i)[0];
				if (b == 0) {
					Random r = new Random(name.hashCode());
					for (FakeModule m : modules) {
						m.enabled = r.nextFloat() < 0.35f;
					}
					log("Config '" + name + "' geladen", ON);
					toast("Config '" + name + "' geladen");
				} else if (b == 1) {
					log("Config '" + name + "' gespeichert", INFO);
					toast("Config '" + name + "' gespeichert");
				} else {
					CONFIGS.remove(i);
					log("Config '" + name + "' gelöscht", WARN);
					toast("Config '" + name + "' gelöscht");
				}
				return true;
			}
		}
		return false;
	}

	private boolean handleSettingsClick(int mx, int my, long now) {
		int rowX = mainX + 6;
		int rowW = mainW - 12;
		for (int i = 0; i < SETTING_LABELS.length; i++) {
			int ry = bodyTop + 26 + i * SETTING_PITCH;
			if (inside(mx, my, rowX, ry, rowW, 28)) {
				settings[i] = !settings[i];
				boolean v = settings[i];
				log(SETTING_LABELS[i] + ": " + (v ? "an" : "aus"), v ? ON : WARN);
				if (i != 0) {
					toast(SETTING_LABELS[i] + (v ? " an" : " aus"));
				}
				return true;
			}
		}
		int ay = accentY();
		for (int i = 0; i < ACCENTS.length; i++) {
			int sx = rowX + 8 + i * 26;
			if (inside(mx, my, sx, ay, 18, 18)) {
				accentIdx = i;
				log("Akzentfarbe: Preset " + (i + 1), INFO);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragSlider != null) {
			computeLayout();
			int x = detX + 10;
			int w = detW - 20;
			dragSlider.setFraction((event.x() - x) / (double) w);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragSlider != null) {
			log("Wert gesetzt: " + dragSlider.label + " = " + dragSlider.display(), INFO);
			dragSlider = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		computeLayout();
		if (tab < ModuleCatalog.MODULE_TABS && inside((int) mouseX, (int) mouseY, mainX, bodyTop, mainW, bodyH)) {
			int count = modulesIn(ModuleCatalog.TAB_IDS[tab]).size();
			scrollOff -= (int) Math.signum(scrollY);
			clampScroll(count, visibleRows());
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (binding != null) {
			if (key != 256) {
				binding.key = key;
				log("Keybind " + binding.name + " -> " + FakeModule.keyName(key), INFO);
			}
			binding = null;
			return true;
		}
		if (key != 256) {
			for (FakeModule m : modules) {
				if (m.key != 0 && m.key == key) {
					toggle(m);
				}
			}
		}
		return super.keyPressed(event);
	}

	// ------------------------------------------------------------------
	// Helfer
	// ------------------------------------------------------------------

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static boolean inside(int mx, int my, int[] r) {
		return inside(mx, my, r[0], r[1], r[2], r[3]);
	}

	private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	private String trim(String s, int maxW) {
		if (font.width(s) <= maxW) {
			return s;
		}
		String out = s;
		while (out.length() > 1 && font.width(out + "...") > maxW) {
			out = out.substring(0, out.length() - 1);
		}
		return out + "...";
	}

	private List<String> wrap(String text, int maxW, int maxLines) {
		List<String> out = new ArrayList<>();
		String cur = "";
		for (String word : text.split(" ")) {
			String cand = cur.isEmpty() ? word : cur + " " + word;
			if (font.width(cand) <= maxW) {
				cur = cand;
			} else {
				if (!cur.isEmpty()) {
					out.add(cur);
				}
				cur = word;
			}
			if (out.size() >= maxLines) {
				return out;
			}
		}
		if (!cur.isEmpty() && out.size() < maxLines) {
			out.add(cur);
		}
		return out;
	}

	private static int argb(int a, int rgb) {
		return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
	}

	private static int mix(int a, int b, float t) {
		return (int) (a + (b - a) * t);
	}

	private static int lerp(int c1, int c2, float t) {
		int a = mix((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t);
		int r = mix((c1 >>> 16) & 0xFF, (c2 >>> 16) & 0xFF, t);
		int g = mix((c1 >>> 8) & 0xFF, (c2 >>> 8) & 0xFF, t);
		int b = mix(c1 & 0xFF, c2 & 0xFF, t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int hsv(float h, float s, float v) {
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = v * (1f - s);
		float q = v * (1f - f * s);
		float t = v * (1f - (1f - f) * s);
		float r;
		float gg;
		float b;
		switch (i % 6) {
			case 0: r = v; gg = t; b = p; break;
			case 1: r = q; gg = v; b = p; break;
			case 2: r = p; gg = v; b = t; break;
			case 3: r = p; gg = q; b = v; break;
			case 4: r = t; gg = p; b = v; break;
			default: r = v; gg = p; b = q; break;
		}
		return ((int) (r * 255) << 16) | ((int) (gg * 255) << 8) | (int) (b * 255);
	}
}
