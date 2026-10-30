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

			if (weapon == 2) {
				// the Satellite Laser opens with the orbital strike-warning view instead of the title card
				if (age < total) {
					orbital(g, w, h);
				}
			} else if (age < INTRO) {
				intro(g, w, h, accent);
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


	// ---------------------------------------------------------------------------------------------------------
	// Orbital strike-warning view (Satellite Laser): Earth from orbit, satellite firing a red beam at the target,
	// cyan targeting HUD and a blinking "PLANETARY STRIKE IMMINENT" panel.
	// ---------------------------------------------------------------------------------------------------------

	private static int hash(int n) {
		n = (n ^ 61) ^ (n >>> 16);
		n *= 9;
		n ^= n >>> 4;
		n *= 0x27d4eb2d;
		n ^= n >>> 15;
		return n & 0x7FFFFFFF;
	}

	private static int mixRgb(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
		int gr = (int) (((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1f - t) + (b & 255) * t);
		return (r << 16) | (gr << 8) | bl;
	}

	private static void textL(GuiGraphicsExtractor g, String s, int x, int y, int color) {
		var f = Minecraft.getInstance().font;
		g.centeredText(f, s, x + f.width(s) / 2, y, color);
	}

	private static void textR(GuiGraphicsExtractor g, String s, int xRight, int y, int color) {
		var f = Minecraft.getInstance().font;
		g.centeredText(f, s, xRight - f.width(s) / 2, y, color);
	}

	private static void textScaledL(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color) {
		var f = Minecraft.getInstance().font;
		g.pose().pushMatrix();
		g.pose().translate(x + f.width(s) * scale / 2f, y);
		g.pose().scale(scale);
		g.centeredText(f, s, 0, 0, color);
		g.pose().popMatrix();
	}

	private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int thick, int step, int color) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		int n = Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy)) / step));
		int half = thick / 2;
		for (int i = 0; i <= n; i++) {
			int x = (int) (x0 + dx * i / n);
			int y = (int) (y0 + dy * i / n);
			g.fill(x - half, y - half, x - half + thick, y - half + thick, color);
		}
	}

	private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int h, int color) {
		int top = (int) Math.max(0, cy - r);
		for (int y = top; y < h; y += 2) {
			float dy = y - cy;
			if (Math.abs(dy) >= r) {
				continue;
			}
			int hw = (int) Math.sqrt(r * r - dy * dy);
			g.fill((int) cx - hw, y, (int) cx + hw, y + 2, color);
		}
	}

	private static boolean insideDisc(float x, float y, float cx, float cy, float r) {
		float dx = x - cx;
		float dy = y - cy;
		return dx * dx + dy * dy < r * r;
	}

	private static void orbital(GuiGraphicsExtractor g, int w, int h) {
		var font = Minecraft.getInstance().font;
		float prog = clamp01(age / (float) total);
		float oa = Math.min(clamp01(age / 8f), clamp01((total - age) / 10f));
		if (oa <= 0f) {
			return;
		}
		int remaining = Math.max(0, total - age);

		// ---- space ----
		g.fill(0, 0, w, h, argb((int) (255 * oa), 0x02040C));
		for (int i = 0; i < 6; i++) {
			g.fill(0, h * i / 6, w, h * (i + 1) / 6, argb((int) (60 * oa), mixRgb(0x050A1E, 0x0A1030, i / 5f)));
		}
		for (int i = 0; i < 110; i++) {
			int sx = hash(i * 3) % Math.max(1, w);
			int sy = hash(i * 3 + 1) % Math.max(1, h);
			float tw = 0.5f + 0.5f * (float) Math.sin(age * 0.2 + i);
			int a = (int) ((90 + 140 * tw) * oa * (0.4f + 0.6f * (hash(i * 3 + 2) % 100) / 100f));
			int sz = hash(i * 7) % 11 == 0 ? 2 : 1;
			g.fill(sx, sy, sx + sz, sy + sz, argb(a, 0xDDEEFF));
		}

		// ---- Earth (limb curving up from the lower left) ----
		float cx = w * 0.40f;
		float cy = h * 1.55f;
		float R = h * 1.0f;
		disc(g, cx, cy, R + 14, h, argb((int) (22 * oa), 0x3CB4FF));
		disc(g, cx, cy, R + 8, h, argb((int) (38 * oa), 0x3CB4FF));
		disc(g, cx, cy, R + 4, h, argb((int) (70 * oa), 0x78D2FF));
		disc(g, cx, cy, R + 1.5f, h, argb((int) (150 * oa), 0xB4EBFF));
		int top = (int) (cy - R);
		for (int y = Math.max(0, top); y < h; y += 2) {
			float dy = y - cy;
			if (Math.abs(dy) >= R) {
				continue;
			}
			int hw = (int) Math.sqrt(R * R - dy * dy);
			float t = clamp01((y - top) / (float) Math.max(1, h - top));
			int rgb = mixRgb(0x2E8CE0, 0x061C48, t);
			g.fill((int) cx - hw, y, (int) cx + hw, y + 2, argb((int) (255 * oa), rgb));
		}
		// land + cloud + city lights
		for (int i = 0; i < 90; i++) {
			int bx = (int) (cx - R + (hash(i * 5) % 1000) / 1000f * 2f * R);
			int by = top + (hash(i * 5 + 1) % 1000) * (h - top) / 1000;
			int bw = 6 + hash(i * 5 + 2) % 22;
			int bh = 3 + hash(i * 5 + 3) % 8;
			if (!insideDisc(bx, by, cx, cy, R - 4) || !insideDisc(bx + bw, by + bh, cx, cy, R - 4)
				|| !insideDisc(bx + bw, by, cx, cy, R - 4) || !insideDisc(bx, by + bh, cx, cy, R - 4)) {
				continue;
			}
			float t = clamp01((by - top) / (float) Math.max(1, h - top));
			int land = hash(i * 5 + 4) % 3 == 0 ? 0x7A6A44 : 0x3F7A46;
			g.fill(bx, by, bx + bw, by + bh, argb((int) (170 * oa * (1f - 0.65f * t)), land));
			if (hash(i * 5 + 4) % 4 == 0) {
				g.fill(bx - 3, by - 2, bx + bw + 4, by + 1, argb((int) (60 * oa * (1f - 0.5f * t)), 0xFFFFFF));
			}
			if (t > 0.45f && hash(i * 5 + 4) % 2 == 0) {
				g.fill(bx + 2, by + 1, bx + 3, by + 2, argb((int) (230 * oa), 0xFFD080));
			}
		}

		// ---- target + tactical grid ----
		float tX = w * 0.47f;
		float tY = h * 0.70f;
		int gx0 = (int) tX - 90;
		int gx1 = (int) tX + 90;
		int gy0 = (int) tY - 62;
		int gy1 = (int) tY + 62;
		int gridA = (int) ((40 + 30 * Math.sin(age * 0.2)) * oa);
		for (int x = gx0; x <= gx1; x += 10) {
			float dx = x - cx;
			float lim = (float) Math.sqrt(Math.max(0, R * R - dx * dx));
			int ya = (int) Math.max(gy0, cy - lim);
			if (ya < gy1) {
				g.fill(x, ya, x + 1, gy1, argb(gridA, 0xA0F0FF));
			}
		}
		for (int y = gy0; y <= gy1; y += 10) {
			float dy = y - cy;
			float lim = (float) Math.sqrt(Math.max(0, R * R - dy * dy));
			int xa = (int) Math.max(gx0, cx - lim);
			int xb = (int) Math.min(gx1, cx + lim);
			if (xa < xb) {
				g.fill(xa, y, xb, y + 1, argb(gridA, 0xA0F0FF));
			}
		}
		// shrinking lock-on reticle
		int rs = (int) (46 * (1f - prog) + 9);
		int rc = argb((int) (230 * oa), prog > 0.8f ? 0xFF4050 : 0x7FE8FF);
		bracket(g, (int) tX - rs, (int) tY - rs, 7, 1, 1, rc);
		bracket(g, (int) tX + rs, (int) tY - rs, 7, -1, 1, rc);
		bracket(g, (int) tX - rs, (int) tY + rs, 7, 1, -1, rc);
		bracket(g, (int) tX + rs, (int) tY + rs, 7, -1, -1, rc);
		g.fill((int) tX - rs - 8, (int) tY, (int) tX - rs + 2, (int) tY + 1, rc);
		g.fill((int) tX + rs - 2, (int) tY, (int) tX + rs + 8, (int) tY + 1, rc);
		g.fill((int) tX, (int) tY - rs - 8, (int) tX + 1, (int) tY - rs + 2, rc);
		g.fill((int) tX, (int) tY + rs - 2, (int) tX + 1, (int) tY + rs + 8, rc);
		textL(g, "TGT", (int) tX + rs + 6, (int) tY - rs - 2, argb((int) (220 * oa), 0xFFFFFF));

		// ---- satellite ----
		int sx = (int) (w * 0.82f);
		int sy = (int) (h * 0.12f + Math.sin(age * 0.12) * 1.5);
		int panel = argb((int) (255 * oa), 0x2A5FA8);
		int panelLine = argb((int) (255 * oa), 0x0C1C3C);
		for (int side = -1; side <= 1; side += 2) {
			int px0 = side < 0 ? sx - 52 : sx + 12;
			g.fill(px0, sy - 5, px0 + 40, sy + 5, panel);
			for (int k = 0; k <= 40; k += 8) {
				g.fill(px0 + k, sy - 5, px0 + k + 1, sy + 5, panelLine);
			}
			g.fill(px0, sy, px0 + 40, sy + 1, panelLine);
			g.fill(px0, sy - 5, px0 + 40, sy - 4, argb((int) (180 * oa), 0x9CD0FF));
		}
		g.fill(sx - 12, sy - 7, sx + 12, sy + 7, argb((int) (255 * oa), 0xB4BCC8));
		g.fill(sx - 12, sy - 7, sx + 12, sy - 5, argb((int) (255 * oa), 0xE6ECF4));
		g.fill(sx - 12, sy + 5, sx + 12, sy + 7, argb((int) (255 * oa), 0x6C7480));
		g.fill(sx - 3, sy - 13, sx + 3, sy - 7, argb((int) (255 * oa), 0xD0D6E0));
		g.fill(sx - 1, sy - 17, sx + 1, sy - 13, argb((int) (255 * oa), 0x8890A0));

		// ---- beam ----
		float ex = sx - 12;
		float ey = sy + 7;
		float beamP = clamp01((prog - 0.18f) / 0.30f);
		float bx = ex + (tX - ex) * beamP;
		float by = ey + (tY - ey) * beamP;
		float pulse = 0.75f + 0.25f * (float) Math.sin(age * 1.3);
		if (beamP > 0f) {
			line(g, ex, ey, bx, by, 9, 2, argb((int) (40 * oa * pulse), 0xFF2030));
			line(g, ex, ey, bx, by, 5, 2, argb((int) (110 * oa * pulse), 0xFF3040));
			line(g, ex, ey, bx, by, 3, 1, argb((int) (230 * oa), 0xFF6070));
			line(g, ex, ey, bx, by, 1, 1, argb((int) (255 * oa), 0xFFE8E8));
		}
		// emitter glow (grows while charging)
		int glow = 3 + (int) (5 * prog);
		g.fill((int) ex - glow - 2, (int) ey - glow - 2, (int) ex + glow + 2, (int) ey + glow + 2, argb((int) (50 * oa), 0xFF2030));
		g.fill((int) ex - glow, (int) ey - glow, (int) ex + glow, (int) ey + glow, argb((int) (150 * oa * pulse), 0xFF4050));
		g.fill((int) ex - 2, (int) ey - 2, (int) ex + 2, (int) ey + 2, argb((int) (255 * oa), 0xFFFFFF));
		// impact bloom
		if (beamP >= 1f) {
			int ir = (int) (5 + 5 * pulse + 6 * (prog - 0.48f));
			g.fill((int) tX - ir - 4, (int) tY - ir - 4, (int) tX + ir + 4, (int) tY + ir + 4, argb((int) (45 * oa), 0xFF2030));
			g.fill((int) tX - ir, (int) tY - ir, (int) tX + ir, (int) tY + ir, argb((int) (120 * oa), 0xFF5060));
			g.fill((int) tX - 2, (int) tY - 2, (int) tX + 2, (int) tY + 2, argb((int) (255 * oa), 0xFFFFFF));
		}

		// ---- side HUD arcs ----
		int arcTop = (int) (h * 0.12f);
		int arcBot = (int) (h * 0.88f);
		int arcCol = argb((int) (200 * oa), 0x40D8F0);
		int arcDim = argb((int) (90 * oa), 0x40D8F0);
		for (int y = arcTop; y < arcBot; y += 2) {
			float u = (y - h / 2f) / (h * 0.38f);
			int ax = (int) (w * 0.045f + u * u * w * 0.035f);
			if ((y / 8) % 6 != 5) {
				g.fill(ax, y, ax + 2, y + 2, arcCol);
				g.fill(w - ax - 2, y, w - ax, y + 2, arcCol);
			}
			int bxo = ax + 8;
			g.fill(bxo, y, bxo + 1, y + 2, arcDim);
			g.fill(w - bxo - 1, y, w - bxo, y + 2, arcDim);
		}
		for (int k = 0; k < 4; k++) {
			int yy = (int) (h * 0.30f) + k * 7;
			g.fill((int) (w * 0.06f), yy, (int) (w * 0.06f) + 10, yy + 4, argb((int) ((120 + 100 * ((age / 4 + k) % 2)) * oa), 0x50E8FF));
			g.fill(w - (int) (w * 0.06f) - 10, yy, w - (int) (w * 0.06f), yy + 4, argb((int) ((120 + 100 * ((age / 4 + k + 1) % 2)) * oa), 0x50E8FF));
		}
		int midY = h / 2;
		int redCol = argb((int) (230 * oa), 0xFF4050);
		g.fill((int) (w * 0.035f), midY, (int) (w * 0.035f) + 9, midY + 1, redCol);
		g.fill(w - (int) (w * 0.035f) - 9, midY, w - (int) (w * 0.035f), midY + 1, redCol);

		// ---- readouts ----
		int cyan = argb((int) (240 * oa), 0xBFF2FF);
		int white = argb((int) (240 * oa), 0xFFFFFF);
		String align = String.format(java.util.Locale.ROOT, "%.1f%%", 99.7f * (float) Math.pow(prog, 0.7));
		String status = prog < 0.35f ? "ACQUIRING" : (prog < 0.8f ? "TRACKING" : "LOCK-ON");
		int lx = (int) (w * 0.08f);
		int ly = h - 44;
		textL(g, "SYSTEM: SKYBREAKER ORBITAL PLATFORM", lx, ly, cyan);
		textL(g, "POWER: " + (int) (60 + 38 * prog) + "%", lx, ly + 11, cyan);
		textL(g, "ARMAMENT: SATELLITE LASER - " + (prog > 0.9f ? "READY" : "CHARGING"), lx, ly + 22, cyan);

		int rx = (int) (w * 0.93f);
		textR(g, "BEAM ALIGNMENT: " + align, rx, (int) (h * 0.30f), cyan);
		g.fill(rx - 120, (int) (h * 0.30f) + 11, rx, (int) (h * 0.30f) + 12, argb((int) (160 * oa), 0xFF5060));
		textR(g, "BEAM ALIGNMENT: " + align, rx, h - 33, cyan);
		textR(g, "IMPACT: T-MINUS " + String.format(java.util.Locale.ROOT, "%.1f", remaining / 20f) + " SECONDS", rx, h - 22, white);

		// ---- warning panel ----
		int pw = (int) Math.max(150, w * 0.40f);
		int ph = 66;
		int px = (int) (w * 0.58f);
		if (px + pw > w - 6) {
			px = w - 6 - pw;
		}
		int py = (int) (h * 0.50f);
		boolean blink = (age / 5) % 2 == 0 || prog < 0.12f;
		int red = argb((int) (255 * oa), blink ? 0xFF4050 : 0xC02838);
		g.fill(px, py, px + pw, py + ph, argb((int) (170 * oa), 0x101820));
		g.fill(px, py, px + pw, py + 1, argb((int) (110 * oa), 0x8090A0));
		g.fill(px, py + ph - 1, px + pw, py + ph, argb((int) (110 * oa), 0x8090A0));
		g.fill(px - 2, py - 2, px + 14, py - 1, red);
		g.fill(px - 2, py - 2, px - 1, py + 14, red);
		g.fill(px + pw - 14, py + ph + 1, px + pw + 2, py + ph + 2, red);
		g.fill(px + pw + 1, py + ph - 14, px + pw + 2, py + ph + 2, red);
		g.fill(px + pw, py - 2, px + pw + 2, py + 14, red);
		g.fill(px + pw - 14, py - 2, px + pw + 2, py - 1, red);
		g.fill(px - 2, py + ph - 14, px - 1, py + ph + 2, red);
		g.fill(px - 2, py + ph + 1, px + 14, py + ph + 2, red);
		textScaledL(g, "WARNING:", px + 8, py + 6, 1.2f, red);
		String big = "PLANETARY STRIKE IMMINENT";
		float bigScale = Math.min(1.5f, (pw - 16) / (float) Math.max(1, font.width(big)));
		textScaledL(g, big, px + 8, py + 18, bigScale, red);
		g.fill(px + 8, py + 18 + (int) (9 * bigScale) + 2, px + pw - 8, py + 18 + (int) (9 * bigScale) + 3, argb((int) (255 * oa), 0xFF9A40));
		int ty0 = py + 18 + (int) (9 * bigScale) + 8;
		textL(g, "TARGET: " + (int) Math.floor(tx) + " " + (int) Math.floor(ty) + " " + (int) Math.floor(tz) + "  RNG " + (int) distance + "m", px + 8, ty0, white);
		textL(g, "STATUS: " + status, px + 8, ty0 + 11, prog >= 0.8f ? red : cyan);

		// ---- scanlines + glitch at the cut-in ----
		for (int y = 0; y < h; y += 3) {
			g.fill(0, y, w, y + 1, argb((int) (26 * oa), 0x000000));
		}
		if (age < 12 && age % 2 == 1) {
			int gy = hash(age * 11) % Math.max(1, h - 10);
			g.fill(0, gy, w, gy + 3 + hash(age) % 6, argb((int) (60 * oa), 0x7FE8FF));
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
