package tnpl.immersiveenchanting.client.mixin.compat;

import moriyashiine.enchancement.client.gui.screens.inventory.ModEnchantmentScreen;
import moriyashiine.enchancement.common.world.inventory.ModEnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModEnchantmentScreen.class)
public abstract class EnchancementScreenCompatMixin {

    @Unique
    private boolean immersive$syncedOnce = false;

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void onContainerTick(CallbackInfo ci) {
        ModEnchantmentScreen screen = (ModEnchantmentScreen) (Object) this;
        if (screen.receivedPacket && !this.immersive$syncedOnce) {
            this.immersive$syncedOnce = true;
            ModEnchantmentMenu menu = screen.getMenu();
            
            Slot firstSlot = menu.getSlot(0);
            ItemStack item = firstSlot.getItem();
            if (!item.isEmpty()) {
                firstSlot.set(ItemStack.EMPTY);
                firstSlot.set(item);
            }
        }
    }
}
