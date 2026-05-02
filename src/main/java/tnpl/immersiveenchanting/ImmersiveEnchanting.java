package tnpl.immersiveenchanting;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import tnpl.immersiveenchanting.network.RuneClickPayload;
import tnpl.immersiveenchanting.recipe.RuneRecipe;
import tnpl.immersiveenchanting.recipe.RuneRecipeRegistry;

import java.util.List;

public class ImmersiveEnchanting implements ModInitializer {
	public static final String MOD_ID = "immersive-enchanting";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(RuneClickPayload.PACKET_TYPE, RuneClickPayload.PACKET_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RuneClickPayload.PACKET_TYPE, (payload, context) ->
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				Level level = player.level();
				BlockEntity blockEntity = level.getBlockEntity(payload.tablePos());

				if (blockEntity instanceof IImmersiveTableData table) {
					if (table.getState() == TableState.RUNE_SELECTION) {

						ItemStack targetItem = table.getTargetItem();
						if (targetItem.isEmpty()) return;

						table.addRuneToSequence(payload.runeIndex());
						List<Integer> currentSequence = table.getRuneSequence();

						if (!RuneRecipeRegistry.hasPartialMatch(currentSequence)) {
							level.playSound(null,
									payload.tablePos(),
									SoundEvents.REDSTONE_TORCH_BURNOUT,
									SoundSource.BLOCKS,
									1.0F,
									0.8F
							);
							table.clearRuneSequence();
							return;
						}

						var match = RuneRecipeRegistry.findMatch(currentSequence);

						if(match.isPresent()) {
							RuneRecipe recipe = match.get();
							processEnchantmentCombination(player, level, table, targetItem, recipe, payload);
						} else {
							float pitch = 1.0F + (currentSequence.size() * 0.2F);
							level.playSound(null,
									payload.tablePos(),
									SoundEvents.AMETHYST_BLOCK_CHIME,
									SoundSource.BLOCKS,
									1.0F,
									pitch
							);
						}
					}
				}
			})
		);
	}

	private void processEnchantmentCombination(
			ServerPlayer player,
			Level level,
			IImmersiveTableData table,
			ItemStack targetItem,
			RuneRecipe recipe,
			RuneClickPayload payload
	) {
		int cost = recipe.getEnchantLevel() * 2;

		var enchantmentLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> enchantment = enchantmentLookup.getOrThrow(recipe.getEnchantment());

		// BASIC CHECK: Can this item be enchanted with this enchantment?
		// enchantment.value().canEnchant(targetItem) checks for tags (for example, you can't enchant a Shovel with Sharpness)
		if (!enchantment.value().canEnchant(targetItem)) {
			level.playSound(null,
					payload.tablePos(),
					SoundEvents.REDSTONE_TORCH_BURNOUT,
					SoundSource.BLOCKS,
					1.0F,
					1.0F
			);
			player.sendOverlayMessage(Component.literal("§cThis enchantment is not suitable for this item."));
			table.clearRuneSequence();
			return;
		}

		if (player.experienceLevel < cost && !player.getAbilities().instabuild) {
			level.playSound(null,
					payload.tablePos(),
					SoundEvents.REDSTONE_TORCH_BURNOUT,
					SoundSource.BLOCKS,
					1.0F,
					1.0F
			);
			table.clearRuneSequence();
			return;
		}

		if (!player.getAbilities().instabuild) {
			player.giveExperienceLevels(-cost);
		}

		targetItem.enchant(enchantment, recipe.getEnchantLevel());

		if (!player.getInventory().add(targetItem)) {
			player.drop(targetItem, false);
		}

		table.clearRuneSequence();
		table.setTargetItem(ItemStack.EMPTY);
		table.transitionTo(TableState.IDLE);
		table.syncToClients();

		level.playSound(null,
				payload.tablePos(),
				SoundEvents.PLAYER_LEVELUP,
				SoundSource.BLOCKS, 1.0F,
				level.getRandom().nextFloat() * 0.1F + 0.9F
		);
		LOGGER.info("Player {} successfully enchanted item", player.getName().getString());
	}
}