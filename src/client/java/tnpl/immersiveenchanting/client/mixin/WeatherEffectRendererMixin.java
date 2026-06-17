package tnpl.immersiveenchanting.client.mixin;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tnpl.immersiveenchanting.MagicAtmosphere;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelFakeRainRender(Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
        if (MagicAtmosphere.magicDarkness > 0.01f) {
            ci.cancel();
        }
    }
}