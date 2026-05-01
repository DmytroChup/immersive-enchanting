package name.modid.mixin;

import name.modid.fsm.IImmersiveTableData;
import name.modid.fsm.TableState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantingTableBlockEntity.class)
public abstract class EnchantingTableBlockEntityMixin extends BlockEntity implements IImmersiveTableData {

    @Unique
    private static final String STATE_KEY = "ImmersiveState";
    @Unique
    private static final String ITEM_KEY = "ImmersiveTargetItem";

    @Unique
    private TableState immersiveState = TableState.IDLE;

    @Unique
    private ItemStack targetItem = ItemStack.EMPTY;

    protected EnchantingTableBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public TableState getState() { return this.immersiveState; }

    @Override
    public void transitionTo(TableState newState) {
        this.immersiveState = newState;
        this.setChanged();
    }

    @Override
    public ItemStack getTargetItem() { return this.targetItem; }

    @Override
    public void setTargetItem(ItemStack stack) {
        this.targetItem = stack;
        this.setChanged();
    }

    @Override
    public void syncToClients() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(ValueOutput output, CallbackInfo ci) {
        output.putString(STATE_KEY, this.immersiveState.name());

        if (!this.targetItem.isEmpty()) {
            output.store(ITEM_KEY, ItemStack.OPTIONAL_CODEC, this.targetItem);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoadAdditional(ValueInput input, CallbackInfo ci) {
        input.getString(STATE_KEY).ifPresent(stateStr ->
                this.immersiveState = TableState.valueOf(stateStr)
        );

        this.targetItem = input.read(ITEM_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public Packet<@NonNull ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}