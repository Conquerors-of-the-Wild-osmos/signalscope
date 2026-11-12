package ru.itmo.ipkn.signalscope.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SignalBlockerStateTest {

    @Test
    void indexedScreensRefreshAfterPowerChanges() {
        var state = new SignalBlockerState();
        BlockPos pos = new BlockPos(16, 64, -1);
        assertTrue(state.activeBlocks(Level.OVERWORLD).isEmpty());
        state.setPower(Level.OVERWORLD.location(), pos, 3);
        var previous = state.activeBlocks(Level.OVERWORLD);
        assertEquals(List.of(new SignalBlockerState.ActiveBlock(pos, 3)), previous);
        state.setPower(Level.OVERWORLD.location(), pos, 5);
        assertEquals(List.of(new SignalBlockerState.ActiveBlock(pos, 5)), state.activeBlocks(Level.OVERWORLD));
        assertEquals(List.of(new SignalBlockerState.ActiveBlock(pos, 3)), previous);
        state.setPower(Level.OVERWORLD.location(), pos, 0);
        assertTrue(state.activeBlocks(Level.OVERWORLD).isEmpty());
    }

    @Test
    void screensApplyToPlayerAndBlockEndpoints() {
        var screens = List.of(new SignalBlockerState.ActiveBlock(BlockPos.ZERO, 2));
        Vec3 playerInside = new Vec3(0.5, 0.9, 0.5);
        Vec3 blockInside = Vec3.atCenterOf(BlockPos.ZERO);
        Vec3 playerOutside = new Vec3(5.5, 0.9, 0.5);
        Vec3 blockOutside = Vec3.atCenterOf(new BlockPos(5, 0, 0));
        for (Vec3 source : List.of(playerInside, blockInside)) {
            for (Vec3 receiver : List.of(playerOutside, blockOutside)) {
                assertTrue(SignalBlockerState.blocks(screens, source, receiver));
                assertTrue(SignalBlockerState.blocks(screens, receiver, source));
            }
        }
        assertFalse(SignalBlockerState.blocks(screens, playerInside, blockInside));
        assertFalse(SignalBlockerState.blocks(List.of(), playerInside, playerOutside));
        assertTrue(SignalBlockerState.blocks(screens, new Vec3(-5, 0.5, 0.5), blockOutside));
        assertFalse(SignalBlockerState.blocks(screens, new Vec3(-5, 4, 0.5), new Vec3(5, 4, 0.5)));
    }

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
