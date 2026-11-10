package ru.itmo.ipkn.signalscope.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.network.RadioControlPayload;
import ru.itmo.ipkn.signalscope.voice.RadioState;

import java.util.*;

public final class SignalscopeServer {

    private static final int TOGGLE_TIMEOUT_TICKS = 30;

    private static final int FULL_SYNC_INTERVAL_TICKS = 200;
    private static final int UPDATE_INTERVAL_TICKS = 5;

    private static final double POSITION_EPSILON_SQUARED = 0.05 * 0.05;

    private static final Map<UUID, ToggleRequest> toggleRequests = new HashMap<>();

    // Last state sent to each Signalscope receiver.
    private static final Map<UUID, ReceiverState> lastSent = new HashMap<>();

    private SignalscopeServer() {
    }

    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(
                RadioControlPayload.TYPE,
                (payload, context) -> setToggle(
                        context.player().getUUID(),
                        payload,
                        context.player(),
                        context.server()
                )
        );

        ServerTickEvents.END_SERVER_TICK.register(SignalscopeServer::tick);

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clear());

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUUID();

            toggleRequests.remove(playerId);
            lastSent.remove(playerId);

            var players = new HashMap<>(RadioState.serverPlayers());
            players.remove(playerId);
            RadioState.publish(players);
        });
    }

    private static void setToggle(
            UUID id,
            RadioControlPayload payload,
            ServerPlayer player,
            MinecraftServer server
    ) {
        if (!payload.transmitting()) {
            toggleRequests.remove(id);
        } else if (!player.isSpectator()
                && player.isAlive()
                && SignalscopeItems.holdsRadio(player, payload.hand())) {
            toggleRequests.put(
                    id,
                    new ToggleRequest(
                            payload.hand(),
                            player.getItemInHand(payload.hand()),
                            server.getTickCount() + TOGGLE_TIMEOUT_TICKS
                    )
            );
        }
    }

    private static void tick(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();

        // Existing radio state logic: preserve its per-tick updates.
        var radioPlayers = new HashMap<UUID, RadioState.Player>();

        for (var player : players) {
            ToggleRequest toggle = toggleRequests.get(player.getUUID());

            boolean toggleTransmitting = toggle != null
                    && toggle.expiresAt() >= server.getTickCount()
                    && !player.isSpectator()
                    && player.isAlive()
                    && player.getItemInHand(toggle.hand()) == toggle.stack()
                    && SignalscopeItems.holdsRadio(player, toggle.hand());

            if (!toggleTransmitting) {
                toggleRequests.remove(player.getUUID());
            }

            radioPlayers.put(
                    player.getUUID(),
                    new RadioState.Player(
                            player.level().dimension().location().toString(),
                            !player.isSpectator()
                                    && (SignalscopeItems.isTransmitting(player)
                                    || toggleTransmitting)
                    )
            );
        }

        RadioState.publish(radioPlayers);

        // Network synchronization is throttled to once every 5 ticks.
        long tick = server.getTickCount();

        if (tick % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer receiver : players) {
            UUID receiverId = receiver.getUUID();

            if (!SignalscopeItems.holdsSignalscope(receiver)
                    || !ServerPlayNetworking.canSend(receiver, PlayerSignalsPayload.TYPE)) {
                lastSent.remove(receiverId);
                continue;
            }

            ResourceLocation dimension = receiver.level().dimension().location();

            Map<UUID, PlayerSignalsPayload.Signal> current =
                    collectSignals(receiver, players);

            ReceiverState previous = lastSent.get(receiverId);

            boolean fullSyncRequired = previous == null
                    || !previous.dimension().equals(dimension)
                    || tick - previous.lastFullSyncTick() >= FULL_SYNC_INTERVAL_TICKS;

            if (fullSyncRequired) {
                sendFullSnapshot(receiver, dimension, current, tick);
            } else {
                sendDelta(receiver, dimension, current, previous);
            }
        }
    }

    private static Map<UUID, PlayerSignalsPayload.Signal> collectSignals(
            ServerPlayer receiver,
            List<ServerPlayer> players
    ) {
        Vec3 receiverPosition = receiver.getBoundingBox().getCenter();

        // Only players in the receiver's dimension are visible.
        // Keep the nearest 128 to enforce the packet limit.
        List<PlayerSignalsPayload.Signal> signals = players.stream()
                .filter(player -> player != receiver
                        && player.level() == receiver.level())
                .filter(player -> player.isAlive() && !player.isSpectator())
                .sorted(Comparator.comparingDouble(
                        (ServerPlayer player) ->
                                player.getBoundingBox().getCenter()
                                        .distanceToSqr(receiverPosition)
                ))
                .limit(PlayerSignalsPayload.MAX_ENTRIES_PER_PACKET)
                .map(player -> new PlayerSignalsPayload.Signal(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        player.getBoundingBox().getCenter()
                ))
                .toList();

        Map<UUID, PlayerSignalsPayload.Signal> result = new LinkedHashMap<>();

        for (var signal : signals) {
            result.put(signal.id(), signal);
        }

        return result;
    }

    private static void sendFullSnapshot(
            ServerPlayer receiver,
            ResourceLocation dimension,
            Map<UUID, PlayerSignalsPayload.Signal> current,
            long tick
    ) {
        ServerPlayNetworking.send(
                receiver,
                new PlayerSignalsPayload(dimension, true, List.copyOf(current.values()), List.of())
        );

        lastSent.put(receiver.getUUID(), new ReceiverState(dimension, tick, current));
    }

    private static void sendDelta(
            ServerPlayer receiver,
            ResourceLocation dimension,
            Map<UUID, PlayerSignalsPayload.Signal> current,
            ReceiverState previous
    ) {
        // Represents the state currently known by the receiver.
        Map<UUID, PlayerSignalsPayload.Signal> sent =
                new LinkedHashMap<>(previous.signals());

        List<UUID> removed = new ArrayList<>();
        List<PlayerSignalsPayload.Signal> updated = new ArrayList<>();

        int remaining = PlayerSignalsPayload.MAX_ENTRIES_PER_PACKET;

        // Process removals first.
        for (UUID id : new ArrayList<>(sent.keySet())) {
            if (remaining == 0) {
                break;
            }

            if (!current.containsKey(id)) {
                removed.add(id);
                sent.remove(id);
                remaining--;
            }
        }

        // Process new players and changed signals.
        for (var entry : current.entrySet()) {
            if (remaining == 0) {
                break;
            }

            UUID id = entry.getKey();

            PlayerSignalsPayload.Signal newSignal = entry.getValue();
            PlayerSignalsPayload.Signal oldSignal = sent.get(id);

            boolean changed = oldSignal == null
                    || !oldSignal.name().equals(newSignal.name())
                    || oldSignal.position().distanceToSqr(newSignal.position()) > POSITION_EPSILON_SQUARED;

            if (changed) {
                updated.add(newSignal);
                sent.put(id, newSignal);
                remaining--;
            }
        }

        // No changes: no packet is sent.
        if (updated.isEmpty() && removed.isEmpty()) {
            return;
        }

        ServerPlayNetworking.send(
                receiver,
                new PlayerSignalsPayload(dimension, false, updated, removed)
        );

        lastSent.put(receiver.getUUID(), new ReceiverState(dimension, previous.lastFullSyncTick(), sent));
    }

    private static void clear() {
        toggleRequests.clear();
        lastSent.clear();
        RadioState.clearServer();
    }

    private record ReceiverState(
            ResourceLocation dimension,
            long lastFullSyncTick,
            Map<UUID, PlayerSignalsPayload.Signal> signals
    ) {
        private ReceiverState {
            signals = Map.copyOf(signals);
        }
    }

    private record ToggleRequest(InteractionHand hand, ItemStack stack, int expiresAt) {
    }
}