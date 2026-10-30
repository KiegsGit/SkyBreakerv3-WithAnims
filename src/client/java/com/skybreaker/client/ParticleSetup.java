package com.skybreaker.client;

import com.skybreaker.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.core.particles.SimpleParticleType;

/** Gives every Skybreaker particle its size (blocks, half-width), size jitter and lifetime (ticks). */
final class ParticleSetup {
	static void register() {
		//   type                  size   jitter life
		reg(ModParticles.PLASMA,  0.32f, 0.25f, 10);
		reg(ModParticles.CORE,    0.16f, 0.20f, 7);
		reg(ModParticles.BOLT,    0.55f, 0.20f, 7);
		reg(ModParticles.ZAP,     0.13f, 0.30f, 9);
		reg(ModParticles.SPARK,   0.10f, 0.50f, 16);
		reg(ModParticles.EMBER,   0.16f, 0.40f, 34);
		reg(ModParticles.FIRE,    1.30f, 0.50f, 24);
		reg(ModParticles.SMOKE,   1.90f, 0.50f, 60);
		reg(ModParticles.FLARE,   5.50f, 0.00f, 7);
		reg(ModParticles.VOID_S,  0.90f, 0.00f, 3);
		reg(ModParticles.VOID_M,  1.80f, 0.00f, 3);
		reg(ModParticles.VOID_L,  2.90f, 0.00f, 3);
		reg(ModParticles.DISK,    4.50f, 0.00f, 3);
		reg(ModParticles.WISP,    0.50f, 0.40f, 12);
		reg(ModParticles.DUST,    0.90f, 0.50f, 36);
	}

	private static void reg(SimpleParticleType type, float size, float jitter, int life) {
		ParticleProviderRegistry.getInstance().register(type, sprites -> new GlowProvider(sprites, size, jitter, life));
	}

	private ParticleSetup() {
	}
}
