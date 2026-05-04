package tnpl.immersiveenchanting.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void onPlayerWillDestroy(Level level,
                                     BlockPos pos,
                                     BlockState state,
                                     Player player,
                                     CallbackInfoReturnable<BlockState> cir
    ) {
        if (state.getBlock() instanceof EnchantingTableBlock && !level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof IImmersiveTableData table) {
                if (!table.getTargetItem().isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), table.getTargetItem());
                    table.setTargetItem(ItemStack.EMPTY);
                }

                if (!table.getLapisStack().isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), table.getLapisStack());
                    table.setLapisStack(ItemStack.EMPTY);
                }
            }
        }
    }
}