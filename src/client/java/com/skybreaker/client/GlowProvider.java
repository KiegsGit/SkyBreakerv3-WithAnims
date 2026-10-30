package com.skybreaker.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Builds Skybreaker particles. They move like end-rod particles (fast start, smooth slow-down) but get an exact
 * size (half-width in blocks), an exact lifetime and their own animated texture from the particle's json.
 */
final class GlowProvider implements ParticleProvider<SimpleParticleType> {
	private final EndRodParticle.Provider base;
	private final float size;
	private final float jitter;
	private final int life;

	GlowProvider(SpriteSet sprites, float size, float jitter, int life) {
		this.base = new EndRodParticle.Provider(sprites);
		this.size = size;
		this.jitter = jitter;
		this.life = life;
	}

	@Override
	public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
	                               double xd, double yd, double zd, RandomSource random) {
		Particle p = this.base.createParticle(type, level, x, y, z, xd, yd, zd, random);
		if (p == null) {
			return null;
		}
		p.setLifetime(Math.max(1, this.life + random.nextInt(Math.max(1, this.life / 4))));
		if (p instanceof SingleQuadParticle quad) {
			float wanted = this.size * (1.0f + this.jitter * (random.nextFloat() * 2.0f - 1.0f));
			quad.scale(wanted / Math.max(0.0001f, quad.getQuadSize(0.0f)));
		}
		return p;
	}
}
