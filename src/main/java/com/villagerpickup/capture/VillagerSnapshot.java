package com.villagerpickup.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Serialises an entity into a {@link CompoundTag} and compares two tags.
 *
 * <p>Everything the entity persists is kept; only state that describes <em>where and how</em> it
 * happened to be at the exact moment of capture (coordinates, motion, burning, air supply, ...)
 * is dropped, because that state is meaningless - and often harmful - at the new location.
 */
public final class VillagerSnapshot {
	private static final String[] TRANSIENT_FIELDS = {
		"Pos",
		"Motion",
		"Rotation",
		"fall_distance",
		"Fire",
		"Air",
		"OnGround",
		"PortalCooldown",
		"invulnerable_time",
		"HurtTime",
		"DeathTime",
		"FallFlying",
		"sleeping_pos",
		"current_impulse_context_reset_grace_time",
		"current_explosion_impact_pos",
		"last_hurt_by_player",
		"last_hurt_by_player_memory_time",
		"last_hurt_by_mob",
		"ticks_since_last_hurt_by_mob"
	};

	private VillagerSnapshot() {
	}

	/** Full persistent snapshot of {@code entity}, without transient location/physics state. */
	public static CompoundTag capture(Entity entity, Level level) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		entity.saveWithoutId(output);
		CompoundTag tag = output.buildResult();

		for (String field : TRANSIENT_FIELDS) {
			tag.remove(field);
		}

		return tag;
	}

	public static Optional<UUID> uuid(CompoundTag tag) {
		return tag.read("UUID", UUIDUtil.CODEC);
	}

	/** Human readable list of everything that differs between the stored and the restored tag. */
	public static List<String> diff(CompoundTag expected, CompoundTag actual, int limit) {
		List<String> out = new ArrayList<>();
		collect(expected, actual, "", out, limit);
		return out;
	}

	private static void collect(CompoundTag expected, CompoundTag actual, String path, List<String> out, int limit) {
		Set<String> keys = new TreeSet<>(expected.keySet());
		keys.addAll(actual.keySet());

		for (String key : keys) {
			if (out.size() >= limit) {
				out.add("... (truncated)");
				return;
			}

			Tag a = expected.get(key);
			Tag b = actual.get(key);
			String child = path.isEmpty() ? key : path + "." + key;

			if (a == null) {
				out.add(child + " -> only in restored entity: " + abbreviate(b));
			} else if (b == null) {
				out.add(child + " -> only in stored snapshot: " + abbreviate(a));
			} else if (a instanceof CompoundTag compoundA && b instanceof CompoundTag compoundB) {
				collect(compoundA, compoundB, child, out, limit);
			} else if (!a.equals(b)) {
				out.add(child + " -> " + abbreviate(a) + " != " + abbreviate(b));
			}
		}
	}

	private static String abbreviate(Tag tag) {
		if (tag == null) {
			return "<null>";
		}

		String text = tag.toString();

		return text.length() <= 120 ? text : text.substring(0, 117) + "...";
	}
}
