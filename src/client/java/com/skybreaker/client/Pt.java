package com.skybreaker.client;

import com.skybreaker.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/** Tiny particle toolbox. Uses "always visible" particles so beams stay visible at long range. */
final class Pt {
	static ClientLevel level;

	static double rnd() {
		return ThreadLocalRandom.current().nextDouble();
	}

	/** -1..1 */
	static double sym() {
		return ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0;
	}

	static void add(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
		level.addAlwaysVisibleParticle(o, x, y, z, vx, vy, vz);
	}

	/** Horizontal ring around c. */
	static void ring(ParticleOptions o, Vec3 c, double r, int n, double phase, double yOff, double outSpeed, double upSpeed) {
		for (int i = 0; i < n; i++) {
			double a = phase + i * (Math.PI * 2.0 / n);
			double cx = Math.cos(a);
			double sz = Math.sin(a);
			add(o, c.x + cx * r, c.y + yOff, c.z + sz * r, cx * outSpeed, upSpeed, sz * outSpeed);
		}
	}

	/** Random burst on a sphere. */
	static void burst(ParticleOptions o, Vec3 c, int n, double minSpeed, double maxSpeed, double upBias) {
		for (int i = 0; i < n; i++) {
			double vx = sym();
			double vy = sym();
			double vz = sym();
			double len = Math.max(0.001, Math.sqrt(vx * vx + vy * vy + vz * vz));
			double s = minSpeed + rnd() * (maxSpeed - minSpeed);
			add(o, c.x, c.y, c.z, vx / len * s, vy / len * s + upBias, vz / len * s);
		}
	}

	/** A full fiery detonation made only of Skybreaker particles. {@code size} ~ blocks of blast radius. */
	static void explosion(Vec3 c, double size) {
		add(ModParticles.FLARE, c.x, c.y + size * 0.15, c.z, 0, 0, 0);
		burst(ModParticles.FIRE, c, (int) (14 + size * 5), 0.03 * size + 0.05, 0.11 * size + 0.15, 0.04);
		burst(ModParticles.SMOKE, c, (int) (8 + size * 2.5), 0.02 * size, 0.07 * size + 0.05, 0.1);
		burst(ModParticles.SPARK, c, (int) (30 + size * 12), 0.25 + 0.05 * size, 0.9 + 0.2 * size, 0.2);
		burst(ModParticles.EMBER, c, (int) (10 + size * 4), 0.1, 0.5 + 0.08 * size, 0.3);
		burst(ModParticles.DUST, c, (int) (10 + size * 3), 0.05 * size, 0.1 * size + 0.1, 0.12);
		ring(ModParticles.PLASMA, c, size * 0.3, (int) (16 + size * 2), 0.0, 0.0, 0.18 + 0.05 * size, 0.0);
	}

	private Pt() {
	}
}
