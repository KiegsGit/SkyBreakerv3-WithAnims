package com.skybreaker.client;

import com.skybreaker.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Skybreaker's own lightning. A jagged trunk (recursive midpoint displacement) with forked branches is built from a
 * seed and drawn out of glowing violet halo + white-hot core particles. It flickers in three pulses and ends in a
 * flash and a spray of sparks where it hits the ground.
 */
final class BoltFx extends Fx {
	private static final double SKY = 130.0;

	private final List<Vec3> trunk;
	private final List<List<Vec3>> branches = new ArrayList<>();
	private final Random rng;

	BoltFx(Vec3 pos, int seed, float power) {
		super(pos);
		this.rng = new Random(seed);
		Vec3 top = new Vec3(pos.x + (rng.nextDouble() - 0.5) * 36.0, pos.y + SKY, pos.z + (rng.nextDouble() - 0.5) * 36.0);
		this.trunk = jagged(top, pos, 0.17, 7, rng);
		int forks = 4 + rng.nextInt(4);
		for (int i = 0; i < forks; i++) {
			int idx = (int) (trunk.size() * (0.22 + rng.nextDouble() * 0.66));
			Vec3 from = trunk.get(Math.min(trunk.size() - 1, idx));
			double len = 14.0 + rng.nextDouble() * 28.0;
			double ang = rng.nextDouble() * Math.PI * 2.0;
			Vec3 to = new Vec3(from.x + Math.cos(ang) * len * 0.8, from.y - len * (0.35 + rng.nextDouble() * 0.5), from.z + Math.sin(ang) * len * 0.8);
			branches.add(jagged(from, to, 0.2, 5, rng));
		}
	}

	@Override
	boolean tick(ClientLevel level) {
		Pt.level = level;
		age++;
		if (age == 1) {
			draw(trunk, 0.9, 0.42, 1.0, true);
			for (List<Vec3> b : branches) {
				draw(b, 1.1, 0.7, 0.8, false);
			}
			impact();
		} else if (age == 3) {
			draw(trunk, 1.0, 0.5, 0.65, true);
			for (List<Vec3> b : branches) {
				draw(b, 1.3, 0.9, 0.5, false);
			}
		} else if (age == 5) {
			draw(trunk, 1.2, 0.7, 0.45, true);
		}
		return age > 7;
	}

	private void impact() {
		Pt.add(ModParticles.FLARE, pos.x, pos.y + 0.6, pos.z, 0, 0, 0);
		Pt.burst(ModParticles.SPARK, new Vec3(pos.x, pos.y + 0.3, pos.z), 26, 0.25, 0.9, 0.3);
		Pt.ring(ModParticles.PLASMA, pos, 1.2, 14, Pt.rnd() * 6.28, 0.2, 0.28, 0.02);
		for (int i = 0; i < 3; i++) {
			Pt.add(ModParticles.SMOKE, pos.x + Pt.sym() * 1.2, pos.y + 0.4, pos.z + Pt.sym() * 1.2, 0, 0.08, 0);
		}
	}

	/** Draws one pulse of a polyline. {@code keep} = chance each sample is drawn (makes the flicker). */
	static void draw(List<Vec3> pts, double haloStep, double coreStep, double keep, boolean bright) {
		walk(pts, haloStep, keep, p -> Pt.add(ModParticles.BOLT, p.x, p.y, p.z, 0, 0, 0));
		walk(pts, coreStep, keep, p -> Pt.add(ModParticles.CORE, p.x + Pt.sym() * 0.05, p.y, p.z + Pt.sym() * 0.05, 0, 0, 0));
		if (bright) {
			walk(pts, haloStep * 3.0, keep, p -> Pt.add(ModParticles.PLASMA, p.x, p.y, p.z, 0, 0, 0));
		}
	}

	/** A short arc between two points (used for crackling energy around the black hole). */
	static void arc(Vec3 a, Vec3 b, long seed) {
		List<Vec3> pts = jagged(a, b, 0.22, 5, new Random(seed));
		walk(pts, 0.45, 1.0, p -> Pt.add(ModParticles.ZAP, p.x, p.y, p.z, 0, 0, 0));
		walk(pts, 1.1, 1.0, p -> Pt.add(ModParticles.WISP, p.x, p.y, p.z, 0, 0, 0));
	}

	private interface Sampler {
		void at(Vec3 p);
	}

	private static void walk(List<Vec3> pts, double step, double keep, Sampler s) {
		for (int i = 0; i < pts.size() - 1; i++) {
			Vec3 a = pts.get(i);
			Vec3 b = pts.get(i + 1);
			double len = a.distanceTo(b);
			int n = Math.max(1, (int) Math.ceil(len / step));
			for (int k = 0; k < n; k++) {
				if (keep < 1.0 && Pt.rnd() > keep) {
					continue;
				}
				double u = k / (double) n;
				s.at(new Vec3(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u, a.z + (b.z - a.z) * u));
			}
		}
	}

	/** Recursive midpoint displacement: every pass splits each segment and pushes the middle sideways. */
	static List<Vec3> jagged(Vec3 a, Vec3 b, double relJitter, int depth, Random r) {
		List<Vec3> pts = new ArrayList<>();
		pts.add(a);
		pts.add(b);
		double amp = relJitter;
		for (int d = 0; d < depth; d++) {
			List<Vec3> next = new ArrayList<>(pts.size() * 2);
			for (int i = 0; i < pts.size() - 1; i++) {
				Vec3 p = pts.get(i);
				Vec3 q = pts.get(i + 1);
				Vec3 along = q.subtract(p);
				double len = along.length();
				Vec3 off = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
				if (len > 1.0e-6) {
					Vec3 u = along.scale(1.0 / len);
					off = off.subtract(u.scale(off.dot(u)));
				}
				Vec3 mid = new Vec3((p.x + q.x) * 0.5, (p.y + q.y) * 0.5, (p.z + q.z) * 0.5).add(off.scale(amp * len * 0.5));
				next.add(p);
				next.add(mid);
			}
			next.add(pts.get(pts.size() - 1));
			pts = next;
			amp *= 0.58;
		}
		return pts;
	}
}
