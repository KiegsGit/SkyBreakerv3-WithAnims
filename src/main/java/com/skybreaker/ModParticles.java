package com.skybreaker;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Skybreaker's own particle types. Every one has its own hand-made texture
 * (assets/skybreaker/textures/particle) and animation (assets/skybreaker/particles).
 * Sizes and lifetimes are set client-side in ParticleSetup.
 */
public final class ModParticles {
	/** Blue-white flickering plasma orb. */
	public static final SimpleParticleType PLASMA = reg("plasma");
	/** Small white-hot core, the bright centre of beams and bolts. */
	public static final SimpleParticleType CORE = reg("core");
	/** Violet-blue electric halo around lightning. */
	public static final SimpleParticleType BOLT = reg("bolt");
	/** Tiny blue-white electric spark. */
	public static final SimpleParticleType ZAP = reg("zap");
	/** Hot spark that cools from yellow to red. */
	public static final SimpleParticleType SPARK = reg("spark");
	/** Glowing coal. */
	public static final SimpleParticleType EMBER = reg("ember");
	/** Big turbulent fireball puff. */
	public static final SimpleParticleType FIRE = reg("fire");
	/** Big dark billowing smoke. */
	public static final SimpleParticleType SMOKE = reg("smoke");
	/** Anamorphic star flash. */
	public static final SimpleParticleType FLARE = reg("flare");
	/** Black hole body, three sizes (small / medium / large). */
	public static final SimpleParticleType VOID_S = reg("void_s");
	public static final SimpleParticleType VOID_M = reg("void_m");
	public static final SimpleParticleType VOID_L = reg("void_l");
	/** Soft orange-magenta accretion glow. */
	public static final SimpleParticleType DISK = reg("disk");
	/** Violet energy streamer. */
	public static final SimpleParticleType WISP = reg("wisp");
	/** Pulverised rock and dirt. */
	public static final SimpleParticleType DUST = reg("dust");

	private static SimpleParticleType reg(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, SkybreakerMod.id(name), FabricParticleTypes.simple());
	}

	public static void init() {
		// touching the class registers every field above
	}

	private ModParticles() {
	}
}
