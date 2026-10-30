package com.skybreaker;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Holds every running strike and advances them once per tick of their own dimension. */
public final class StrikeManager {
	private static final List<Strike> ACTIVE = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(StrikeManager::tickLevel);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ACTIVE.clear();
			StrikeItem.clearCooldowns();
		});
	}

	public static void add(Strike strike) {
		strike.start();
		ACTIVE.add(strike);
	}

	private static void tickLevel(ServerLevel level) {
		Iterator<Strike> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Strike s = it.next();
			if (s.level != level) {
				continue;
			}
			try {
				if (s.tick()) {
					it.remove();
				}
			} catch (Exception e) {
				SkybreakerMod.LOGGER.error("Strike failed, removing it", e);
				try {
					s.cleanup();
				} catch (Exception ignored) {
				}
				it.remove();
			}
		}
	}

	private StrikeManager() {
	}
}
