package tnpl.immersiveenchanting.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import tnpl.immersiveenchanting.ImmersiveEnchanting;

import java.util.function.Function;

public class ModItems {

    public static final Item VFX_MAGIC_CIRCLE = registerItem("vfx_magic_circle", Item::new, new Item.Properties());
    public static final Item VFX_PILLAR = registerItem("vfx_pillar", Item::new, new Item.Properties());

    public static final Item VFX_LIGHT_BLUE_ORB = registerItem("vfx_light_blue_orb", Item::new, new Item.Properties());
    public static final Item VFX_YELLOW_ORB = registerItem("vfx_yellow_orb", Item::new, new Item.Properties());
    public static final Item VFX_BLUE_ORB = registerItem("vfx_blue_orb", Item::new, new Item.Properties());
    public static final Item VFX_PURPLE_ORB = registerItem("vfx_purple_orb", Item::new, new Item.Properties());

    public static <T extends Item> T registerItem(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(ImmersiveEnchanting.MOD_ID, name)
        );

        T item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void initialize() {
    }
}
