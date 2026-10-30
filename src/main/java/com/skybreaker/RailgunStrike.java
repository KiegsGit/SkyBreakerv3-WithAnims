package com.skybreaker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class RailgunStrike extends Strike {
	private final int countdown;
	private final int impactTick;
	private int removed = 0;

	public RailgunStrike(ServerLevel level, UUID owner, Vec3 pos) {
		super(level, owner, pos);
		this.countdown = SkybreakerConfig.ticks(cfg.railgunCountdownSeconds);
		this.impactTick = countdown + FxPayload.BOLT_TICKS;
	}

	@Override
	public void start() {
		StrikeUtil.broadcast(level, pos, new FxPayload(FxPayload.RAILGUN, pos.x, pos.y, pos.z,
			countdown, FxPayload.BOLT_TICKS, (float) cfg.railgunExplosionRadius, new float[0]));
		cinema(countdown, 1);
		StrikeUtil.sound(level, pos, SoundEvents.BEACON_ACTIVATE, 4.0f, 1.7f);
		StrikeUtil.sound(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 4.0f, 1.4f);
		countdownTick(countdown, "Orbital Railgun");
	}

	@Override
	public boolean tick() {
		age++;
		countdownTick(countdown, "Orbital Railgun");
		if (age == countdown) {
			// the beam fires: sonic crack, recoil and a flash for the shooter
			StrikeUtil.sound(level, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 6.0f, 2.0f);
			StrikeUtil.sound(level, pos, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 1.6f);
			StrikeUtil.recoil(ownerPlayer(), 12, 2.5f);
			StrikeUtil.flash(level, pos, 0.35f, 10);
		}
		if (age == impactTick) {
			impact();
		}
		int s = age - impactTick;
		if (s > 0 && s <= 10 && s % 2 == 0) {
			double r = cfg.railgunExplosionRadius;
			StrikeUtil.lightningNear(level, pos, r * 0.3, r * 1.5);
		}
		if (s == 8) {
			secondWave();
		}
		return s >= 12;
	}

	private void impact() {
		double r = cfg.railgunExplosionRadius;
		StrikeUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 12.0f, 0.5f);
		StrikeUtil.sound(level, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 14.0f, 0.5f);
		StrikeUtil.sound(level, pos, SoundEvents.WARDEN_SONIC_BOOM, 6.0f, 0.7f);
		StrikeUtil.blast(level, pos, r * 2.2, cfg.railgunDamage, owner, true, 8.0f);
		if (cfg.terrainDestruction && cfg.railgunTerrain) {
			removed += StrikeUtil.crater(level, pos.add(0, -r * 0.25, 0), r, cfg.maxBlocksPerStrike, cfg.debrisCount);
		}
		StrikeUtil.lightning(level, pos.x, pos.y, pos.z);
		StrikeUtil.shake(level, pos, 70, 3.2f);
		StrikeUtil.flash(level, pos, 1.0f, 45);
		SlowMo.trigger(level.getServer());
	}

	/** A second, wider pressure wave rolling out of the crater. */
	private void secondWave() {
		double r = cfg.railgunExplosionRadius;
		StrikeUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 10.0f, 0.4f);
		StrikeUtil.blast(level, pos, r * 3.2, cfg.railgunDamage * 0.35f, owner, true, 0.0f);
		if (cfg.terrainDestruction && cfg.railgunTerrain) {
			StrikeUtil.crater(level, pos.add(0, -r * 0.1, 0), r * 1.35, Math.max(0, cfg.maxBlocksPerStrike - removed), cfg.debrisCount / 2);
		}
		StrikeUtil.shake(level, pos, 40, 2.0f);
	}
}
