package de.chaos.fakeclient.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Zeichen-Helfer. Alle Rechtecke werden ohne Überlappung gezeichnet,
 * damit transparente Farben nicht doppelt aufblenden.
 */
public final class Ui {

	private Ui() {
	}

	public static int argb(int a, int rgb) {
		return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
	}

	public static int alpha(int c) {
		return (c >>> 24) & 0xFF;
	}

	/** Multipliziert den Alpha-Kanal einer Farbe mit f (0..1). */
	public static int withAlpha(int c, float f) {
		return argb((int) (alpha(c) * clamp(f, 0f, 1f)), c);
	}

	public static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}

	public static float easeOut(float t) {
		float x = clamp(t, 0f, 1f);
		float inv = 1f - x;
		return 1f - inv * inv * inv;
	}

	public static int mix(int c1, int c2, float t) {
		float x = clamp(t, 0f, 1f);
		int a = (int) (((c1 >>> 24) & 0xFF) + (((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * x);
		int r = (int) (((c1 >>> 16) & 0xFF) + (((c2 >>> 16) & 0xFF) - ((c1 >>> 16) & 0xFF)) * x);
		int g = (int) (((c1 >>> 8) & 0xFF) + (((c2 >>> 8) & 0xFF) - ((c1 >>> 8) & 0xFF)) * x);
		int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * x);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int hsv(float h, float s, float v) {
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

	public static void rect(GuiGraphics g, int x, int y, int w, int h, int c) {
		if (w <= 0 || h <= 0 || alpha(c) == 0) {
			return;
		}
		g.fill(x, y, x + w, y + h, c);
	}

	/** Rechteck mit 1px abgeschnittenen Ecken. */
	public static void round(GuiGraphics g, int x, int y, int w, int h, int c) {
		if (w <= 0 || h <= 0 || alpha(c) == 0) {
			return;
		}
		if (w <= 2 || h <= 2) {
			g.fill(x, y, x + w, y + h, c);
			return;
		}
		g.fill(x + 1, y, x + w - 1, y + 1, c);
		g.fill(x, y + 1, x + w, y + h - 1, c);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, c);
	}

	public static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
		if (w <= 1 || h <= 1 || alpha(c) == 0) {
			return;
		}
		g.fill(x, y, x + w, y + 1, c);
		g.fill(x, y + h - 1, x + w, y + h, c);
		g.fill(x, y + 1, x + 1, y + h - 1, c);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
	}

	/** Weicher Leuchtrand um ein Rechteck. */
	public static void glow(GuiGraphics g, int x, int y, int w, int h, int c, int spread) {
		for (int i = 1; i <= spread; i++) {
			float f = 1f - (i - 1) / (float) spread;
			outline(g, x - i, y - i, w + 2 * i, h + 2 * i, withAlpha(c, f * 0.6f));
		}
	}

	/** Weiche Fläche aus geschichteten Rechtecken. */
	public static void blob(GuiGraphics g, int cx, int cy, int r, int c, int layers) {
		int a = alpha(c) / layers;
		for (int k = 0; k < layers; k++) {
			int rr = r * (layers - k) / layers;
			round(g, cx - rr, cy - rr, rr * 2, rr * 2, argb(a, c));
		}
	}

	public static void text(GuiGraphics g, Font font, String s, int x, int y, int c) {
		// Vanilla macht Text mit Alpha 0 wieder undurchsichtig, daher hier auslassen.
		if (alpha(c) < 6) {
			return;
		}
		g.drawString(font, s, x, y, c);
	}

	public static String trim(Font font, String s, int maxW) {
		if (font.width(s) <= maxW) {
			return s;
		}
		String out = s;
		while (out.length() > 1 && font.width(out + "...") > maxW) {
			out = out.substring(0, out.length() - 1);
		}
		return out + "...";
	}

	public static List<String> wrap(Font font, String text, int maxW, int maxLines) {
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
				if (out.size() >= maxLines) {
					return out;
				}
			}
		}
		if (!cur.isEmpty() && out.size() < maxLines) {
			out.add(cur);
		}
		return out;
	}
}
