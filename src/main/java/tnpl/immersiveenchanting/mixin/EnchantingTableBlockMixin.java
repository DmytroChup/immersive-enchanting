package tnpl.immersiveenchanting.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
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
import tnpl.immersiveenchanting.registry.ModSounds;

@Mixin(EnchantingTableBlock.class)
public class EnchantingTableBlockMixin {

    @Inject(method = "getTicker", at = @At("RETURN"), cancellable = true)
    private <T extends BlockEntity> void interceptGetTicker(
            Level level, BlockState state, BlockEntityType<T> type,
            CallbackInfoReturnable<BlockEntityTicker<T>> cir
    ) {
        BlockEntityTicker<T> vanillaTicker = cir.getReturnValue();

        cir.setReturnValue((lvl, pos, blockState, blockEntity) -> {
            if (vanillaTicker != null) {
                vanillaTicker.tick(lvl, pos, blockState, blockEntity);
            }

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
                            LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(lvl, EntitySpawnReason.TRIGGERED);
                            if (lightning != null) {
                                lightning.setPos(pos.getX() + 3.0, pos.getY(), pos.getZ() + 3.5);
                                lightning.setVisualOnly(true);
                                lvl.addFreshEntity(lightning);
                            }
                        }
                    }
                }

                if (table.getState() == TableState.CRAFTING) {
                    table.incrementAnimationTick();
                    int craftTick = table.getAnimationTick();

                    if (lvl.isClientSide()) {
                        double cx = pos.getX() + 0.5;
                        double cy = pos.getY() + 1.2;
                        double cz = pos.getZ() + 0.5;

                        // PHASE 1: PUSH (Ticks 1–30)
                        if (craftTick == 1) {
                            lvl.playLocalSound(cx, cy, cz,
                                    SoundEvents.MINECART_RIDING,
                                    SoundSource.BLOCKS,
                                    0.4f, 1.5f, false
                            );
                        }

                        if (craftTick > 1 && craftTick < 30) {
                            if (lvl.getRandom().nextInt(3) == 0) { // Don't spawn every tick, so as not to clutter the screen
                                double px = cx + (lvl.getRandom().nextDouble() - 0.5) * 4.0;
                                double py = cy + (lvl.getRandom().nextDouble() - 0.5) * 2.0;
                                double pz = cz + (lvl.getRandom().nextDouble() - 0.5) * 4.0;
                                lvl.addParticle(ParticleTypes.ENCHANT, px, py, pz, (cx - px) * 0.1, (cy - py) * 0.1, (cz - pz) * 0.1);
                            }
                        }

                        // PHASE 2: VACUUM JUMP (Tick 30)
                        if (craftTick == 30) {
                            lvl.playLocalSound(cx, cy, cz, ModSounds.CRAFT_IMPLOSION, SoundSource.BLOCKS, 1.0f, 1.0f, false);

                            // A torrent of glowing sparks hurtling toward the center
                            for (int i = 0; i < 60; i++) {
                                double px = cx + (lvl.getRandom().nextDouble() - 0.5) * 5.0;
                                double py = cy + (lvl.getRandom().nextDouble() - 0.5) * 3.0;
                                double pz = cz + (lvl.getRandom().nextDouble() - 0.5) * 5.0;
                                lvl.addParticle(ParticleTypes.END_ROD, px, py, pz, (cx - px) * 0.2, (cy - py) * 0.2, (cz - pz) * 0.2);
                            }
                        }

                        // PHASE 3: RELEASE (Tick 40)
                        if (craftTick == 40) {
                            lvl.playLocalSound(cx, cy, cz, ModSounds.CRAFT_SUCCESS, SoundSource.BLOCKS, 1.0f, 1.0f, false);

                            // A shockwave of fiery souls, rushing outward
                            for (int i = 0; i < 40; i++) {
                                double vx = (lvl.getRandom().nextDouble() - 0.5) * 0.8;
                                double vy = (lvl.getRandom().nextDouble() - 0.5) * 0.8;
                                double vz = (lvl.getRandom().nextDouble() - 0.5) * 0.8;
                                lvl.addParticle(ParticleTypes.SOUL_FIRE_FLAME, cx, cy + 0.6, cz, vx, vy, vz);
                            }

                            lvl.addParticle(
                                    ColorParticleOption.create(ParticleTypes.FLASH, 0.4f, 0.1f, 1.0f),
                                    cx, cy + 0.6, cz, 0, 0, 0
                            );
                        }
                    } else {
                        if (craftTick >= 60) {
                            table.transitionTo(TableState.CRAFTING_FINISHED);
                            table.syncToClients();
                        }
                    }
                }
            }
        });
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void onRightClick(BlockState state,
                              Level level,
                              BlockPos pos,
                              Player player,
                              BlockHitResult hitResult,
                              CallbackInfoReturnable<InteractionResult> cir
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IImmersiveTableData tableData) {
            TableState currentState = tableData.getState();

            if (currentState == TableState.CRAFTING && tableData.getAnimationTick() < 60) {
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            if (currentState == TableState.CRAFTING_FINISHED) {
                ItemStack targetItem = tableData.getTargetItem();

                if (!targetItem.isEmpty()) {
                    if (!level.isClientSide()) {

                        player.getInventory().placeItemBackInInventory(targetItem.copy());

                        tableData.setTargetItem(ItemStack.EMPTY);
                        tableData.setAnimationTick(0);
                        tableData.transitionTo(TableState.IDLE);
                        tableData.syncToClients();
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
            }
        }
    }
}