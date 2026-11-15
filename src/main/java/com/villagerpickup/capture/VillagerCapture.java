package com.villagerpickup.capture;

import com.villagerpickup.item.CapturedVillagerItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Sneak + right-click a villager with an empty main hand to seal it into an item.
 *
 * <p>Everything that changes world state happens on the server thread only. The client half of this
 * handler just answers {@link InteractionResult#SUCCESS}, which is what makes Fabric send the
 * interaction packet - so the same code path works in single player, on a LAN world and on a
 * dedicated server, without any custom networking.
 */
public final class VillagerCapture {
	private VillagerCapture() {
	}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity target,
			EntityHitResult hitResult) {
		if (hand != InteractionHand.MAIN_HAND || !(target instanceof Villager villager)) {
			return InteractionResult.PASS;
		}

		// Must be empty-handed and deliberately sneaking.
		if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
			return InteractionResult.PASS;
		}

		if (player.isSpectator() || villager.isRemoved()) {
			return InteractionResult.PASS;
		}

		if (!(level instanceof ServerLevel serverLevel)) {
			// Client side only reports success so the packet is sent; nothing is mutated here.
			return InteractionResult.SUCCESS;
		}

		// Never pull a villager out of somebody else's active trade screen.
		if (villager.isTrading()) {
			player.sendSystemMessage(Component.translatable("message.villager-pickup.busy"));
			return InteractionResult.CONSUME;
		}

		CompoundTag snapshot = VillagerSnapshot.capture(villager, serverLevel);
		ItemStack stack = CapturedVillagerItem.createStack(villager, snapshot);

		player.setItemInHand(hand, stack);
		player.sendSystemMessage(Component.translatable("message.villager-pickup.captured", villager.getName()));
		serverLevel.playSound(null, villager.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0F, 1.0F);

		villager.discard();
		return InteractionResult.SUCCESS;
	}
}
