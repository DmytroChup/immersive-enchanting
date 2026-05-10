package tnpl.immersiveenchanting.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    @Unique
    private boolean isCraftingTransition = false;

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
            ItemStack slotItem = this.enchantSlots.getItem(0);
            ItemStack lapisStack = this.enchantSlots.getItem(1);

            this.access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) instanceof IImmersiveTableData tableData) {

                    if (this.isCraftingTransition) {
                        tableData.setTargetItem(slotItem.copy());
                        tableData.setLapisStack(lapisStack.copy());
                        tableData.setAnimationTick(0);
                        tableData.transitionTo(TableState.CRAFTING);
                        tableData.syncToClients();
                        return;
                    }

                    if (tableData.getState() == TableState.CRAFTING) {
                        if (slotItem.isEmpty()) {
                            tableData.setTargetItem(ItemStack.EMPTY);
                            tableData.setLapisStack(ItemStack.EMPTY);
                            tableData.setAnimationTick(0);
                            tableData.transitionTo(TableState.IDLE);
                            tableData.syncToClients();
                        } else {
                            tableData.setTargetItem(slotItem.copy());
                            tableData.setLapisStack(lapisStack.copy());
                            tableData.syncToClients();
                        }
                        return;
                    }

                    ItemStack targetItem = (!slotItem.isEmpty() && slotItem.isEnchantable()) ? slotItem.copy() : ItemStack.EMPTY;

                    tableData.setTargetItem(targetItem);
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

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void onClickEnchantHead(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.isCraftingTransition = true;
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void onClickEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.isCraftingTransition = false;

        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(Player player, CallbackInfo ci) {
        this.isClosing = true;

        this.access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof IImmersiveTableData tableData) {

                if (!tableData.getTargetItem().isEmpty()) {
                    this.enchantSlots.setItem(0, ItemStack.EMPTY);
                }
                if (!tableData.getLapisStack().isEmpty()) {
                    this.enchantSlots.setItem(1, ItemStack.EMPTY);
                }
            }
        });
    }
}