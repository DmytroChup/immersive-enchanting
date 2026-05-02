package tnpl.immersiveenchanting.client.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import tnpl.immersiveenchanting.ImmersiveEnchanting;
import tnpl.immersiveenchanting.client.math.RuneRaycaster;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tnpl.immersiveenchanting.network.RuneClickPayload;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    private int rightClickDelay;

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
            int hit = this.checkRuneHit(client.level, player, pos, partialTicks);

            if (hit != -1) {
                // Hit a rune. Save the data and break the search loop.
                targetTablePos = pos.immutable();
                hitRuneIndex = hit;
                break;
            }
        }

        // If successfully aimed at a rune belonging to a nearby active table
        if (targetTablePos != null && hitRuneIndex != -1) {
            // Cancel the vanilla right-click.
            // This prevents the player from accidentally placing a block or eating food.
            ci.cancel();

            if (this.rightClickDelay == 0) {

                this.rightClickDelay = 5;

                // Trigger local visual feedback: client-side hand swing animation
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

                // Send a custom payload to the server containing the table's position and the clicked rune index.
                ClientPlayNetworking.send(new RuneClickPayload(targetTablePos, hitRuneIndex));

                // Debug logging to verify our math works
                ImmersiveEnchanting.LOGGER.info("[Immersive Enchanting] Raycast Hit! Rune index: {} at table: {}",
                        hitRuneIndex,
                        targetTablePos.toShortString()
                );
            }
        }
    }

    @Unique
    private int checkRuneHit(ClientLevel level, LocalPlayer player, BlockPos pos, float partialTicks) {
        if (!(level.getBlockEntity(pos) instanceof IImmersiveTableData table)) {
            return -1;
        }
        if (table.getState() == TableState.IDLE) {
            return -1;
        }

        return RuneRaycaster.raycastActiveRunes(player, pos, 4, partialTicks);
    }
}