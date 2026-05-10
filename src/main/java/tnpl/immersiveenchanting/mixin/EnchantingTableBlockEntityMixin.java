package tnpl.immersiveenchanting.mixin;

import tnpl.immersiveenchanting.fsm.IImmersiveTableData;
import tnpl.immersiveenchanting.fsm.TableState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
    private static final String LAPIS_KEY = "ImmersiveLapisItem";

    @Unique
    private TableState immersiveState = TableState.IDLE;
    @Unique
    private ItemStack targetItem = ItemStack.EMPTY;
    @Unique
    private ItemStack lapisStack = ItemStack.EMPTY;

    @Unique
    private int animationTick = 0;

    protected EnchantingTableBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getAnimationTick() { return this.animationTick; }

    @Override
    public void setAnimationTick(int tick) { this.animationTick = tick; }

    @Override
    public void incrementAnimationTick() { this.animationTick++; }

    @Override
    public TableState getState() { return this.immersiveState; }

    @Override
    public void transitionTo(TableState newState) {
        if (this.immersiveState != newState) {
            this.immersiveState = newState;
            this.animationTick = 0;
            this.setChanged();
        }
    }

    @Override
    public ItemStack getTargetItem() { return this.targetItem; }

    @Override
    public void setTargetItem(ItemStack stack) {
        this.targetItem = stack;
        this.setChanged();
    }

    @Override
    public ItemStack getLapisStack() { return this.lapisStack; }

    @Override
    public void setLapisStack(ItemStack stack) {
        this.lapisStack = stack;
        this.setChanged();
    }

    @Override
    public void syncToClients() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString(STATE_KEY, this.immersiveState.name());

        if (!this.targetItem.isEmpty()) {
            ItemStack.OPTIONAL_CODEC.encodeStart(
                    registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                    this.targetItem
            ).result().ifPresent(itemTag -> tag.put(ITEM_KEY, itemTag));
        } else {
            tag.remove(ITEM_KEY);
        }

        if (!this.lapisStack.isEmpty()) {
            ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                            this.lapisStack
                    ).result().ifPresent(itemTag -> tag.put(LAPIS_KEY, itemTag));
        } else {
            tag.remove(LAPIS_KEY);
        }

        return tag;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(ValueOutput output, CallbackInfo ci) {
        output.putString(STATE_KEY, this.immersiveState.name());

        if (!this.targetItem.isEmpty()) {
            output.store(ITEM_KEY, ItemStack.OPTIONAL_CODEC, this.targetItem);
        }
        if (!this.lapisStack.isEmpty()) {
            output.store(LAPIS_KEY, ItemStack.OPTIONAL_CODEC, this.lapisStack);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoadAdditional(ValueInput input, CallbackInfo ci) {
        input.getString(STATE_KEY).ifPresent(stateStr -> {
            TableState incomingState = TableState.valueOf(stateStr);
            if (this.immersiveState != incomingState) {
                this.immersiveState = incomingState;
                this.animationTick = 0;
            }
        });

        this.targetItem = input.read(ITEM_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.lapisStack = input.read(LAPIS_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public Packet<@NonNull ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}