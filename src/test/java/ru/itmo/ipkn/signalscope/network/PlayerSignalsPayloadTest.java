package ru.itmo.ipkn.signalscope.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSignalsPayloadTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.withDefaultNamespace("overworld");

    @Test
    void roundTripsPlayersBeyondEntityTrackingRange() {
        var payload = new PlayerSignalsPayload(DIMENSION, List.of(
                new PlayerSignalsPayload.Signal(UUID.randomUUID(), "Explorer", new Vec3(30_000, 80, -20_000)),
                new PlayerSignalsPayload.Signal(UUID.randomUUID(), "Nearby", new Vec3(0, 65, 0))));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            PlayerSignalsPayload.CODEC.encode(buffer, payload);
            assertEquals(payload, PlayerSignalsPayload.CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    void supportsEmptySnapshotsToRemoveDepartedPlayers() {
        var payload = new PlayerSignalsPayload(DIMENSION, List.of());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            PlayerSignalsPayload.CODEC.encode(buffer, payload);
            assertEquals(payload, PlayerSignalsPayload.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsMalformedCountsBeforeAllocating() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeResourceLocation(DIMENSION);
            buffer.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> PlayerSignalsPayload.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSignalsPayload.Signal(UUID.randomUUID(), "Explorer", new Vec3(Double.NaN, 0, 0)));
    }
}
