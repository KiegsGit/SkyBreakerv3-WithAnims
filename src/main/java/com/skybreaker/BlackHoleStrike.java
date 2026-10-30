package com.skybreaker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Singularity Cannon. Timeline (all decided here, the client only draws it):
 * 1. cinematic intro (countdown): letterbox, title card, a spacetime tear opens above the target;
 * 2. the black hole hangs {@link FxPayload#BH_HEIGHT} blocks over the aimed block and drags every living thing
 *    in range toward it; anything that reaches the core is held there and crushed;
 * 3. collapse: everything still stuck in the core is killed for certain, then a huge implosion blast,
 *    crater, shockwave, lightning and bullet-time.
 */
public final class BlackHoleStrike extends Strike {
	private final int countdown;
	private final int pull;
	private final Vec3 core;
	private int removed = 0;

	public BlackHoleStrike(ServerLevel level, UUID owner, Vec3 pos) {
		super(level, owner, pos);
		this.countdown = SkybreakerConfig.ticks(cfg.blackHoleCountdownSeconds);
		this.pull = SkybreakerConfig.ticks(cfg.blackHolePullSeconds);
		this.core = pos.add(0.0, FxPayload.BH_HEIGHT, 0.0);
	}

	@Override
	public void start() {
		StrikeUtil.broadcast(level, pos, new FxPayload(FxPayload.BLACKHOLE, pos.x, pos.y, pos.z,
			countdown, pull, (float) cfg.blackHolePullRadius, new float[] {(float) cfg.blackHoleCollapseRadius}));
		cinema(countdown, 4, new float[] {(float) pull});
		StrikeUtil.sound(level, pos, SoundEvents.BEACON_ACTIVATE, 5.0f, 0.5f);
		StrikeUtil.sound(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 5.0f, 0.6f);
		countdownTick(countdown, "Singularity Cannon");
	}

	@Override
	public boolean tick() {
		age++;
		countdownTick(countdown, "Singularity Cannon");
		if (age < countdown) {
			if (age % 20 == 0) {
				float k = age / (float) countdown;
				StrikeUtil.sound(level, pos, SoundEvents.BEACON_AMBIENT, 5.0f, 0.45f + 0.35f * k);
				StrikeUtil.sound(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 4.0f, 0.5f + 0.5f * k);
			}
			return false;
		}
		int t = age - countdown;
		if (t == 0) {
			// the tear opens
			StrikeUtil.sound(level, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 10.0f, 0.5f);
			StrikeUtil.sound(level, pos, SoundEvents.WARDEN_SONIC_BOOM, 5.0f, 0.5f);
			StrikeUtil.recoil(ownerPlayer(), 14, 2.0f);
			StrikeUtil.flash(level, pos, 0.55f, 22);
			StrikeUtil.shake(level, pos, pull + 20, 0.9f);
		}
		if (t < pull) {
			pull(t);
			return false;
		}
		if (t == pull) {
			collapse();
			return false;
		}
		int s = t - pull;
		if (s <= 24 && s % 3 == 0) {
			StrikeUtil.lightningNear(level, pos, cfg.blackHoleCollapseRadius * 0.2, cfg.blackHoleCollapseRadius * 1.6);
		}
		if (s == 10) {
			secondWave();
		}
		return s >= 44;
	}

	private DamageSource source() {
		ServerPlayer shooter = ownerPlayer();
		return shooter != null ? level.damageSources().playerAttack(shooter) : level.damageSources().generic();
	}

	private void pull(int t) {
		double prog = t / (double) pull;
		double radius = cfg.blackHolePullRadius * (0.4 + 0.6 * Math.min(1.0, prog * 3.0));
		double capture = cfg.blackHoleKillRadius * (0.6 + 0.4 * prog);

		if (t % 25 == 0) {
			StrikeUtil.sound(level, pos, SoundEvents.BEACON_AMBIENT, 7.0f, 0.5f + 0.3f * (float) prog);
			StrikeUtil.sound(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 6.0f, 0.5f + 0.5f * (float) prog);
		}
		if (t % 12 == 0) {
			StrikeUtil.shake(level, pos, 16, 0.6f + 1.6f * (float) prog);
		}

		AABB box = new AABB(core.x - radius, core.y - radius, core.z - radius, core.x + radius, core.y + radius, core.z + radius);
		DamageSource source = source();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
			if (!StrikeUtil.validTarget(e, owner)) {
				continue;
			}
			Vec3 mid = e.getBoundingBox().getCenter();
			Vec3 to = core.subtract(mid);
			double dist = to.length();
			if (dist > radius || dist < 0.001) {
				continue;
			}
			Vec3 dir = to.scale(1.0 / dist);
			if (dist <= capture) {
				// stuck in the core: held in place and crushed
				e.setDeltaMovement(dir.scale(Math.min(0.25, dist * 0.2)).add(0.0, 0.03, 0.0));
				e.push(0.0, 0.0, 0.0); // marks the new velocity to be sent to clients
				if (t % 8 == 0) {
					e.hurtServer(level, source, 6.0f + (float) (14.0 * prog));
				}
			} else {
				double near = 1.0 - dist / radius;
				double force = 0.10 + 0.55 * near + 0.15 * prog;
				Vec3 swirl = new Vec3(-dir.z, 0.0, dir.x).scale(0.12 * near);
				Vec3 v = e.getDeltaMovement().scale(0.82).add(dir.scale(force)).add(swirl).add(0.0, 0.04, 0.0);
				double sp = v.length();
				if (sp > 1.6) {
					v = v.scale(1.6 / sp);
				}
				e.setDeltaMovement(v);
				e.push(0.0, 0.0, 0.0); // marks the new velocity to be sent to clients
			}
		}

		if (cfg.terrainDestruction && cfg.blackHoleTerrain && t % 3 == 0 && prog > 0.12) {
			removed += StrikeUtil.pullBlocks(level, pos, core, Math.min(radius, 24.0), 3, Math.max(0, cfg.maxBlocksPerStrike - removed));
		}
	}

	private void collapse() {
		double r = cfg.blackHoleCollapseRadius;
		// 1. whatever is stuck in the core dies, no exceptions
		StrikeUtil.secureKill(level, core, cfg.blackHoleKillRadius * 1.6 + 1.0, owner);
		// 2. the implosion
		StrikeUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 16.0f, 0.4f);
		StrikeUtil.sound(level, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 18.0f, 0.5f);
		StrikeUtil.sound(level, pos, SoundEvents.WARDEN_SONIC_BOOM, 8.0f, 0.5f);
		StrikeUtil.blast(level, pos, r * 2.4, cfg.blackHoleCollapseDamage, owner, true, 10.0f);
		if (cfg.terrainDestruction && cfg.blackHoleTerrain) {
			removed += StrikeUtil.crater(level, pos.add(0.0, -r * 0.2, 0.0), r, Math.max(0, cfg.maxBlocksPerStrike - removed), cfg.debrisCount);
		}
		StrikeUtil.lightning(level, pos.x, pos.y, pos.z);
		StrikeUtil.shake(level, pos, 100, 3.8f);
		StrikeUtil.flash(level, pos, 1.0f, 60);
		SlowMo.trigger(level.getServer());
	}

	/** A second, wider pressure wave rolling out of the crater. */
	private void secondWave() {
		double r = cfg.blackHoleCollapseRadius;
		StrikeUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 12.0f, 0.4f);
		StrikeUtil.blast(level, pos, r * 3.4, cfg.blackHoleCollapseDamage * 0.3f, owner, true, 0.0f);
		if (cfg.terrainDestruction && cfg.blackHoleTerrain) {
			StrikeUtil.crater(level, pos.add(0.0, -r * 0.1, 0.0), r * 1.3, Math.max(0, cfg.maxBlocksPerStrike - removed), cfg.debrisCount / 2);
		}
		StrikeUtil.shake(level, pos, 50, 2.2f);
	}
}