package tnpl.immersiveenchanting.mixin;

import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantingTableBlock.class)
public class EnchantingTableBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void interceptTableUse(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        cir.setReturnValue(InteractionResult.SUCCESS);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IImmersiveTableData table)) {
            return;
        }

        if (!level.isClientSide()) {
            ItemStack stack = player.getMainHandItem();

            // Flow 1: The table is empty. We try to place an item on it
            if (table.getState() == TableState.IDLE) {
                if (!stack.isEmpty() && stack.isEnchantable()) {
                    ItemStack insertedItem = stack.split(1);
                    table.setTargetItem(insertedItem);
                    table.transitionTo(TableState.RUNE_SELECTION);
                    table.syncToClients();
                }
            }
            // Flow 2: There is an item on the table. The player clicks with an empty hand to pick it up.
            // The item can be picked up at any stage, except during the enchantment animation (CRAFTING).
            else if (table.getState() != TableState.CRAFTING && stack.isEmpty()) {
                    ItemStack returnedItem = table.getTargetItem().copy();

                    // Put it back in the hand if it's empty
                    player.setItemInHand(InteractionHand.MAIN_HAND, returnedItem);

                    // Reset the FSM state
                    table.setTargetItem(ItemStack.EMPTY);
                    table.transitionTo(TableState.IDLE);
                    table.syncToClients();
                }

        }
    }
}