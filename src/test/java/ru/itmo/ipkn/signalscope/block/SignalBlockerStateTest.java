package ru.itmo.ipkn.signalscope.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalBlockerStateTest {

    @Test
    void persistsPowerAndDimensionAcrossReload() {
        CompoundTag entry = new CompoundTag();
        entry.putString("dimension", "minecraft:the_nether");
        entry.putLong("position", new BlockPos(-10, 64, 42).asLong());
        entry.putInt("power", 12);
        ListTag entries = new ListTag();
        entries.add(entry);
        CompoundTag data = new CompoundTag();
        data.put("entries", entries);

        SignalBlockerState state = SignalBlockerState.load(data, null);
        SignalBlockerState restored = SignalBlockerState.load(state.save(new CompoundTag(), null), null);
        assertTrue(restored.activeBlocks(Level.OVERWORLD).isEmpty());
        var screens = restored.activeBlocks(Level.NETHER);
        assertEquals(1, screens.size());
        assertEquals(new SignalBlockerState.ActiveBlock(new BlockPos(-10, 64, 42), 12), screens.iterator().next());
    }
}
