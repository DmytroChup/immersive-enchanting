package tnpl.immersiveenchanting.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import tnpl.immersiveenchanting.ImmersiveEnchanting;

public record RuneClickPayload(BlockPos tablePos, int runeIndex) implements CustomPacketPayload {

    // A unique identifier for the custom network packet.
    public static final CustomPacketPayload.Type<RuneClickPayload> PACKET_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ImmersiveEnchanting.MOD_ID, "rune_click"));

    // StreamCodec automatically serializes and deserializes the record for network transmission.
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneClickPayload> PACKET_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RuneClickPayload::tablePos,
            ByteBufCodecs.INT, RuneClickPayload::runeIndex,
            RuneClickPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return PACKET_TYPE;
    }
}
