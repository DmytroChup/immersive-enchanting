package tnpl.immersiveenchanting.fsm;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IImmersiveTableData {
    TableState getState();
    void transitionTo(TableState newState);

    ItemStack getTargetItem();
    void setTargetItem(ItemStack stack);

    List<Integer> getRuneSequence();
    void addRuneToSequence(int runeIndex);
    void clearRuneSequence();

    int getAnimationTick();
    void setAnimationTick(int tick);
    void incrementAnimationTick();

    void syncToClients();
}