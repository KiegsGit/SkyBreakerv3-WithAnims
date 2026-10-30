package com.skybreaker.client;

import com.skybreaker.FxPayload;
import com.skybreaker.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/**
 * Singularity Cannon visuals.
 * Intro: violet energy streams in, a ground sigil spins up, the tear flickers into being and arcs crackle.
 * Pull: a black sphere with a photon ring, a tilted spinning accretion disk, polar jets, matter spiralling in.
 * Collapse: the sphere implodes, flash, fireball, expanding shockwave rings, dust and a smoke column.
 */
final class BlackHoleFx extends Fx {
	private static final double TILT = 0.5;

	private final int countdown;
	private final int pull;
	private final double pullRadius;
	private final double collapseRadius;
	private final Vec3 core;

	BlackHoleFx(Vec3 pos, int countdown, int pull, double pullRadius, float[] data) {
		super(pos);
		this.countdown = Math.max(1, countdown);
		this.pull = Math.max(1, pull);
		this.pullRadius = pullRadius;
		this.collapseRadius = data.length > 0 ? data[0] : 16.0;
		this.core = new Vec3(pos.x, pos.y + FxPayload.BH_HEIGHT, pos.z);
	}

	@Override
	boolean tick(ClientLevel level) {
		Pt.level = level;
		age++;
		if (age <= countdown) {
			intro();
		} else if (age <= countdown + pull) {
			suck(age - countdown);
		} else {
			aftermath(age - (countdown + pull));
		}
		return age > countdown + pull + 56;
	}

	private Vec3 randomDir() {
		double vx = Pt.sym();
		double vy = Pt.sym();
		double vz = Pt.sym();
		double len = Math.max(0.001, Math.sqrt(vx * vx + vy * vy + vz * vz));
		return new Vec3(vx / len, vy / len, vz / len);
	}

	private void intro() {
		double prog = age / (double) countdown;

		// violet energy streaming into the point where the tear will open
		int n = 3 + (int) (prog * 16);
		for (int i = 0; i < n; i++) {
			Vec3 d = randomDir();
			double r = (7.0 + Pt.rnd() * 12.0) * (1.0 - 0.35 * prog);
			double sp = 0.30 + 0.25 * prog;
			Pt.add(ModParticles.WISP, core.x + d.x * r, core.y + d.y * r, core.z + d.z * r, -d.x * sp, -d.y * sp, -d.z * sp);
		}

		// spinning ground sigil and a ring that marks the pull radius
		Pt.ring(ModParticles.PLASMA, pos, 5.0 + 3.0 * (1.0 - prog), 30, age * 0.11, 0.12, 0.0, 0.0);
		Pt.ring(ModParticles.ZAP, pos, 2.5 + 1.5 * (1.0 - prog), 18, -age * 0.17, 0.15, 0.0, 0.0);
		double rr = pullRadius * prog;
		Pt.ring(ModParticles.ZAP, pos, rr, (int) Math.min(120, 16 + rr * 3.5), -age * 0.04, 0.2, 0.0, 0.0);
		for (int k = 0; k < 4; k++) {
			double a = age * 0.08 + k * Math.PI / 2.0;
			for (int s = 1; s <= 5; s++) {
				double r = 5.0 * s / 5.0;
				Pt.add(ModParticles.ZAP, pos.x + Math.cos(a) * r, pos.y + 0.15, pos.z + Math.sin(a) * r, 0, 0, 0);
			}
		}

		// the tear flickers into being
		if (prog > 0.30 && (prog > 0.6 || age % 2 == 0)) {
			Pt.add(ModParticles.DISK, core.x, core.y, core.z, 0, 0, 0);
			Pt.add(prog > 0.75 ? ModParticles.VOID_M : ModParticles.VOID_S, core.x, core.y, core.z, 0, 0, 0);
		}
		// arcs crackling between the tear and the ground
		if (prog > 0.35 && age % 4 == 0) {
			double a = Pt.rnd() * Math.PI * 2.0;
			double r = 3.0 + Pt.rnd() * 9.0;
			BoltFx.arc(core, new Vec3(pos.x + Math.cos(a) * r, pos.y + 0.2, pos.z + Math.sin(a) * r), (long) (Pt.rnd() * 1.0e9));
		}
		if (age == countdown) {
			Pt.add(ModParticles.FLARE, core.x, core.y, core.z, 0, 0, 0);
			Pt.burst(ModParticles.PLASMA, core, 40, 0.3, 1.0, 0.0);
		}
	}

	private void suck(int t) {
		double prog = t / (double) pull;
		boolean collapsing = t > pull - 8;

		// accretion glow first, then the black sphere on top of it
		Pt.add(ModParticles.DISK, core.x, core.y, core.z, 0, 0, 0);
		SimpleParticleType body = collapsing ? ModParticles.VOID_S : (prog < 0.25 ? ModParticles.VOID_M : ModParticles.VOID_L);
		Pt.add(body, core.x, core.y, core.z, 0, 0, 0);
		if (collapsing) {
			Pt.add(ModParticles.FLARE, core.x, core.y, core.z, 0, 0, 0);
		}

		disk(t, prog);
		jets(prog);
		infall(t, prog, collapsing);

		// lightning stabbing into the core from above
		if (t % 14 == 0) {
			double a = Pt.rnd() * Math.PI * 2.0;
			double r = 4.0 + Pt.rnd() * 10.0;
			BoltFx.arc(new Vec3(core.x + Math.cos(a) * r, core.y + 8.0 + Pt.rnd() * 10.0, core.z + Math.sin(a) * r), core, (long) (Pt.rnd() * 1.0e9));
		}
		// pull ring on the ground, collapsing inward
		if (t % 3 == 0) {
			double r = pullRadius * (1.0 - ((t % 30) / 30.0) * 0.8);
			Pt.ring(ModParticles.DUST, pos, r, (int) Math.min(40, 14 + r), Pt.rnd() * 6.28, 0.3, -0.22, 0.02);
		}
	}

	/** Tilted, spinning accretion disk: four spiral arms, hot white near the hole, orange further out, violet streamers at the rim. */
	private void disk(int t, double prog) {
		double rMax = 6.5 + 5.5 * Math.min(1.0, prog * 2.0);
		double cosT = Math.cos(TILT);
		double sinT = Math.sin(TILT);
		for (int arm = 0; arm < 4; arm++) {
			for (int j = 0; j < 15; j++) {
				double r = 2.7 + j * (rMax - 2.7) / 14.0;
				double spin = t * (3.2 / (r + 1.0)) + arm * Math.PI * 0.5 + 1.9 * Math.log(r);
				double px = Math.cos(spin) * r;
				double pz = Math.sin(spin) * r;
				// tilt the disk plane around the X axis
				double py = -pz * sinT;
				double pz2 = pz * cosT;
				double x = core.x + px;
				double y = core.y + py;
				double z = core.z + pz2;
				SimpleParticleType o = r < 4.3 ? ModParticles.CORE : (r < 7.0 ? ModParticles.SPARK : ModParticles.EMBER);
				Pt.add(o, x, y, z, 0, 0, 0);
				if (j % 4 == 3) {
					Pt.add(ModParticles.WISP, x, y, z, 0, 0, 0);
				}
			}
		}
	}

	/** Polar jets shooting out along the disk's axis. */
	private void jets(double prog) {
		double nx = 0.0;
		double ny = Math.cos(TILT);
		double nz = Math.sin(TILT);
		int n = 4 + (int) (prog * 6);
		for (int i = 0; i < n; i++) {
			double h = 2.8 + Pt.rnd() * (8.0 + 8.0 * prog);
			double w = 0.15 + h * 0.03;
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				Pt.add(ModParticles.WISP, core.x + nx * h * sgn + Pt.sym() * w, core.y + ny * h * sgn + Pt.sym() * w,
					core.z + nz * h * sgn + Pt.sym() * w, 0, 0, 0);
			}
		}
	}

	/** Dust, embers and violet energy spiralling in from the edge of the pull radius, climbing up into the core. */
	private void infall(int t, double prog, boolean collapsing) {
		double r0 = pullRadius * (0.5 + 0.5 * Math.min(1.0, prog * 3.0));
		for (int stream = 0; stream < 3; stream++) {
			for (int i = 0; i < 9; i++) {
				double u = Pt.rnd();               // 0 = at the core, 1 = at the rim
				double r = 2.0 + u * (r0 - 2.0);
				double ang = -t * 0.13 + stream * (Math.PI * 2.0 / 3.0) + 3.4 * Math.log(r);
				double lift = FxPayload.BH_HEIGHT * Math.pow(1.0 - u, 1.6);
				double x = pos.x + Math.cos(ang) * r;
				double z = pos.z + Math.sin(ang) * r;
				double y = pos.y + 0.3 + lift;
				if (i % 3 == 0) {
					Pt.add(ModParticles.EMBER, x, y, z, 0, 0, 0);
				} else if (i % 3 == 1) {
					Pt.add(ModParticles.WISP, x, y, z, 0, 0, 0);
				} else if (u < 0.5) {
					Pt.add(ModParticles.DUST, x, y, z, 0, 0.02, 0);
				}
			}
		}
		if (collapsing) {
			// everything gets squeezed toward the centre
			for (int i = 0; i < 24; i++) {
				Vec3 d = randomDir();
				double r = 3.0 + Pt.rnd() * 6.0;
				Pt.add(ModParticles.PLASMA, core.x + d.x * r, core.y + d.y * r, core.z + d.z * r, -d.x * 0.7, -d.y * 0.7, -d.z * 0.7);
			}
		}
	}

	private void aftermath(int s) {
		Vec3 ground = new Vec3(pos.x, pos.y + 0.6, pos.z);
		if (s == 1) {
			Pt.add(ModParticles.FLARE, core.x, core.y, core.z, 0, 0, 0);
			Pt.add(ModParticles.FLARE, ground.x, ground.y, ground.z, 0, 0, 0);
			Pt.explosion(ground, collapseRadius * 0.55);
			Pt.explosion(core, collapseRadius * 0.35);
			Pt.burst(ModParticles.SPARK, core, 220, 0.5, 2.8, 0.0);
			Pt.burst(ModParticles.PLASMA, core, 120, 0.6, 2.2, 0.0);
			for (int i = 0; i < 6; i++) {
				BoltFx.arc(core, new Vec3(pos.x + Pt.sym() * collapseRadius, pos.y + 0.3, pos.z + Pt.sym() * collapseRadius), (long) (Pt.rnd() * 1.0e9));
			}
		}
		if (s <= 4) {
			Pt.add(ModParticles.VOID_S, core.x, core.y, core.z, 0, 0, 0);
		}
		// expanding shockwave rings at the core height and on the ground
		double maxR = collapseRadius * 2.6;
		double r = Math.min(maxR, s * 1.8);
		int n = (int) Math.min(200, 28 + r * 6);
		if (r < maxR) {
			Pt.ring(ModParticles.PLASMA, core, r, n, s * 0.2, 0.0, 0.0, 0.0);
			Pt.ring(ModParticles.CORE, ground, r * 0.9, n, 0.0, 0.2, 0.0, 0.04);
			Pt.ring(ModParticles.DUST, ground, r * 0.8, n / 2, 0.5, 0.4, 0.12, 0.05);
		}
		if (s < 14) {
			Pt.ring(ModParticles.FIRE, ground, r * 0.5, 14, 0.0, 0.3, 0.2, 0.08);
		}
		// smoke column and falling embers
		if (s < 44) {
			for (int i = 0; i < 4; i++) {
				Pt.add(ModParticles.SMOKE, pos.x + Pt.sym() * collapseRadius * 0.4, pos.y + 0.5, pos.z + Pt.sym() * collapseRadius * 0.4, 0, 0.14 + Pt.rnd() * 0.12, 0);
			}
			Pt.add(ModParticles.EMBER, pos.x + Pt.sym() * collapseRadius, pos.y + 1.0 + Pt.rnd() * 6.0, pos.z + Pt.sym() * collapseRadius, 0, -0.03, 0);
		}
	}
}
