package name.modid.client.mixin;

import name.modid.ImmersiveEnchanting;
import name.modid.client.math.RuneRaycaster;
import name.modid.fsm.IImmersiveTableData;
import name.modid.fsm.TableState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalRightClick(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        LocalPlayer player = client.player;

        if (player == null || client.level == null) return;

        // The maximum distance a player can interact with blocks
        int interactionReach = 5;
        BlockPos playerPos = player.blockPosition();

        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        BlockPos targetTablePos = null;
        int hitRuneIndex = -1;

        // Scan a 3D area around the player for active enchanting tables.
        Iterable<BlockPos> nearbyBlocks = BlockPos.betweenClosed(
                playerPos.offset(-interactionReach, -interactionReach, -interactionReach),
                playerPos.offset(interactionReach, interactionReach, interactionReach)
        );

        for (BlockPos pos : nearbyBlocks) {
            if (client.level.getBlockEntity(pos) instanceof IImmersiveTableData table) {

                // We only care about tables that are currently processing an item.
                // TODO: Later, restrict this specifically to TableState.RUNE_SELECTION
                if (table.getState() != TableState.IDLE) {

                    // Raycast against this specific table's floating runes
                    int hit = RuneRaycaster.raycastActiveRunes(player, pos, 3, partialTicks);

                    if (hit != -1) {
                        // Hit a rune. Save the data and break the search loop.
                        targetTablePos = pos.immutable();
                        hitRuneIndex = hit;
                        break;
                    }
                }
            }
        }

        // If we successfully aimed at a rune belonging to a nearby active table
        if (targetTablePos != null && hitRuneIndex != -1) {
            // 1. Cancel the vanilla right-click.
            // This prevents the player from accidentally placing a block or eating food.
            ci.cancel();

            // 2. Trigger local visual feedback: client-side hand swing animation
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

            // 3. TODO: Network communication
            // Send a custom packet to the server containing the table's position and the clicked rune index.

            // Debug logging to verify our math works
            ImmersiveEnchanting.LOGGER.info("[Immersive Enchanting] Raycast Hit! Rune index: {} at table: {}", hitRuneIndex, targetTablePos.toShortString());
        }
    }
}