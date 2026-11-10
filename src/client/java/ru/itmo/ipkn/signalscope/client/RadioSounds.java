package ru.itmo.ipkn.signalscope.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import ru.itmo.ipkn.signalscope.Signalscope;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload.Signal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Passive signals share the same directional gain as radio voice.
 */
final class RadioSounds {
    private static final int RETRY_TICKS = 20;
    private final ClientSignals signals;
    private final Map<UUID, PlayingSound> sounds = new HashMap<>();
    private long tick;

    RadioSounds(ClientSignals signals) {
        this.signals = signals;
    }

    void tick(Minecraft client, Map<UUID, Float> gains) {
        if (client.player == null || !client.player.isAlive() || !SignalscopeItems.holdsSignalscope(client.player)) {
            clear(client);
            return;
        }
        if (client.isPaused()) return;
        boolean audible = client.options.getSoundSourceVolume(SoundSource.MASTER) > 0
                && client.options.getSoundSourceVolume(SoundSource.PLAYERS) > 0;
        update(signals.players(), client.player.getUUID(), gains,
                audible, new MinecraftPlayback(client.getSoundManager()));
    }

    void update(List<Signal> signals, UUID listener, Map<UUID, Float> gains,
                boolean audible, Playback playback) {
        tick++;
        if (!audible || listener == null) {
            clear(playback);
            return;
        }
        Map<UUID, Float> wanted = new HashMap<>();
        for (var signal : signals) {
            float volume = 0.15F * gains.getOrDefault(signal.id(), 0F);
            if (!signal.transmitting() && !signal.id().equals(listener) && volume > 0) {
                wanted.put(signal.id(), volume);
            }
        }

        // Free inaudible channels before starting any new sources, even if the engine no longer ticks them.
        sounds.entrySet().removeIf(entry -> {
            if (wanted.containsKey(entry.getKey())) return false;
            stop(entry.getValue().sound(), playback);
            return true;
        });
        wanted.forEach((id, volume) -> {
            PlayingSound playing = sounds.get(id);
            if (playing != null && !playing.sound().isStopped() && playback.isActive(playing.sound())) {
                playing.sound().volume(volume);
                return;
            }
            // A reload, muted engine or exhausted channel pool can discard play() without setting isStopped().
            // Bound retries while the engine is unavailable; do not create a new source every tick.
            if (playing != null && tick < playing.retryAt()) return;
            if (playing != null) stop(playing.sound(), playback);
            BeaconSound sound = new BeaconSound(id, volume);
            sounds.put(id, new PlayingSound(sound, tick + RETRY_TICKS));
            playback.play(sound);
        });
    }

    void clear(Minecraft client) {
        clear(new MinecraftPlayback(client.getSoundManager()));
    }

    void clear(Playback playback) {
        sounds.values().forEach(playing -> stop(playing.sound(), playback));
        sounds.clear();
    }

    private static void stop(BeaconSound sound, Playback playback) {
        sound.finish();
        playback.stop(sound);
    }

    private record PlayingSound(BeaconSound sound, long retryAt) {
    }

    interface Playback {
        void play(SoundInstance sound);
        void stop(SoundInstance sound);
        boolean isActive(SoundInstance sound);
    }

    private record MinecraftPlayback(SoundManager manager) implements Playback {
        public void play(SoundInstance sound) { manager.play(sound); }
        public void stop(SoundInstance sound) { manager.stop(sound); }
        public boolean isActive(SoundInstance sound) { return manager.isActive(sound); }
    }

    private static final class BeaconSound extends AbstractTickableSoundInstance {
        BeaconSound(UUID id, float volume) {
            super(SoundEvent.createVariableRangeEvent(Signalscope.id("beacon_" + Math.floorMod(id.hashCode(), 5))),
                    SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            looping = true;
            relative = true;
            attenuation = Attenuation.NONE;
            this.volume = volume;
        }

        void volume(float volume) {
            this.volume = volume;
        }

        void finish() {
            volume = 0;
            stop();
        }

        @Override
        public void tick() {
            // SoundEngine reads the volume updated by RadioSounds on each sound tick.
        }
    }
}
