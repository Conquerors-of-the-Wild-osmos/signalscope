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

/**
 * A complete snapshot replaces the previous one, including departed players.
 */
public record PlayerSignalsPayload(ResourceLocation dimension, List<Signal> players) implements CustomPacketPayload {

    public static final Type<PlayerSignalsPayload> TYPE = new Type<>(Signalscope.id("player_signals"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSignalsPayload> CODEC =
            StreamCodec.ofMember(PlayerSignalsPayload::write, PlayerSignalsPayload::read);

    public PlayerSignalsPayload {
        players = List.copyOf(players);
    }

    private static PlayerSignalsPayload read(RegistryFriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int size = buffer.readVarInt();
        // Each entry needs at least 16 UUID bytes, a string length, and three doubles.
        if (size < 0 || size > buffer.readableBytes() / 41) {
            throw new IllegalArgumentException("Invalid player signal count: " + size);
        }
        List<Signal> players = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            players.add(new Signal(buffer.readUUID(), buffer.readUtf(16),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())));
        }
        return new PlayerSignalsPayload(dimension, players);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeVarInt(players.size());
        for (Signal player : players) {
            buffer.writeUUID(player.id());
            buffer.writeUtf(player.name(), 16);
            buffer.writeDouble(player.position().x);
            buffer.writeDouble(player.position().y);
            buffer.writeDouble(player.position().z);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Signal(UUID id, String name, Vec3 position) {
        public Signal {
            if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
                throw new IllegalArgumentException("Non-finite signal position");
            }
        }
    }
}
