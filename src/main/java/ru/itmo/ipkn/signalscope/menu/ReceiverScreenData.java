package ru.itmo.ipkn.signalscope.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ReceiverScreenData(BlockPos pos, int azimuth, int elevation, int coneAngle) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ReceiverScreenData> STREAM_CODEC =
            StreamCodec.ofMember(ReceiverScreenData::write, ReceiverScreenData::read);

    private static ReceiverScreenData read(RegistryFriendlyByteBuf buffer) {
        return new ReceiverScreenData(buffer.readBlockPos(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(azimuth);
        buffer.writeVarInt(elevation);
        buffer.writeVarInt(coneAngle);
    }
}
