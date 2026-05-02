package tnpl.immersiveenchanting.recipe;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RuneRecipeRegistry {
    private RuneRecipeRegistry() {
    }

    private static final List<RuneRecipe> RECIPES = new ArrayList<>();

    static {
        // 0 = Amethyst, 1 = Lapis, 2 = Gold, 3 = Diamond

        // Sharpness I: Gold, Gold, Lapis
        register(List.of(1, 2, 0), Enchantments.SHARPNESS, 1);
        // Sharpness II: Gold, Diamond, Diamond, Lapis
        register(List.of(2, 3, 3, 1), Enchantments.SHARPNESS, 2);

        // Fortune I: Lapis, Amethyst, Lapis
        register(List.of(1, 0, 1), Enchantments.FORTUNE, 1);

        // Silk Touch: Amethyst, Gold, Amethyst
        register(List.of(0, 2, 0), Enchantments.SILK_TOUCH, 1);

        // Unbreaking I: Gold, Diamond, Gold
        register(List.of(2, 3, 2), Enchantments.UNBREAKING, 1);
    }

    private static void register(List<Integer> seq, ResourceKey<Enchantment> ench, int level) {
        RECIPES.add(new RuneRecipe(seq, ench, level));
    }

    // Exact match
    public static Optional<RuneRecipe> findMatch(List<Integer> sequence) {
        return RECIPES.stream()
                .filter(r -> r.matches(sequence))
                .findFirst();
    }

    // Is there at least one recipe that starts with the current sequence?
    public static boolean hasPartialMatch(List<Integer> sequence) {
        return RECIPES.stream().anyMatch(r -> r.startsWith(sequence));
    }
}