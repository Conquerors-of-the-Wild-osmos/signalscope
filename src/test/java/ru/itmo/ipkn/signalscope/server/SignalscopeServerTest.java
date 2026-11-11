package ru.itmo.ipkn.signalscope.server;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload.Signal;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SignalscopeServerTest {
    @Test
    void transmitterRenameAndResetProduceDelta() {
        UUID id = UUID.randomUUID();
        var unnamed = new Signal(id, "", Vec3.ZERO, false);
        var named = new Signal(id, "Маяк", Vec3.ZERO, false);
        assertTrue(SignalscopeServer.signalChanged(unnamed, named));
        assertTrue(SignalscopeServer.signalChanged(named, unnamed));
        assertFalse(SignalscopeServer.signalChanged(unnamed, unnamed));
    }

    @Test
    void transmissionChangeProducesDeltaWithoutMovement() {
        UUID id = UUID.randomUUID();
        var silent = new Signal(id, "Explorer", Vec3.ZERO, false);
        var speaking = new Signal(id, "Explorer", Vec3.ZERO, true);
        assertFalse(SignalscopeServer.signalChanged(silent, silent));
        assertTrue(SignalscopeServer.signalChanged(silent, speaking));
        assertTrue(SignalscopeServer.signalChanged(speaking, silent));
    }
}
