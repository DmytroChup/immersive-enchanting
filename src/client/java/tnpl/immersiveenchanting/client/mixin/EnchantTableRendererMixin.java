package tnpl.immersiveenchanting.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import tnpl.immersiveenchanting.client.fsm.IImmersiveRenderState;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tnpl.immersiveenchanting.registry.ModItems;

@Mixin(EnchantTableRenderer.class)
public class EnchantTableRendererMixin {

    @Unique
    private ItemModelResolver itemModelResolver;

    @Unique
    private static final Item[] RUNE_TYPES = {
            Items.AMETHYST_SHARD,
            Items.LAPIS_LAZULI,
            Items.GOLD_INGOT,
            Items.DIAMOND
    };

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(
            EnchantingTableBlockEntity blockEntity,
            EnchantTableRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress,
            CallbackInfo ci
    ) {
        if (!(blockEntity instanceof IImmersiveTableData table) || !(state instanceof IImmersiveRenderState customState)) {
            return;
        }

        if (table.getState() != TableState.IDLE) {
            customState.setImmersiveActive(true);
            ItemStack targetItem = table.getTargetItem();

            if (!targetItem.isEmpty() && this.itemModelResolver != null) {
                int seed = (int) blockEntity.getBlockPos().asLong();

                // 1. Resolve Main Target Item
                this.itemModelResolver.updateForTopItem(
                        customState.getImmersiveItemState(),
                        targetItem,
                        ItemDisplayContext.FIXED,
                        blockEntity.getLevel(),
                        null,
                        seed
                );

                for (int i = 0; i < RUNE_TYPES.length; i++) {
                    ItemStack runeStack = new ItemStack(RUNE_TYPES[i]);
                    this.itemModelResolver.updateForTopItem(
                            customState.getRuneItemState(i),
                            runeStack,
                            ItemDisplayContext.FIXED,
                            blockEntity.getLevel(),
                            null,
                            seed + i + 1
                    );
                }

                this.itemModelResolver.updateForTopItem(
                        customState.getMagicCircleState(),
                        new ItemStack(ModItems.VFX_MAGIC_CIRCLE),
                        ItemDisplayContext.FIXED,
                        blockEntity.getLevel(),
                        null,
                        seed + 10
                );
                this.itemModelResolver.updateForTopItem(
                        customState.getBeamState(),
                        new ItemStack(ModItems.VFX_BEAM),
                        ItemDisplayContext.FIXED,
                        blockEntity.getLevel(),
                        null,
                        seed + 11
                );
                this.itemModelResolver.updateForTopItem(
                        customState.getPillarState(),
                        new ItemStack(ModItems.VFX_PILLAR),
                        ItemDisplayContext.FIXED,
                        blockEntity.getLevel(),
                        null,
                        seed + 12
                );

                // 3. Calculate and store shared animation variables
                if (blockEntity.getLevel() != null) {
                    float renderTime = blockEntity.getLevel().getGameTime() + partialTicks;
                    customState.setRenderTime(renderTime);
                    customState.setImmersiveAngle((renderTime * 3.0F) % 360.0F);

                    float bobbing = (float) Math.sin(renderTime * 0.1F) * 0.1F;
                    customState.setImmersiveBobbing(bobbing);
                }

                customState.setAnimationTick(table.getAnimationTick());
                customState.setTableState(table.getState());
                customState.setRunesVisible(true);
            }
        } else {
            customState.setImmersiveActive(false);
            customState.setRunesVisible(false);
        }
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void onSubmit(
            EnchantTableRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera,
            CallbackInfo ci
    ) {
        if (!(state instanceof IImmersiveRenderState customState)) return;

        TableState tableState = customState.getTableState();
        if (customState.isImmersiveActive() && tableState != TableState.IDLE) {
            ci.cancel();

            float time = customState.getRenderTime();
            int tick = customState.getAnimationTick();
            // Interpolate out of 100 ticks
            float exactTick = tableState == TableState.ITEM_INSERTED ? tick + (time - (int) time) : 100f;

            int FULL_BRIGHT = 15728880;
            float tableCenterX = 0.5F;
            float tableCenterZ = 0.5F;

            // The target item levitates up dynamically
            float baseItemY = 1.2F;
            float itemYOffset = baseItemY + customState.getImmersiveBobbing();
            if (exactTick > 60f && exactTick < 85f) {
                // Suspense phase: levitate higher
                float raiseProgress = (exactTick - 60f) / 25f;
                itemYOffset += easeInOutCubic(raiseProgress) * 0.5f;
            }

            // ==========================================
            // 1. RENDER MAGIC CIRCLE
            // ==========================================
            if (exactTick > 0.0f) {
                poseStack.pushPose();
                // Magic circle sits right above the table mesh
                poseStack.translate(tableCenterX, 0.76F, tableCenterZ);

                // IMPORTANT: The texture must lay flat.
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(time * 1.5F)); // Slow ambient rotation

                float circleScale;
                if (exactTick < 20.0f) {
                    circleScale = easeOutBack(exactTick / 20.0f) * 3.0f;
                } else {
                    // Pulsing effect based on time
                    circleScale = 3.0f + Mth.sin(time * 0.2f) * 0.1f;
                }

                poseStack.scale(circleScale, circleScale, 1.0f); // 1.0f on Z since it's flat
                customState.getMagicCircleState().submit(poseStack, submitNodeCollector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }

            // ==========================================
            // 2. RENDER THE CENTRAL ITEM
            // ==========================================
            if (!customState.getImmersiveItemState().isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(tableCenterX, itemYOffset, tableCenterZ);

                poseStack.scale(0.5F, 0.5F, 0.5F);

                float itemSpin = customState.getImmersiveAngle();
                if (exactTick > 60f && exactTick < 85f) {
                    // Item spins violently during suspense
                    itemSpin += (exactTick - 60f) * 20f;
                }

                poseStack.mulPose(Axis.YP.rotationDegrees(itemSpin));
                customState.getImmersiveItemState().submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }

            // ==========================================
            // 3. RUNE ORBITS AND BEAMS
            // ==========================================
            float radius;
            float runeYPos = itemYOffset;
            float spinAngle = time * 2.0f;

            if (tableState == TableState.ITEM_INSERTED) {
                if (exactTick < 20.0f) {
                    // Runes haven't appeared yet
                    radius = 0.0f;
                } else if (exactTick < 60.0f) {
                    // Emergence
                    float prog = (exactTick - 20.0f) / 40.0f;
                    radius = easeOutBack(prog) * 1.8f;
                    runeYPos = itemYOffset + Mth.sin(prog * (float)Math.PI) * 0.5f;
                    spinAngle = time * 3.0f;
                } else if (exactTick < 85.0f) {
                    // Suspense: Holding position, intense spin
                    radius = 1.8f;
                    runeYPos = itemYOffset;
                    spinAngle = time * 8.0f; // Fast spin
                } else {
                    // Slam inward
                    float prog = (exactTick - 85.0f) / 15.0f;
                    radius = 1.8f * (1.0f - easeInOutCubic(prog)); // Suck into the sword
                    spinAngle = time * 15.0f;
                }
            } else {
                // IDLE/RUNE_SELECTION state
                radius = 1.8f;
                runeYPos = itemYOffset;
            }

            if (customState.areRunesVisible() && radius > 0.05f) {
                for (int i = 0; i < 4; i++) {
                    if (!customState.getRuneItemState(i).isEmpty()) {
                        double angleRad = Math.toRadians((spinAngle + (90.0 * i)) % 360.0);
                        float runeX = tableCenterX + (float) Math.cos(angleRad) * radius;
                        float runeZ = tableCenterZ + (float) Math.sin(angleRad) * radius;

                        // --- DRAW BEAMS (Rune to Item) ---
                        if (tableState == TableState.ITEM_INSERTED && exactTick > 30.0f && exactTick < 85.0f) {
                            poseStack.pushPose();
                            poseStack.translate(runeX, runeYPos, runeZ);

                            float dirX = tableCenterX - runeX;
                            float dirY = itemYOffset - runeYPos;
                            float dirZ = tableCenterZ - runeZ;

                            float distance = Mth.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                            float horizontalDistance = Mth.sqrt(dirX * dirX + dirZ * dirZ);

                            float yaw = (float) (-Math.atan2(dirX, dirZ));
                            float pitch = (float) (-Math.atan2(dirY, horizontalDistance));

                            poseStack.mulPose(Axis.YP.rotation(yaw));
                            poseStack.mulPose(Axis.XP.rotation(pitch));

                            poseStack.translate(0.0F, 0.0F, distance / 2.0F);

                            float beamThickness = 0.2f + Mth.sin(time * 0.5f) * 0.1f;
                            if (exactTick > 60f) beamThickness += 0.3f;

                            poseStack.scale(beamThickness, beamThickness, distance);

                            customState.getBeamState().submit(poseStack, submitNodeCollector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                            poseStack.popPose();
                        }

                        // --- DRAW RUNE ---
                        poseStack.pushPose();
                        poseStack.translate(runeX, runeYPos, runeZ);
                        poseStack.mulPose(Axis.YP.rotationDegrees(time * 5.0f)); // Self rotation

                        float scale = 0.3f;
                        poseStack.scale(scale, scale, scale);
                        customState.getRuneItemState(i).submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                        poseStack.popPose();
                    }
                }
            }

            // ==========================================
            // 4. THE CLIMAX PILLAR
            // ==========================================
            if (tableState == TableState.ITEM_INSERTED && exactTick >= 85.0f) {
                poseStack.pushPose();

                float flashProgress = (exactTick - 85.0f) / 15.0f;
                float pillarWidth = 4.0f * (1.0f - easeInOutCubic(flashProgress));
                float pillarHeight = 20.0f;

                poseStack.translate(tableCenterX, 0.76F + (pillarHeight / 2.0F), tableCenterZ);
                poseStack.scale(pillarWidth, pillarHeight, pillarWidth);

                customState.getPillarState().submit(poseStack, submitNodeCollector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }

    // Helper for smooth bouncy animations
    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float)Math.pow(x - 1, 3) + c1 * (float)Math.pow(x - 1, 2);
    }

    // Helper for smooth S-curve animations
    private float easeInOutCubic(float x) {
        return x < 0.5f ? 4f * x * x * x : 1f - (float)Math.pow(-2f * x + 2f, 3f) / 2f;
    }
}