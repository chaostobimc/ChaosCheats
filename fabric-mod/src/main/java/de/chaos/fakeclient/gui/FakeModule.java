package de.chaos.fakeclient.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Ein Modul der Fake-Liste. Hat nur Zustand und Animationen, keinerlei Wirkung. */
public final class FakeModule {

	public static final class Slider {
		public final String label;
		public final double min;
		public final double max;
		public final double step;
		public final String unit;
		public double value;

		Slider(String label, double min, double max, double step, String unit, Random rnd) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.step = step;
			this.unit = unit;
			int steps = (int) Math.round((max - min) / step);
			this.value = min + rnd.nextInt(steps + 1) * step;
		}

		public double fraction() {
			return (value - min) / (max - min);
		}

		public void setFraction(double f) {
			double clamped = Math.max(0.0, Math.min(1.0, f));
			double raw = min + clamped * (max - min);
			double snapped = Math.round(raw / step) * step;
			value = Math.max(min, Math.min(max, snapped));
		}

		public String display() {
			if (step < 1.0) {
				return String.format(Locale.ROOT, "%.1f%s", value, unit);
			}
			return String.format(Locale.ROOT, "%d%s", Math.round(value), unit);
		}
	}

	public static final class Check {
		public final String label;
		public boolean value;
		public final Anim anim = new Anim(0f);

		Check(String label, boolean value) {
			this.label = label;
			this.value = value;
			this.anim.snap(value ? 1f : 0f);
		}
	}

	private static final String[] SLIDER_LABELS = {"Reichweite", "Verzögerung", "Glättung", "FOV", "Intensität"};
	private static final double[] SLIDER_MIN = {1.0, 0.0, 0.0, 10.0, 0.0};
	private static final double[] SLIDER_MAX = {6.0, 500.0, 100.0, 180.0, 10.0};
	private static final double[] SLIDER_STEP = {0.1, 5.0, 1.0, 1.0, 0.5};
	private static final String[] SLIDER_UNIT = {" m", " ms", "%", "\u00B0", ""};

	private static final String[] CHECK_LABELS = {
		"Teams ignorieren", "Nur sichtbare Ziele", "Stealth-Modus",
		"Zufallswerte", "Tooltip anzeigen", "Sound-Feedback"
	};

	/** GLFW-Codes: R G V X B N K F H J Z */
	private static final int[] KEY_CODES = {82, 71, 86, 88, 66, 78, 75, 70, 72, 74, 90};

	public final String category;
	public final String name;
	public final String desc;
	public boolean enabled;
	/** GLFW-Tastencode, 0 = keiner. */
	public int key;
	public final List<Slider> sliders = new ArrayList<>();
	public final List<Check> checks = new ArrayList<>();

	/** Animationen: Schalter, Hover-Glow und Einblendung in der HUD-Liste. */
	public final Anim toggle = new Anim(0f);
	public final Anim hover = new Anim(0f);
	public final Anim hud = new Anim(0f);

	public FakeModule(String category, String name, String desc) {
		this.category = category;
		this.name = name;
		this.desc = desc;

		Random rnd = new Random((category + "|" + name).hashCode());
		this.enabled = rnd.nextFloat() < 0.22f;
		this.key = rnd.nextFloat() < 0.25f ? KEY_CODES[rnd.nextInt(KEY_CODES.length)] : 0;

		toggle.snap(enabled ? 1f : 0f);
		hud.snap(enabled ? 1f : 0f);

		int[] order = {0, 1, 2, 3, 4};
		for (int i = order.length - 1; i > 0; i--) {
			int j = rnd.nextInt(i + 1);
			int tmp = order[i];
			order[i] = order[j];
			order[j] = tmp;
		}
		int sliderCount = 2 + rnd.nextInt(2);
		for (int i = 0; i < sliderCount; i++) {
			int k = order[i];
			sliders.add(new Slider(SLIDER_LABELS[k], SLIDER_MIN[k], SLIDER_MAX[k], SLIDER_STEP[k], SLIDER_UNIT[k], rnd));
		}

		for (String label : CHECK_LABELS) {
			if (checks.size() >= 3) {
				break;
			}
			if (rnd.nextFloat() < 0.3f) {
				checks.add(new Check(label, rnd.nextBoolean()));
			}
		}
	}

	public static String keyName(int code) {
		if (code == 0) {
			return "-";
		}
		if ((code >= 65 && code <= 90) || (code >= 48 && code <= 57)) {
			return String.valueOf((char) code);
		}
		switch (code) {
			case 256: return "ESC";
			case 260: return "INS";
			case 32: return "SPACE";
			case 340: return "LSHIFT";
			case 344: return "RSHIFT";
			default: return "K" + code;
		}
	}
}
