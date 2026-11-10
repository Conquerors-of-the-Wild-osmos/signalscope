package ru.itmo.ipkn.signalscope.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload.Signal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RadioSoundsTest {
    private final UUID listener = UUID.randomUUID();
    private final UUID source = UUID.randomUUID();
    private final RadioSounds sounds = new RadioSounds(new ClientSignals());
    private final FakePlayback playback = new FakePlayback();

    @Test
    void startsAudiblyAndUpdatesVolumeWithoutRestartingTheLoop() {
        update(1F, true);
        SoundInstance sound = playback.started.getFirst();
        assertEquals(0.15F, sound.getVolume(), 1.0e-6);
        update(0.5F, true);
        assertEquals(1, playback.started.size());
        assertEquals(0.075F, sound.getVolume(), 1.0e-6);
        assertTrue(playback.isActive(sound));
    }

    @Test
    void zeroOrMissingGainNeverAllocatesChannelsAndStopsExistingLoops() {
        update(0F, true);
        sounds.update(List.of(signal(source, false)), listener, Map.of(), true, playback);
        assertTrue(playback.started.isEmpty());

        update(1F, true);
        SoundInstance first = playback.started.getFirst();
        update(0F, true);
        assertStopped(first);

        update(1F, true);
        SoundInstance second = playback.started.getLast();
        assertNotSame(first, second);
        sounds.update(List.of(signal(source, false)), listener, Map.of(), true, playback);
        assertStopped(second);
    }

    @Test
    void mutingEitherMinecraftVolumeReleasesSoundsAndUnmutingRestartsThem() {
        update(1F, false);
        assertTrue(playback.started.isEmpty());
        update(1F, true);
        SoundInstance first = playback.started.getFirst();
        update(1F, false);
        assertStopped(first);
        for (int i = 0; i < 40; i++) update(1F, false);
        assertEquals(1, playback.started.size());
        update(1F, true);
        assertEquals(2, playback.started.size());
        assertTrue(playback.isActive(playback.started.getLast()));
    }

    @Test
    void recoversWhenResourceReloadDiscardsChannelsWithoutStoppingInstances() {
        update(1F, true);
        SoundInstance first = playback.started.getFirst();
        playback.active.clear();
        assertFalse(((TickableSoundInstance) first).isStopped());

        for (int i = 0; i < 20; i++) update(1F, true);
        assertEquals(2, playback.started.size());
        assertStopped(first);
        SoundInstance replacement = playback.started.getLast();
        assertTrue(playback.isActive(replacement));
        assertEquals(first.getLocation(), replacement.getLocation());
    }

    @Test
    void retriesFailedPlaybackWithoutCreatingSourcesEveryTick() {
        playback.available = false;
        update(1F, true);
        for (int i = 0; i < 19; i++) update(1F, true);
        assertEquals(1, playback.started.size());
        update(1F, true);
        assertEquals(2, playback.started.size());

        playback.available = true;
        for (int i = 0; i < 20; i++) update(1F, true);
        assertEquals(3, playback.started.size());
        assertEquals(1, playback.active.size());
    }

    @Test
    void transmissionAndRemovalStopPassiveSoundWithoutWaitingForEngineTicks() {
        update(1F, true);
        SoundInstance first = playback.started.getFirst();
        sounds.update(List.of(signal(source, true)), listener, Map.of(source, 1F), true, playback);
        assertStopped(first);

        update(1F, true);
        SoundInstance second = playback.started.getLast();
        sounds.update(List.of(), listener, Map.of(source, 1F), true, playback);
        assertStopped(second);
    }

    @Test
    void neverPlaysOwnSignalAndClearsOnDisconnect() {
        sounds.update(List.of(signal(listener, false)), listener, Map.of(listener, 1F), true, playback);
        assertTrue(playback.started.isEmpty());
        update(1F, true);
        SoundInstance first = playback.started.getFirst();
        sounds.clear(playback);
        assertStopped(first);
        update(1F, true);
        assertEquals(2, playback.started.size());
        sounds.update(List.of(signal(source, false)), null, Map.of(source, 1F), true, playback);
        assertStopped(playback.started.getLast());
    }

    @Test
    void freesSilentChannelsBeforeStartingOtherSources() {
        update(1F, true);
        UUID other = UUID.randomUUID();
        playback.capacity = 1;
        sounds.update(List.of(signal(source, false), signal(other, false)), listener,
                Map.of(source, 0F, other, 1F), true, playback);
        assertStopped(playback.started.getFirst());
        assertTrue(playback.isActive(playback.started.getLast()));
        assertEquals(1, playback.active.size());
    }

    private void update(float gain, boolean audible) {
        sounds.update(List.of(signal(source, false)), listener, Map.of(source, gain), audible, playback);
    }

    private Signal signal(UUID id, boolean transmitting) {
        return new Signal(id, "Player", Vec3.ZERO, transmitting);
    }

    private void assertStopped(SoundInstance sound) {
        assertTrue(((TickableSoundInstance) sound).isStopped());
        assertFalse(playback.isActive(sound));
    }

    private static final class FakePlayback implements RadioSounds.Playback {
        private final List<SoundInstance> started = new ArrayList<>();
        private final Set<SoundInstance> active = new HashSet<>();
        private boolean available = true;
        private int capacity = Integer.MAX_VALUE;

        public void play(SoundInstance sound) {
            // Model asset resolution without a resource manager or an OpenAL device.
            // Minecraft's getVolume() multiplies the instance gain by the resolved asset's gain (1 here).
            try {
                var resolved = AbstractSoundInstance.class.getDeclaredField("sound");
                resolved.setAccessible(true);
                resolved.set(sound, new Sound(sound.getLocation(), random -> 1F, random -> 1F,
                        1, Sound.Type.FILE, false, false, 16));
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
            started.add(sound);
            if (available && active.size() < capacity) active.add(sound);
        }

        public void stop(SoundInstance sound) {
            active.remove(sound);
        }

        public boolean isActive(SoundInstance sound) {
            return active.contains(sound);
        }
    }
}
