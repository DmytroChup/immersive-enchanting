package tnpl.immersiveenchanting.recipe;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class RuneRecipe {
    private final List<Integer> sequence;
    private final ResourceKey<Enchantment> enchantment;
    private final int enchantLevel;

    public RuneRecipe(List<Integer> sequence, ResourceKey<Enchantment> enchantment, int enchantLevel) {
        this.sequence = sequence;
        this.enchantment = enchantment;
        this.enchantLevel = enchantLevel;
    }

    public boolean matches(List<Integer> input) {
        return this.sequence.equals(input);
    }

    public boolean startsWith(List<Integer> input) {
        if (input.size() > this.sequence.size()) return false;
        return this.sequence.subList(0, input.size()).equals(input);
    }

    public ResourceKey<Enchantment> getEnchantment() { return enchantment; }
    public int getEnchantLevel() { return enchantLevel; }
    public int getSequenceLength() { return sequence.size(); }
}