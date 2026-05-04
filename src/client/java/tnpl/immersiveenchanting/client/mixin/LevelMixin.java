package tnpl.immersiveenchanting.client.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tnpl.immersiveenchanting.MagicAtmosphere;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
    private void onGetRainLevel(float partialTicks, CallbackInfoReturnable<Float> cir) {
        Level self = (Level) (Object) this;
        if (self.isClientSide()) {
            float vanillaRain = cir.getReturnValue();
            cir.setReturnValue(Math.max(vanillaRain, MagicAtmosphere.magicDarkness));
        }
    }

    @Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
    private void onGetThunderLevel(float partialTicks, CallbackInfoReturnable<Float> cir) {
        Level self = (Level) (Object) this;
        if (self.isClientSide()) {
            float vanillaThunder = cir.getReturnValue();
            cir.setReturnValue(Math.max(vanillaThunder, MagicAtmosphere.magicDarkness * 0.8f));
        }
    }
}