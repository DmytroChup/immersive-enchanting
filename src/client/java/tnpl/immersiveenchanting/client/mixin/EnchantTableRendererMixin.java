package tnpl.immersiveenchanting.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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

    @Unique
    private static final Item[] ORB_TYPES = {
            ModItems.VFX_PURPLE_ORB,
            ModItems.VFX_BLUE_ORB,
            ModItems.VFX_YELLOW_ORB,
            ModItems.VFX_LIGHT_BLUE_ORB
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
                    this.itemModelResolver.updateForTopItem(
                            customState.getRuneItemState(i),
                            new ItemStack(RUNE_TYPES[i]),
                            ItemDisplayContext.FIXED,
                            blockEntity.getLevel(),
                            null,
                            seed + i + 1
                    );

                    this.itemModelResolver.updateForTopItem(
                            customState.getOrbItemState(i),
                            new ItemStack(ORB_TYPES[i]),
                            ItemDisplayContext.FIXED,
                            blockEntity.getLevel(),
                            null,
                            seed + i + 5
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
            float exactTick = tick + (time - (int) time);

            int FULL_BRIGHT = 15728880;
            float tableCenterX = 0.5F;
            float tableCenterZ = 0.5F;
            float itemYOffset = 1.2F + customState.getImmersiveBobbing();

            // 1. RENDER THE MAGIC CIRCLE BELOW
            poseStack.pushPose();
            poseStack.translate(tableCenterX, 0.76F, tableCenterZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 1.5F));
            float circleScale = 3.0f + Mth.sin(time * 0.2f) * 0.1f;
            poseStack.scale(circleScale, circleScale, 1.0f);
            customState.getMagicCircleState().submit(poseStack, submitNodeCollector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();

            // 2. RENDER THE CENTRAL ITEM
            if (!customState.getImmersiveItemState().isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(tableCenterX, itemYOffset, tableCenterZ);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(customState.getImmersiveAngle()));
                customState.getImmersiveItemState().submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }

            // 3. RUNE APPEARANCE ANIMATION (READY_TO_ENCHANT)
            if (tableState == TableState.READY_TO_ENCHANT && customState.areRunesVisible()) {

                float pillarWidth = 0.0f;

                for (int i = 0; i < 4; i++) {
                    if (customState.getRuneItemState(i).isEmpty()) continue;

                    poseStack.pushPose();

                    // Coordinates of the table corners from which the balls are launched
                    float startDx = (i % 2 == 0 ? 2.5f : -2.5f);
                    float startDz = (i < 2 ? 2.5f : -2.5f);

                    float runeX, runeY, runeZ;
                    boolean drawEnergyShell = false;

                    if (exactTick < 20.0f) {
                        // PHASE 1 (0-20): The balls fly from the void toward the sword
                        float prog = exactTick / 20.0f;
                        float invProg = 1.0f - easeOutBack(prog);

                        // Target orbit at the center (very narrow, radius 0.4)
                        double targetAngle = Math.toRadians((time * 15.0f + (90.0 * i)) % 360.0);
                        float targetX = (float) Math.cos(targetAngle) * 0.4f;
                        float targetZ = (float) Math.sin(targetAngle) * 0.4f;

                        runeX = tableCenterX + (startDx * invProg) + (targetX * prog);
                        runeY = itemYOffset + (2.0f * invProg);
                        runeZ = tableCenterZ + (startDz * invProg) + (targetZ * prog);
                        drawEnergyShell = true;

                    } else if (exactTick < 40.0f) {
                        // PHASE 2 (20–40): The balls spin wildly around the item
                        double angleRad = Math.toRadians((time * 20.0f + (90.0 * i)) % 360.0);
                        runeX = tableCenterX + (float) Math.cos(angleRad) * 0.4f;
                        runeY = itemYOffset + Mth.sin(time * 0.5f) * 0.1f;
                        runeZ = tableCenterZ + (float) Math.sin(angleRad) * 0.4f;
                        drawEnergyShell = true;

                    } else if (exactTick < 60.0f) {
                        // PHASE 3 (40–60): A beam of light strikes, scattering the runes into a wide orbit
                        float prog = (exactTick - 40.0f) / 20.0f;

                        // Runes gradually move away from 0.4 to 1.8 and slow down the rotation
                        double angleRad = Math.toRadians((time * 3.0f + (1.0f - prog) * 17.0f + (90.0 * i)) % 360.0);
                        float currentRadius = 0.4f + easeOutBack(prog) * 1.4f;

                        runeX = tableCenterX + (float) Math.cos(angleRad) * currentRadius;
                        runeY = itemYOffset + Mth.sin(time * 0.1f) * 0.2f;
                        runeZ = tableCenterZ + (float) Math.sin(angleRad) * currentRadius;

                        // The shell falls off during the first third of the flight
                        if (prog < 0.3f) drawEnergyShell = true;

                        // Animation of the light beam expanding and contracting (Pillar)
                        float pillarProg = Math.min(1.0f, (exactTick - 40.0f) / 10.0f);
                        if (exactTick < 50.0f) {
                            pillarWidth = easeInOutCubic(pillarProg) * 1.5f;
                        } else {
                            float shrinkProg = (exactTick - 50.0f) / 10.0f;
                            pillarWidth = (1.0f - easeInOutCubic(shrinkProg)) * 1.5f;
                        }

                    } else {
                        // PHASE 4 (60+): A steady, endless cycle of completed runes
                        double angleRad = Math.toRadians((time * 3.0f + (90.0 * i)) % 360.0);
                        runeX = tableCenterX + (float) Math.cos(angleRad) * 1.8f;
                        runeY = itemYOffset + Mth.sin(time * 0.1f) * 0.2f;
                        runeZ = tableCenterZ + (float) Math.sin(angleRad) * 1.8f;
                    }

                    poseStack.translate(runeX, runeY, runeZ);

                    if (drawEnergyShell) {
                        poseStack.pushPose();

                        Camera mainCamera = Minecraft.getInstance().gameRenderer.getMainCamera();

                        // Rotate the sphere so that it ALWAYS faces the player directly
                        poseStack.mulPose(Axis.YP.rotationDegrees(-mainCamera.yRot()));
                        poseStack.mulPose(Axis.XP.rotationDegrees(mainCamera.xRot()));

                        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 15.0f));

                        float orbScale = 0.45f;
                        poseStack.scale(orbScale, orbScale, orbScale);
                        customState.getOrbItemState(i).submit(poseStack, submitNodeCollector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

                        poseStack.popPose();
                    } else {
                        poseStack.mulPose(Axis.YP.rotationDegrees(time * 5.0f));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.1f) * 15.0f));

                        float scale = 0.3f;
                        poseStack.scale(scale, scale, scale);
                        customState.getRuneItemState(i).submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                    }

                    poseStack.popPose();
                }

                // ILLUSTRATION OF A PILLAR OF LIGHT IN THE CENTER
                if (pillarWidth > 0.01f) {
                    poseStack.pushPose();
                    float pillarHeight = 15.0f;
                    poseStack.translate(tableCenterX, 0.76F + (pillarHeight / 2.0f), tableCenterZ);
                    poseStack.scale(pillarWidth, pillarHeight, pillarWidth);
                    customState.getPillarState().submit(poseStack, submitNodeCollector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                    poseStack.popPose();
                }
            }

            if (tableState == TableState.CRAFTING) {
            }
        }
    }

    /**
     * easeInOutCubic: Accelerates smoothly at the beginning and decelerates smoothly at the end.
     * Ideal for light rays, zooming, and smooth transitions.
     */
    @Unique
    private float easeInOutCubic(float x) {
        return x < 0.5f ? 4f * x * x * x : 1f - (float)Math.pow(-2f * x + 2f, 3f) / 2f;
    }

    /**
     * easeOutBack: Fades out slightly beyond the target point and bounces back.
     * Creates a cool “spring” or rebound effect. We use this for runes flying into orbit.
     */
    @Unique
    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float)Math.pow(x - 1f, 3f) + c1 * (float)Math.pow(x - 1f, 2f);
    }
}