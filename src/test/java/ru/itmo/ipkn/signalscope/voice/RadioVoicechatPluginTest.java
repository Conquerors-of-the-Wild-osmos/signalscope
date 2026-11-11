package ru.itmo.ipkn.signalscope.voice;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RadioVoicechatPluginTest {

    private final RadioVoicechatPlugin plugin = new RadioVoicechatPlugin();
    private final UUID speaker = UUID.randomUUID();
    private final Map<UUID, RadioState.Player> players = new HashMap<>();
    private final Map<UUID, VoicechatConnection> connections = new HashMap<>();
    private final List<UUID> delivered = new ArrayList<>();
    private boolean cancelled;
    private byte[] audio = {1, 2, 3};
    private UUID channel;

    @AfterEach
    void clearState() {
        RadioState.clearServer();
        RadioState.setClientTransmitting(false);
        RadioState.setClientGains(Map.of());
    }

    @Test
    void clientGainUsesImmutableChannelSnapshotAndDefaultsToSilence() {
        UUID channel = RadioVoicechatPlugin.channelId(speaker);
        var gains = new HashMap<UUID, Float>();
        gains.put(channel, 0.5f);
        RadioState.setClientGains(gains);
        gains.clear();
        assertEquals(0.5f, RadioState.clientGain(channel));
        assertEquals(0f, RadioState.clientGain(UUID.randomUUID()));
        assertEquals(0f, RadioState.clientGain(null));
    }

    @Test
    void broadcastsOnceToOtherConnectedPlayersInTheSameDimension() {
        addPlayer(speaker, "overworld", true, true, false);
        UUID first = addReceiver("overworld", true, false);
        UUID second = addReceiver("overworld", true, false);
        addReceiver("nether", true, false);
        addReceiver("overworld", false, false);
        addReceiver("overworld", true, true);
        UUID noVoicechat = addReceiver("overworld", true, false);
        connections.remove(noVoicechat);
        route();
        assertTrue(cancelled, "Proximity/group routing must always be cancelled");
        assertEquals(2, delivered.size());
        assertTrue(delivered.containsAll(List.of(first, second)));
        assertNotEquals(speaker, channel, "Radio must use a separate audio channel");
        assertEquals(RadioVoicechatPlugin.channelId(speaker), channel);
    }

    @Test
    void screenBlocksOnlySelectedReceivers() {
        addPlayer(speaker, "overworld", true, true, false);
        UUID blocked = addReceiver("overworld", true, false);
        UUID allowed = addReceiver("overworld", true, false);
        players.put(speaker, new RadioState.Player("overworld", true, java.util.Set.of(blocked)));
        route();
        assertEquals(List.of(allowed), delivered);
    }

    @Test
    void rejectsVoiceWhenTheRadioIsNotBeingUsed() {
        addPlayer(speaker, "overworld", false, true, false);
        addReceiver("overworld", true, false);
        route();
        assertTrue(cancelled);
        assertTrue(delivered.isEmpty());
    }

    @Test
    void releaseStopsVoiceButStillDeliversTheStreamTerminator() {
        addPlayer(speaker, "overworld", true, true, false);
        UUID receiver = addReceiver("overworld", true, false);
        route();
        assertEquals(List.of(receiver), delivered);
        delivered.clear();
        addPlayer(speaker, "overworld", false, true, false);
        route();
        assertTrue(delivered.isEmpty());
        audio = new byte[0];
        route();
        assertEquals(List.of(receiver), delivered);
    }

    @Test
    void respectsCancellationByAnotherPlugin() {
        addPlayer(speaker, "overworld", true, true, false);
        addReceiver("overworld", true, false);
        RadioState.publish(players);
        cancelled = true;
        plugin.onMicrophonePacket(event());
        assertTrue(delivered.isEmpty());
    }

    @Test
    void rejectsUnknownAndDisconnectedSenders() {
        addReceiver("overworld", true, false);
        route();
        assertTrue(cancelled);
        assertTrue(delivered.isEmpty());
        addPlayer(speaker, "overworld", true, false, false);
        route();
        assertTrue(delivered.isEmpty());
    }

    @Test
    void clearsRoutingOnServerStopAndDoesNotRetainMutableMaps() {
        addPlayer(speaker, "overworld", true, true, false);
        addReceiver("overworld", true, false);
        RadioState.publish(players);
        players.clear();
        plugin.onMicrophonePacket(event());
        assertEquals(1, delivered.size());
        delivered.clear();
        RadioState.clearServer();
        cancelled = false;
        plugin.onMicrophonePacket(event());
        assertTrue(delivered.isEmpty());
    }

    @Test
    void gatesMicrophoneFramesOnTheClient() {
        ClientSoundEvent event = stub(ClientSoundEvent.class, (proxy, method, args) -> {
            if (method.getName().equals("cancel")) {
                cancelled = true;
                return true;
            }
            throw new AssertionError(method);
        });
        plugin.onClientSound(event);
        assertTrue(cancelled);
        cancelled = false;
        RadioState.setClientTransmitting(true);
        plugin.onClientSound(event);
        assertFalse(cancelled);
        RadioState.setClientTransmitting(false);
        plugin.onClientSound(event);
        assertTrue(cancelled);
    }

    private UUID addReceiver(String dimension, boolean connected, boolean disabled) {
        UUID id = UUID.randomUUID();
        addPlayer(id, dimension, false, connected, disabled);
        return id;
    }

    private void addPlayer(UUID id, String dimension, boolean transmitting, boolean connected, boolean disabled) {
        players.put(id, new RadioState.Player(dimension, transmitting));
        ServerPlayer player = stub(ServerPlayer.class, (proxy, method, args) -> {
            if (method.getName().equals("getUuid")) {
                return id;
            }
            throw new AssertionError(method);
        });
        connections.put(id, stub(VoicechatConnection.class, (proxy, method, args) -> switch (method.getName()) {
            case "getPlayer" -> player;
            case "isConnected" -> connected;
            case "isDisabled" -> disabled;
            default -> throw new AssertionError(method);
        }));
    }

    private void route() {
        RadioState.publish(players);
        cancelled = false;
        plugin.onMicrophonePacket(event());
    }

    private MicrophonePacketEvent event() {
        StaticSoundPacket packet = stub(StaticSoundPacket.class, (proxy, method, args) -> {
            throw new AssertionError(method);
        });
        var builder = stub(StaticSoundPacket.Builder.class, (proxy, method, args) -> {
            if (method.getName().equals("channelId")) {
                channel = (UUID) args[0];
                return proxy;
            }
            if (method.getName().equals("category")) {
                assertEquals("signalscope", args[0]);
                return proxy;
            }
            if (method.getName().equals("build")) {
                return packet;
            }
            throw new AssertionError(method);
        });
        MicrophonePacket microphone = stub(MicrophonePacket.class, (proxy, method, args) -> switch (method.getName()) {
            case "getOpusEncodedData" -> audio;
            case "staticSoundPacketBuilder" -> builder;
            default -> throw new AssertionError(method);
        });
        VoicechatServerApi api = stub(VoicechatServerApi.class, (proxy, method, args) -> {
            if (method.getName().equals("getConnectionOf")) {
                return connections.get(args[0]);
            }
            if (method.getName().equals("sendStaticSoundPacketTo")) {
                assertSame(packet, args[1]);
                delivered.add(((VoicechatConnection) args[0]).getPlayer().getUuid());
                return null;
            }
            throw new AssertionError(method);
        });
        return stub(MicrophonePacketEvent.class, (proxy, method, args) -> switch (method.getName()) {
            case "isCancelled" -> cancelled;
            case "cancel" -> {
                cancelled = true;
                yield true;
            }
            case "getSenderConnection" -> connections.get(speaker);
            case "getPacket" -> microphone;
            case "getVoicechat" -> api;
            default -> throw new AssertionError(method);
        });
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
