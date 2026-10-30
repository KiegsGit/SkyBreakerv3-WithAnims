package com.skybreaker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class MeteorStrike extends Strike {
	/** Each meteor is a small cluster of real falling-block entities: a magma core wrapped in blackstone. */
	private static final int[][] CLUSTER = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

	private final int countdown;
	private final int count;
	private final int[] delay;
	private final Vec3[] impact;
	private final List<List<FallingBlockEntity>> bodies = new ArrayList<>();
	private final int fall = FxPayload.METEOR_FALL_TICKS;
	private int maxDelay = 0;

	public MeteorStrike(ServerLevel level, UUID owner, Vec3 pos) {
		super(level, owner, pos);
		this.countdown = SkybreakerConfig.ticks(cfg.meteorCountdownSeconds);
		this.count = cfg.meteorCount;
		this.delay = new int[count];
		this.impact = new Vec3[count];
		Random rnd = new Random();
		int spread = SkybreakerConfig.ticks(cfg.meteorSpreadSeconds);
		for (int i = 0; i < count; i++) {
			double ang = rnd.nextDouble() * Math.PI * 2.0;
			double rr = cfg.meteorAreaRadius * Math.sqrt(rnd.nextDouble());
			double x = pos.x + Math.cos(ang) * rr;
			double z = pos.z + Math.sin(ang) * rr;
			double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
			if (y > pos.y + 20.0 || y < pos.y - 40.0) {
				y = pos.y; // roofs / deep pits: fall back to the aimed height
			}
			delay[i] = rnd.nextInt(spread + 1);
			maxDelay = Math.max(maxDelay, delay[i]);
			impact[i] = new Vec3(x, y, z);
			bodies.add(new ArrayList<>());
		}
	}

	@Override
	public void start() {
		// data[0] = area radius; then 4 floats per meteor: delay, dx, dy, dz (relative to the aimed point)
		float[] data = new float[1 + count * 4];
		data[0] = (float) cfg.meteorAreaRadius;
		for (int i = 0; i < count; i++) {
			data[1 + i * 4] = delay[i];
			data[2 + i * 4] = (float) (impact[i].x - pos.x);
			data[3 + i * 4] = (float) (impact[i].y - pos.y);
			data[4 + i * 4] = (float) (impact[i].z - pos.z);
		}
		StrikeUtil.broadcast(level, pos, new FxPayload(FxPayload.METEOR, pos.x, pos.y, pos.z,
			countdown, fall, (float) cfg.meteorImpactRadius, data));
		cinema(countdown, 3);
		StrikeUtil.sound(level, pos, SoundEvents.BEACON_ACTIVATE, 4.0f, 0.8f);
		StrikeUtil.sound(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 4.0f, 0.8f);
		countdownTick(countdown, "Meteor Barrage");
	}

	private Vec3 bodyPos(int i, double p) {
		double f = FxPayload.meteorFactor(p);
		return new Vec3(impact[i].x + FxPayload.ENTRY_X * f, impact[i].y + FxPayload.ENTRY_Y * f, impact[i].z + FxPayload.ENTRY_Z * f);
	}

	private void spawnBody(int i) {
		Vec3 p = bodyPos(i, 0.0);
		// spawn inside the (loaded) impact column, then move to the real entry point
		BlockPos spawnAt = BlockPos.containing(impact[i].x, impact[i].y + 60.0, impact[i].z);
		for (int k = 0; k < CLUSTER.length; k++) {
			BlockState st = k == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
			FallingBlockEntity fe = FallingBlockEntity.fall(level, spawnAt, st);
			fe.dropItem = false;
			fe.setNoGravity(true);
			fe.setDeltaMovement(0, 0, 0);
			fe.snapTo(p.x + CLUSTER[k][0], p.y + CLUSTER[k][1], p.z + CLUSTER[k][2]);
			bodies.get(i).add(fe);
		}
	}

	private void moveBody(int i, double progress) {
		Vec3 p = bodyPos(i, progress);
		List<FallingBlockEntity> list = bodies.get(i);
		for (int k = 0; k < list.size(); k++) {
			FallingBlockEntity fe = list.get(k);
			if (!fe.isRemoved()) {
				fe.setDeltaMovement(0, 0, 0);
				fe.snapTo(p.x + CLUSTER[k][0], p.y + CLUSTER[k][1], p.z + CLUSTER[k][2]);
			}
		}
	}

	private void removeBody(int i) {
		for (FallingBlockEntity fe : bodies.get(i)) {
			if (!fe.isRemoved()) {
				fe.discard();
			}
		}
		bodies.get(i).clear();
	}

	@Override
	public void cleanup() {
		for (int i = 0; i < count; i++) {
			removeBody(i);
		}
	}

	@Override
	public boolean tick() {
		age++;
		countdownTick(countdown, "Meteor Barrage");
		int perMeteorBudget = Math.max(1, cfg.maxBlocksPerStrike / Math.max(1, count));
		int perMeteorDebris = Math.max(4, cfg.debrisCount / 10);
		for (int i = 0; i < count; i++) {
			int startTick = countdown + delay[i];
			if (age == startTick) {
				StrikeUtil.sound(level, impact[i], SoundEvents.FIRECHARGE_USE, 3.0f, 0.5f);
				spawnBody(i);
			} else if (age > startTick && age < startTick + fall) {
				moveBody(i, (age - startTick) / (double) fall);
			}
			if (age == startTick + fall) {
				removeBody(i);
				double r = cfg.meteorImpactRadius;
				StrikeUtil.sound(level, impact[i], SoundEvents.GENERIC_EXPLODE, 7.0f, 0.7f);
				StrikeUtil.blast(level, impact[i], r * 1.8, cfg.meteorDamage, owner, true, 6.0f);
				if (cfg.terrainDestruction && cfg.meteorTerrain) {
					StrikeUtil.crater(level, impact[i].add(0, -r * 0.3, 0), r, perMeteorBudget, perMeteorDebris);
				}
				StrikeUtil.shake(level, impact[i], 16, 1.2f);
				StrikeUtil.flash(level, impact[i], 0.3f, 8);
			}
		}
		return age > countdown + maxDelay + fall + 1;
	}
}
