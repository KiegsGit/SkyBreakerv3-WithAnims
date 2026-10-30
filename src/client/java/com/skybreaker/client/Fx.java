package com.skybreaker.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** One running client-side visual effect. */
abstract class Fx {
	protected final Vec3 pos;
	protected int age = 0;

	Fx(Vec3 pos) {
		this.pos = pos;
	}

	/** Advance one tick; return true when finished. */
	abstract boolean tick(ClientLevel level);
}
