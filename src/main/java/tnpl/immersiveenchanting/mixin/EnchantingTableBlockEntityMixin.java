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
    private static final String immersive$STATE_KEY = "ImmersiveState";
    @Unique
    private static final String immersive$ITEM_KEY = "ImmersiveTargetItem";
    @Unique
    private static final String immersive$LAPIS_KEY = "ImmersiveLapisItem";
    @Unique
    private static final String immersive$TICK_KEY = "ImmersiveAnimationTick";

    @Unique
    private TableState immersive$immersiveState = TableState.IDLE;
    @Unique
    private ItemStack immersive$targetItem = ItemStack.EMPTY;
    @Unique
    private ItemStack immersive$lapisStack = ItemStack.EMPTY;

    @Unique
    private int immersive$animationTick = 0;

    protected EnchantingTableBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getAnimationTick() { return this.immersive$animationTick; }

    @Override
    public void setAnimationTick(int tick) { this.immersive$animationTick = tick; }

    @Override
    public void incrementAnimationTick() { this.immersive$animationTick++; }

    @Override
    public TableState getState() { return this.immersive$immersiveState; }

    @Override
    public void transitionTo(TableState newState) {
        if (this.immersive$immersiveState != newState) {
            this.immersive$immersiveState = newState;
            this.immersive$animationTick = 0;
            this.setChanged();
        }
    }

    @Override
    public ItemStack getTargetItem() { return this.immersive$targetItem; }

    @Override
    public void setTargetItem(ItemStack stack) {
        this.immersive$targetItem = stack;
        this.setChanged();
    }

    @Override
    public ItemStack getLapisStack() { return this.immersive$lapisStack; }

    @Override
    public void setLapisStack(ItemStack stack) {
        this.immersive$lapisStack = stack;
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
        tag.putString(immersive$STATE_KEY, this.immersive$immersiveState.name());

        tag.putInt(immersive$TICK_KEY, this.immersive$animationTick);

        if (!this.immersive$targetItem.isEmpty()) {
            ItemStack.OPTIONAL_CODEC.encodeStart(
                    registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                    this.immersive$targetItem
            ).result().ifPresent(itemTag -> tag.put(immersive$ITEM_KEY, itemTag));
        } else {
            tag.remove(immersive$ITEM_KEY);
        }

        if (!this.immersive$lapisStack.isEmpty()) {
            ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                            this.immersive$lapisStack
                    ).result().ifPresent(itemTag -> tag.put(immersive$LAPIS_KEY, itemTag));
        } else {
            tag.remove(immersive$LAPIS_KEY);
        }

        return tag;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(ValueOutput output, CallbackInfo ci) {
        output.putString(immersive$STATE_KEY, this.immersive$immersiveState.name());

        output.putInt(immersive$TICK_KEY, this.immersive$animationTick);

        if (!this.immersive$targetItem.isEmpty()) {
            output.store(immersive$ITEM_KEY, ItemStack.OPTIONAL_CODEC, this.immersive$targetItem);
        }
        if (!this.immersive$lapisStack.isEmpty()) {
            output.store(immersive$LAPIS_KEY, ItemStack.OPTIONAL_CODEC, this.immersive$lapisStack);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoadAdditional(ValueInput input, CallbackInfo ci) {
        input.getString(immersive$STATE_KEY).ifPresent(stateStr -> {
            this.immersive$immersiveState = TableState.valueOf(stateStr);
        });

        input.getInt(immersive$TICK_KEY).ifPresent(tick -> this.immersive$animationTick = tick);

        this.immersive$targetItem = input.read(immersive$ITEM_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.immersive$lapisStack = input.read(immersive$LAPIS_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public Packet<@NonNull ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}