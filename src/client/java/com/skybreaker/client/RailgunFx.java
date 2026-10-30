package com.skybreaker.client;

import net.minecraft.client.multiplayer.ClientLevel;
import com.skybreaker.ModParticles;
import net.minecraft.world.phys.Vec3;

/** Targeting ring + converging lock-on, orbital beam, descending bolt, impact flash and expanding shockwave. */
final class RailgunFx extends Fx {
	private static final double SKY = 110.0;
	private final int countdown;
	private final int bolt;
	private final double radius;

	RailgunFx(Vec3 pos, int countdown, int bolt, double radius) {
		super(pos);
		this.countdown = Math.max(1, countdown);
		this.bolt = bolt;
		this.radius = radius;
	}

	@Override
	boolean tick(ClientLevel level) {
		Pt.level = level;
		age++;
		if (age <= countdown) {
			charge();
		} else if (age <= countdown + bolt) {
			bolt();
		} else {
			aftermath();
		}
		return age > countdown + bolt + 45;
	}

	private void charge() {
		double prog = age / (double) countdown;
		double rot = age * 0.22;
		// fixed futuristic targeting ring + 4 rotating spokes
		Pt.ring(ModParticles.PLASMA, pos, 3.6, 40, rot * 0.5, 0.12, 0.0, 0.0);
		for (int k = 0; k < 4; k++) {
			double a = rot * 0.6 + k * Math.PI / 2.0;
			for (int s = 0; s < 6; s++) {
				double r = 1.2 + s * 0.65;
				Pt.add(ModParticles.ZAP, pos.x + Math.cos(a) * r, pos.y + 0.15, pos.z + Math.sin(a) * r, 0, 0, 0);
			}
		}
		// converging lock-on ring
		double r2 = 8.0 - 6.2 * prog;
		Pt.ring(ModParticles.ZAP, pos, r2, 30, -rot * 1.4, 0.15, 0.0, 0.0);
		// orbital guide beam that grows brighter as the shot approaches
		int n = 3 + (int) (prog * prog * 40);
		double width = 0.05 + 0.3 * prog;
		for (int i = 0; i < n; i++) {
			double h = Pt.rnd() * 95.0;
			Pt.add(ModParticles.ZAP, pos.x + Pt.sym() * width, pos.y + h, pos.z + Pt.sym() * width, 0, 0, 0);
		}
		if (age % 2 == 0) {
			for (int i = 0; i < 4; i++) {
				double h = 40.0 + Pt.rnd() * 60.0;
				Pt.add(ModParticles.CORE, pos.x + Pt.sym() * 0.2, pos.y + h, pos.z + Pt.sym() * 0.2, 0, -0.9, 0);
			}
		}
		// ground glow in the last second
		if (age > countdown - 20) {
			Pt.ring(ModParticles.CORE, pos, 1.0 + Pt.rnd() * 2.0, 3, Pt.rnd() * 6.28, 0.1, 0.0, 0.15);
		}
	}

	private void bolt() {
		double t = (age - countdown) / (double) bolt;
		double head = SKY * (1.0 - t);
		// the beam column, brightest at the falling head
		for (double h = head; h <= SKY; h += 1.0) {
			double a = Pt.rnd() * Math.PI * 2.0;
			double rr = Pt.rnd() * 0.9;
			Pt.add(ModParticles.CORE, pos.x + Math.cos(a) * rr, pos.y + h, pos.z + Math.sin(a) * rr, 0, -0.4, 0);
			if (((int) h) % 2 == 0) {
				Pt.add(ModParticles.ZAP, pos.x + Pt.sym() * 1.4, pos.y + h, pos.z + Pt.sym() * 1.4, 0, 0, 0);
			}
		}
		Pt.burst(ModParticles.CORE, new Vec3(pos.x, pos.y + head, pos.z), 14, 0.2, 0.8, 0.0);
		Pt.ring(ModParticles.ZAP, pos, 1.5, 16, Pt.rnd(), 0.2, 0.5, 0.1);
		if (age == countdown + bolt) {
			impact();
		}
	}

	private void impact() {
		Vec3 c = new Vec3(pos.x, pos.y + 0.5, pos.z);
		Pt.explosion(c, 5.0);
		Pt.add(ModParticles.FLARE, c.x, c.y + 0.5, c.z, 0, 0, 0);
		Pt.burst(ModParticles.CORE, c, 260, 0.6, 2.4, 0.15);
		Pt.burst(ModParticles.ZAP, c, 160, 1.0, 2.8, 0.2);
		Pt.burst(ModParticles.DUST, c, 70, 0.3, 1.2, 0.5);
		Pt.burst(ModParticles.SPARK, c, 60, 0.5, 1.6, 0.4);
	}

	private void aftermath() {
		int s = age - (countdown + bolt);
		double maxR = radius * 2.3;
		double r = Math.min(maxR, s * 0.95);
		int n = (int) Math.min(170, 24 + r * 7);
		Pt.ring(ModParticles.DUST, pos, r, n, 0.0, 0.3, 0.05, 0.02);
		Pt.ring(ModParticles.ZAP, pos, r * 0.85, n / 2, 0.5, 0.4, 0.0, 0.05);
		if (s < 12) {
			int cnt = (int) (45 * (1.0 - s / 12.0));
			for (int i = 0; i < cnt; i++) {
				double h = Pt.rnd() * 80.0;
				Pt.add(ModParticles.CORE, pos.x + Pt.sym() * 0.8, pos.y + h, pos.z + Pt.sym() * 0.8, 0, -0.3, 0);
			}
		}
		if (s < 35) {
			for (int i = 0; i < 5; i++) {
				Pt.add(ModParticles.SMOKE, pos.x + Pt.sym() * radius * 0.5, pos.y + 0.5, pos.z + Pt.sym() * radius * 0.5, 0, 0.12 + Pt.rnd() * 0.1, 0);
			}
		}
	}
}
