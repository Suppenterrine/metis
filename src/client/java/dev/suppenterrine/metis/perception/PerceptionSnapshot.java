package dev.suppenterrine.metis.perception;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable, plain-data snapshot of everything Metis perceives in a single
 * client tick.
 *
 * <p>It is captured on the client/game thread (see {@link #capture}) and holds
 * only Java primitives, strings and small records — <em>no</em> live Minecraft
 * objects escape into the HTTP threads that read it. The snapshot is published
 * through a {@code volatile} reference, which safely makes all of its fields
 * visible to readers.
 *
 * <p>What is captured here is governed by the Law of Embodied Perception
 * (see {@code docs/PERCEPTION_RULES.md}): only data a co-located human could
 * perceive or infer. Private internal state (health, hunger, breath, experience,
 * status effects, the hidden inventory) is deliberately <em>never</em> read.
 * Every group is tagged with its perception class so consumers (MCDS) know what
 * gating the rule requires; Metis itself applies none.
 */
public final class PerceptionSnapshot {

	// ── identity + coordinates (legacy /api/coords contract) ───────────────────
	public final double x, y, z;
	public final float yaw, pitch;
	public final String world, biome, uuid, username;

	// ── movement & pose — perception class DISTANT ─────────────────────────────
	public final String pose;
	public final boolean onGround, sprinting, sneaking, swimming, crawling,
			climbing, gliding, onFire, inWater, submerged, inLava;
	public final double velX, velY, velZ, horizontalSpeed;
	public final String vehicle; // nullable: entity id of the ridden vehicle

	// ── crosshair target — perception class PROXIMATE ──────────────────────────
	public final String lookType; // BLOCK | ENTITY | MISS | NONE
	public final LookBlock lookBlock;   // nullable
	public final LookEntity lookEntity; // nullable

	// ── held item & visible equipment — perception class PROXIMATE ─────────────
	public final int selectedSlot;
	public final ItemInfo mainHand, offHand, helmet, chestplate, leggings, boots; // nullable

	// ── ambient self-environment — perception class AMBIENT ────────────────────
	public final long timeOfDay, dayTime;
	public final boolean isDay, isNight, raining, thundering;
	public final int moonPhase;
	public final double rainGradient, thunderGradient;
	public final int lightBlock, lightSky, lightEffective;

	// ── out-of-band common knowledge — perception class BROADCAST ──────────────
	public final boolean singleplayer;
	public final String serverAddress; // nullable
	public final List<PlayerInfo> players;

	// ── small plain-data carriers ──────────────────────────────────────────────
	public record ItemInfo(String id, int count, String name, Integer damage, Integer maxDamage) {}
	public record LookBlock(int x, int y, int z, String face, String id, double distance) {}
	public record LookEntity(String id, String uuid, String name, double distance) {}
	public record PlayerInfo(String uuid, String name, int latency, String gameMode) {}

	/**
	 * Captures the current perceptual state, or {@code null} when the player is
	 * not in a world. Must be called on the client thread.
	 */
	public static PerceptionSnapshot capture(MinecraftClient client) {
		PlayerEntity player = client.player;
		ClientWorld clientWorld = client.world;
		if (player == null || clientWorld == null) {
			return null;
		}
		return new PerceptionSnapshot(client, player, clientWorld);
	}

	private PerceptionSnapshot(MinecraftClient client, PlayerEntity player, ClientWorld clientWorld) {
		BlockPos pos = player.getBlockPos();

		// identity + coordinates
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
		this.yaw = player.getYaw();
		this.pitch = player.getPitch();
		this.world = clientWorld.getRegistryKey().getValue().toString();
		this.biome = clientWorld.getBiome(pos).getKey().map(k -> k.getValue().toString()).orElse("unknown");
		this.uuid = player.getUuid().toString();
		this.username = player.getName().getString();

		// movement & pose (DISTANT)
		this.pose = player.getPose().name();
		this.onGround = player.isOnGround();
		this.sprinting = player.isSprinting();
		this.sneaking = player.isSneaking();
		this.swimming = player.isSwimming();
		this.crawling = player.isCrawling();
		this.climbing = player.isClimbing();
		this.gliding = player.isFallFlying();
		this.onFire = player.isOnFire();
		this.inWater = player.isTouchingWater();
		this.submerged = player.isSubmergedInWater();
		this.inLava = player.isInLava();
		Vec3d v = player.getVelocity();
		this.velX = round(v.x, 4);
		this.velY = round(v.y, 4);
		this.velZ = round(v.z, 4);
		this.horizontalSpeed = round(Math.sqrt(v.x * v.x + v.z * v.z), 4);
		Entity ridden = player.getVehicle();
		this.vehicle = ridden == null ? null : Registries.ENTITY_TYPE.getId(ridden.getType()).toString();

		// crosshair target (PROXIMATE)
		HitResult hit = client.crosshairTarget;
		if (hit == null) {
			this.lookType = "NONE";
			this.lookBlock = null;
			this.lookEntity = null;
		} else {
			switch (hit.getType()) {
				case BLOCK -> {
					BlockHitResult bhr = (BlockHitResult) hit;
					BlockPos bp = bhr.getBlockPos();
					BlockState state = clientWorld.getBlockState(bp);
					String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
					double dist = round(player.getEyePos().distanceTo(bhr.getPos()), 3);
					this.lookType = "BLOCK";
					this.lookBlock = new LookBlock(bp.getX(), bp.getY(), bp.getZ(), bhr.getSide().name(), blockId, dist);
					this.lookEntity = null;
				}
				case ENTITY -> {
					EntityHitResult ehr = (EntityHitResult) hit;
					Entity e = ehr.getEntity();
					String entityId = Registries.ENTITY_TYPE.getId(e.getType()).toString();
					double dist = round(player.getEyePos().distanceTo(ehr.getPos()), 3);
					this.lookType = "ENTITY";
					this.lookEntity = new LookEntity(entityId, e.getUuid().toString(), e.getName().getString(), dist);
					this.lookBlock = null;
				}
				default -> {
					this.lookType = "MISS";
					this.lookBlock = null;
					this.lookEntity = null;
				}
			}
		}

		// held item & visible equipment (PROXIMATE)
		this.selectedSlot = player.getInventory().selectedSlot;
		this.mainHand = itemInfo(player.getMainHandStack());
		this.offHand = itemInfo(player.getOffHandStack());
		this.helmet = itemInfo(player.getEquippedStack(EquipmentSlot.HEAD));
		this.chestplate = itemInfo(player.getEquippedStack(EquipmentSlot.CHEST));
		this.leggings = itemInfo(player.getEquippedStack(EquipmentSlot.LEGS));
		this.boots = itemInfo(player.getEquippedStack(EquipmentSlot.FEET));

		// ambient self-environment (AMBIENT) — read at this body's own position
		this.timeOfDay = clientWorld.getTimeOfDay();
		this.dayTime = clientWorld.getTime();
		this.isDay = clientWorld.isDay();
		this.isNight = clientWorld.isNight();
		this.raining = clientWorld.isRaining();
		this.thundering = clientWorld.isThundering();
		this.moonPhase = (int) (this.timeOfDay / 24000L % 8L + 8L) % 8; // derived; no method in 1.21.1
		this.rainGradient = round(clientWorld.getRainGradient(1.0F), 3);
		this.thunderGradient = round(clientWorld.getThunderGradient(1.0F), 3);
		this.lightBlock = clientWorld.getLightLevel(LightType.BLOCK, pos);
		this.lightSky = clientWorld.getLightLevel(LightType.SKY, pos);
		this.lightEffective = clientWorld.getLightLevel(pos);

		// out-of-band common knowledge (BROADCAST)
		this.singleplayer = client.isInSingleplayer();
		ServerInfo server = client.getCurrentServerEntry();
		this.serverAddress = server == null ? null : server.address;
		List<PlayerInfo> tab = new ArrayList<>();
		if (client.getNetworkHandler() != null) {
			for (PlayerListEntry entry : client.getNetworkHandler().getListedPlayerListEntries()) {
				GameMode mode = entry.getGameMode();
				tab.add(new PlayerInfo(
						entry.getProfile().getId().toString(),
						entry.getProfile().getName(),
						entry.getLatency(),
						mode == null ? null : mode.getName()
				));
			}
		}
		this.players = List.copyOf(tab);
	}

	private static ItemInfo itemInfo(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		String id = Registries.ITEM.getId(stack.getItem()).toString();
		Integer damage = null;
		Integer maxDamage = null;
		if (stack.isDamageable()) {
			damage = stack.getDamage();
			maxDamage = stack.getMaxDamage();
		}
		return new ItemInfo(id, stack.getCount(), stack.getName().getString(), damage, maxDamage);
	}

	private static double round(double value, int places) {
		double factor = Math.pow(10, places);
		return Math.round(value * factor) / factor;
	}

	// ── endpoint payloads ──────────────────────────────────────────────────────
	// Each returns an ordered map; the HTTP layer serializes it with Gson.
	// "perception" carries the class from the rule system so consumers know the gate.

	/** {@code /api/movement} — DISTANT: silhouette & gross behaviour. */
	public Map<String, Object> movementPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("perception", "DISTANT");
		m.put("pose", pose);
		m.put("onGround", onGround);
		m.put("sprinting", sprinting);
		m.put("sneaking", sneaking);
		m.put("swimming", swimming);
		m.put("crawling", crawling);
		m.put("climbing", climbing);
		m.put("gliding", gliding);
		m.put("onFire", onFire);
		m.put("inWater", inWater);
		m.put("submerged", submerged);
		m.put("inLava", inLava);
		Map<String, Object> vel = new LinkedHashMap<>();
		vel.put("x", velX);
		vel.put("y", velY);
		vel.put("z", velZ);
		m.put("velocity", vel);
		m.put("horizontalSpeed", horizontalSpeed);
		m.put("vehicle", vehicle);
		return m;
	}

	/** {@code /api/look} — PROXIMATE: requires nearness + line of sight (MCDS gates). */
	public Map<String, Object> lookPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("perception", "PROXIMATE");
		m.put("type", lookType);
		m.put("block", lookBlock);
		m.put("entity", lookEntity);
		return m;
	}

	/** {@code /api/equipment} — PROXIMATE: visible held/worn gear (MCDS gates). */
	public Map<String, Object> equipmentPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("perception", "PROXIMATE");
		m.put("selectedSlot", selectedSlot);
		m.put("mainHand", mainHand);
		m.put("offHand", offHand);
		Map<String, Object> armor = new LinkedHashMap<>();
		armor.put("head", helmet);
		armor.put("chest", chestplate);
		armor.put("legs", leggings);
		armor.put("feet", boots);
		m.put("armor", armor);
		return m;
	}

	/** {@code /api/environment} — AMBIENT: the sensing body's own surroundings. */
	public Map<String, Object> environmentPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("perception", "AMBIENT");
		m.put("world", world);
		m.put("biome", biome);
		m.put("timeOfDay", timeOfDay);
		m.put("dayTime", dayTime);
		m.put("isDay", isDay);
		m.put("isNight", isNight);
		m.put("moonPhase", moonPhase);
		m.put("raining", raining);
		m.put("thundering", thundering);
		m.put("rainGradient", rainGradient);
		m.put("thunderGradient", thunderGradient);
		Map<String, Object> light = new LinkedHashMap<>();
		light.put("block", lightBlock);
		light.put("sky", lightSky);
		light.put("effective", lightEffective);
		m.put("light", light);
		return m;
	}

	/** {@code /api/players} — BROADCAST: meta common knowledge from the tab list. */
	public Map<String, Object> playersPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("perception", "BROADCAST");
		m.put("singleplayer", singleplayer);
		m.put("server", serverAddress);
		m.put("players", players);
		return m;
	}
}
