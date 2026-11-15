package com.villagerpickup;

import com.villagerpickup.capture.VillagerCapture;
import com.villagerpickup.item.CapturedVillagerItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerPickup implements ModInitializer {
	public static final String MOD_ID = "villager-pickup";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Creative tab keys are private in 26.3, so the key for the vanilla "Tools & Utilities" tab is
	 * rebuilt here and Fabric adds our item to its output.
	 */
	private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		Identifier.withDefaultNamespace("tools_and_utilities")
	);

	@Override
	public void onInitialize() {
		ModItems.register();

		// Sneak + right-click a villager with an empty hand -> seal it into an item.
		UseEntityCallback.EVENT.register(VillagerCapture::onUseEntity);

		CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES)
			.register(output -> output.accept(CapturedVillagerItem.createEmptyStack()));

		LOGGER.info("[villager-pickup] initialised");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
