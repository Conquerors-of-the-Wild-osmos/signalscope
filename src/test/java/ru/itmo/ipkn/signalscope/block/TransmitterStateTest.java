package ru.itmo.ipkn.signalscope.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransmitterStateTest {
    @Test
    void remembersPowerAndNamesAcrossReloadAndRemovesInactive() {
        var state = new TransmitterState();
        var pos = new BlockPos(-100, 70, 230);
        state.update(pos, true, "Маяк");
        var restored = TransmitterState.load(state.save(new CompoundTag(), null), null);
        assertEquals("Маяк", restored.active().get(pos));
        restored.update(pos, true, "Новое имя");
        assertEquals("Новое имя", restored.active().get(pos));
        restored.update(pos, false, "");
        assertTrue(TransmitterState.load(restored.save(new CompoundTag(), null), null).active().isEmpty());
    }
}
