package ru.itmo.ipkn.signalscope.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import ru.itmo.ipkn.signalscope.Signalscope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Global passive signals; each source can later receive its own directional gain. */
final class RadioSounds {
    private final ClientSignals signals;
    private final Map<UUID, BeaconSound> sounds = new HashMap<>();

    RadioSounds(ClientSignals signals) {
        this.signals = signals;
    }

    void tick(Minecraft client) {
        var active = new HashSet<UUID>();
        for (var signal : signals.players()) {
            if (signal.transmitting() || client.player == null || signal.id().equals(client.player.getUUID())) continue;
            active.add(signal.id());
            BeaconSound sound = sounds.get(signal.id());
            if (sound == null || sound.isStopped()) {
                sound = new BeaconSound(signal.id());
                sounds.put(signal.id(), sound);
                client.getSoundManager().play(sound);
            }
            sound.audible = true;
        }
        sounds.forEach((id, sound) -> {
            if (!active.contains(id)) sound.audible = false;
        });
        sounds.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }

    void clear(Minecraft client) {
        sounds.values().forEach(client.getSoundManager()::stop);
        sounds.clear();
    }

    private static final class BeaconSound extends AbstractTickableSoundInstance {
        private boolean audible = true;

        BeaconSound(UUID id) {
            super(SoundEvent.createVariableRangeEvent(Signalscope.id("beacon_" + Math.floorMod(id.hashCode(), 5))),
                    SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            looping = true;
            relative = true;
            attenuation = Attenuation.NONE;
            volume = 0;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            // Five ticks in, eight ticks out; changing direction keeps the current level.
            volume = audible ? Math.min(0.15f, volume + 0.03f) : Math.max(0, volume - 0.01875f);
            if (!audible && volume == 0) stop();
        }
    }
}
