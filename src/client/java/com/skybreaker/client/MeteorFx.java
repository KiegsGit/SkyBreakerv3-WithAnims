package com.skybreaker.client;

import com.skybreaker.FxPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import com.skybreaker.ModParticles;
import net.minecraft.world.phys.Vec3;

/** Area marker ring, then every meteor falls with its own fiery trail and explodes on impact. */
final class MeteorFx extends Fx {
	private final int countdown;
	private final int fall;
	private final double impactRadius;
	private final double areaRadius;
	private final int count;
	private final int[] delay;
	private final Vec3[] impact;
	private int maxDelay = 0;

	MeteorFx(Vec3 pos, int countdown, int fall, double impactRadius, float[] data) {
		super(pos);
		this.countdown = Math.max(1, countdown);
		this.fall = Math.max(1, fall);
		this.impactRadius = impactRadius;
		this.areaRadius = data.length > 0 ? data[0] : 10.0;
		this.count = Math.max(0, (data.length - 1) / 4);
		this.delay = new int[count];
		this.impact = new Vec3[count];
		for (int i = 0; i < count; i++) {
			delay[i] = (int) data[1 + i * 4];
			maxDelay = Math.max(maxDelay, delay[i]);
			impact[i] = new Vec3(pos.x + data[2 + i * 4], pos.y + data[3 + i * 4], pos.z + data[4 + i * 4]);
		}
	}

	@Override
	boolean tick(ClientLevel level) {
		Pt.level = level;
		age++;
		if (age <= countdown) {
			marker();
		}
		for (int i = 0; i < count; i++) {
			int start = countdown + delay[i];
			if (age >= start - 20 && age < start + fall && age % 4 == 0) {
				Pt.ring(ModParticles.EMBER, impact[i], impactRadius * 0.8, 10, age * 0.2, 0.15, 0.0, 0.0);
			}
			if (age >= start && age < start + fall) {
				trail(i, age - start);
			} else if (age == start + fall) {
				boom(i);
			}
		}
		return age > countdown + maxDelay + fall + 20;
	}

	private void marker() {
		double prog = age / (double) countdown;
		double rot = age * 0.12;
		int n = (int) Math.min(220, areaRadius * 9);
		Pt.ring(ModParticles.PLASMA, pos, areaRadius, n, rot * 0.3, 0.15, 0.0, 0.0);
		Pt.ring(ModParticles.ZAP, pos, areaRadius * (1.0 - 0.75 * prog), n / 2, -rot, 0.2, 0.0, 0.0);
		for (int k = 0; k < 6; k++) {
			double a = rot + k * Math.PI / 3.0;
			for (int s = 1; s <= 5; s++) {
				double r = areaRadius * s / 5.0 * 0.95;
				Pt.add(ModParticles.ZAP, pos.x + Math.cos(a) * r, pos.y + 0.2, pos.z + Math.sin(a) * r, 0, 0, 0);
			}
		}
		for (int i = 0; i < 3 + (int) (prog * 12); i++) {
			Pt.add(ModParticles.ZAP, pos.x + Pt.sym() * 0.2, pos.y + Pt.rnd() * 90.0, pos.z + Pt.sym() * 0.2, 0, 0, 0);
		}
	}

	private Vec3 at(int i, double p) {
		double f = FxPayload.meteorFactor(p);
		return new Vec3(impact[i].x + FxPayload.ENTRY_X * f, impact[i].y + FxPayload.ENTRY_Y * f, impact[i].z + FxPayload.ENTRY_Z * f);
	}

	private void trail(int i, int t) {
		double p0 = Math.max(0.0, (t - 1) / (double) fall);
		double p1 = t / (double) fall;
		Vec3 a = at(i, p0);
		Vec3 b = at(i, p1);
		for (int k = 0; k <= 6; k++) {
			double u = k / 6.0;
			double x = a.x + (b.x - a.x) * u;
			double y = a.y + (b.y - a.y) * u;
			double z = a.z + (b.z - a.z) * u;
			Pt.add(ModParticles.EMBER, x + Pt.sym() * 0.3, y + Pt.sym() * 0.3, z + Pt.sym() * 0.3, Pt.sym() * 0.03, Pt.sym() * 0.03, Pt.sym() * 0.03);
			if (k % 2 == 0) {
				Pt.add(ModParticles.SMOKE, x, y, z, 0, 0.02, 0);
			}
		}
		Pt.burst(ModParticles.EMBER, b, 8, 0.02, 0.15, 0.0);
		Pt.add(ModParticles.CORE, b.x, b.y, b.z, 0, 0, 0);
	}

	private void boom(int i) {
		Vec3 c = new Vec3(impact[i].x, impact[i].y + 0.4, impact[i].z);
		Pt.explosion(c, impactRadius * 0.55);
		Pt.burst(ModParticles.EMBER, c, 60, 0.2, 1.2, 0.25);
		Pt.burst(ModParticles.SMOKE, c, 30, 0.1, 0.6, 0.3);
		Pt.burst(ModParticles.EMBER, c, 12, 0.2, 0.8, 0.3);
		Pt.ring(ModParticles.DUST, c, impactRadius * 0.5, 28, 0.0, 0.1, 0.55, 0.04);
	}
}
