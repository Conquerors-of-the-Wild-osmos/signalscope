package ru.itmo.ipkn.signalscope.voice;

import java.util.Map;
import java.util.UUID;

/** Immutable snapshots keep SVC's audio threads away from mutable Minecraft world state. */
public final class RadioState {
    private static volatile Snapshot server = new Snapshot(Map.of(), 0);
    private static volatile boolean clientTransmitting;

    private RadioState() {
    }

    public static void publish(Map<UUID, Player> players) {
        server = new Snapshot(Map.copyOf(players), System.nanoTime());
    }

    public static Map<UUID, Player> serverPlayers() {
        Snapshot current = server;
        // A paused/stalled/stopped server must not keep an old transmission open indefinitely.
        return System.nanoTime() - current.updatedAt() < 1_000_000_000L ? current.players() : Map.of();
    }

    public static void clearServer() {
        server = new Snapshot(Map.of(), 0);
    }

    public static void setClientTransmitting(boolean transmitting) {
        clientTransmitting = transmitting;
    }

    public static boolean isClientTransmitting() {
        return clientTransmitting;
    }

    public record Player(String dimension, boolean transmitting) {
    }

    private record Snapshot(Map<UUID, Player> players, long updatedAt) {
    }
}
