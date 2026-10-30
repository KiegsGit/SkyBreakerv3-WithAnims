package com.skybreaker.client;

import com.skybreaker.FxPayload;
import com.skybreaker.ModParticles;
import com.skybreaker.SkybreakerMod;
import com.skybreaker.SkybreakerConfig;
import com.skybreaker.StrikeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Owns all running client effects, the camera shake, and the aiming reticle shown while holding a weapon. */
final class FxManager {
	private static final List<Fx> ACTIVE = new ArrayList<>();

	// camera shake state
	private static int shakeTicks = 0;
	private static int shakeTotal = 1;
	private static float shakeIntensity = 0f;
	private static double shakeRadius = 64.0;
	private static Vec3 shakeOrigin = Vec3.ZERO;
	private static float lastYaw = 0f;
	private static float lastPitch = 0f;

	private static int reticleTick = 0;

	static void handle(FxPayload p) {
		Vec3 pos = new Vec3(p.x(), p.y(), p.z());
		switch (p.kind()) {
			case FxPayload.RAILGUN -> ACTIVE.add(new RailgunFx(pos, p.a(), p.b(), p.f()));
			case FxPayload.LASER -> ACTIVE.add(new LaserFx(pos, p.a(), p.b(), p.f()));
			case FxPayload.METEOR -> ACTIVE.add(new MeteorFx(pos, p.a(), p.b(), p.f(), p.data()));
			case FxPayload.BOLT -> {
				int bolts = 0;
				for (Fx fx : ACTIVE) {
					if (fx instanceof BoltFx) {
						bolts++;
					}
				}
				if (bolts < 12) {
					ACTIVE.add(new BoltFx(pos, p.a(), p.f()));
				}
			}
			case FxPayload.BLACKHOLE -> ACTIVE.add(new BlackHoleFx(pos, p.a(), p.b(), p.f(), p.data()));
			case FxPayload.CINEMA -> CinematicHud.start(p);
			case FxPayload.FLASH -> CinematicHud.flash(p.f(), p.a());
			case FxPayload.SHAKE -> {
				if (p.f() >= shakeIntensity * ((float) shakeTicks / shakeTotal) || shakeTicks <= 0) {
					shakeTicks = Math.max(1, p.a());
					shakeTotal = shakeTicks;
					shakeIntensity = p.f();
					shakeRadius = Math.max(1, p.b());
					shakeOrigin = pos;
				}
			}
			default -> SkybreakerMod.LOGGER.warn("Unknown Skybreaker fx kind {}", p.kind());
		}
	}

	static void clear() {
		ACTIVE.clear();
		CinematicHud.clear();
		shakeTicks = 0;
		lastYaw = 0f;
		lastPitch = 0f;
	}

	static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) {
			clear();
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		CinematicHud.tick();
		Iterator<Fx> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Fx fx = it.next();
			try {
				if (fx.tick(level)) {
					it.remove();
				}
			} catch (Exception e) {
				SkybreakerMod.LOGGER.error("Skybreaker client effect failed", e);
				it.remove();
			}
		}
		aimCamera(player);
		shake(player);
		reticle(level, player);
	}

	/** During the cinematic intro the firing player's view is eased toward the target. */
	private static void aimCamera(LocalPlayer player) {
		if (!CinematicHud.cameraActive() || !SkybreakerConfig.get().cinematicCamera) {
			return;
		}
		Vec3 t = CinematicHud.lookTarget();
		Vec3 eye = player.getEyePosition();
		double dx = t.x - eye.x;
		double dy = t.y - eye.y;
		double dz = t.z - eye.z;
		double horiz = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
		float pitch = (float) (-(Math.atan2(dy, horiz) * 180.0 / Math.PI));
		float k = 0.10f;
		float dYaw = Mth.wrapDegrees(yaw - player.getYRot());
		player.setYRot(player.getYRot() + dYaw * k);
		player.setXRot(Mth.clamp(player.getXRot() + (pitch - player.getXRot()) * k, -90.0f, 90.0f));
	}

	/** Removes last tick's offset, then applies a new random one that decays over time and with distance. */
	private static void shake(LocalPlayer player) {
		player.setYRot(player.getYRot() - lastYaw);
		player.setXRot(player.getXRot() - lastPitch);
		lastYaw = 0f;
		lastPitch = 0f;
		if (shakeTicks <= 0) {
			return;
		}
		double dist = player.position().distanceTo(shakeOrigin);
		double falloff = Math.max(0.0, 1.0 - dist / shakeRadius);
		double amp = shakeIntensity * falloff * (shakeTicks / (double) shakeTotal);
		shakeTicks--;
		if (amp < 0.01) {
			return;
		}
		float dYaw = (float) (Pt.sym() * amp * 2.2);
		float dPitch = (float) (Pt.sym() * amp * 1.4);
		float beforePitch = player.getXRot();
		player.setYRot(player.getYRot() + dYaw);
		player.setXRot(beforePitch + dPitch);
		lastYaw = dYaw;
		lastPitch = player.getXRot() - beforePitch;
	}

	/** Shows a faint ring where the weapon in hand would strike. */
	private static void reticle(ClientLevel level, LocalPlayer player) {
		StrikeItem weapon = null;
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (main.getItem() instanceof StrikeItem s) {
			weapon = s;
		} else if (off.getItem() instanceof StrikeItem s) {
			weapon = s;
		}
		if (weapon == null) {
			return;
		}
		reticleTick++;
		if (reticleTick % 2 != 0) {
			return;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(SkybreakerMod.MAX_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) {
			return;
		}
		Pt.level = level;
		Vec3 c = hit.getLocation();
		double r = weapon.kind.previewRadius;
		int n = (int) Math.min(90, 14 + r * 4);
		Pt.ring(ModParticles.PLASMA, c, r, n, reticleTick * 0.08, 0.1, 0.0, 0.0);
		Pt.add(ModParticles.CORE, c.x, c.y + 0.15, c.z, 0, 0, 0);
		for (int k = 0; k < 4; k++) {
			double a = reticleTick * 0.08 + k * Math.PI / 2.0;
			Pt.add(ModParticles.ZAP, c.x + Math.cos(a) * r * 1.2, c.y + 0.1, c.z + Math.sin(a) * r * 1.2, 0, 0, 0);
		}
	}

	private FxManager() {
	}
}
