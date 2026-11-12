package ru.itmo.ipkn.signalscope.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RadioControlPayloadTest {

    @Test
    void roundTripsToggleModesAndHands() {
        for (boolean toggleMode : new boolean[]{false, true}) {
            for (boolean transmitting : new boolean[]{false, true}) {
                for (InteractionHand hand : InteractionHand.values()) {
                    var payload = new RadioControlPayload(toggleMode, transmitting, hand);
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                    try {
                        RadioControlPayload.CODEC.encode(buffer, payload);
                        assertEquals(payload, RadioControlPayload.CODEC.decode(buffer));
                        assertFalse(buffer.isReadable());
                    } finally {
                        buffer.release();
                    }
                }
            }
        }
    }
}
