package com.skybreaker;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** One class for all four weapons. Right-click aims at a block up to 128 blocks away and starts a strike on the server. */
public class StrikeItem extends Item {
	public enum Kind {
		RAILGUN("Orbital Railgun", 2.5),
		LASER("Satellite Laser", 2.5),
		METEOR("Meteor Barrage", 8.0),
		BLACKHOLE("Singularity Cannon", 9.0);

		public final String display;
		/** Radius of the client-side aiming reticle. */
		public final double previewRadius;

		Kind(String display, double previewRadius) {
			this.display = display;
			this.previewRadius = previewRadius;
		}
	}

	private static final Map<String, Long> COOLDOWNS = new HashMap<>();

	public final Kind kind;

	public StrikeItem(Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public static void clearCooldowns() {
		COOLDOWNS.clear();
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS; // client: just swing; the server decides everything
		}
		SkybreakerConfig cfg = SkybreakerConfig.get();

		long now = server.getGameTime();
		String key = sp.getUUID() + ":" + kind.name();
		Long until = COOLDOWNS.get(key);
		if (until != null && now < until) {
			sp.sendOverlayMessage(Component.literal("\u00a7c" + kind.display + " recharging: "
				+ String.format("%.1f", (until - now) / 20.0) + "s"));
			return InteractionResult.FAIL;
		}

		double range = Math.min(cfg.maxTargetRange, SkybreakerMod.MAX_RANGE);
		Vec3 eye = sp.getEyePosition();
		Vec3 end = eye.add(sp.getLookAngle().scale(range));
		BlockHitResult hit = server.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
		if (hit.getType() == HitResult.Type.MISS) {
			sp.sendOverlayMessage(Component.literal("\u00a7eNo target within " + (int) range + " blocks."));
			return InteractionResult.FAIL;
		}
		Vec3 target = hit.getLocation();

		Strike strike;
		double cooldown;
		switch (kind) {
			case RAILGUN -> {
				strike = new RailgunStrike(server, sp.getUUID(), target);
				cooldown = cfg.railgunCooldownSeconds;
			}
			case LASER -> {
				strike = new LaserStrike(server, sp.getUUID(), target);
				cooldown = cfg.laserCooldownSeconds;
			}
			case BLACKHOLE -> {
				strike = new BlackHoleStrike(server, sp.getUUID(), target);
				cooldown = cfg.blackHoleCooldownSeconds;
			}
			default -> {
				strike = new MeteorStrike(server, sp.getUUID(), target);
				cooldown = cfg.meteorCooldownSeconds;
			}
		}
		COOLDOWNS.put(key, now + SkybreakerConfig.ticks(cooldown));
		StrikeManager.add(strike);
		return InteractionResult.SUCCESS;
	}
}
