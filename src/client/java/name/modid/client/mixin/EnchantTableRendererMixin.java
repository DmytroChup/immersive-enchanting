package name.modid.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import name.modid.client.fsm.IImmersiveRenderState;
import name.modid.fsm.IImmersiveTableData;
import name.modid.fsm.TableState;
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
                // 1. Resolve 3D Model
                int seed = (int) blockEntity.getBlockPos().asLong();
                this.itemModelResolver.updateForTopItem(
                        customState.getImmersiveItemState(),
                        targetItem,
                        ItemDisplayContext.GROUND,
                        blockEntity.getLevel(),
                        null,
                        seed
                );

                // 2. Calculate smooth animations based on the game tick loop
                if (blockEntity.getLevel() != null) {
                    // Combine total ticks and partial frames for 60+ FPS smoothness
                    float renderTime = blockEntity.getLevel().getGameTime() + partialTicks;

                    // Rotation: Multiply by 3 to set speed (3 degrees per tick)
                    customState.setImmersiveAngle((renderTime * 3.0F) % 360.0F);

                    // Bobbing: Use sine wave. Multiplying time by 0.1 alters the frequency (speed).
                    // Multiplying the result by 0.1 alters the amplitude (height).
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

            if (!customState.getImmersiveItemState().isEmpty()) {
                poseStack.pushPose();

                // 1. Apply Y-offset (bobbing) calculated in extractRenderState
                // Base height is 1.2 blocks above the bottom of the table
                float currentY = 1.2F + customState.getImmersiveBobbing();
                poseStack.translate(0.5F, currentY, 0.5F);

                // 2. Apply continuous rotation
                poseStack.mulPose(Axis.YP.rotationDegrees(customState.getImmersiveAngle()));

                poseStack.scale(1.3F, 1.3F, 1.3F);

                // 3. Render the item
                customState.getImmersiveItemState().submit(
                        poseStack,
                        submitNodeCollector,
                        state.lightCoords,
                        OverlayTexture.NO_OVERLAY,
                        0
                );

                poseStack.popPose();
            }
        }
    }
}