package com.skybreaker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Software-rendered 3D orbital strike view shown during every weapon's countdown: a rotating, lit Earth (day/night,
 * clouds, city lights, atmosphere), ray-cast 3D satellite models, and a weapon specific strike (laser beam, tungsten
 * rod, meteor swarm, singularity) with the targeting HUD on top.
 */
final class OrbitalView {
	private static final float PI = (float) Math.PI;
	private static final int TW = 256;
	private static final int TH = 128;
	private static final float LX = 0.75f, LY = 0.50f, LZ = 0.43f;

	private static final String[] NAMES = {"", "ORBITAL RAILGUN", "SATELLITE LASER", "METEOR BARRAGE", "SINGULARITY CANNON"};
	private static final String[] WARN = {"", "KINETIC BOMBARDMENT IMMINENT", "PLANETARY STRIKE IMMINENT", "METEOR BARRAGE INBOUND", "SPACETIME COLLAPSE IMMINENT"};
	private static final String[] ARM = {"", "TUNGSTEN ROD", "SATELLITE LASER", "KINETIC DEBRIS", "SINGULARITY CANNON"};
	private static final String[] ALIGN = {"", "TRAJECTORY LOCK", "BEAM ALIGNMENT", "IMPACT VECTOR", "SINGULARITY SYNC"};
	private static final int[] ACCENT = {0, 0xFFB030, 0xFF3040, 0xFF6A20, 0xC070FF};

	private static final int[] albedo = new int[TW * TH];
	private static final boolean[] ocean = new boolean[TW * TH];
	private static final boolean[] city = new boolean[TW * TH];
	private static final float[] cloud = new float[TW * TH];
	private static final float[] height = new float[TW * TH];
	private static boolean ready = false;

	private static final class Part {
		final float x, y, z, hx, hy, hz;
		final int rgb, mat;

		Part(float x, float y, float z, float hx, float hy, float hz, int rgb, int mat) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.hx = hx;
			this.hy = hy;
			this.hz = hz;
			this.rgb = rgb;
			this.mat = mat;
		}
	}

	private static final Part[][] MODELS = new Part[5][];
	private static final float[] tmp2 = new float[2];
	private static final float[] hitOut = new float[3];

	// ------------------------------------------------------------------------------------------------ utilities

	private static int argb(int a, int rgb) {
		return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
	}

	private static float cl(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	private static float sm(float a, float b, float x) {
		float t = cl((x - a) / (b - a));
		return t * t * (3f - 2f * t);
	}

	private static int mix(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
		int g = (int) (((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1f - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	private static int rgbf(float r, float g, float b) {
		int ri = (int) Math.max(0, Math.min(255, r));
		int gi = (int) Math.max(0, Math.min(255, g));
		int bi = (int) Math.max(0, Math.min(255, b));
		return (ri << 16) | (gi << 8) | bi;
	}

	private static int hash(int n) {
		n = (n ^ 61) ^ (n >>> 16);
		n *= 9;
		n ^= n >>> 4;
		n *= 0x27d4eb2d;
		n ^= n >>> 15;
		return n & 0x7FFFFFFF;
	}

	private static float hf(int x, int y, int z) {
		int n = x * 374761393 + y * 668265263 + z * 1274126177;
		n = (n ^ (n >>> 13)) * 1274126177;
		n ^= n >>> 16;
		return (n & 0xFFFF) / 65535f;
	}

	private static float vn(float x, float y, float z) {
		int xi = (int) Math.floor(x);
		int yi = (int) Math.floor(y);
		int zi = (int) Math.floor(z);
		float xf = x - xi;
		float yf = y - yi;
		float zf = z - zi;
		xf = xf * xf * (3f - 2f * xf);
		yf = yf * yf * (3f - 2f * yf);
		zf = zf * zf * (3f - 2f * zf);
		float x00 = hf(xi, yi, zi) + (hf(xi + 1, yi, zi) - hf(xi, yi, zi)) * xf;
		float x10 = hf(xi, yi + 1, zi) + (hf(xi + 1, yi + 1, zi) - hf(xi, yi + 1, zi)) * xf;
		float x01 = hf(xi, yi, zi + 1) + (hf(xi + 1, yi, zi + 1) - hf(xi, yi, zi + 1)) * xf;
		float x11 = hf(xi, yi + 1, zi + 1) + (hf(xi + 1, yi + 1, zi + 1) - hf(xi, yi + 1, zi + 1)) * xf;
		float y0 = x00 + (x10 - x00) * yf;
		float y1 = x01 + (x11 - x01) * yf;
		return y0 + (y1 - y0) * zf;
	}

	private static float fbm(float x, float y, float z, int oct) {
		float sum = 0f;
		float amp = 0.5f;
		float norm = 0f;
		float f = 1f;
		for (int i = 0; i < oct; i++) {
			sum += amp * vn(x * f, y * f, z * f);
			norm += amp;
			amp *= 0.5f;
			f *= 2.03f;
		}
		return sum / norm;
	}

	// ------------------------------------------------------------------------------------------------ Earth texture

	private static void build() {
		for (int j = 0; j < TH; j++) {
			float lat = (0.5f - (j + 0.5f) / TH) * PI;
			float cl = (float) Math.cos(lat);
			float sl = (float) Math.sin(lat);
			for (int i = 0; i < TW; i++) {
				float lon = (i + 0.5f) / TW * 2f * PI;
				float x = cl * (float) Math.sin(lon);
				float z = cl * (float) Math.cos(lon);
				int idx = j * TW + i;
				float hgt = fbm(x * 2.3f + 7.1f, sl * 2.3f + 3.3f, z * 2.3f + 1.9f, 5);
				float dry = fbm(x * 3f + 20f, sl * 3f + 5f, z * 3f + 11f, 3);
				float cld = fbm(x * 3.2f + 50f, sl * 3.2f + 9f, z * 3.2f + 31f, 5);
				float cty = fbm(x * 14f + 3f, sl * 14f + 8f, z * 14f + 2f, 2);
				height[idx] = hgt;
				cloud[idx] = cld;
				float ay = Math.abs(sl);
				boolean oc = hgt < 0.53f;
				ocean[idx] = oc;
				int rgb;
				if (ay > 0.91f || (ay > 0.82f && !oc && hgt > 0.58f)) {
					rgb = 0xE8F0F8;
				} else if (oc) {
					float t = Math.max(0f, Math.min(1f, (0.53f - hgt) / 0.16f));
					rgb = mix(0x3FA6D0, 0x082C66, t);
				} else {
					float e = Math.max(0f, Math.min(1f, (hgt - 0.53f) / 0.2f));
					float dryness = Math.max(0f, Math.min(1f, (dry - 0.48f) * 4f + (0.45f - ay) * 0.5f));
					int base = mix(0x3E7C3C, 0xC9A96A, dryness);
					base = mix(base, 0x244F2C, Math.max(0f, 0.5f - dryness) * 0.6f);
					rgb = mix(base, 0x8E887A, Math.max(0f, Math.min(1f, (e - 0.55f) * 2.4f)));
				}
				albedo[idx] = rgb;
				city[idx] = !oc && ay < 0.75f && cty > 0.6f && hgt < 0.64f;
			}
		}
		// models ------------------------------------------------------------------------------------------
		for (int wpn = 1; wpn <= 4; wpn++) {
			java.util.ArrayList<Part> l = new java.util.ArrayList<>();
			l.add(new Part(0, 0, 0, 1.0f, 0.7f, 0.7f, 0xB8C0CC, 0));
			l.add(new Part(0, 0.36f, 0, 1.03f, 0.1f, 0.73f, 0xD0A040, 3));
			l.add(new Part(0, -0.36f, 0, 1.03f, 0.1f, 0.73f, 0xD0A040, 3));
			for (int side = -1; side <= 1; side += 2) {
				l.add(new Part(side * 1.7f, 0, 0, 0.7f, 0.06f, 0.06f, 0x8890A0, 0));
				l.add(new Part(side * 3.4f, 0, 0, 1.6f, 0.04f, 1.1f, 0x2358B0, 1));
			}
			l.add(new Part(0, 1.0f, 0, 0.08f, 0.3f, 0.08f, 0x8890A0, 0));
			l.add(new Part(0, 1.4f, 0, 0.5f, 0.05f, 0.5f, 0xE0E4EC, 0));
			if (wpn == 2) {
				l.add(new Part(0, -1.1f, 0, 0.38f, 0.4f, 0.38f, 0x5A6270, 0));
				l.add(new Part(0, -1.55f, 0, 0.24f, 0.06f, 0.24f, 0xFF3040, 2));
			} else if (wpn == 1) {
				l.add(new Part(0, -2.7f, 0, 0.3f, 2.0f, 0.3f, 0x4A505C, 0));
				l.add(new Part(-0.38f, -2.7f, 0, 0.06f, 2.0f, 0.3f, 0x9AA2B0, 0));
				l.add(new Part(0.38f, -2.7f, 0, 0.06f, 2.0f, 0.3f, 0x9AA2B0, 0));
				for (int k = 0; k < 4; k++) {
					l.add(new Part(0, -1.6f - k * 0.9f, 0, 0.44f, 0.09f, 0.44f, 0xFFB030, 2));
				}
			} else if (wpn == 4) {
				l.add(new Part(0, -1.1f, 0, 0.5f, 0.35f, 0.5f, 0x5A6270, 0));
				l.add(new Part(0.7f, -1.9f, 0, 0.08f, 0.55f, 0.08f, 0x9AA2B0, 0));
				l.add(new Part(-0.7f, -1.9f, 0, 0.08f, 0.55f, 0.08f, 0x9AA2B0, 0));
				l.add(new Part(0, -1.9f, 0.7f, 0.08f, 0.55f, 0.08f, 0x9AA2B0, 0));
				l.add(new Part(0, -1.9f, -0.7f, 0.08f, 0.55f, 0.08f, 0x9AA2B0, 0));
				l.add(new Part(0, -1.95f, 0, 0.2f, 0.2f, 0.2f, 0xC070FF, 2));
			}
			MODELS[wpn] = l.toArray(new Part[0]);
		}
		ready = true;
	}

	// ------------------------------------------------------------------------------------------------ Earth

	private static void globe(GuiGraphicsExtractor g, int w, int h, int cs, float gcx, float gcy, float R, float spin, float tilt, float oa, int age) {
		float ct = (float) Math.cos(tilt);
		float st = (float) Math.sin(tilt);
		int y0 = (int) Math.max(0, gcy - R * 1.1f);
		int shift = (int) (age * 0.15f);
		for (int py = y0; py < h; py += cs) {
			float ny = -(py + cs * 0.5f - gcy) / R;
			for (int px = 0; px < w; px += cs) {
				float nx = (px + cs * 0.5f - gcx) / R;
				float d2 = nx * nx + ny * ny;
				if (d2 >= 1.19f) {
					continue;
				}
				if (d2 >= 1f) {
					float dd = (float) Math.sqrt(d2);
					float t = (dd - 1f) / 0.09f;
					float a = (1f - t) * (1f - t);
					float lf = Math.max(0.1f, Math.min(1f, 0.35f + 0.9f * (nx * LX + ny * LY) / dd));
					g.fill(px, py, px + cs, py + cs, argb((int) (150 * a * lf * oa), 0x5CB8FF));
					continue;
				}
				float nz = (float) Math.sqrt(1f - d2);
				float qy = ny * ct - nz * st;
				float qz = ny * st + nz * ct;
				float lon = (float) Math.atan2(nx, qz) + spin;
				float lat = (float) Math.asin(Math.max(-1f, Math.min(1f, qy)));
				float u = lon / (2f * PI);
				u -= (float) Math.floor(u);
				int ti = Math.min(TW - 1, (int) (u * TW));
				int tj = Math.max(0, Math.min(TH - 1, (int) ((0.5f - lat / PI) * TH)));
				int idx = tj * TW + ti;
				int al = albedo[idx];
				float dot = nx * LX + ny * LY + nz * LZ;
				float diff = Math.max(0f, dot);
				float lit = 0.07f + 0.93f * diff;
				float r = ((al >> 16) & 255) * lit;
				float gg = ((al >> 8) & 255) * lit;
				float b = (al & 255) * lit;
				if (ocean[idx]) {
					float hh = (nx * LX + ny * LY + nz * (LZ + 1f)) / 1.66f;
					if (hh > 0f) {
						float sp = (float) Math.pow(hh, 160.0) * 0.55f;
						r += 255 * sp;
						gg += 255 * sp;
						b += 255 * sp;
					}
				}
				float c = sm(0.52f, 0.72f, cloud[tj * TW + ((ti + shift) & (TW - 1))]);
				float cl = (0.12f + 0.88f * diff) * 255f;
				r = r * (1f - c * 0.85f) + cl * c * 0.85f;
				gg = gg * (1f - c * 0.85f) + cl * c * 0.85f;
				b = b * (1f - c * 0.85f) + cl * c * 0.85f;
				if (city[idx]) {
					float nl = (1f - sm(-0.1f, 0.12f, dot)) * (1f - c);
					r += 255 * 0.9f * nl;
					gg += 190 * 0.9f * nl;
					b += 110 * 0.9f * nl;
				}
				float rim = (float) Math.pow(1f - nz, 3.0) * (0.25f + 0.75f * sm(-0.35f, 0.45f, dot));
				r += 60 * rim;
				gg += 140 * rim;
				b += 235 * rim;
				g.fill(px, py, px + cs, py + cs, argb((int) (255 * oa), rgbf(r, gg, b)));
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ 3D models

	private static float[] mul(float[] a, float[] b) {
		float[] o = new float[9];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				o[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
			}
		}
		return o;
	}

	/** R = Rz(th) * Rx(ph) * Ry(ya). */
	private static float[] rot(float th, float ph, float ya) {
		float c = (float) Math.cos(th), s = (float) Math.sin(th);
		float[] rz = {c, -s, 0, s, c, 0, 0, 0, 1};
		c = (float) Math.cos(ph);
		s = (float) Math.sin(ph);
		float[] rx = {1, 0, 0, 0, c, -s, 0, s, c};
		c = (float) Math.cos(ya);
		s = (float) Math.sin(ya);
		float[] ry = {c, 0, s, 0, 1, 0, -s, 0, c};
		return mul(mul(rz, rx), ry);
	}

	/** Slab test of a ray against a local-space box. Returns the entry distance or -1; hitOut = {axis, sign, 0}. */
	private static float hit(Part p, float ox, float oy, float oz, float dx, float dy, float dz) {
		float tmin = -1e9f, tmax = 1e9f;
		int axis = 0;
		float sign = 1f;
		float[] o = {ox - p.x, oy - p.y, oz - p.z};
		float[] d = {dx, dy, dz};
		float[] e = {p.hx, p.hy, p.hz};
		for (int a = 0; a < 3; a++) {
			if (Math.abs(d[a]) < 1e-6f) {
				if (Math.abs(o[a]) > e[a]) {
					return -1f;
				}
				continue;
			}
			float t1 = (-e[a] - o[a]) / d[a];
			float t2 = (e[a] - o[a]) / d[a];
			float sg = d[a] > 0 ? -1f : 1f;
			if (t1 > t2) {
				float t = t1;
				t1 = t2;
				t2 = t;
			}
			if (t1 > tmin) {
				tmin = t1;
				axis = a;
				sign = sg;
			}
			if (t2 < tmax) {
				tmax = t2;
			}
			if (tmin > tmax) {
				return -1f;
			}
		}
		if (tmax < 0f) {
			return -1f;
		}
		hitOut[0] = axis;
		hitOut[1] = sign;
		return tmin;
	}

	private static void model(GuiGraphicsExtractor g, Part[] parts, float[] m, float s, float posX, float posY, float ext, int cs, float oa, int age) {
		float rad = ext * s;
		int x0 = (int) (posX - rad);
		int x1 = (int) (posX + rad);
		int y0 = (int) (posY - rad);
		int y1 = (int) (posY + rad);
		float hlen = (float) Math.sqrt(LX * LX + LY * LY + (LZ + 1f) * (LZ + 1f));
		float dx = -m[6], dy = -m[7], dz = -m[8];
		for (int py = y0; py < y1; py += cs) {
			float v = -(py + cs * 0.5f - posY) / s;
			for (int px = x0; px < x1; px += cs) {
				float u = (px + cs * 0.5f - posX) / s;
				float ox = m[0] * u + m[3] * v + m[6] * 20f;
				float oy = m[1] * u + m[4] * v + m[7] * 20f;
				float oz = m[2] * u + m[5] * v + m[8] * 20f;
				float best = 1e9f;
				Part bp = null;
				int ba = 0;
				float bs = 1f;
				for (Part p : parts) {
					float t = hit(p, ox, oy, oz, dx, dy, dz);
					if (t >= 0f && t < best) {
						best = t;
						bp = p;
						ba = (int) hitOut[0];
						bs = hitOut[1];
					}
				}
				if (bp == null) {
					continue;
				}
				float[] nl = {0, 0, 0};
				nl[ba] = bs;
				float nvx = m[0] * nl[0] + m[1] * nl[1] + m[2] * nl[2];
				float nvy = m[3] * nl[0] + m[4] * nl[1] + m[5] * nl[2];
				float nvz = m[6] * nl[0] + m[7] * nl[1] + m[8] * nl[2];
				float diff = Math.max(0f, nvx * LX + nvy * LY + nvz * LZ);
				float sp = (float) Math.pow(Math.max(0f, (nvx * LX + nvy * LY + nvz * (LZ + 1f)) / hlen), 28.0);
				int base = bp.rgb;
				float r, gg, b;
				if (bp.mat == 2) {
					float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.5);
					r = ((base >> 16) & 255) * pulse;
					gg = ((base >> 8) & 255) * pulse;
					b = (base & 255) * pulse;
					r += 60;
					gg += 60;
					b += 60;
				} else {
					float hx = ox + dx * best - bp.x;
					float hz = oz + dz * best - bp.z;
					if (bp.mat == 1) {
						float fx = Math.abs(hx * 1.5f) % 1f;
						float fz = Math.abs(hz * 1.8f) % 1f;
						if (fx < 0.07f || fz < 0.07f) {
							base = 0x0A1838;
						}
					}
					float lit = 0.16f + 0.84f * diff;
					r = ((base >> 16) & 255) * lit + 255 * sp * (bp.mat == 1 ? 0.6f : 0.45f);
					gg = ((base >> 8) & 255) * lit + 255 * sp * (bp.mat == 1 ? 0.7f : 0.45f);
					b = (base & 255) * lit + 255 * sp * 0.5f + 18f * (1f - diff);
				}
				g.fill(px, py, px + cs, py + cs, argb((int) (255 * oa), rgbf(r, gg, b)));
			}
		}
	}

	private static float[] proj(float[] m, float s, float posX, float posY, float lx, float ly, float lz) {
		float vx = m[0] * lx + m[1] * ly + m[2] * lz;
		float vy = m[3] * lx + m[4] * ly + m[5] * lz;
		tmp2[0] = posX + vx * s;
		tmp2[1] = posY - vy * s;
		return tmp2;
	}

	// ------------------------------------------------------------------------------------------------ drawing helpers

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

	private static void glowDot(GuiGraphicsExtractor g, float x, float y, int r, int rgb, float oa, float k) {
		g.fill((int) x - r - 4, (int) y - r - 4, (int) x + r + 4, (int) y + r + 4, argb((int) (40 * oa * k), rgb));
		g.fill((int) x - r - 2, (int) y - r - 2, (int) x + r + 2, (int) y + r + 2, argb((int) (90 * oa * k), rgb));
		g.fill((int) x - r, (int) y - r, (int) x + r, (int) y + r, argb((int) (170 * oa * k), mix(rgb, 0xFFFFFF, 0.5f)));
		g.fill((int) x - r / 2, (int) y - r / 2, (int) x + r / 2 + 1, (int) y + r / 2 + 1, argb((int) (255 * oa * k), 0xFFFFFF));
	}

	private static void squareRing(GuiGraphicsExtractor g, float cx, float cy, int r, int thick, int color) {
		g.fill((int) cx - r, (int) cy - r, (int) cx + r, (int) cy - r + thick, color);
		g.fill((int) cx - r, (int) cy + r - thick, (int) cx + r, (int) cy + r, color);
		g.fill((int) cx - r, (int) cy - r, (int) cx - r + thick, (int) cy + r, color);
		g.fill((int) cx + r - thick, (int) cy - r, (int) cx + r, (int) cy + r, color);
	}

	private static void rock(GuiGraphicsExtractor g, float cx, float cy, float rad, float spin, float oa, float heat) {
		int cs = Math.max(1, (int) (rad / 5f));
		int ri = (int) Math.ceil(rad);
		for (int py = (int) cy - ri; py <= (int) cy + ri; py += cs) {
			for (int px = (int) cx - ri; px <= (int) cx + ri; px += cs) {
				float nx = (px + cs * 0.5f - cx) / rad;
				float ny = -(py + cs * 0.5f - cy) / rad;
				float d2 = nx * nx + ny * ny;
				if (d2 >= 1f) {
					continue;
				}
				float nz = (float) Math.sqrt(1f - d2);
				float lon = (float) Math.atan2(nx, nz) + spin;
				float u = lon / (2f * PI);
				u -= (float) Math.floor(u);
				int ti = Math.min(TW - 1, (int) (u * TW));
				int tj = Math.max(0, Math.min(TH - 1, (int) ((0.5f - (float) Math.asin(ny) / PI) * TH)));
				float bump = height[tj * TW + ti];
				float dot = Math.max(0f, nx * LX + ny * LY + nz * LZ);
				float k = (0.12f + 0.88f * dot) * (0.45f + 0.9f * bump);
				float r = 118 * k, gg = 100 * k, b = 86 * k;
				float edge = heat * (1f - nz);
				r += 255 * edge;
				gg += 120 * edge;
				b += 30 * edge;
				g.fill(px, py, px + cs, py + cs, argb((int) (255 * oa), rgbf(r, gg, b)));
			}
		}
	}

	private static void disk(GuiGraphicsExtractor g, float cx, float cy, float r, float oa, int age, boolean front) {
		float tilt = -0.22f;
		float ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
		int x0 = (int) (cx - 3.8f * r), x1 = (int) (cx + 3.8f * r);
		int y0 = (int) (cy - 1.9f * r), y1 = (int) (cy + 1.9f * r);
		for (int py = y0; py < y1; py += 2) {
			for (int px = x0; px < x1; px += 2) {
				float dx = px - cx, dy = py - cy;
				float xr = dx * ct + dy * st;
				float yr = (-dx * st + dy * ct) / 0.28f;
				float rho = (float) Math.sqrt(xr * xr + yr * yr);
				if (rho < 1.35f * r || rho > 3.6f * r || (yr > 0f) != front) {
					continue;
				}
				float rn = (rho - 1.35f * r) / (2.25f * r);
				float ang = (float) Math.atan2(yr, xr);
				float swirl = 0.65f + 0.35f * (float) Math.sin(ang * 3f - age * 0.3f + rn * 9f);
				float dop = 0.7f + 0.5f * cl((-xr / Math.max(1f, rho) + 1f) * 0.5f);
				int col = mix(mix(0xFFF2D0, 0xFF8A20, cl(rn * 1.5f)), 0x6A1A5A, cl((rn - 0.45f) * 1.8f));
				float a = 235f * swirl * dop * (1f - rn * 0.75f) * cl((rho - 1.35f * r) / (0.15f * r));
				g.fill(px, py, px + 2, py + 2, argb((int) (a * oa), col));
			}
		}
	}

	private static void blackHole(GuiGraphicsExtractor g, float cx, float cy, float r, float oa, int age) {
		if (r < 2f) {
			return;
		}
		disk(g, cx, cy, r, oa, age, false);
		for (int py = (int) (cy - r); py < (int) (cy + r); py += 2) {
			float dy = py + 1 - cy;
			if (Math.abs(dy) >= r) {
				continue;
			}
			int hw = (int) Math.sqrt(r * r - dy * dy);
			g.fill((int) cx - hw, py, (int) cx + hw, py + 2, argb((int) (255 * oa), 0x000000));
		}
		// photon ring + the lensed top arc of the far side of the disk
		for (int py = (int) (cy - 1.7f * r); py < (int) (cy + 1.4f * r); py += 2) {
			for (int px = (int) (cx - 1.7f * r); px < (int) (cx + 1.7f * r); px += 2) {
				float dx = px - cx, dy = py - cy;
				float rho = (float) Math.sqrt(dx * dx + dy * dy);
				float k = 0f;
				if (Math.abs(rho - 1.12f * r) < 1.6f) {
					k = 1f;
				} else if (dy < 0f && rho > 1.12f * r && rho < 1.65f * r) {
					k = 0.8f * (1f - (rho - 1.12f * r) / (0.53f * r)) * (0.7f + 0.3f * (float) Math.sin(dx * 0.25f - age * 0.3f));
				} else if (dy > 0f && rho > 1.12f * r && rho < 1.3f * r) {
					k = 0.35f * (1f - (rho - 1.12f * r) / (0.18f * r));
				}
				if (k > 0f) {
					g.fill(px, py, px + 2, py + 2, argb((int) (230 * k * oa), 0xFFB060));
				}
			}
		}
		disk(g, cx, cy, r, oa, age, true);
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

	private static void bracket(GuiGraphicsExtractor g, int x, int y, int len, int dx, int dy, int color) {
		g.fill(Math.min(x, x + dx * len), Math.min(y, y + dy * 2), Math.max(x, x + dx * len), Math.max(y, y + dy * 2), color);
		g.fill(Math.min(x, x + dx * 2), Math.min(y, y + dy * len), Math.max(x, x + dx * 2), Math.max(y, y + dy * len), color);
	}

	// ------------------------------------------------------------------------------------------------ main

	static void render(GuiGraphicsExtractor g, int w, int h, int weapon, int age, int total, int tgx, int tgy, int tgz, int distance) {
		if (!ready) {
			build();
		}
		weapon = Math.max(1, Math.min(4, weapon));
		var font = Minecraft.getInstance().font;
		float prog = cl(age / (float) total);
		float oa = Math.min(cl(age / 8f), cl((total - age) / 10f));
		if (oa <= 0f) {
			return;
		}
		int remaining = Math.max(0, total - age);
		int accent = ACCENT[weapon];
		int cs = Math.max(2, Math.round(h / 130f));

		// ---- space ----
		g.fill(0, 0, w, h, argb((int) (255 * oa), 0x02040C));
		for (int i = 0; i < 6; i++) {
			g.fill(0, h * i / 6, w, h * (i + 1) / 6, argb((int) (60 * oa), mix(0x050A1E, 0x0A1030, i / 5f)));
		}
		for (int i = 0; i < 130; i++) {
			int sx = (hash(i * 3) % Math.max(1, w) + (int) (age * (0.02f + (i % 3) * 0.02f))) % Math.max(1, w);
			int sy = hash(i * 3 + 1) % Math.max(1, h);
			float tw = 0.5f + 0.5f * (float) Math.sin(age * 0.2 + i);
			int a = (int) ((90 + 140 * tw) * oa * (0.4f + 0.6f * (hash(i * 3 + 2) % 100) / 100f));
			int sz = hash(i * 7) % 11 == 0 ? 2 : 1;
			g.fill(sx, sy, sx + sz, sy + sz, argb(a, 0xDDEEFF));
		}

		// ---- Earth ----
		float gcx = w * 0.42f;
		float gcy = h * 1.55f;
		float R = h * 1.05f * (1f + 0.05f * prog);
		globe(g, w, h, cs, gcx, gcy, R, 0.9f + age * 0.006f, 0.38f, oa, age);

		float tX = w * 0.47f;
		float tY = h * 0.70f;

		// ---- tactical grid over the target area ----
		int gx0 = (int) tX - 90, gx1 = (int) tX + 90, gy0 = (int) tY - 62, gy1 = (int) tY + 62;
		int gridA = (int) ((38 + 28 * Math.sin(age * 0.2)) * oa);
		for (int x = gx0; x <= gx1; x += 10) {
			float dxx = x - gcx;
			float lim = (float) Math.sqrt(Math.max(0, R * R - dxx * dxx));
			int ya = (int) Math.max(gy0, gcy - lim);
			if (ya < gy1) {
				g.fill(x, ya, x + 1, gy1, argb(gridA, 0xA0F0FF));
			}
		}
		for (int y = gy0; y <= gy1; y += 10) {
			float dyy = y - gcy;
			float lim = (float) Math.sqrt(Math.max(0, R * R - dyy * dyy));
			int xa = (int) Math.max(gx0, gcx - lim);
			int xb = (int) Math.min(gx1, gcx + lim);
			if (xa < xb) {
				g.fill(xa, y, xb, y + 1, argb(gridA, 0xA0F0FF));
			}
		}

		float pulse = 0.75f + 0.25f * (float) Math.sin(age * 1.3);

		// ---- weapon scene ----
		if (weapon == 3) {
			meteors(g, w, h, tX, tY, prog, oa, age);
		} else {
			int sx = (int) (w * 0.82f);
			int sy = (int) (h * 0.15f + Math.sin(age * 0.1) * 1.5);
			float s = h * 0.03f;
			float ux = tX - sx;
			float uy = -(tY - sy);
			float len = (float) Math.max(1.0, Math.sqrt(ux * ux + uy * uy));
			float th = (float) Math.atan2(ux / len, -(uy / len));
			float[] m = rot(th, 0.55f, 0.6f + 0.25f * (float) Math.sin(age * 0.04));
			float ext = weapon == 1 ? 6.4f : 5.4f;
			model(g, MODELS[weapon], m, s, sx, sy, ext, 2, oa, age);
			float tipLen = weapon == 1 ? 4.7f : (weapon == 4 ? 2.1f : 1.65f);
			float[] tp = proj(m, s, sx, sy, 0f, -tipLen, 0f);
			float ex = tp[0], ey = tp[1];

			if (weapon == 2 || weapon == 4) {
				float beamP = cl((prog - 0.18f) / 0.30f);
				float hbX = tX + w * 0.015f, hbY = tY - h * 0.16f;
				float destX = weapon == 4 ? hbX : tX;
				float destY = weapon == 4 ? hbY : tY;
				int bc = weapon == 4 ? 0xB060FF : 0xFF3040;
				int bc2 = weapon == 4 ? 0xD8A8FF : 0xFF6070;
				if (beamP > 0f) {
					float bx = ex + (destX - ex) * beamP;
					float by = ey + (destY - ey) * beamP;
					line(g, ex, ey, bx, by, 9, 2, argb((int) (40 * oa * pulse), bc));
					line(g, ex, ey, bx, by, 5, 2, argb((int) (110 * oa * pulse), bc));
					line(g, ex, ey, bx, by, 3, 1, argb((int) (230 * oa), bc2));
					line(g, ex, ey, bx, by, 1, 1, argb((int) (255 * oa), 0xFFF0F0));
				}
				glowDot(g, ex, ey, 3 + (int) (5 * prog), bc, oa, pulse);
				if (weapon == 2 && beamP >= 1f) {
					glowDot(g, tX, tY, (int) (5 + 5 * pulse + 6 * (prog - 0.48f)), 0xFF3040, oa, 1f);
				}
				if (weapon == 4) {
					float rr = h * 0.085f * (1f - (float) Math.pow(1f - cl(prog / 0.75f), 3.0));
					blackHole(g, hbX, hbY, rr, oa, age);
				}
			} else {
				// orbital railgun: charge, fire a tungsten rod, impact
				float charge = cl(prog / 0.45f);
				glowDot(g, ex, ey, 2 + (int) (6 * charge), 0xFFB030, oa, pulse);
				int arcs = (int) (charge * 6);
				for (int i = 0; i < arcs; i++) {
					float a = hash(age * 13 + i) % 628 / 100f;
					float l = 6 + hash(age * 7 + i) % 14;
					line(g, ex, ey, ex + (float) Math.cos(a) * l, ey + (float) Math.sin(a) * l, 1, 1, argb((int) (220 * oa), 0xFFE9A0));
				}
				float e = cl((prog - 0.45f) / 0.45f);
				if (prog >= 0.45f && e < 1f) {
					float ee = e * e;
					float px = ex + (tX - ex) * ee;
					float py = ey + (tY - ey) * ee;
					float dx = (tX - ex) / Math.max(1f, (float) Math.hypot(tX - ex, tY - ey));
					float dy = (tY - ey) / Math.max(1f, (float) Math.hypot(tX - ex, tY - ey));
					float trail = 24f + 90f * e;
					for (int k = 0; k < 8; k++) {
						float f0 = k / 8f;
						line(g, px - dx * trail * f0, py - dy * trail * f0, px - dx * trail * (f0 + 0.125f), py - dy * trail * (f0 + 0.125f),
							Math.max(1, (int) (7 * (1f - f0))), 2, argb((int) (150 * (1f - f0) * oa), 0xFF7A20));
					}
					line(g, px - dx * 14f, py - dy * 14f, px, py, 3, 1, argb((int) (240 * oa), 0xFFD890));
					line(g, px - dx * 14f, py - dy * 14f, px, py, 1, 1, argb((int) (255 * oa), 0xFFFFFF));
					glowDot(g, px, py, 3, 0xFFB030, oa, 1f);
					if (e < 0.08f) {
						glowDot(g, ex, ey, 14, 0xFFE0A0, oa, 1f - e / 0.08f);
					}
				} else if (e >= 1f) {
					float k = cl((prog - 0.9f) / 0.1f);
					glowDot(g, tX, tY, (int) (6 + 10 * k), 0xFFB030, oa, 1f - 0.4f * k);
					squareRing(g, tX, tY, (int) (6 + 40 * k), 2, argb((int) (220 * (1f - k) * oa), 0xFFD890));
				}
			}
		}

		// ---- reticle ----
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

		// ---- side HUD arcs ----
		int arcTop = (int) (h * 0.12f), arcBot = (int) (h * 0.88f);
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
		int redCol = argb((int) (230 * oa), 0xFF4050);
		g.fill((int) (w * 0.035f), h / 2, (int) (w * 0.035f) + 9, h / 2 + 1, redCol);
		g.fill(w - (int) (w * 0.035f) - 9, h / 2, w - (int) (w * 0.035f), h / 2 + 1, redCol);

		// ---- readouts ----
		int cyan = argb((int) (240 * oa), 0xBFF2FF);
		int white = argb((int) (240 * oa), 0xFFFFFF);
		String align = String.format(Locale.ROOT, "%.1f%%", 99.7f * (float) Math.pow(prog, 0.7));
		String status = prog < 0.35f ? "ACQUIRING" : (prog < 0.8f ? "TRACKING" : "LOCK-ON");
		int lx = (int) (w * 0.08f);
		int ly = h - 44;
		textL(g, "SYSTEM: SKYBREAKER ORBITAL PLATFORM", lx, ly, cyan);
		textL(g, "POWER: " + (int) (60 + 38 * prog) + "%", lx, ly + 11, cyan);
		textL(g, "ARMAMENT: " + ARM[weapon] + " - " + (prog > 0.9f ? "READY" : "CHARGING"), lx, ly + 22, cyan);
		int rx = (int) (w * 0.93f);
		textR(g, ALIGN[weapon] + ": " + align, rx, (int) (h * 0.30f), cyan);
		g.fill(rx - 120, (int) (h * 0.30f) + 11, rx, (int) (h * 0.30f) + 12, argb((int) (160 * oa), accent));
		textR(g, ALIGN[weapon] + ": " + align, rx, h - 33, cyan);
		textR(g, "IMPACT: T-MINUS " + String.format(Locale.ROOT, "%.1f", remaining / 20f) + " SECONDS", rx, h - 22, white);

		// ---- warning panel ----
		int pw = (int) Math.max(150, w * 0.40f);
		int ph = 66;
		int px = (int) (w * 0.58f);
		if (px + pw > w - 6) {
			px = w - 6 - pw;
		}
		int py = (int) (h * 0.50f);
		boolean blink = (age / 5) % 2 == 0 || prog < 0.12f;
		int red = argb((int) (255 * oa), blink ? mix(accent, 0xFFFFFF, 0.15f) : mix(accent, 0x000000, 0.25f));
		g.fill(px, py, px + pw, py + ph, argb((int) (170 * oa), 0x101820));
		g.fill(px, py, px + pw, py + 1, argb((int) (110 * oa), 0x8090A0));
		g.fill(px, py + ph - 1, px + pw, py + ph, argb((int) (110 * oa), 0x8090A0));
		g.fill(px - 2, py - 2, px + 14, py - 1, red);
		g.fill(px - 2, py - 2, px - 1, py + 14, red);
		g.fill(px + pw - 14, py - 2, px + pw + 2, py - 1, red);
		g.fill(px + pw + 1, py - 2, px + pw + 2, py + 14, red);
		g.fill(px - 2, py + ph + 1, px + 14, py + ph + 2, red);
		g.fill(px - 2, py + ph - 14, px - 1, py + ph + 2, red);
		g.fill(px + pw - 14, py + ph + 1, px + pw + 2, py + ph + 2, red);
		g.fill(px + pw + 1, py + ph - 14, px + pw + 2, py + ph + 2, red);
		textScaledL(g, "WARNING:", px + 8, py + 6, 1.2f, red);
		String big = WARN[weapon];
		float bigScale = Math.min(1.5f, (pw - 16) / (float) Math.max(1, font.width(big)));
		textScaledL(g, big, px + 8, py + 18, bigScale, red);
		int ul = py + 18 + (int) (9 * bigScale) + 2;
		g.fill(px + 8, ul, px + pw - 8, ul + 1, argb((int) (255 * oa), 0xFF9A40));
		textL(g, "TARGET: " + tgx + " " + tgy + " " + tgz + "  RNG " + distance + "m", px + 8, ul + 6, white);
		textL(g, "STATUS: " + status, px + 8, ul + 17, prog >= 0.8f ? red : cyan);

		// ---- scanlines + glitch at the cut-in ----
		for (int y = 0; y < h; y += 3) {
			g.fill(0, y, w, y + 1, argb((int) (26 * oa), 0x000000));
		}
		if (age < 12 && age % 2 == 1) {
			int gy = hash(age * 11) % Math.max(1, h - 10);
			g.fill(0, gy, w, gy + 3 + hash(age) % 6, argb((int) (60 * oa), 0x7FE8FF));
		}
	}

	private static void meteors(GuiGraphicsExtractor g, int w, int h, float tX, float tY, float prog, float oa, int age) {
		for (int i = 0; i < 7; i++) {
			float t0 = 0.04f + i * 0.085f;
			float q = (prog - t0) / 0.55f;
			if (q <= 0f) {
				continue;
			}
			float ex = tX + (hash(i * 9) % 90 - 45);
			float ey = tY + (hash(i * 9 + 1) % 50 - 25);
			float sx = w * (0.98f + 0.02f * i);
			float sy = -h * 0.1f - i * 8f;
			float rad0 = 3f + (hash(i * 9 + 2) % 4);
			if (q >= 1f) {
				float k = cl((q - 1f) / 0.25f);
				if (k < 1f) {
					glowDot(g, ex, ey, (int) (4 + 8 * k), 0xFF6A20, oa, 1f - k);
					squareRing(g, ex, ey, (int) (4 + 26 * k), 2, argb((int) (200 * (1f - k) * oa), 0xFFB070));
				}
				continue;
			}
			float px = sx + (ex - sx) * q;
			float py = sy + (ey - sy) * q;
			float len = (float) Math.max(1.0, Math.hypot(ex - sx, ey - sy));
			float dx = (ex - sx) / len;
			float dy = (ey - sy) / len;
			float rad = rad0 * (0.5f + 1.4f * q);
			float trail = 40f + 140f * q;
			for (int k = 0; k < 10; k++) {
				float f0 = k / 10f;
				float f1 = (k + 1) / 10f;
				int thick = Math.max(2, (int) (rad * 1.7f * (1f - f0)));
				line(g, px - dx * trail * f0, py - dy * trail * f0, px - dx * trail * f1, py - dy * trail * f1, thick, 2,
					argb((int) (140 * (1f - f0) * oa), f0 < 0.4f ? 0xFF9A30 : 0xFF4A18));
			}
			glowDot(g, px, py, (int) (rad * 1.3f), 0xFF7A20, oa, 0.8f);
			rock(g, px, py, rad, age * 0.05f + i, oa, 0.9f);
		}
	}

	private OrbitalView() {
	}
}
