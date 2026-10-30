package com.skybreaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkybreakerMod implements ModInitializer {
	public static final String MOD_ID = "skybreaker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	/** Hard cap on targeting distance, regardless of config. */
	public static final double MAX_RANGE = 128.0;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		SkybreakerConfig.load();
		PayloadTypeRegistry.clientboundPlay().register(FxPayload.TYPE, FxPayload.CODEC);
		ModParticles.init();
		ModItems.init();
		StrikeManager.init();
		SlowMo.init();
		LOGGER.info("Skybreaker: Orbital Arsenal loaded.");
	}
}
