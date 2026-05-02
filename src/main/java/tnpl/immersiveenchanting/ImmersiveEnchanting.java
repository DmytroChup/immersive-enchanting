package tnpl.immersiveenchanting;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import tnpl.immersiveenchanting.network.RuneClickPayload;

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

						float pitch = 1.0F + (currentSequence.size() * 0.2F);
						level.playSound(null,
								payload.tablePos(),
								SoundEvents.AMETHYST_BLOCK_CHIME,
								SoundSource.BLOCKS,
								1.0F,
								pitch
						);

						if(currentSequence.size() >= 3) {
							processEnchantmentCombination(player, level, table, targetItem, currentSequence, payload);
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
			List<Integer> sequence,
			RuneClickPayload payload
	) {
		int cost = 3;

		// Determine which spells match the entered combination
		ResourceKey<Enchantment> targetEnchantmentKey = getEnchantmentForSequence(sequence);

		if (targetEnchantmentKey == null) {
			// Invalid combination (no such recipe exists)
			failEnchantment(level, table, payload);
			return;
		}

		var enchantmentLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> enchantment = enchantmentLookup.getOrThrow(targetEnchantmentKey);

		// BASIC CHECK: Can this item be enchanted with this enchantment?
		// enchantment.value().canEnchant(targetItem) checks for tags (for example, you can't enchant a Shovel with Sharpness)
		if (!enchantment.value().canEnchant(targetItem)) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This enchantment is not suitable for this item."), true);
			failEnchantment(level, table, payload);
			return;
		}

		if (player.experienceLevel < cost && !player.getAbilities().instabuild) {
			failEnchantment(level, table, payload);
			return;
		}

		if (!player.getAbilities().instabuild) {
			player.giveExperienceLevels(-cost);
		}

		targetItem.enchant(enchantment, 1);

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
		LOGGER.info("Player {} successfully enchanted item with sequence {}", player.getName().getString(), sequence);
	}

	private void failEnchantment(Level level, IImmersiveTableData table, RuneClickPayload payload) {
		level.playSound(null, payload.tablePos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.5F);
		table.clearRuneSequence();
	}

	private ResourceKey<Enchantment> getEnchantmentForSequence(List<Integer> seq) {
		// Example: 1 (Lapis) -> 2 (Gold) -> 0 (Amethyst) = Good Luck
		if (seq.equals(List.of(1, 2, 0))) return Enchantments.FORTUNE;

		// Example: 0 -> 0 -> 0 = Sharpness
		if (seq.equals(List.of(0, 0, 0))) return Enchantments.SHARPNESS;

		// Example: 3 (Diamond) -> 3 -> 3 = Indestructibility
		if (seq.equals(List.of(3, 3, 3))) return Enchantments.UNBREAKING;

		return null;
	}
}