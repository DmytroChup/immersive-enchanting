package tnpl.immersiveenchanting;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import tnpl.immersiveenchanting.network.RuneClickPayload;

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

			// Network operations happen on a networking thread.
			// This MUST execute world interactions on the main server thread.
			context.server().execute(() -> {
				BlockEntity blockEntity = context.player().level().getBlockEntity(payload.tablePos());

				if (blockEntity instanceof IImmersiveTableData table) {
					if (table.getState() == TableState.ITEM_INSERTED || table.getState() == TableState.RUNE_SELECTION) {
						// TODO: Here is where the actual enchanting logic will go
                        ImmersiveEnchanting.LOGGER.info("Server received rune click: {} from player: {}",
								payload.runeIndex(),
								context.player().getName().getString()
						);
					}
				}
			})
		);
	}
}