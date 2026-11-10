package ru.itmo.ipkn.signalscope.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.Signalscope;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PlayerSignalsPayload(ResourceLocation dimension, boolean fullSnapshot,
                                   List<Signal> players, List<UUID> removed
) implements CustomPacketPayload {

    public static final int MAX_ENTRIES_PER_PACKET = 128;

    public static final Type<PlayerSignalsPayload> TYPE = new Type<>(Signalscope.id("player_signals"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSignalsPayload> CODEC =
            StreamCodec.ofMember(PlayerSignalsPayload::write, PlayerSignalsPayload::read);

    public PlayerSignalsPayload {
        players = List.copyOf(players);
        removed = List.copyOf(removed);

        if (players.size() + removed.size() > MAX_ENTRIES_PER_PACKET) {
            throw new IllegalArgumentException("Too many entries in player signals packet");
        }

        if (fullSnapshot && !removed.isEmpty()) {
            throw new IllegalArgumentException("Full snapshot cannot contain removed players");
        }
    }

    private static PlayerSignalsPayload read(RegistryFriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        boolean fullSnapshot = buffer.readBoolean();

        int playerCount = buffer.readVarInt();
        int removedCount = buffer.readVarInt();

        long totalCount = (long) playerCount + removedCount;

        if (playerCount < 0 || removedCount < 0 || totalCount > MAX_ENTRIES_PER_PACKET) {
            throw new IllegalArgumentException("Invalid player signal count");
        }

        // Minimum size: 42 bytes per signal and 16 bytes per removed UUID.
        long minimumBytes = (long) playerCount * 42 + (long) removedCount * 16;

        if (minimumBytes > buffer.readableBytes()) {
            throw new IllegalArgumentException("Incomplete player signals packet");
        }

        List<Signal> players = new ArrayList<>(playerCount);

        for (int i = 0; i < playerCount; i++) {
            players.add(new Signal(buffer.readUUID(), buffer.readUtf(16),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readBoolean()));
        }

        List<UUID> removed = new ArrayList<>(removedCount);

        for (int i = 0; i < removedCount; i++) {
            removed.add(buffer.readUUID());
        }

        return new PlayerSignalsPayload(dimension, fullSnapshot, players, removed);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeBoolean(fullSnapshot);
        buffer.writeVarInt(players.size());
        buffer.writeVarInt(removed.size());
        for (Signal player : players) {
            buffer.writeUUID(player.id());
            buffer.writeUtf(player.name(), 16);
            buffer.writeDouble(player.position().x);
            buffer.writeDouble(player.position().y);
            buffer.writeDouble(player.position().z);
            buffer.writeBoolean(player.transmitting());
        }
        for (UUID id : removed) {
            buffer.writeUUID(id);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Signal(UUID id, String name, Vec3 position, boolean transmitting) {
        public Signal {
            if (id == null || name == null || position == null) {
                throw new IllegalArgumentException("Signal fields cannot be null");
            }

            if (name.length() > 16) {
                throw new IllegalArgumentException("Player name is too long");
            }

            if (!Double.isFinite(position.x)
                    || !Double.isFinite(position.y)
                    || !Double.isFinite(position.z)) {
                throw new IllegalArgumentException("Non-finite signal position");
            }
        }
    }
}
