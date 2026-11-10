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

    private static final ResourceLocation DIMENSION =
            ResourceLocation.withDefaultNamespace("overworld");

    @Test
    void roundTripsPlayersBeyondEntityTrackingRange() {
        var payload = new PlayerSignalsPayload(
                DIMENSION,
                true,
                List.of(
                        new PlayerSignalsPayload.Signal(
                                UUID.randomUUID(),
                                "Explorer",
                                new Vec3(30_000, 80, -20_000)
                        ),
                        new PlayerSignalsPayload.Signal(
                                UUID.randomUUID(),
                                "Nearby",
                                new Vec3(0, 65, 0)
                        )
                ),
                List.of()
        );

        var buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                RegistryAccess.EMPTY
        );

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
        var payload = new PlayerSignalsPayload(
                DIMENSION,
                true,
                List.of(),
                List.of()
        );

        var buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                RegistryAccess.EMPTY
        );

        try {
            PlayerSignalsPayload.CODEC.encode(buffer, payload);

            assertEquals(payload, PlayerSignalsPayload.CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    void roundTripsDeltaWithUpdatedAndRemovedPlayers() {
        UUID updatedId = UUID.randomUUID();
        UUID removedId = UUID.randomUUID();

        var payload = new PlayerSignalsPayload(
                DIMENSION,
                false,
                List.of(
                        new PlayerSignalsPayload.Signal(
                                updatedId,
                                "Explorer",
                                new Vec3(100, 70, 200)
                        )
                ),
                List.of(removedId)
        );

        var buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                RegistryAccess.EMPTY
        );

        try {
            PlayerSignalsPayload.CODEC.encode(buffer, payload);

            assertEquals(payload, PlayerSignalsPayload.CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsMalformedCountsBeforeAllocating() {
        var buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                RegistryAccess.EMPTY
        );

        try {
            buffer.writeResourceLocation(DIMENSION);
            buffer.writeBoolean(true);
            buffer.writeVarInt(Integer.MAX_VALUE);
            buffer.writeVarInt(0);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> PlayerSignalsPayload.CODEC.decode(buffer)
            );
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerSignalsPayload.Signal(
                        UUID.randomUUID(),
                        "Explorer",
                        new Vec3(Double.NaN, 0, 0)
                )
        );
    }

    @Test
    void rejectsTooManyEntries() {
        var players = java.util.Collections.nCopies(
                PlayerSignalsPayload.MAX_ENTRIES_PER_PACKET + 1,
                new PlayerSignalsPayload.Signal(
                        UUID.randomUUID(),
                        "Explorer",
                        new Vec3(0, 64, 0)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerSignalsPayload(
                        DIMENSION,
                        true,
                        players,
                        List.of()
                )
        );
    }
}
