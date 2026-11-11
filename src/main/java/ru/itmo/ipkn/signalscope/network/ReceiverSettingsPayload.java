package ru.itmo.ipkn.signalscope.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ru.itmo.ipkn.signalscope.Signalscope;

public record ReceiverSettingsPayload(BlockPos pos, int azimuth, int elevation, int coneAngle)
        implements CustomPacketPayload {

    public static final Type<ReceiverSettingsPayload> TYPE = new Type<>(Signalscope.id("receiver_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReceiverSettingsPayload> CODEC =
            StreamCodec.ofMember(ReceiverSettingsPayload::write, ReceiverSettingsPayload::read);

    private static ReceiverSettingsPayload read(RegistryFriendlyByteBuf buffer) {
        return new ReceiverSettingsPayload(buffer.readBlockPos(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(azimuth);
        buffer.writeVarInt(elevation);
        buffer.writeVarInt(coneAngle);
    }

    @Override
    public Type<ReceiverSettingsPayload> type() {
        return TYPE;
    }
}
