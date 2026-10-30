package com.skybreaker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client "play this visual effect" message. All gameplay stays on the server;
 * the client only renders particles and camera shake from this data.
 */
public record FxPayload(int kind, double x, double y, double z, int a, int b, float f, float[] data) implements CustomPacketPayload {
	public static final int RAILGUN = 1;
	public static final int LASER = 2;
	public static final int METEOR = 3;
	public static final int SHAKE = 4;
	/** Sent to the firing player only: starts the cinematic HUD (a = countdown ticks, b = weapon 1..3, f = distance). */
	public static final int CINEMA = 5;
	/** Screen flash (a = fade ticks, f = intensity 0..1). */
	public static final int FLASH = 6;
	/** A lightning bolt drawn by the client (a = random seed, f = power). */
	public static final int BOLT = 7;
	/** Singularity Cannon: a = countdown ticks, b = pull ticks, f = pull radius, data[0] = collapse radius. */
	public static final int BLACKHOLE = 8;
	/** How high above the aimed block the black hole hangs. */
	public static final double BH_HEIGHT = 4.0;

	/** Number of ticks the railgun bolt takes to fall from orbit. */
	public static final int BOLT_TICKS = 6;
	/** Meteor flight time in ticks, and the offset (relative to the impact point) where each meteor enters. */
	public static final int METEOR_FALL_TICKS = 36;
	public static final double ENTRY_X = 30.0;
	public static final double ENTRY_Y = 95.0;
	public static final double ENTRY_Z = 14.0;

	/** Meteor flight curve shared by server (real entities) and client (trail particles): p 0..1 -> 1..0 distance factor. */
	public static double meteorFactor(double p) {
		return 1.0 - (0.35 * p + 0.65 * p * p);
	}

	public static final CustomPacketPayload.Type<FxPayload> TYPE = new CustomPacketPayload.Type<>(SkybreakerMod.id("fx"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = CustomPacketPayload.codec(FxPayload::write, FxPayload::new);

	public FxPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), readData(buf));
	}

	private static float[] readData(RegistryFriendlyByteBuf buf) {
		int n = Math.min(buf.readVarInt(), 1024);
		float[] d = new float[n];
		for (int i = 0; i < n; i++) {
			d[i] = buf.readFloat();
		}
		return d;
	}

	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(kind);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeVarInt(a);
		buf.writeVarInt(b);
		buf.writeFloat(f);
		buf.writeVarInt(data.length);
		for (float v : data) {
			buf.writeFloat(v);
		}
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
