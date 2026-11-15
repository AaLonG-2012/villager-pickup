package com.villagerpickup.item;

import com.villagerpickup.ModItems;
import com.villagerpickup.capture.VillagerSnapshot;
import com.villagerpickup.debug.RoundTripCheck;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Holds one captured villager inside {@link DataComponents#ENTITY_DATA} and releases it on use.
 */
public class CapturedVillagerItem extends Item {
	public CapturedVillagerItem(Item.Properties properties) {
		super(properties);
	}

	// ------------------------------------------------------------------
	// Stack factories
	// ------------------------------------------------------------------

	public static ItemStack createStack(Villager villager, CompoundTag snapshot) {
		ItemStack stack = new ItemStack(ModItems.CAPTURED_VILLAGER);
		stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(villager.getType(), snapshot));

		// Only a villager that really is named gets a name; otherwise the item keeps its own name,
		// so nothing extra shows up above the villager's head after it is released.
		if (villager.hasCustomName()) {
			stack.set(DataComponents.CUSTOM_NAME, villager.getName().copy().withStyle(ChatFormatting.YELLOW));
		}

		List<Component> lore = new ArrayList<>();
		lore.add(professionLine(villager));
		lore.add(Component.translatable("tooltip.villager-pickup.how_to_release").withStyle(ChatFormatting.GRAY));
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}

	/** "Profession: librarian (level 5)" - uses vanilla's own translation keys. */
	private static Component professionLine(Villager villager) {
		VillagerData villagerData = villager.getVillagerData();
		String profession = villagerData.profession().unwrapKey()
			.map(key -> key.identifier().getPath())
			.orElse("none");

		Component professionName = "none".equals(profession)
			? Component.translatable("tooltip.villager-pickup.unemployed")
			: Component.translatable("entity.minecraft.villager." + profession);

		return Component.translatable("tooltip.villager-pickup.profession", professionName, villagerData.level())
			.withStyle(ChatFormatting.GRAY);
	}

	public static ItemStack createEmptyStack() {
		ItemStack stack = new ItemStack(ModItems.CAPTURED_VILLAGER);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			Component.translatable("tooltip.villager-pickup.empty").withStyle(ChatFormatting.GRAY)
		)));
		return stack;
	}

	// ------------------------------------------------------------------
	// Release
	// ------------------------------------------------------------------

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);

		if (data == null) {
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();

		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		EntityType<?> type = data.type();

		if (!type.canSpawn(level)) {
			return InteractionResult.FAIL;
		}

		BlockPos clicked = context.getClickedPos();
		Direction face = context.getClickedFace();
		BlockState clickedState = level.getBlockState(clicked);
		BlockPos spawnPos = clickedState.getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);

		Entity spawned = spawn(serverLevel, type, stack, context.getPlayer(), spawnPos, face);

		if (spawned == null) {
			return InteractionResult.FAIL;
		}

		// Must run before the stack is consumed, otherwise the component is already gone.
		RoundTripCheck.verify(spawned, stack, serverLevel);

		stack.consume(1, context.getPlayer());
		level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
		serverLevel.playSound(null, spawnPos, SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0F, 0.8F);

		return InteractionResult.SUCCESS;
	}

	/**
	 * Creates the entity with vanilla's placement rules, applies only the stored snapshot,
	 * restores the original UUID, then adds it to the world.
	 */
	@SuppressWarnings("unchecked")
	private static Entity spawn(ServerLevel level, EntityType<?> rawType, ItemStack stack, Player user, BlockPos pos,
			Direction face) {
		EntityType<Entity> type = (EntityType<Entity>) rawType;
		TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);

		// Only the stored entity snapshot is applied. Vanilla's createDefaultStackConfig() would also
		// copy the item's own components (CUSTOM_NAME, CUSTOM_DATA, ...) onto the entity, which made the
		// villager come back wearing the item's yellow display name as a nameplate.
		PostSpawnProcessor<Entity> config = entity -> {
			if (data != null) {
				data.loadInto(entity);
			}
		};

		Entity entity = type.create(level, config, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, face == Direction.UP);

		if (entity == null) {
			return null;
		}

		restoreOriginalUuid(entity, stack, level);
		level.addFreshEntityWithPassengers(entity);
		return entity;
	}

	/**
	 * Vanilla spawn eggs give the entity a brand new UUID. We keep the original one so the villager
	 * really is the same entity (same identity for gossips, raids, iron golems, other mods...).
	 * If that UUID is already alive in the world (duplicated item), we leave the fresh one untouched.
	 */
	private static void restoreOriginalUuid(Entity entity, ItemStack stack, ServerLevel level) {
		TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);

		if (data == null) {
			return;
		}

		Optional<UUID> original = VillagerSnapshot.uuid(data.copyTagWithoutId());

		if (original.isEmpty()) {
			return;
		}

		UUID uuid = original.get();

		if (uuid.equals(entity.getUUID()) || level.getEntityInAnyDimension(uuid) != null) {
			return;
		}

		entity.setUUID(uuid);
	}
}