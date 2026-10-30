package com.skybreaker.client;

import com.skybreaker.FxPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import com.skybreaker.SkybreakerMod;

public class SkybreakerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleSetup.register();
		ClientPlayNetworking.registerGlobalReceiver(FxPayload.TYPE, (payload, context) -> FxManager.handle(payload));
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, SkybreakerMod.id("cinematic"), CinematicHud::render);
		ClientTickEvents.END_CLIENT_TICK.register(FxManager::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FxManager.clear());
	}
}
