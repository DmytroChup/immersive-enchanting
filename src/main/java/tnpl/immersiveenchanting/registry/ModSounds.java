package tnpl.immersiveenchanting.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import tnpl.immersiveenchanting.ImmersiveEnchanting;

public class ModSounds {
    private ModSounds() {
        /* This utility class should not be instantiated */
    }

    public static final SoundEvent CRAFT_IMPLOSION = registerSound("craft_implosion");
    public static final SoundEvent CRAFT_SUCCESS = registerSound("craft_success");

    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.fromNamespaceAndPath(ImmersiveEnchanting.MOD_ID, id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }

    public static void initialize() {
    }
}
