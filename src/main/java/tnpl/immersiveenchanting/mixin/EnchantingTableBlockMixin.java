package tnpl.immersiveenchanting.mixin;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
                    table.setAnimationTick(0);
                    table.transitionTo(TableState.ITEM_INSERTED);
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

    @Inject(method = "getTicker", at = @At("HEAD"), cancellable = true)
    private <T extends BlockEntity> void interceptGetTicker(
            Level level, BlockState state, BlockEntityType<T> type,
            CallbackInfoReturnable<BlockEntityTicker<T>> cir
    ) {
        cir.setReturnValue((lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof IImmersiveTableData table) {

                if (lvl.isClientSide() && table.getState() == TableState.IDLE) {
                    table.setAnimationTick(0);
                }

                if (table.getState() == TableState.ITEM_INSERTED) {
                    table.incrementAnimationTick();
                    int tick = table.getAnimationTick();

                    // CLIENT-SIDE: Cinematic audio cues
                    if (lvl.isClientSide()) {
                        double cx = pos.getX() + 0.5;
                        double cy = pos.getY() + 1.2;
                        double cz = pos.getZ() + 0.5;

                        if (tick == 5) {
                            // Phase 1: Magic circle powers up
                            lvl.playLocalSound(cx, cy, cz,
                                    SoundEvents.BEACON_ACTIVATE,
                                    SoundSource.BLOCKS,
                                    1.0f, 0.5f, false);
                        } else if (tick == 60) {
                            // Phase 2: Suspense - high pitched hum
                            lvl.playLocalSound(cx, cy, cz,
                                    SoundEvents.ILLUSIONER_PREPARE_BLINDNESS,
                                    SoundSource.BLOCKS,
                                    1.2f, 1.5f, false);
                        } else if (tick == 85) {
                            // Phase 3: The Climax / Strike
                            lvl.playLocalSound(cx, cy, cz,
                                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                                    SoundSource.BLOCKS,
                                    1.5f, 2.0f, false);
                            lvl.playLocalSound(cx, cy, cz,
                                    SoundEvents.END_PORTAL_SPAWN,
                                    SoundSource.BLOCKS,
                                    1.0f, 1.0f, false);
                        }
                    }

                    // SERVER-SIDE: Expanded to 100 ticks for cinematic pacing
                    if (!lvl.isClientSide() && tick >= 100) {
                        table.setAnimationTick(0);
                        table.transitionTo(TableState.RUNE_SELECTION);
                        table.syncToClients();
                    }
                }
            }
        });
    }
}