package de.chaos.fakeclient.gui;

/**
 * Weiche Animation: folgt dem Ziel exponentiell, unabhängig von der Framerate.
 */
public final class Anim {
	private float value;
	private float target;

	public Anim(float start) {
		this.value = start;
		this.target = start;
	}

	public void to(float t) {
		this.target = t;
	}

	public void snap(float v) {
		this.value = v;
		this.target = v;
	}

	public void update(float dt, float speed) {
		float k = 1f - (float) Math.exp(-speed * dt);
		value += (target - value) * k;
	}

	public float get() {
		return value;
	}
}
