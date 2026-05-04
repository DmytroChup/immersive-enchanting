package tnpl.immersiveenchanting.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private Container enchantSlots;

    @Unique
    private boolean isClosing = false;

    @Unique
    private boolean isInitializing = false;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    private void onInit(int syncId, net.minecraft.world.entity.player.Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof IImmersiveTableData tableData) {
                this.isInitializing = true;
                this.enchantSlots.setItem(0, tableData.getTargetItem().copy());
                this.enchantSlots.setItem(1, tableData.getLapisStack().copy());
                this.isInitializing = false;
            }
        });
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void onSlotsChanged(Container container, CallbackInfo ci) {
        if (this.isClosing || this.isInitializing) return;

        if (container == this.enchantSlots) {
            ItemStack targetItem = this.enchantSlots.getItem(0);
            ItemStack lapisStack = this.enchantSlots.getItem(1);

            this.access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) instanceof IImmersiveTableData tableData) {

                    if (tableData.getState() == TableState.CRAFTING) return;

                    tableData.setTargetItem(targetItem.copy());
                    tableData.setLapisStack(lapisStack.copy());

                    TableState newState;
                    if (!targetItem.isEmpty() && lapisStack.isEmpty()) {
                        newState = TableState.ITEM_INSERTED;
                    } else if (!targetItem.isEmpty() && !lapisStack.isEmpty()) {
                        newState = TableState.READY_TO_ENCHANT;
                    } else {
                        newState = TableState.IDLE;
                    }

                    if (tableData.getState() != newState) {
                        tableData.setAnimationTick(0);
                        tableData.transitionTo(newState);
                        tableData.syncToClients();
                    }
                }
            });
        }
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void onClickEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            this.access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) instanceof IImmersiveTableData tableData) {
                    tableData.setAnimationTick(0);
                    tableData.transitionTo(TableState.CRAFTING);
                    tableData.syncToClients();
                }
            });
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(Player player, CallbackInfo ci) {
        this.isClosing = true;

        // Clear the GUI slots. The vanilla code will attempt to drop items from the GUI,
        // but the slots are already empty, so nothing will drop into the inventory.
        // However, the items have already been safely saved in the BlockEntity in the previous step.
        this.enchantSlots.setItem(0, ItemStack.EMPTY);
        this.enchantSlots.setItem(1, ItemStack.EMPTY);
    }
}