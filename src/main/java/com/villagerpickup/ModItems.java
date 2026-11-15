package com.villagerpickup;

import com.villagerpickup.item.CapturedVillagerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static final ResourceKey<Item> CAPTURED_VILLAGER_KEY =
		ResourceKey.create(Registries.ITEM, VillagerPickup.id("captured_villager"));

	public static final Item CAPTURED_VILLAGER = new CapturedVillagerItem(
		new Item.Properties()
			// One villager per item, never stackable.
			.stacksTo(1)
			.rarity(Rarity.RARE)
			// Survives fire and lava, so a captured villager cannot be destroyed by accident.
			.fireResistant()
			// Enchanted-looking glint without carrying any enchantment.
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE)
			// NOTE: deliberately no enchantable(n). Without the ENCHANTABLE component the item
			// cannot be enchanted at an enchanting table, cannot take enchanted books in an anvil
			// (EnchantmentHelper.canStoreEnchantments is false) and /enchant rejects it.
			.setId(CAPTURED_VILLAGER_KEY)
	);

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, CAPTURED_VILLAGER_KEY, CAPTURED_VILLAGER);
	}

	private ModItems() {
	}
}
