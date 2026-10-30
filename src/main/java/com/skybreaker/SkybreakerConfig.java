package com.skybreaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON config stored at config/skybreaker.json. Missing keys are filled with defaults and written back. */
public final class SkybreakerConfig {
	private static SkybreakerConfig instance = new SkybreakerConfig();

	public static SkybreakerConfig get() {
		return instance;
	}

	/** Bumped when defaults change; an older file is reset to the new defaults. */
	public int configVersion = 2;

	// ---- cinematic / spectacle ----
	public boolean cinematic = true;
	/** During the intro the firing player's camera is eased toward the target. */
	public boolean cinematicCamera = true;
	/** Real lightning bolts (visual only) around impacts. */
	public boolean lightning = true;
	/** Crater blocks are flung into the air as real falling blocks. */
	public boolean debris = true;
	public int debrisCount = 80;
	/** Brief bullet-time at impact. Only ever used when ONE player is on the server. */
	public boolean slowMotion = true;
	public float slowMotionRate = 5.0f;
	public int slowMotionMillis = 1100;

	// ---- global safety / behaviour ----
	/** Master switch. false = NO block is ever destroyed by any Skybreaker weapon. */
	public boolean terrainDestruction = true;
	/** Maximum number of blocks a single strike may remove. */
	public int maxBlocksPerStrike = 6000;
	/** Blocks harder than this (obsidian = 50, stone = 1.5) are never destroyed. Unbreakable blocks (bedrock) never are. */
	public float maxBlockHardness = 50.0f;
	/** Never destroy blocks that have inventories/data (chests, spawners, beacons...). */
	public boolean protectBlockEntities = true;
	public double maxTargetRange = 128.0;
	public boolean damagePlayers = true;
	public boolean damageOwner = false;
	public boolean cameraShake = true;
	public double shakeRadius = 64.0;
	/** Players farther than this from a strike do not receive its visual effects. */
	public double effectBroadcastRange = 320.0;

	// ---- Orbital Railgun ----
	public double railgunCooldownSeconds = 20.0;
	public double railgunCountdownSeconds = 3.0;
	public float railgunDamage = 600.0f;
	/** "Explosion strength": crater radius in blocks; damage radius is 2x this. */
	public double railgunExplosionRadius = 16.0;
	public boolean railgunTerrain = true;

	// ---- Satellite Laser ----
	public double laserCooldownSeconds = 30.0;
	public double laserCountdownSeconds = 3.0;
	public double laserDurationSeconds = 8.0;
	public float laserDamagePerSecond = 160.0f;
	public double laserBeamRadius = 4.0;
	public boolean laserTerrain = true;
	/** The beam ends in a huge detonation. */
	public double laserFinaleRadius = 10.0;
	public float laserFinaleDamage = 400.0f;
	/** Maximum depth the sustained beam can bore into the ground. */
	public int laserMaxDepth = 40;

	// ---- Meteor Barrage ----
	public double meteorCooldownSeconds = 40.0;
	public double meteorCountdownSeconds = 3.0;
	public int meteorCount = 24;
	public double meteorAreaRadius = 20.0;
	public double meteorSpreadSeconds = 4.0;
	public float meteorDamage = 120.0f;
	public double meteorImpactRadius = 6.0;
	public boolean meteorTerrain = true;

	// ---- Singularity Cannon (black hole) ----
	public double blackHoleCooldownSeconds = 60.0;
	/** Length of the cinematic intro before the black hole opens. */
	public double blackHoleCountdownSeconds = 4.0;
	/** How long the black hole drags everything in before it collapses. */
	public double blackHolePullSeconds = 7.0;
	public double blackHolePullRadius = 32.0;
	/** Anything this close to the core is held there and crushed. */
	public double blackHoleKillRadius = 4.0;
	/** Crater radius of the final collapse (damage radius is 2.4x this). */
	public double blackHoleCollapseRadius = 16.0;
	public float blackHoleCollapseDamage = 3000.0f;
	/** Loose surface blocks are torn out and flung toward the hole while it pulls. */
	public boolean blackHoleTerrain = true;

	public static int ticks(double seconds) {
		return Math.max(1, (int) Math.round(seconds * 20.0));
	}

	private void validate() {
		maxBlocksPerStrike = Math.max(0, Math.min(maxBlocksPerStrike, 50000));
		maxBlockHardness = Math.max(0f, maxBlockHardness);
		maxTargetRange = Math.max(8.0, Math.min(maxTargetRange, SkybreakerMod.MAX_RANGE));
		shakeRadius = Math.max(1.0, shakeRadius);
		effectBroadcastRange = Math.max(32.0, Math.min(effectBroadcastRange, 1000.0));
		railgunCooldownSeconds = Math.max(0, railgunCooldownSeconds);
		railgunCountdownSeconds = Math.max(0.5, Math.min(railgunCountdownSeconds, 30));
		railgunExplosionRadius = Math.max(1.0, Math.min(railgunExplosionRadius, 24.0));
		laserCooldownSeconds = Math.max(0, laserCooldownSeconds);
		laserCountdownSeconds = Math.max(0.5, Math.min(laserCountdownSeconds, 30));
		laserDurationSeconds = Math.max(1.0, Math.min(laserDurationSeconds, 30));
		laserBeamRadius = Math.max(0.5, Math.min(laserBeamRadius, 8.0));
		laserMaxDepth = Math.max(0, Math.min(laserMaxDepth, 96));
		meteorCooldownSeconds = Math.max(0, meteorCooldownSeconds);
		meteorCountdownSeconds = Math.max(0.5, Math.min(meteorCountdownSeconds, 30));
		meteorCount = Math.max(1, Math.min(meteorCount, 48));
		meteorAreaRadius = Math.max(2.0, Math.min(meteorAreaRadius, 40.0));
		meteorSpreadSeconds = Math.max(0.0, Math.min(meteorSpreadSeconds, 20.0));
		laserFinaleRadius = Math.max(0.0, Math.min(laserFinaleRadius, 16.0));
		debrisCount = Math.max(0, Math.min(debrisCount, 300));
		slowMotionRate = Math.max(1.0f, Math.min(slowMotionRate, 20.0f));
		slowMotionMillis = Math.max(0, Math.min(slowMotionMillis, 4000));
		meteorImpactRadius = Math.max(1.0, Math.min(meteorImpactRadius, 12.0));
		blackHoleCooldownSeconds = Math.max(0, blackHoleCooldownSeconds);
		blackHoleCountdownSeconds = Math.max(1.0, Math.min(blackHoleCountdownSeconds, 15));
		blackHolePullSeconds = Math.max(1.0, Math.min(blackHolePullSeconds, 20));
		blackHolePullRadius = Math.max(6.0, Math.min(blackHolePullRadius, 48.0));
		blackHoleKillRadius = Math.max(1.5, Math.min(blackHoleKillRadius, 8.0));
		blackHoleCollapseRadius = Math.max(2.0, Math.min(blackHoleCollapseRadius, 24.0));
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("skybreaker.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					JsonObject raw = JsonParser.parseReader(reader).getAsJsonObject();
					if (raw.has("configVersion")) {
						SkybreakerConfig loaded = gson.fromJson(raw, SkybreakerConfig.class);
						if (loaded != null) {
							instance = loaded;
						}
					} else {
						SkybreakerMod.LOGGER.info("Old skybreaker.json found - resetting to the new, stronger defaults.");
					}
				}
			}
			instance.validate();
			try (Writer writer = Files.newBufferedWriter(path)) {
				gson.toJson(instance, writer);
			}
		} catch (Exception e) {
			SkybreakerMod.LOGGER.error("Could not read/write config/skybreaker.json, using defaults", e);
			instance = new SkybreakerConfig();
			instance.validate();
		}
	}
}
