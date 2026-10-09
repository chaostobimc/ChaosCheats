package de.chaos.fakeclient.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Das CHAOS-Menü. Alles hier ist Optik: Module lassen sich umschalten,
 * Slider verschieben und Tasten belegen, bewirkt wird aber nichts.
 */
public final class ChaosScreen extends Screen {

	private static final long BOOT_MS = 3200L;
	private static boolean bootedOnce = false;

	private static final int TOP_H = 44;
	private static final int FOOT_H = 96;
	private static final int CARD_H = 46;
	private static final int GAP = 6;

	private static final int TEXT = 0xFFE6E1F5;
	private static final int MUTED = 0xFF7D7896;
	private static final int ON = 0xFF34D399;
	private static final int WARN = 0xFFFBBF24;
	private static final int INFO = 0xFF22D3EE;
	private static final int DANGER = 0xFFF43F5E;
	private static final int OFF_TRACK = 0xFF2A2539;
	private static final int BORDER = 0xFF3B2A6B;
	private static final int INK = 0xFF0A0812;

	private static final String[] TAB_ICONS = {"C", "M", "V", "P", "W", "E", "R", "S", "+", "#", "*"};
	private static final String[] BOOT_LINES = {
		"Initialisiere CHAOS-Kern",
		"Lade Modul-Register (1247 Einträge)",
		"Prüfe Lizenz: Founder Tier",
		"Starte Render-Pipeline",
		"Synchronisiere Config legit_v3",
		"Fertig"
	};

	private interface Action {
		void run(int ax, int ay);
	}

	private static final class Hit {
		final int x;
		final int y;
		final int w;
		final int h;
		final Action action;

		Hit(int x, int y, int w, int h, Action action) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.action = action;
		}

		boolean contains(int mx, int my) {
			return inside(mx, my, x, y, w, h);
		}
	}

	private final List<Hit> hits = new ArrayList<>();
	private final Anim openAnim = new Anim(0f);
	private final Anim indicator = new Anim(0f);
	private final Anim scrollAnim = new Anim(0f);
	private boolean indicatorReady = false;
	private boolean booting;
	private long bootStart;
	private long tabChangedAt;

	private int tab = 0;
	private int scrollRow = 0;
	private int lastRows = 0;
	private int lastVisible = 1;
	private FakeModule selected = null;
	private FakeModule binding = null;
	private boolean searchFocus = false;
	private String search = "";
	private boolean showingSearch = false;
	private FakeModule.Slider dragSlider = null;
	private int dragX;
	private int dragW;
	private float fade;

	// Layout, wird pro Frame berechnet
	private int wx;
	private int wy;
	private int ww;
	private int wh;
	private int sideW;
	private int bodyTop;
	private int bodyBottom;
	private int mainX;
	private int mainW;
	private int detX;
	private int detW;
	private int contentTop;
	private int contentBottom;
	private int footTop;

	public ChaosScreen() {
		super(Component.literal("CHAOS Client"));
		this.booting = !bootedOnce;
		bootedOnce = true;
		this.bootStart = System.currentTimeMillis();
		this.tabChangedAt = bootStart - 5000L;
		this.openAnim.to(1f);
	}

	// ------------------------------------------------------------------
	// Render
	// ------------------------------------------------------------------

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		ChaosState.frame();
		long now = System.currentTimeMillis();
		float dt = ChaosState.dt();
		openAnim.update(dt, 6f);
		fade = Ui.easeOut(openAnim.get());

		hits.clear();
		for (FakeModule m : ChaosState.modules()) {
			m.hover.to(0f);
		}

		g.fill(0, 0, width, height, Ui.withAlpha(0xFF05040A, fade * 0.82f));
		drawBackdrop(g, now, fade);

		if (booting) {
			if (now - bootStart >= BOOT_MS) {
				finishBoot();
			} else {
				drawBoot(g, now);
				return;
			}
		}

		ChaosState.streamLog();
		showingSearch = !search.isEmpty();
		computeLayout();
		drawWindow(g, mouseX, mouseY, now, dt);
	}

	private void computeLayout() {
		ww = Math.min(width - 8, Math.max(600, Math.min(width - 24, 900)));
		wh = Math.min(height - 8, Math.max(420, Math.min(height - 24, 520)));
		wx = (width - ww) / 2;
		int slide = (int) ((1f - fade) * 18f);
		wy = (height - wh) / 2 + slide;
		boolean narrow = ww < 680;
		sideW = narrow ? 132 : 150;
		detW = narrow ? 190 : 232;
		bodyTop = wy + TOP_H;
		footTop = wy + wh - FOOT_H;
		bodyBottom = footTop;
		mainX = wx + sideW + 8;
		detX = wx + ww - 8 - detW;
		mainW = (detW > 0 ? detX - 8 : wx + ww - 8) - mainX;
		contentTop = bodyTop + 64;
		contentBottom = bodyBottom - 8;
	}

	private int tabY(int i) {
		return bodyTop + 10 + i * 26 + (i >= ModuleCatalog.MODULE_TABS ? 10 : 0);
	}

	private void drawBackdrop(GuiGraphics g, long now, float k) {
		float t = now / 1000f;
		int a1 = ChaosState.accent();
		int a2 = ChaosState.accent2();
		int cx1 = (int) (width * (0.25f + 0.08f * Math.sin(t * 0.4f)));
		int cy1 = (int) (height * (0.30f + 0.06f * Math.cos(t * 0.3f)));
		Ui.blob(g, cx1, cy1, 170, Ui.withAlpha(a1, k * 0.45f), 8);
		int cx2 = (int) (width * (0.75f + 0.07f * Math.sin(t * 0.35f + 2f)));
		int cy2 = (int) (height * (0.70f + 0.07f * Math.cos(t * 0.45f)));
		Ui.blob(g, cx2, cy2, 210, Ui.withAlpha(a2, k * 0.35f), 8);

		if (!ChaosState.settings[ChaosState.S_PARTICLES]) {
			return;
		}
		double ts = now / 1000.0;
		for (int i = 0; i < 70; i++) {
			double fx = ((i * 0.6180339) + ts * (0.004 + (i % 5) * 0.002)) % 1.0;
			double fy = ((i * 0.3819660) + ts * 0.01 * (1 + i % 3)) % 1.0;
			int x = (int) (fx * width);
			int y = (int) (fy * height);
			int a = 0x30 + (i % 4) * 0x18;
			int size = (i % 7 == 0) ? 2 : 1;
			g.fill(x, y, x + size, y + size, Ui.withAlpha(argb(a, a2), k));
		}
	}

	private static int argb(int a, int rgb) {
		return Ui.argb(a, rgb);
	}

	// ------------------------------------------------------------------
	// Boot
	// ------------------------------------------------------------------

	private void drawBoot(GuiGraphics g, long now) {
		int a1 = ChaosState.accent();
		int a2 = ChaosState.accent2();
		long el = now - bootStart;
		float p = Ui.clamp(el / (float) BOOT_MS, 0f, 1f);
		int cx = width / 2;
		int cy = height / 2 - 78;

		String title = "CHAOS CLIENT";
		float tt = now / 1000f;
		int tx = cx - font.width(title) / 2;
		for (int i = 0; i < title.length(); i++) {
			String ch = String.valueOf(title.charAt(i));
			float wave = (float) (Math.sin(tt * 2.5f + i * 0.5f) + 1.0) / 2f;
			Ui.text(g, font, ch, tx, cy, Ui.mix(a1, a2, wave));
			tx += font.width(ch);
		}

		String sub = "v4.7.2 · injection";
		int subChars = (int) Math.min(sub.length(), el / 40L);
		String subShown = sub.substring(0, subChars);
		Ui.text(g, font, subShown, cx - font.width(sub) / 2, cy + 12, MUTED);

		int bw = 300;
		int bx = cx - bw / 2;
		int by = cy + 40;
		Ui.round(g, bx, by, bw, 4, 0x22FFFFFF);
		int fw = (int) (bw * p);
		Ui.round(g, bx, by, fw, 4, a1);
		Ui.rect(g, bx + fw - 4, by - 3, 6, 10, Ui.withAlpha(a2, 0.8f));

		int n = BOOT_LINES.length;
		for (int i = 0; i < n; i++) {
			long start = BOOT_MS * i / (n + 1);
			if (el < start) {
				continue;
			}
			String line = BOOT_LINES[i];
			int chars = (int) Math.min(line.length(), (el - start) / 22L);
			boolean done = chars >= line.length();
			String shown = "> " + line.substring(0, chars);
			if (!done && (now / 180L) % 2L == 0L) {
				shown = shown + "_";
			}
			int color = done ? MUTED : a1;
			Ui.text(g, font, shown, bx, by + 14 + i * 12, color);
		}

		String hint = "Klick zum Überspringen";
		Ui.text(g, font, hint, cx - font.width(hint) / 2, height / 2 + 110, 0xFF4D4766);
	}

	private void finishBoot() {
		if (!booting) {
			return;
		}
		booting = false;
		ChaosState.log("CHAOS Client geladen", ON);
		ChaosState.toast("Willkommen, xX_Founder_Xx");
	}

	// ------------------------------------------------------------------
	// Fenster
	// ------------------------------------------------------------------

	private void drawWindow(GuiGraphics g, int mx, int my, long now, float dt) {
		int a1 = ChaosState.accent();
		int a2 = ChaosState.accent2();
		float k = fade;

		Ui.glow(g, wx, wy, ww, wh, Ui.withAlpha(a1, k * 0.9f), 10);
		Ui.round(g, wx, wy, ww, wh, Ui.withAlpha(INK, k * 0.95f));
		Ui.outline(g, wx, wy, ww, wh, Ui.withAlpha(BORDER, k));

		drawTopBar(g, mx, my, now, k, a1, a2);
		drawSidebar(g, mx, my, now, k, a1, a2);
		drawHeader(g, mx, my, now, k, a1);

		if (showingSearch) {
			drawGrid(g, mx, my, now, k, a1, a2, ChaosState.search(search));
		} else if (tab < ModuleCatalog.MODULE_TABS) {
			drawGrid(g, mx, my, now, k, a1, a2, ChaosState.inCategory(ModuleCatalog.TAB_IDS[tab]));
		} else if (tab == 9) {
			drawConfigs(g, mx, my, now, k, a1, a2);
		} else {
			drawSettings(g, mx, my, now, k, a1);
		}

		drawDetails(g, mx, my, k, a1, a2);
		drawFooter(g, now, k, a1, a2);
		drawToasts(g, now, k, a1);
	}

	private void drawTopBar(GuiGraphics g, int mx, int my, long now, float k, int a1, int a2) {
		for (int i = 0; i < TOP_H; i++) {
			int c = Ui.mix(0xFF1B1038, INK, i / (float) TOP_H);
			g.fill(wx + 1, wy + i, wx + ww - 1, wy + i + 1, Ui.withAlpha(c, k));
		}

		int lx = wx + 14;
		int ly = wy + 11;
		for (int i = 0; i < 22; i++) {
			g.fill(lx, ly + i, lx + 22, ly + i + 1, Ui.withAlpha(Ui.mix(a1, a2, i / 21f), k));
		}
		Ui.text(g, font, "C", lx + 7, ly + 7, Ui.withAlpha(INK, k));

		float t = now / 1000f;
		int tx = lx + 30;
		String brand = "CHAOS";
		for (int i = 0; i < brand.length(); i++) {
			String ch = String.valueOf(brand.charAt(i));
			float wave = (float) (Math.sin(t * 2.5f + i * 0.6f) + 1.0) / 2f;
			Ui.text(g, font, ch, tx, wy + 12, Ui.withAlpha(Ui.mix(a1, a2, wave), k));
			tx += font.width(ch);
		}
		Ui.text(g, font, "client", tx + 4, wy + 12, Ui.withAlpha(MUTED, k));

		int vx = tx + 4 + font.width("client") + 8;
		String ver = "v4.7.2";
		Ui.round(g, vx, wy + 11, font.width(ver) + 10, 13, Ui.withAlpha(0xFFFFFFFF, k * 0.07f));
		Ui.text(g, font, ver, vx + 5, wy + 13, Ui.withAlpha(MUTED, k));

		int closeX = wx + ww - 28;
		int closeY = wy + 14;
		boolean hovClose = inside(mx, my, closeX, closeY, 16, 16);
		Ui.round(g, closeX, closeY, 16, 16, Ui.withAlpha(DANGER, k * (hovClose ? 0.45f : 0.12f)));
		Ui.text(g, font, "x", closeX + 5, closeY + 4, Ui.withAlpha(TEXT, k));
		hits.add(new Hit(closeX, closeY, 16, 16, (ax, ay) -> this.onClose()));

		String fpsTxt = "FPS " + ChaosState.fps();
		String pingTxt = "PING " + ChaosState.ping() + "ms";
		String actTxt = ChaosState.activeTotal() + " aktiv";
		int rx = closeX - 12;
		rx = drawChip(g, rx, wy + 13, actTxt, k, ON, true, now);
		rx = drawChip(g, rx - 6, wy + 13, pingTxt, k, MUTED, false, now);
		drawChip(g, rx - 6, wy + 13, fpsTxt, k, MUTED, false, now);

		Ui.rect(g, wx + 1, wy + TOP_H - 1, ww - 2, 1, Ui.withAlpha(0x88A855F7, k));
	}

	/** Zeichnet einen Chip rechtsbündig bei rx und gibt die neue linke Kante zurück. */
	private int drawChip(GuiGraphics g, int rx, int y, String text, float k, int color, boolean dot, long now) {
		int w = font.width(text) + 14 + (dot ? 10 : 0);
		int x = rx - w;
		Ui.round(g, x, y, w, 16, Ui.withAlpha(0xFFFFFFFF, k * 0.05f));
		int tx = x + 7;
		if (dot) {
			float pulse = 0.5f + 0.5f * (float) Math.sin(now / 350.0);
			Ui.round(g, x + 6, y + 5, 6, 6, Ui.withAlpha(color, k * (0.5f + 0.5f * pulse)));
			tx += 10;
		}
		Ui.text(g, font, text, tx, y + 4, Ui.withAlpha(color, k));
		return x;
	}

	private void drawSidebar(GuiGraphics g, int mx, int my, long now, float k, int a1, int a2) {
		int sx = wx + 8;
		int sw = sideW - 8;
		int targetY = tabY(tab);
		if (!indicatorReady) {
			indicator.snap(targetY);
			indicatorReady = true;
		}
		indicator.to(targetY);
		indicator.update(ChaosState.dt(), 16f);
		int iy = Math.round(indicator.get());
		Ui.round(g, sx, iy, sw, 22, Ui.withAlpha(a1, k * 0.18f));
		Ui.rect(g, sx, iy + 4, 2, 14, Ui.withAlpha(a1, k));

		for (int i = 0; i < ModuleCatalog.TAB_IDS.length; i++) {
			int ty = tabY(i);
			boolean act = !showingSearch && tab == i;
			boolean hov = inside(mx, my, sx, ty, sw, 22);
			if (hov && !act) {
				Ui.round(g, sx, ty, sw, 22, Ui.withAlpha(0xFFFFFFFF, k * 0.05f));
			}
			Ui.round(g, sx + 6, ty + 3, 16, 16, Ui.withAlpha(act ? a1 : 0x22FFFFFF, k));
			int iconW = font.width(TAB_ICONS[i]);
			Ui.text(g, font, TAB_ICONS[i], sx + 14 - iconW / 2, ty + 7, Ui.withAlpha(act ? INK : MUTED, k));
			Ui.text(g, font, ModuleCatalog.TAB_LABELS[i], sx + 28, ty + 7, Ui.withAlpha(act ? TEXT : MUTED, k));
			if (i < ModuleCatalog.MODULE_TABS) {
				String cnt = ChaosState.activeIn(ModuleCatalog.TAB_IDS[i]) + "/"
					+ ChaosState.inCategory(ModuleCatalog.TAB_IDS[i]).size();
				Ui.text(g, font, cnt, sx + sw - 8 - font.width(cnt), ty + 7, Ui.withAlpha(MUTED, k * 0.8f));
			}
			final int idx = i;
			hits.add(new Hit(sx, ty, sw, 22, (ax, ay) -> selectTab(idx)));
		}

		if (bodyBottom - bodyTop >= 330) {
			int py = bodyBottom - 50;
			Ui.round(g, sx, py, sw, 42, Ui.withAlpha(0xFFFFFFFF, k * 0.05f));
			Ui.round(g, sx + 8, py + 8, 26, 26, Ui.withAlpha(a1, k));
			Ui.text(g, font, "X", sx + 8 + 13 - font.width("X") / 2, py + 16, Ui.withAlpha(INK, k));
			Ui.text(g, font, Ui.trim(font, "xX_Founder_Xx", sw - 50), sx + 42, py + 8, Ui.withAlpha(TEXT, k));
			long secs = (now - ChaosState.sessionStart()) / 1000L;
			String sess = String.format("Session %02d:%02d:%02d", secs / 3600L, (secs / 60L) % 60L, secs % 60L);
			Ui.text(g, font, sess, sx + 42, py + 21, Ui.withAlpha(MUTED, k));
		}
	}

	private void selectTab(int i) {
		tab = i;
		search = "";
		searchFocus = false;
		scrollRow = 0;
		scrollAnim.snap(0f);
		tabChangedAt = System.currentTimeMillis();
	}

	private void drawHeader(GuiGraphics g, int mx, int my, long now, float k, int a1) {
		int x = mainX;
		int y = bodyTop + 10;
		int w = mainW;
		boolean hov = inside(mx, my, x, y, w, 22);
		float bgA = searchFocus ? 0.10f : (hov ? 0.08f : 0.05f);
		Ui.round(g, x, y, w, 22, Ui.withAlpha(0xFFFFFFFF, k * bgA));
		if (searchFocus) {
			Ui.glow(g, x, y, w, 22, Ui.withAlpha(a1, k * 0.5f), 3);
			Ui.outline(g, x, y, w, 22, Ui.withAlpha(a1, k));
		}

		String shown;
		int col;
		if (search.isEmpty() && !searchFocus) {
			shown = "Suchen … (alle Module)";
			col = MUTED;
		} else {
			boolean caret = searchFocus && (now / 500L) % 2L == 0L;
			shown = search + (caret ? "|" : "");
			col = TEXT;
		}
		Ui.text(g, font, Ui.trim(font, shown, w - 24), x + 9, y + 7, Ui.withAlpha(col, k));
		hits.add(new Hit(x, y, w, 22, (ax, ay) -> searchFocus = true));

		String title;
		String info;
		if (showingSearch) {
			title = "Suche";
			info = ChaosState.search(search).size() + " Treffer";
		} else if (tab < ModuleCatalog.MODULE_TABS) {
			title = ModuleCatalog.TAB_LABELS[tab];
			info = ChaosState.activeIn(ModuleCatalog.TAB_IDS[tab]) + " von "
				+ ChaosState.inCategory(ModuleCatalog.TAB_IDS[tab]).size() + " aktiv";
		} else if (tab == 9) {
			title = "Configs";
			info = ChaosState.configs().size() + " gespeichert";
		} else {
			title = "Settings";
			info = "Client-Einstellungen";
		}
		Ui.rect(g, x, bodyTop + 41, 3, 12, Ui.withAlpha(a1, k));
		Ui.text(g, font, title, x + 9, bodyTop + 42, Ui.withAlpha(TEXT, k));
		Ui.text(g, font, info, x + w - font.width(info), bodyTop + 42, Ui.withAlpha(MUTED, k));
	}

	// ------------------------------------------------------------------
	// Inhalt
	// ------------------------------------------------------------------

	private void drawGrid(GuiGraphics g, int mx, int my, long now, float k, int a1, int a2, List<FakeModule> list) {
		int cols = mainW >= 440 ? 2 : 1;
		int cardW = (mainW - GAP * (cols - 1)) / cols;
		int pitch = CARD_H + GAP;
		int rows = (list.size() + cols - 1) / cols;
		int vis = Math.max(1, (contentBottom - contentTop + GAP) / pitch);
		lastRows = rows;
		lastVisible = vis;
		clampRow(rows, vis);
		scrollAnim.to(scrollRow);
		scrollAnim.update(ChaosState.dt(), 14f);
		float sr = scrollAnim.get();

		if (list.isEmpty()) {
			Ui.text(g, font, "Keine Module gefunden.", mainX + 4, contentTop + 6, Ui.withAlpha(MUTED, k));
			return;
		}

		for (int i = 0; i < list.size(); i++) {
			int row = i / cols;
			int col = i % cols;
			float yf = contentTop + (row - sr) * pitch;
			if (yf < contentTop || yf + CARD_H > contentBottom) {
				continue;
			}
			float enter = Ui.easeOut((now - tabChangedAt - Math.min(i, 14) * 28L) / 320f);
			if (enter <= 0f) {
				continue;
			}
			int x = mainX + col * (cardW + GAP);
			int y = Math.round(yf) + (int) ((1f - enter) * 12f);
			FakeModule m = list.get(i);
			boolean hov = inside(mx, my, x, y, cardW, CARD_H);
			if (hov) {
				m.hover.to(1f);
			}
			drawModuleCard(g, m, x, y, cardW, hov, k * enter, a1);
		}

		if (rows > vis) {
			int trackX = mainX + mainW - 3;
			int listH = contentBottom - contentTop;
			Ui.round(g, trackX, contentTop, 3, listH, Ui.withAlpha(0xFFFFFFFF, k * 0.06f));
			int thumbH = Math.max(20, listH * vis / Math.max(1, rows));
			float frac = sr / (float) (rows - vis);
			int thumbY = contentTop + Math.round((listH - thumbH) * Ui.clamp(frac, 0f, 1f));
			Ui.round(g, trackX, thumbY, 3, thumbH, Ui.withAlpha(a1, k * 0.8f));
		}
	}

	private void drawModuleCard(GuiGraphics g, FakeModule m, int x, int y, int w, boolean hov, float k, int a1) {
		float hv = m.hover.get();
		float tg = m.toggle.get();

		Ui.round(g, x, y, w, CARD_H, Ui.withAlpha(0xFFFFFFFF, k * (0.035f + 0.04f * hv)));
		if (tg > 0.01f) {
			Ui.round(g, x, y, w, CARD_H, Ui.withAlpha(a1, k * 0.13f * tg));
		}
		int border = Ui.mix(0xFFFFFFFF, a1, tg);
		Ui.outline(g, x, y, w, CARD_H, Ui.withAlpha(border, k * (0.08f + 0.35f * tg + 0.15f * hv)));
		Ui.rect(g, x, y + 8, 3, CARD_H - 16, Ui.withAlpha(a1, k * tg));

		int nameCol = Ui.mix(TEXT, a1, tg * 0.55f);
		int textMax = w - 150;
		Ui.text(g, font, Ui.trim(font, m.name, textMax), x + 12, y + 8, Ui.withAlpha(nameCol, k));
		Ui.text(g, font, Ui.trim(font, m.desc, textMax), x + 12, y + 22, Ui.withAlpha(MUTED, k));

		int pillX = x + w - 52;
		int pillY = y + (CARD_H - 16) / 2;
		drawSwitch(g, pillX, pillY, 38, 16, tg, k, a1);

		boolean bindingThis = binding == m;
		String keyTxt = bindingThis ? "..." : FakeModule.keyName(m.key);
		int kw = Math.max(30, font.width(keyTxt) + 12);
		int kx = pillX - 8 - kw;
		int ky = y + (CARD_H - 14) / 2;
		Ui.round(g, kx, ky, kw, 14, Ui.withAlpha(bindingThis ? a1 : 0xFFFFFFFF, k * (bindingThis ? 0.25f : 0.06f)));
		Ui.outline(g, kx, ky, kw, 14, Ui.withAlpha(bindingThis ? a1 : BORDER, k));
		Ui.text(g, font, keyTxt, kx + (kw - font.width(keyTxt)) / 2, ky + 3, Ui.withAlpha(bindingThis ? TEXT : MUTED, k));

		if (m == selected) {
			Ui.outline(g, x, y, w, CARD_H, Ui.withAlpha(a1, k));
		}

		hits.add(new Hit(x, y, w, CARD_H, (ax, ay) -> selectModule(m)));
		hits.add(new Hit(pillX, pillY, 38, 16, (ax, ay) -> ChaosState.toggle(m)));
		hits.add(new Hit(kx, ky, kw, 14, (ax, ay) -> {
			binding = m;
			ChaosState.log("Warte auf Taste für " + m.name, INFO);
		}));
	}

	private void drawSwitch(GuiGraphics g, int x, int y, int w, int h, float on, float k, int a1) {
		int track = Ui.mix(OFF_TRACK, a1, on);
		Ui.round(g, x, y, w, h, Ui.withAlpha(track, k));
		int knob = h - 4;
		int kx = x + 2 + Math.round((w - knob - 4) * on);
		Ui.round(g, kx, y + 2, knob, knob, Ui.withAlpha(0xFFFFFFFF, k));
	}

	private void drawConfigs(GuiGraphics g, int mx, int my, long now, float k, int a1, int a2) {
		List<String[]> cfgs = ChaosState.configs();
		for (int i = 0; i < cfgs.size(); i++) {
			int cy = contentTop + i * (CARD_H + GAP);
			if (cy + CARD_H > contentBottom) {
				break;
			}
			float e = Ui.easeOut((now - tabChangedAt - i * 60L) / 320f);
			if (e <= 0f) {
				continue;
			}
			float kk = k * e;
			String[] c = cfgs.get(i);
			final String cname = c[0];
			boolean hovCard = inside(mx, my, mainX, cy, mainW, CARD_H);
			Ui.round(g, mainX, cy, mainW, CARD_H, Ui.withAlpha(0xFFFFFFFF, kk * (hovCard ? 0.07f : 0.04f)));
			Ui.outline(g, mainX, cy, mainW, CARD_H, Ui.withAlpha(0xFFFFFFFF, kk * 0.06f));
			Ui.text(g, font, c[0], mainX + 14, cy + 9, Ui.withAlpha(TEXT, kk));
			Ui.text(g, font, c[1], mainX + 14, cy + 24, Ui.withAlpha(MUTED, kk));

			String[] labels = {"Laden", "Speichern", "Löschen"};
			int right = mainX + mainW - 14;
			for (int b = 0; b < 3; b++) {
				int[] r = configButton(right, cy, b);
				boolean hov = inside(mx, my, r[0], r[1], r[2], r[3]);
				int col = b == 0 ? a1 : (b == 1 ? a2 : DANGER);
				Ui.round(g, r[0], r[1], r[2], r[3], Ui.withAlpha(col, kk * (hov ? 0.35f : 0.14f)));
				Ui.outline(g, r[0], r[1], r[2], r[3], Ui.withAlpha(col, kk * (hov ? 0.9f : 0.5f)));
				int tw = font.width(labels[b]);
				Ui.text(g, font, labels[b], r[0] + (r[2] - tw) / 2, r[1] + 6, Ui.withAlpha(TEXT, kk));
				final int btn = b;
				hits.add(new Hit(r[0], r[1], r[2], r[3], (ax, ay) -> configAction(btn, cname)));
			}
		}
	}

	private static int[] configButton(int right, int cy, int b) {
		int delX = right - 62;
		int saveX = delX - 6 - 72;
		int loadX = saveX - 6 - 58;
		if (b == 0) {
			return new int[] {loadX, cy + 13, 58, 20};
		}
		if (b == 1) {
			return new int[] {saveX, cy + 13, 72, 20};
		}
		return new int[] {delX, cy + 13, 62, 20};
	}

	private void configAction(int b, String name) {
		if (b == 0) {
			ChaosState.applyConfig(name);
		} else if (b == 1) {
			ChaosState.log("Config '" + name + "' gespeichert", INFO);
			ChaosState.toast("Config '" + name + "' gespeichert");
		} else {
			ChaosState.deleteConfig(name);
		}
	}

	private void drawSettings(GuiGraphics g, int mx, int my, long now, float k, int a1) {
		int cols = mainW >= 440 ? 2 : 1;
		int cardW = (mainW - GAP * (cols - 1)) / cols;
		for (int i = 0; i < ChaosState.SETTING_LABELS.length; i++) {
			int row = i / cols;
			int col = i % cols;
			int x = mainX + col * (cardW + GAP);
			int y = contentTop + row * (CARD_H + GAP);
			float e = Ui.easeOut((now - tabChangedAt - i * 50L) / 320f);
			if (e <= 0f) {
				continue;
			}
			float kk = k * e;
			boolean on = ChaosState.settings[i];
			boolean hov = inside(mx, my, x, y, cardW, CARD_H);
			Ui.round(g, x, y, cardW, CARD_H, Ui.withAlpha(0xFFFFFFFF, kk * (hov ? 0.07f : 0.04f)));
			Ui.outline(g, x, y, cardW, CARD_H, Ui.withAlpha(on ? a1 : 0xFFFFFFFF, kk * (on ? 0.5f : 0.06f)));
			Ui.text(g, font, Ui.trim(font, ChaosState.SETTING_LABELS[i], cardW - 70), x + 12, y + 9,
				Ui.withAlpha(TEXT, kk));
			Ui.text(g, font, Ui.trim(font, ChaosState.SETTING_SUBS[i], cardW - 70), x + 12, y + 24,
				Ui.withAlpha(MUTED, kk));
			drawSwitch(g, x + cardW - 46, y + (CARD_H - 16) / 2, 34, 16, on ? 1f : 0f, kk, a1);
			final int idx = i;
			hits.add(new Hit(x, y, cardW, CARD_H, (ax, ay) -> toggleSetting(idx)));
		}

		int rowsUsed = (ChaosState.SETTING_LABELS.length + cols - 1) / cols;
		int ay0 = contentTop + rowsUsed * (CARD_H + GAP) + 10;
		Ui.text(g, font, "AKZENT", mainX + 2, ay0, Ui.withAlpha(MUTED, k));
		for (int i = 0; i < ChaosState.ACCENTS.length; i++) {
			int sx = mainX + 2 + i * 32;
			int sy = ay0 + 14;
			Ui.round(g, sx, sy, 22, 22, Ui.withAlpha(ChaosState.ACCENTS[i][0], k));
			Ui.round(g, sx + 11, sy, 11, 22, Ui.withAlpha(ChaosState.ACCENTS[i][1], k));
			if (ChaosState.accentIdx == i && !ChaosState.settings[ChaosState.S_RAINBOW]) {
				Ui.outline(g, sx - 3, sy - 3, 28, 28, Ui.withAlpha(TEXT, k));
			}
			final int idx = i;
			hits.add(new Hit(sx, sy, 22, 22, (ax, ay) -> setAccent(idx)));
		}
	}

	private void toggleSetting(int idx) {
		ChaosState.settings[idx] = !ChaosState.settings[idx];
		boolean v = ChaosState.settings[idx];
		ChaosState.log(ChaosState.SETTING_LABELS[idx] + ": " + (v ? "an" : "aus"), v ? ON : WARN);
		if (idx != ChaosState.S_TOASTS) {
			ChaosState.toast(ChaosState.SETTING_LABELS[idx] + (v ? " an" : " aus"));
		}
	}

	private void setAccent(int idx) {
		ChaosState.accentIdx = idx;
		ChaosState.log("Akzentfarbe: Preset " + (idx + 1), INFO);
	}

	// ------------------------------------------------------------------
	// Details
	// ------------------------------------------------------------------

	private void drawDetails(GuiGraphics g, int mx, int my, float k, int a1, int a2) {
		if (detW <= 0) {
			return;
		}
		int x = detX;
		int y = bodyTop + 10;
		int w = detW;
		int h = bodyBottom - y - 10;
		Ui.round(g, x, y, w, h, Ui.withAlpha(0xFF0F0C1C, k * 0.9f));
		Ui.outline(g, x, y, w, h, Ui.withAlpha(BORDER, k));

		int ix = x + 14;
		int iw = w - 28;
		if (selected == null) {
			drawOverview(g, ix, y + 14, iw, k, a1);
			return;
		}

		FakeModule m = selected;
		Ui.text(g, font, Ui.trim(font, m.name, iw - 60), ix, y + 14, Ui.withAlpha(TEXT, k));
		String st = m.enabled ? "AKTIV" : "AUS";
		int stW = font.width(st) + 14;
		int stCol = m.enabled ? ON : MUTED;
		Ui.round(g, ix + iw - stW, y + 12, stW, 14, Ui.withAlpha(stCol, k * 0.18f));
		Ui.text(g, font, st, ix + iw - stW + 7, y + 15, Ui.withAlpha(stCol, k));

		int by = y + 40;
		boolean hovBig = inside(mx, my, ix, by, iw, 28);
		Ui.round(g, ix, by, iw, 28, Ui.withAlpha(0xFFFFFFFF, k * (hovBig ? 0.09f : 0.05f)));
		Ui.outline(g, ix, by, iw, 28, Ui.withAlpha(a1, k * (m.enabled ? 0.8f : 0.25f)));
		Ui.text(g, font, "Modul aktiviert", ix + 10, by + 10, Ui.withAlpha(TEXT, k));
		drawSwitch(g, ix + iw - 46, by + 7, 36, 14, m.toggle.get(), k, a1);
		hits.add(new Hit(ix, by, iw, 28, (ax, ay) -> ChaosState.toggle(m)));

		int dy = by + 38;
		for (String line : Ui.wrap(font, m.desc, iw, 3)) {
			Ui.text(g, font, line, ix, dy, Ui.withAlpha(MUTED, k));
			dy += 11;
		}

		int ky = dy + 8;
		Ui.text(g, font, "Taste", ix, ky + 3, Ui.withAlpha(MUTED, k));
		boolean bindingThis = binding == m;
		String keyTxt = bindingThis ? "Drücke ..." : FakeModule.keyName(m.key);
		int kw = Math.max(64, font.width(keyTxt) + 16);
		int kx = ix + iw - kw;
		Ui.round(g, kx, ky, kw, 16, Ui.withAlpha(bindingThis ? a1 : 0xFFFFFFFF, k * (bindingThis ? 0.25f : 0.06f)));
		Ui.outline(g, kx, ky, kw, 16, Ui.withAlpha(bindingThis ? a1 : BORDER, k));
		Ui.text(g, font, keyTxt, kx + (kw - font.width(keyTxt)) / 2, ky + 4,
			Ui.withAlpha(bindingThis ? TEXT : a2, k));
		hits.add(new Hit(kx, ky, kw, 16, (ax, ay) -> {
			binding = m;
			ChaosState.log("Warte auf Taste für " + m.name, INFO);
		}));

		int sy = ky + 30;
		for (int i = 0; i < m.sliders.size(); i++) {
			FakeModule.Slider s = m.sliders.get(i);
			Ui.text(g, font, s.label, ix, sy, Ui.withAlpha(MUTED, k));
			String val = s.display();
			Ui.text(g, font, val, ix + iw - font.width(val), sy, Ui.withAlpha(TEXT, k));
			int bar = sy + 14;
			Ui.round(g, ix, bar, iw, 6, Ui.withAlpha(0xFFFFFFFF, k * 0.08f));
			int fw = Math.max(0, Math.round((float) s.fraction() * iw));
			Ui.round(g, ix, bar, fw, 6, Ui.withAlpha(a1, k));
			int knobX = ix + Math.min(iw - 6, Math.max(0, fw - 3));
			Ui.round(g, knobX, bar - 3, 6, 12, Ui.withAlpha(0xFFFFFFFF, k));
			final FakeModule.Slider sl = s;
			hits.add(new Hit(ix, sy + 10, iw, 16, (ax, ay) -> {
				dragSlider = sl;
				dragX = ix;
				dragW = iw;
				sl.setFraction((ax - ix) / (double) iw);
			}));
			sy += 36;
		}

		sy += 2;
		for (int i = 0; i < m.checks.size(); i++) {
			FakeModule.Check c = m.checks.get(i);
			float ca = c.anim.get();
			Ui.round(g, ix, sy, 12, 12, Ui.withAlpha(0xFFFFFFFF, k * 0.06f));
			Ui.outline(g, ix, sy, 12, 12, Ui.withAlpha(a1, k * (0.3f + 0.5f * ca)));
			if (ca > 0.02f) {
				int inset = Math.round(3f * (1f - ca));
				Ui.round(g, ix + 3 + inset / 2, sy + 3 + inset / 2, 6 - inset, 6 - inset, Ui.withAlpha(a1, k * ca));
			}
			Ui.text(g, font, c.label, ix + 20, sy + 2, Ui.withAlpha(c.value ? TEXT : MUTED, k));
			final FakeModule.Check cc = c;
			hits.add(new Hit(ix, sy - 2, iw, 16, (ax, ay) -> {
				cc.value = !cc.value;
				ChaosState.log(m.name + ": " + cc.label + " = " + (cc.value ? "an" : "aus"),
					cc.value ? ON : WARN);
			}));
			sy += 18;
		}
	}

	private void drawOverview(GuiGraphics g, int x, int y, int w, float k, int a1) {
		Ui.text(g, font, "Übersicht", x, y, Ui.withAlpha(TEXT, k));
		int yy = y + 18;
		String[][] rows = {
			{"Aktive Module", ChaosState.activeTotal() + " / " + ChaosState.modules().size()},
			{"Kategorien", "9"},
			{"Konfigurationen", String.valueOf(ChaosState.configs().size())},
			{"Belegte Tasten", String.valueOf(ChaosState.keyCount())}
		};
		for (String[] r : rows) {
			Ui.text(g, font, r[0], x, yy, Ui.withAlpha(MUTED, k));
			Ui.text(g, font, r[1], x + w - font.width(r[1]), yy, Ui.withAlpha(a1, k));
			yy += 16;
		}
		yy += 12;
		String tip = "Klicke ein Modul an, um Slider und Tasten zu sehen. Rechts-Shift schließt das Menü.";
		for (String line : Ui.wrap(font, tip, w, 6)) {
			Ui.text(g, font, line, x, yy, Ui.withAlpha(MUTED, k));
			yy += 11;
		}
	}

	private void selectModule(FakeModule m) {
		selected = m;
	}

	// ------------------------------------------------------------------
	// Footer und Toasts
	// ------------------------------------------------------------------

	private void drawFooter(GuiGraphics g, long now, float k, int a1, int a2) {
		int bottom = wy + wh;
		Ui.rect(g, wx + 1, footTop, ww - 2, bottom - footTop - 1, Ui.withAlpha(0xFF000000, k * 0.4f));
		Ui.rect(g, wx + 1, footTop, ww - 2, 1, Ui.withAlpha(BORDER, k));

		Ui.text(g, font, "KONSOLE", wx + 14, footTop + 8, Ui.withAlpha(a1, k));

		int sw = 190;
		int sx = wx + ww - sw - 12;
		int sy = footTop + 8;
		int sh = FOOT_H - 16;
		Ui.round(g, sx, sy, sw, sh, Ui.withAlpha(0xFFFFFFFF, k * 0.04f));
		Ui.text(g, font, "FPS", sx + 8, sy + 6, Ui.withAlpha(MUTED, k));
		Ui.text(g, font, String.valueOf(ChaosState.fps()), sx + 30, sy + 6, Ui.withAlpha(a2, k));
		int base = sy + sh - 6;
		int plotH = sh - 22;
		for (int i = 0; i < ChaosState.FPS_SAMPLES; i++) {
			int v = ChaosState.fpsSample(i);
			float frac = Ui.clamp((v - 90) / 90f, 0.06f, 1f);
			int bh = Math.round(frac * plotH);
			int bx = sx + 8 + i * 3;
			Ui.rect(g, bx, base - bh, 2, bh, Ui.withAlpha(Ui.mix(a2, a1, i / (float) ChaosState.FPS_SAMPLES), k * 0.9f));
		}

		if (!ChaosState.settings[ChaosState.S_CONSOLE]) {
			Ui.text(g, font, "Konsole ausgeblendet (Settings)", wx + 14, footTop + 22, Ui.withAlpha(MUTED, k));
			return;
		}

		int cx = wx + 14;
		int cw = sx - cx - 14;
		int ly = footTop + 22;
		int lh = 11;
		int maxLines = Math.max(1, (bottom - ly - 4) / lh);
		List<ChaosState.LogLine> lines = ChaosState.console();
		int start = Math.max(0, lines.size() - maxLines);
		for (int i = start; i < lines.size(); i++) {
			ChaosState.LogLine l = lines.get(i);
			float a = Ui.easeOut((now - l.at) / 350f) * k;
			if (a <= 0.01f) {
				ly += lh;
				continue;
			}
			Ui.text(g, font, l.time, cx, ly, Ui.withAlpha(0xFF5D5778, a));
			Ui.text(g, font, Ui.trim(font, l.text, cw - 60), cx + 56, ly, Ui.withAlpha(l.color, a));
			ly += lh;
		}
	}

	private void drawToasts(GuiGraphics g, long now, float k, int a1) {
		List<ChaosState.Toast> toasts = ChaosState.toasts();
		int idx = 0;
		for (int i = toasts.size() - 1; i >= 0; i--) {
			ChaosState.Toast t = toasts.get(i);
			long age = now - t.created;
			float in = Ui.easeOut(age / 280f);
			float out = age > 2400L ? Ui.clamp((2800L - age) / 400f, 0f, 1f) : 1f;
			float a = in * out * k;
			if (a <= 0.02f) {
				continue;
			}
			int tw = font.width(t.text) + 24;
			int tx = wx + ww - tw - 14 + Math.round((1f - in) * 60f);
			int ty = footTop - 12 - (idx + 1) * 26;
			Ui.round(g, tx, ty, tw, 20, Ui.withAlpha(0xFF120E22, a * 0.95f));
			Ui.outline(g, tx, ty, tw, 20, Ui.withAlpha(a1, a * 0.6f));
			Ui.rect(g, tx + 1, ty + 1, 3, 18, Ui.withAlpha(a1, a));
			Ui.text(g, font, t.text, tx + 12, ty + 6, Ui.withAlpha(TEXT, a));
			idx++;
		}
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mx = (int) event.x();
		int my = (int) event.y();
		if (booting) {
			bootStart = System.currentTimeMillis() - BOOT_MS;
			return true;
		}
		binding = null;
		searchFocus = false;
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (h.contains(mx, my)) {
				h.action.run(mx, my);
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragSlider != null) {
			dragSlider.setFraction((event.x() - dragX) / (double) dragW);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragSlider != null) {
			ChaosState.log("Wert gesetzt: " + dragSlider.label + " = " + dragSlider.display(), INFO);
			dragSlider = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		boolean listTab = showingSearch || tab < ModuleCatalog.MODULE_TABS;
		if (listTab && inside((int) mouseX, (int) mouseY, mainX, contentTop, mainW, contentBottom - contentTop)) {
			scrollRow -= (int) Math.signum(scrollY);
			clampRow(lastRows, lastVisible);
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
				ChaosState.log("Keybind " + binding.name + " -> " + FakeModule.keyName(key), INFO);
			}
			binding = null;
			return true;
		}

		if (searchFocus) {
			if (key == 256) {
				if (!search.isEmpty()) {
					search = "";
				} else {
					searchFocus = false;
				}
				return true;
			}
			if (key == 259) {
				if (!search.isEmpty()) {
					search = search.substring(0, search.length() - 1);
				}
				return true;
			}
			if (key == 257 || key == 335) {
				searchFocus = false;
				return true;
			}
			char c = keyToChar(key);
			if (c != 0 && search.length() < 24) {
				search = search + c;
				scrollRow = 0;
				scrollAnim.snap(0f);
				return true;
			}
			return super.keyPressed(event);
		}

		if (key != 256) {
			for (FakeModule m : ChaosState.modules()) {
				if (m.key != 0 && m.key == key) {
					ChaosState.toggle(m);
				}
			}
		}
		return super.keyPressed(event);
	}

	private static char keyToChar(int key) {
		if (key >= 65 && key <= 90) {
			return (char) (key + 32);
		}
		if (key >= 48 && key <= 57) {
			return (char) key;
		}
		if (key == 32) {
			return ' ';
		}
		if (key == 45) {
			return '-';
		}
		if (key == 46) {
			return '.';
		}
		return 0;
	}

	private void clampRow(int rows, int vis) {
		int max = Math.max(0, rows - vis);
		scrollRow = Math.max(0, Math.min(max, scrollRow));
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
