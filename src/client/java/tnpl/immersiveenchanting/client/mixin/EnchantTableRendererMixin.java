package tnpl.immersiveenchanting.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
                        ItemDisplayContext.GROUND,
                        blockEntity.getLevel(),
                        null,
                        seed
                );

                // 2. Resolve 4 Different Rune Items (Placeholders)
                for (int i = 0; i < RUNE_TYPES.length; i++) {
                    ItemStack runeStack = new ItemStack(RUNE_TYPES[i]);
                    this.itemModelResolver.updateForTopItem(
                            customState.getRuneItemState(i),
                            runeStack,
                            ItemDisplayContext.GROUND,
                            blockEntity.getLevel(),
                            null,
                            seed + i + 1
                    );
                }

                // 3. Calculate and store shared animation variables
                if (blockEntity.getLevel() != null) {
                    float renderTime = blockEntity.getLevel().getGameTime() + partialTicks;
                    customState.setRenderTime(renderTime);

                    customState.setImmersiveAngle((renderTime * 3.0F) % 360.0F);

                    float bobbing = (float) Math.sin(renderTime * 0.1F) * 0.1F;
                    customState.setImmersiveBobbing(bobbing);
                }
            }
        } else {
            customState.setImmersiveActive(false);
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

        if (customState.isImmersiveActive()) {
            ci.cancel();

            // --- Render Main Target Item ---
            if (!customState.getImmersiveItemState().isEmpty()) {
                poseStack.pushPose();
                float currentY = 1.2F + customState.getImmersiveBobbing();
                poseStack.translate(0.5F, currentY, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(customState.getImmersiveAngle()));

                customState.getImmersiveItemState().submit(
                        poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0
                );
                poseStack.popPose();
            }

            // --- Render 4 Floating Runes ---
            float time = customState.getRenderTime();
            int totalRunes = 4;
            double radius = 1.0; // Must match RuneRaycaster radius

            for (int i = 0; i < totalRunes; i++) {
                if (!customState.getRuneItemState(i).isEmpty()) {
                    poseStack.pushPose();

                    // Same polar math as RuneRaycaster, but mapped to local 0.0 - 1.0 block space
                    double angleDeg = (time * 2.0 + (360.0 / totalRunes) * i) % 360.0;
                    double angleRad = Math.toRadians(angleDeg);

                    float x = 0.5F + (float) Math.cos(angleRad) * (float) radius;
                    float y = 1.2F + customState.getImmersiveBobbing(); // Share the same breathing animation
                    float z = 0.5F + (float) Math.sin(angleRad) * (float) radius;

                    poseStack.translate(x, y, z);

                    // Make each rune spin around its own Y-axis for extra flair
                    poseStack.mulPose(Axis.YP.rotationDegrees(time * 5.0F));

                    // Scale down the runes so they are visually smaller than the main item
                    poseStack.scale(0.6F, 0.6F, 0.6F);

                    customState.getRuneItemState(i).submit(
                            poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0
                    );

                    poseStack.popPose();
                }
            }
        }
    }
}