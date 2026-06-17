package tnpl.immersiveenchanting.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tnpl.immersiveenchanting.MagicAtmosphere;

import java.util.function.BooleanSupplier;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(BooleanSupplier haveTime, CallbackInfo ci) {
        if (MagicAtmosphere.darknessTimeout > 0) {
            MagicAtmosphere.darknessTimeout--;
            MagicAtmosphere.magicDarkness += (1.0f - MagicAtmosphere.magicDarkness) * 0.03f;
        } else {
            MagicAtmosphere.magicDarkness -= MagicAtmosphere.magicDarkness * 0.05f;
        }
    }

    @Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
    private void cancelFakeRainSoundAndParticles(CallbackInfo ci) {
        if (MagicAtmosphere.magicDarkness > 0.01f) {
            ci.cancel();
        }
    }
}
