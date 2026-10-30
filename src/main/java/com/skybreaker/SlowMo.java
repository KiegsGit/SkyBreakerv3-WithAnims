package com.skybreaker;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Brief bullet-time at impact. Uses the vanilla tick-rate manager (same thing as /tick rate),
 * so particles, debris and shockwaves really play in slow motion. Restored by real time, never by ticks,
 * and only ever when a single player is on the server.
 */
public final class SlowMo {
	private static boolean active = false;
	private static long restoreAtMs = 0;
	private static float original = 20.0f;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (active && System.currentTimeMillis() >= restoreAtMs) {
				restore(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(SlowMo::restore);
	}

	public static void trigger(MinecraftServer server) {
		SkybreakerConfig cfg = SkybreakerConfig.get();
		if (!cfg.slowMotion || active || cfg.slowMotionMillis <= 0 || server.getPlayerCount() > 1) {
			return;
		}
		var manager = server.tickRateManager();
		if (Math.abs(manager.tickrate() - 20.0f) > 0.01f) {
			return; // someone else changed the tick rate; don't interfere
		}
		original = manager.tickrate();
		manager.setTickRate(cfg.slowMotionRate);
		active = true;
		restoreAtMs = System.currentTimeMillis() + cfg.slowMotionMillis;
	}

	private static void restore(MinecraftServer server) {
		if (!active) {
			return;
		}
		active = false;
		server.tickRateManager().setTickRate(original);
	}

	private SlowMo() {
	}
}
