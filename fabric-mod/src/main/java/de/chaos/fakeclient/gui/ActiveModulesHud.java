package de.chaos.fakeclient.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Die Liste aktiver Module oben rechts im Spiel. Reine Anzeige:
 * Zeilen gleiten beim Ein- und Ausschalten, längere Namen stehen oben.
 */
public final class ActiveModulesHud {

	private static final int ROW_H = 12;
	private static final int ROW_GAP = 2;
	private static final int TOP = 6;
	private static final int RIGHT = 6;
	private static final int HEADER_H = 12;
	private static final int TEXT = 0xFFE6E1F5;
	private static final int PANEL = 0xFF0A0812;

	/** Vertikale Position jeder Zeile, damit sich Reihenfolge-Wechsel weich bewegen. */
	private static final Map<FakeModule, Anim> ROW_Y = new IdentityHashMap<>();

	private ActiveModulesHud() {
	}

	public static void render(GuiGraphics g) {
		if (!ChaosState.settings[ChaosState.S_HUD]) {
			return;
		}
		ChaosState.frame();
		Font font = Minecraft.getInstance().font;
		int sw = g.guiWidth();

		List<FakeModule> rows = new ArrayList<>();
		for (FakeModule m : ChaosState.modules()) {
			if (m.enabled || m.hud.get() > 0.02f) {
				rows.add(m);
			}
		}
		if (rows.isEmpty()) {
			ROW_Y.clear();
			return;
		}
		rows.sort(Comparator
			.comparingInt((FakeModule m) -> -font.width(m.name))
			.thenComparing(m -> m.name));

		ROW_Y.keySet().removeIf(m -> !rows.contains(m));

		float dt = ChaosState.dt();
		int accent = ChaosState.accent();
		int accent2 = ChaosState.accent2();
		int count = ChaosState.activeTotal();

		int headerAlpha = count > 0 ? 1 : 0;
		String head = "AKTIVE MODULE";
		String headCount = String.valueOf(count);
		int headX = sw - RIGHT - font.width(headCount) - 6 - font.width(head);
		Ui.text(g, font, head, headX, TOP + 1, Ui.withAlpha(0xFF7D7896, headAlpha));
		Ui.text(g, font, headCount, sw - RIGHT - font.width(headCount), TOP + 1, Ui.withAlpha(accent, headAlpha));
		Ui.rect(g, sw - RIGHT - 80, TOP + HEADER_H, 80, 1, Ui.withAlpha(accent, 0.35f * headAlpha));

		int firstY = TOP + HEADER_H + 4;
		int n = rows.size();
		for (int i = 0; i < n; i++) {
			FakeModule m = rows.get(i);
			float a = m.hud.get();
			if (a <= 0.01f) {
				continue;
			}
			float targetY = firstY + i * (ROW_H + ROW_GAP);
			Anim y = ROW_Y.get(m);
			if (y == null) {
				y = new Anim(targetY);
				ROW_Y.put(m, y);
			}
			y.to(targetY);
			y.update(dt, 12f);

			int textW = font.width(m.name);
			int w = textW + 14;
			int slide = Math.round((1f - Ui.easeOut(a)) * 26f);
			int x = sw - RIGHT - w + slide;
			int ry = Math.round(y.get());
			int barCol = Ui.mix(accent, accent2, n <= 1 ? 0f : i / (float) (n - 1));

			Ui.rect(g, x, ry, w, ROW_H, Ui.withAlpha(PANEL, a * 0.62f));
			Ui.rect(g, x + w - 2, ry, 2, ROW_H, Ui.withAlpha(barCol, a));
			Ui.text(g, font, m.name, x + 6, ry + 2, Ui.withAlpha(TEXT, a));
		}
	}
}
