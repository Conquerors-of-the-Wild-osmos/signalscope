package ru.itmo.ipkn.signalscope.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Last known active transmitters in one dimension, including unloaded chunks. */
public final class TransmitterState extends SavedData {
    private static final Factory<TransmitterState> FACTORY = new Factory<>(
            TransmitterState::new, TransmitterState::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    private final Map<BlockPos, String> active = new HashMap<>();

    public static TransmitterState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "signalscope_transmitters");
    }

    static TransmitterState load(CompoundTag tag, HolderLookup.Provider registries) {
        var state = new TransmitterState();
        ListTag entries = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            String name = net.minecraft.util.StringUtil.filterText(entry.getString("name")).strip();
            state.active.put(BlockPos.of(entry.getLong("pos")), name.substring(0, Math.min(16, name.length())));
        }
        return state;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        active.forEach((pos, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos.asLong());
            entry.putString("name", name);
            entries.add(entry);
        });
        tag.put("entries", entries);
        return tag;
    }

    void update(BlockPos pos, boolean powered, String name) {
        String previous = powered ? active.put(pos.immutable(), name) : active.remove(pos);
        if (powered ? !name.equals(previous) : previous != null) setDirty();
    }

    public Map<BlockPos, String> active() {
        return Map.copyOf(active);
    }
}
