package tnpl.immersiveenchanting.fsm;

import net.minecraft.world.item.ItemStack;

public interface IImmersiveTableData {
    TableState getState();
    void transitionTo(TableState newState);

    ItemStack getTargetItem();
    void setTargetItem(ItemStack stack);

    int getAnimationTick();
    void setAnimationTick(int tick);
    void incrementAnimationTick();

    ItemStack getLapisStack();
    void setLapisStack(ItemStack stack);

    void syncToClients();
}