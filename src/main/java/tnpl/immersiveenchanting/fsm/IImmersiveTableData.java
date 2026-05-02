package tnpl.immersiveenchanting.fsm;

import net.minecraft.world.item.ItemStack;

public interface IImmersiveTableData {
    TableState getState();
    void transitionTo(TableState newState);

    ItemStack getTargetItem();
    void setTargetItem(ItemStack stack);

    void syncToClients();
}