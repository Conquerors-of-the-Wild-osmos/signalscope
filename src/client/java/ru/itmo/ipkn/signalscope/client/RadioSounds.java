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

/**
 * Passive signals share the same directional gain as radio voice.
 */
final class RadioSounds {
    private final ClientSignals signals;
    private final Map<UUID, BeaconSound> sounds = new HashMap<>();

    RadioSounds(ClientSignals signals) {
        this.signals = signals;
    }

    void tick(Minecraft client, Map<UUID, Float> gains) {
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
            sound.stopWhenSilent = false;
            sound.targetVolume = 0.15f * gains.getOrDefault(signal.id(), 0f);
        }
        sounds.forEach((id, sound) -> {
            if (!active.contains(id)) sound.stopWhenSilent = true;
        });
        sounds.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }

    void clear(Minecraft client) {
        sounds.values().forEach(client.getSoundManager()::stop);
        sounds.clear();
    }

    private static final class BeaconSound extends AbstractTickableSoundInstance {
        private float targetVolume;
        private boolean stopWhenSilent;

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
            volume = stopWhenSilent ? 0 : targetVolume;
            if (stopWhenSilent) stop();
        }
    }
}
