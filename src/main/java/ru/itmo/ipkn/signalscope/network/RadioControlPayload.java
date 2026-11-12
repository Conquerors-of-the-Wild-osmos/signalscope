package ru.itmo.ipkn.signalscope.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import ru.itmo.ipkn.signalscope.Signalscope;

/**
 * Client-selected radio state; the server validates the held hand.
 */
public record RadioControlPayload(boolean toggleMode, boolean transmitting,
                                  InteractionHand hand) implements CustomPacketPayload {

    public static final Type<RadioControlPayload> TYPE = new Type<>(Signalscope.id("radio_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadioControlPayload> CODEC =
            StreamCodec.ofMember(RadioControlPayload::write, RadioControlPayload::read);

    private static RadioControlPayload read(RegistryFriendlyByteBuf buffer) {
        return new RadioControlPayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readEnum(InteractionHand.class));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(toggleMode);
        buffer.writeBoolean(transmitting);
        buffer.writeEnum(hand);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
