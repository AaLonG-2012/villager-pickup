package com.villagerpickup.debug;

import com.villagerpickup.VillagerPickup;
import com.villagerpickup.capture.VillagerSnapshot;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;

/**
 * After a villager is released, re-serialise it and diff it against the snapshot that was stored in
 * the item. Any difference is logged, so "the villager comes back absolutely identical" is not a
 * claim but something we can read off the log.
 */
public final class RoundTripCheck {
	private static final int MAX_LISTED = 25;

	private RoundTripCheck() {
	}

	public static void verify(Entity entity, ItemStack stack, ServerLevel level) {
		TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);

		if (data == null) {
			return;
		}

		CompoundTag stored = data.copyTagWithoutId();
		CompoundTag restored = VillagerSnapshot.capture(entity, level);
		List<String> differences = VillagerSnapshot.diff(stored, restored, MAX_LISTED);

		if (differences.isEmpty()) {
			VillagerPickup.LOGGER.info("[villager-pickup] round-trip check: IDENTICAL ({} keys)", restored.keySet().size());
		} else {
			VillagerPickup.LOGGER.warn("[villager-pickup] round-trip check: {} difference(s) -> {}",
				differences.size(), differences);
		}
	}
}
