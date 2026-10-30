package com.skybreaker.client;

import com.skybreaker.FxPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic overlay. Sequence: cut from black, glitching title card, letterbox + targeting HUD with the giant
 * countdown, then FIRE. The Singularity Cannon adds a darkening vignette and a status readout while the black hole
 * drags everything in, and a COLLAPSE call-out. A white impact flash sits on top of everything.
 */
final class CinematicHud {
	private static final String[] NAMES = {"", "ORBITAL RAILGUN", "SATELLITE LASER", "METEOR BARRAGE", "SINGULARITY CANNON"};
	private static final String[] SUBS = {"", "KINETIC ORBITAL BOMBARDMENT", "SUSTAINED SATELLITE BEAM",
		"KINETIC DEBRIS FIELD INBOUND", "SPACETIME COLLAPSE IMMINENT"};
	private static final int CYAN = 0xFF7FE0FF;
	private static final int CYAN_DIM = 0xFF3FA8D8;
	private static final int VIOLET = 0xFFD08CFF;
	private static final int INTRO = 46;
	/** Ticks over which the camera is eased toward the target. */
	private static final int CAMERA_TICKS = 38;

	private static boolean active = false;
	private static int age = 0;
	private static int total = 60;
	private static int extra = 0;
	private static int weapon = 1;
	private static double distance = 0;
	private static double tx, ty, tz;

	private static float flashAlpha = 0f;
	private static float flashDecay = 0.05f;

	static void start(FxPayload p) {
		active = true;
		age = 0;
		total = Math.max(1, p.a());
		weapon = Math.max(1, Math.min(4, p.b()));
		extra = p.data().length > 0 ? Math.max(0, (int) p.data()[0]) : 0;
		distance = p.f();
		tx = p.x();
		ty = p.y();
		tz = p.z();
	}

	static void flash(float intensity, int ticks) {
		if (intensity > flashAlpha) {
			flashAlpha = Math.min(1f, intensity);
		}
		flashDecay = Math.max(0.01f, flashAlpha / Math.max(1, ticks));
	}

	static void clear() {
		active = false;
		flashAlpha = 0f;
	}

	static void tick() {
		if (active) {
			age++;
			if (age > total + extra + 26) {
				active = false;
			}
		}
		if (flashAlpha > 0f) {
			flashAlpha = Math.max(0f, flashAlpha - flashDecay);
		}
	}

	/** True while the camera should still be easing toward the target. */
	static boolean cameraActive() {
		return active && age < CAMERA_TICKS;
	}

	/** The point the camera looks at (for the black hole: the point where it hangs). */
	static Vec3 lookTarget() {
		return new Vec3(tx, ty + (weapon == 4 ? FxPayload.BH_HEIGHT : 0.0), tz);
	}

	private static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static String spaced(String s) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			b.append(s.charAt(i));
			if (i < s.length() - 1 && s.charAt(i) != ' ') {
				b.append(' ');
			}
		}
		return b.toString();
	}

	/** Darkens the screen edges with stacked translucent bands. */
	private static void vignette(GuiGraphicsExtractor g, int w, int h, int alphaPerLayer, int rgb) {
		for (int i = 0; i < 9; i++) {
			int bandY = (i + 1) * h / 34;
			int bandX = (i + 1) * w / 48;
			int c = argb(alphaPerLayer, rgb);
			g.fill(0, 0, w, bandY, c);
			g.fill(0, h - bandY, w, h, c);
			g.fill(0, bandY, bandX, h - bandY, c);
			g.fill(w - bandX, bandY, w, h - bandY, c);
		}
	}

	static void render(GuiGraphicsExtractor g, DeltaTracker tracker) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		var font = Minecraft.getInstance().font;

		if (active) {
			boolean bh = weapon == 4;
			int pullEnd = total + extra;
			boolean pulling = bh && age >= total && age < pullEnd;

			float in = Math.min(1f, age / 10f);
			float out = Math.max(0f, Math.min(1f, (age - (pullEnd + 10)) / 14f));
			float bar = in * (1f - out);
			int barH = (int) (h * 0.12f * bar);

			// tint: cold blue, red in the final second; the black hole goes deep violet instead
			int remaining = total - age;
			if (pulling) {
				float pp = (age - total) / (float) Math.max(1, extra);
				g.fill(0, 0, w, h, argb((int) (20 + 46 * pp), 0x20003A));
				vignette(g, w, h, (int) (10 + 22 * pp), 0x0A0014);
			} else if (remaining > 0 && remaining < 20) {
				g.fill(0, 0, w, h, argb((int) (46 * (1f - remaining / 20f)), bh ? 0x9030FF : 0xFF2030));
			} else {
				g.fill(0, 0, w, h, argb((int) (14 * bar), bh ? 0x7030FF : 0x2090FF));
			}
			vignette(g, w, h, (int) (7 * bar), 0x000000);

			g.fill(0, 0, w, barH, 0xFF000000);
			g.fill(0, h - barH, w, h, 0xFF000000);

			int accent = bh ? VIOLET : CYAN;
			int accentDim = bh ? 0xFF9A5CD0 : CYAN_DIM;
			int bracketRgb = bh ? 0xC070FF : 0x5CD6FF;

			if (bar > 0.6f) {
				g.centeredText(font, "SKYBREAKER // " + NAMES[weapon], w / 2, Math.max(2, barH / 2 - 4), accent);
				String line = pulling
					? "SINGULARITY ACTIVE   T+" + (age - total) / 20 + "s   EVENT HORIZON EXPANDING"
					: "TGT " + (int) Math.floor(tx) + " " + (int) Math.floor(ty) + " " + (int) Math.floor(tz) + "   RANGE " + (int) distance + " m";
				g.centeredText(font, line, w / 2, h - barH / 2 - 4, accentDim);
			}

			// corner brackets
			int m = barH + 12;
			int len = 26;
			int col = argb(150 + (int) (90 * Math.abs(Math.sin(age * 0.25))), bracketRgb);
			bracket(g, m, m, len, 1, 1, col);
			bracket(g, w - m, m, len, -1, 1, col);
			bracket(g, m, h - m, len, 1, -1, col);
			bracket(g, w - m, h - m, len, -1, -1, col);

			// scanline sweeping down the screen
			int span = Math.max(1, h - 2 * barH);
			int sy = barH + (age * 9) % span;
			g.fill(0, sy, w, sy + 1, argb(70, bracketRgb));

			if (age < total) {
				float prog = age / (float) total;
				int secs = (total - age + 19) / 20;
				float frac = ((total - age) % 20) / 20f;
				float scale = 4f + 3f * frac;
				g.pose().pushMatrix();
				g.pose().translate(w / 2f, h / 2f - 34f);
				g.pose().scale(scale);
				g.centeredText(font, String.valueOf(secs), 0, 0, remaining < 20 ? 0xFFFF5060 : 0xFFFFFFFF);
				g.pose().popMatrix();

				int bw = 150;
				int x0 = w / 2 - bw / 2;
				int y0 = h / 2 + 28;
				g.fill(x0 - 1, y0 - 1, x0 + bw + 1, y0 + 5, 0xAA000000);
				g.fill(x0, y0, x0 + (int) (bw * prog), y0 + 4, accent);
				String lock = bh ? (prog > 0.92f ? "TEAR STABLE" : "OPENING  " + (int) (prog * 100) + "%")
					: (prog > 0.92f ? "TARGET LOCKED" : "LOCKING  " + (int) (prog * 100) + "%");
				g.centeredText(font, lock, w / 2, y0 + 10, accent);
			} else if (pulling) {
				float pp = (age - total) / (float) Math.max(1, extra);
				int bw = 190;
				int x0 = w / 2 - bw / 2;
				int y0 = h - barH - 26;
				g.fill(x0 - 1, y0 - 1, x0 + bw + 1, y0 + 5, 0xAA000000);
				g.fill(x0, y0, x0 + (int) (bw * pp), y0 + 4, argb(255, 0xC070FF));
				g.centeredText(font, pp > 0.85f ? "COLLAPSE IMMINENT" : "EVENT HORIZON  " + (int) (pp * 100) + "%", w / 2, y0 + 8, VIOLET);
			} else if (age < pullEnd + 14) {
				float pop = 1f - (age - pullEnd) / 14f;
				g.pose().pushMatrix();
				g.pose().translate(w / 2f, h / 2f - 20f);
				g.pose().scale(5f + 3f * pop);
				g.centeredText(font, bh ? "COLLAPSE" : "FIRE", 0, 0, argb((int) (255 * Math.min(1f, pop + 0.3f)), bh ? 0xE0A0FF : 0xFF4050));
				g.pose().popMatrix();
			}

			if (age < total) {
				// every weapon opens with its own 3D orbital strike view (replaces the old title card)
				OrbitalView.render(g, w, h, weapon, age, total, (int) Math.floor(tx), (int) Math.floor(ty), (int) Math.floor(tz), (int) distance);
			}
		}

		if (flashAlpha > 0.01f) {
			g.fill(0, 0, w, h, argb((int) (Math.min(1f, flashAlpha) * 255), 0xEAF6FF));
		}
	}

	/** Cut from black into a glitching title card. */
	private static void intro(GuiGraphicsExtractor g, int w, int h, int accent) {
		var font = Minecraft.getInstance().font;
		int black = (int) (255 * Math.max(0f, 1f - age / 14f));
		if (black > 0) {
			g.fill(0, 0, w, h, argb(black, 0x000000));
		}
		float fade = Math.min(clamp01((age - 3) / 8f), clamp01((INTRO - age) / 9f));
		if (fade <= 0f) {
			return;
		}
		int a = (int) (255 * fade);
		String title = spaced(NAMES[weapon]);
		boolean glitch = age >= 5 && age <= 13 && age % 2 == 1;
		float scale = 2.6f + 0.5f * clamp01(age / (float) INTRO);
		int cy = h / 2 - 30;

		g.centeredText(font, "S K Y B R E A K E R   I N D U S T R I E S", w / 2, cy - 22, argb((int) (a * 0.7f), 0xFFFFFF));
		if (glitch) {
			g.pose().pushMatrix();
			g.pose().translate(w / 2f - 3f, cy);
			g.pose().scale(scale);
			g.centeredText(font, title, 0, 0, argb((int) (a * 0.55f), 0xFF2040));
			g.pose().popMatrix();
			g.pose().pushMatrix();
			g.pose().translate(w / 2f + 3f, cy);
			g.pose().scale(scale);
			g.centeredText(font, title, 0, 0, argb((int) (a * 0.55f), 0x2080FF));
			g.pose().popMatrix();
		}
		g.pose().pushMatrix();
		g.pose().translate(w / 2f, cy);
		g.pose().scale(scale);
		g.centeredText(font, title, 0, 0, argb(a, accent & 0xFFFFFF));
		g.pose().popMatrix();

		int lineW = (int) (w * 0.34f * fade);
		g.fill(w / 2 - lineW, cy + 22, w / 2 + lineW, cy + 23, argb(a, accent & 0xFFFFFF));
		g.centeredText(font, SUBS[weapon], w / 2, cy + 30, argb((int) (a * 0.85f), accent & 0xFFFFFF));
		g.centeredText(font, "AUTHORIZATION CONFIRMED", w / 2, cy + 44, argb((int) (a * 0.5f), 0xFFFFFF));

		// faint scanlines over the card
		for (int y = 0; y < h; y += 4) {
			g.fill(0, y, w, y + 1, argb((int) (22 * fade), 0x000000));
		}
	}


	private static void bracket(GuiGraphicsExtractor g, int x, int y, int len, int dx, int dy, int color) {
		// horizontal arm
		g.fill(Math.min(x, x + dx * len), Math.min(y, y + dy * 2), Math.max(x, x + dx * len), Math.max(y, y + dy * 2), color);
		// vertical arm
		g.fill(Math.min(x, x + dx * 2), Math.min(y, y + dy * len), Math.max(x, x + dx * 2), Math.max(y, y + dy * len), color);
	}

	private CinematicHud() {
	}
}
