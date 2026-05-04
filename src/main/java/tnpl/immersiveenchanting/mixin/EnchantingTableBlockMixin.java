package tnpl.immersiveenchanting.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import tnpl.immersiveenchanting.MagicAtmosphere;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantingTableBlock.class)
public class EnchantingTableBlockMixin {

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

                // Animation logic for different states
                if (table.getState() == TableState.ITEM_INSERTED || table.getState() == TableState.READY_TO_ENCHANT) {
                    if (lvl.isClientSide() && table.getState() == TableState.READY_TO_ENCHANT) {
                        if (table.getAnimationTick() < 120) {
                            MagicAtmosphere.darknessTimeout = 5;
                        }
                    }

                    table.incrementAnimationTick();
                    int tick = table.getAnimationTick();

                    if (lvl.isClientSide()) {
                        double cx = pos.getX() + 0.5;
                        double cy = pos.getY() + 1.2;
                        double cz = pos.getZ() + 0.5;

                        // Sound when the item appears
                        if (tick == 5 && table.getState() == TableState.ITEM_INSERTED) {
                            lvl.playLocalSound(cx, cy, cz,
                                    SoundEvents.AMETHYST_BLOCK_CHIME,
                                    SoundSource.BLOCKS,
                                    1.0f, 1.5f, false
                            );
                        }

                        if (table.getState() == TableState.READY_TO_ENCHANT) {
                            if (tick == 1) {
                                lvl.playLocalSound(cx, cy, cz,
                                        SoundEvents.ILLUSIONER_CAST_SPELL,
                                        SoundSource.BLOCKS,
                                        1.0f, 1.2f, false
                                );
                            }
                            if (tick == 40) {
                                lvl.playLocalSound(cx, cy, cz,
                                        SoundEvents.BEACON_ACTIVATE,
                                        SoundSource.BLOCKS,
                                        1.0f, 2.0f, false
                                );
                            }
                        }
                    }
                    if (!lvl.isClientSide()) {
                        if (tick == 1 && table.getState() == TableState.READY_TO_ENCHANT) {
                            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(lvl, EntitySpawnReason.TRIGGERED);
                            if (lightning != null) {
                                lightning.setPos(pos.getX() + 3.0, pos.getY(), pos.getZ() + 3.5);
                                lightning.setVisualOnly(true);
                                lvl.addFreshEntity(lightning);
                            }
                        }
                    }
                }

                if (table.getState() == TableState.CRAFTING) {
                }
            }
        });
    }
}