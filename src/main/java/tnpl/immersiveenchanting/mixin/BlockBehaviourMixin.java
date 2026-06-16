package tnpl.immersiveenchanting.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;

import java.util.function.BiConsumer;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
    @Inject(method = "onExplosionHit", at = @At("HEAD"))
    private void onExplosionHit(BlockState state,
                                ServerLevel level,
                                BlockPos pos,
                                Explosion explosion,
                                BiConsumer<ItemStack,BlockPos> onHit,
                                CallbackInfo ci
    ) {
        if (state.getBlock() instanceof EnchantingTableBlock) {
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