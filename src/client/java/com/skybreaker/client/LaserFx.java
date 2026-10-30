package com.skybreaker.client;

import net.minecraft.client.multiplayer.ClientLevel;
import com.skybreaker.ModParticles;
import net.minecraft.world.phys.Vec3;

/** Charging orb in the sky, converging lock-on, then a sustained blue-white beam with spiral and ground sparks. */
final class LaserFx extends Fx {
	private static final double SKY = 120.0;
	private final int countdown;
	private final int duration;
	private final double radius;

	LaserFx(Vec3 pos, int countdown, int duration, double radius) {
		super(pos);
		this.countdown = Math.max(1, countdown);
		this.duration = duration;
		this.radius = radius;
	}

	@Override
	boolean tick(ClientLevel level) {
		Pt.level = level;
		age++;
		if (age <= countdown) {
			charge();
		} else if (age <= countdown + duration) {
			sustain();
		} else {
			fade();
		}
		return age > countdown + duration + 14;
	}

	private void orb(double size, int count, double inward) {
		Vec3 top = new Vec3(pos.x, pos.y + SKY - 8.0, pos.z);
		for (int i = 0; i < count; i++) {
			double vx = Pt.sym();
			double vy = Pt.sym();
			double vz = Pt.sym();
			double len = Math.max(0.001, Math.sqrt(vx * vx + vy * vy + vz * vz));
			double r = size * (0.7 + 0.3 * Pt.rnd());
			Pt.add(ModParticles.CORE, top.x + vx / len * r, top.y + vy / len * r, top.z + vz / len * r,
				-vx / len * inward, -vy / len * inward, -vz / len * inward);
		}
	}

	private void charge() {
		double prog = age / (double) countdown;
		double rot = age * 0.25;
		Pt.ring(ModParticles.PLASMA, pos, radius + 1.5, 36, rot * 0.4, 0.12, 0.0, 0.0);
		double r2 = (radius + 7.0) * (1.0 - prog) + radius * 0.5;
		Pt.ring(ModParticles.ZAP, pos, r2, 28, -rot, 0.15, 0.0, 0.0);
		// double helix climbing a thin guide beam
		if (age % 2 == 0) {
			for (int i = 0; i < 20; i++) {
				double h = i * 5.0;
				double ang = h * 0.35 + age * 0.35;
				Pt.add(ModParticles.CORE, pos.x + Math.cos(ang) * 1.0, pos.y + h, pos.z + Math.sin(ang) * 1.0, 0, 0, 0);
				Pt.add(ModParticles.CORE, pos.x - Math.cos(ang) * 1.0, pos.y + h, pos.z - Math.sin(ang) * 1.0, 0, 0, 0);
			}
		}
		int n = 2 + (int) (prog * 18);
		for (int i = 0; i < n; i++) {
			Pt.add(ModParticles.ZAP, pos.x + Pt.sym() * 0.15, pos.y + Pt.rnd() * SKY, pos.z + Pt.sym() * 0.15, 0, 0, 0);
		}
		orb(0.6 + 3.0 * prog, 6 + (int) (prog * 14), 0.25);
	}

	private void sustain() {
		int t = age - countdown;
		double core = radius * 0.35;
		for (int i = 0; i < 95; i++) {
			Pt.add(ModParticles.ZAP, pos.x + Pt.sym() * core, pos.y + Pt.rnd() * SKY, pos.z + Pt.sym() * core, 0, 0, 0);
		}
		for (int i = 0; i < 26; i++) {
			double a = Pt.rnd() * Math.PI * 2.0;
			double rr = Pt.rnd() * core * 0.7;
			Pt.add(ModParticles.CORE, pos.x + Math.cos(a) * rr, pos.y + Pt.rnd() * SKY, pos.z + Math.sin(a) * rr, 0, -0.7, 0);
		}
		for (int i = 0; i < 30; i++) {
			double h = i * 4.0;
			double ang = h * 0.3 - age * 0.5;
			double rr = radius * 0.75;
			Pt.add(ModParticles.PLASMA, pos.x + Math.cos(ang) * rr, pos.y + h, pos.z + Math.sin(ang) * rr, 0, 0, 0);
		}
		// where the beam meets the ground
		Pt.ring(ModParticles.EMBER, pos, radius, 24, age * 0.3, 0.1, 0.06, 0.04);
		Pt.burst(ModParticles.ZAP, new Vec3(pos.x, pos.y + 0.3, pos.z), 8, 0.3, 0.9, 0.35);
		for (int i = 0; i < 2; i++) {
			Pt.add(ModParticles.SMOKE, pos.x + Pt.sym() * radius * 0.5, pos.y + 0.3, pos.z + Pt.sym() * radius * 0.5, 0, 0.15, 0);
		}
		if (t % 10 == 0) {
			Pt.ring(ModParticles.DUST, pos, 0.5, 20, 0, 0.2, 0.35, 0.02);
		}
		orb(3.4, 10, 0.0);
	}

	private void fade() {
		int s = age - (countdown + duration);
		double k = 1.0 - s / 14.0;
		int n = (int) (60 * k);
		for (int i = 0; i < n; i++) {
			Pt.add(ModParticles.ZAP, pos.x + Pt.sym() * radius * 0.3, pos.y + Pt.rnd() * SKY * k, pos.z + Pt.sym() * radius * 0.3, 0, 0, 0);
		}
		if (s == 1) {
			Vec3 c = new Vec3(pos.x, pos.y + 0.5, pos.z);
			Pt.explosion(c, 4.0);
			Pt.burst(ModParticles.CORE, c, 90, 0.4, 1.6, 0.2);
		}
	}
}
