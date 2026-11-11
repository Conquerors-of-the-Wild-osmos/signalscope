package ru.itmo.ipkn.signalscope.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Persistent active signal blockers, shared by every dimension of one server.
 */
public final class SignalBlockerState extends SavedData {

    private static final String DATA_ID = "signalscope_signal_blockers";
    private static final String ENTRIES_TAG = "entries";
    private static final String DIMENSION_TAG = "dimension";
    private static final String POSITION_TAG = "position";
    private static final String POWER_TAG = "power";

    private static final Factory<SignalBlockerState> FACTORY = new Factory<>(
            SignalBlockerState::new,
            SignalBlockerState::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<Key, Integer> powers = new HashMap<>();

    public static SignalBlockerState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    static SignalBlockerState load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        SignalBlockerState state = new SignalBlockerState();
        ListTag entries = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString(DIMENSION_TAG));
            int power = entry.getInt(POWER_TAG);
            if (dimension != null && power > 0 && power <= 15) {
                state.powers.put(new Key(dimension, BlockPos.of(entry.getLong(POSITION_TAG))), power);
            }
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (var entry : powers.entrySet()) {
            CompoundTag saved = new CompoundTag();
            saved.putString(DIMENSION_TAG, entry.getKey().dimension().toString());
            saved.putLong(POSITION_TAG, entry.getKey().position().asLong());
            saved.putInt(POWER_TAG, entry.getValue());
            entries.add(saved);
        }
        tag.put(ENTRIES_TAG, entries);
        return tag;
    }

    public void setPower(ServerLevel level, BlockPos position, int power) {
        Key key = new Key(level.dimension().location(), position.immutable());
        if (power > 0) {
            Integer oldPower = powers.put(key, power);
            if (oldPower == null || oldPower != power) {
                setDirty();
            }
        } else if (powers.remove(key) != null) {
            setDirty();
        }
    }

    public void remove(ServerLevel level, BlockPos position) {
        setPower(level, position, 0);
    }

    public Collection<ActiveBlock> activeBlocks(ResourceKey<Level> dimension) {
        List<ActiveBlock> result = new ArrayList<>();
        ResourceLocation dimensionId = dimension.location();
        for (var entry : powers.entrySet()) {
            if (entry.getKey().dimension().equals(dimensionId)) {
                result.add(new ActiveBlock(entry.getKey().position(), entry.getValue()));
            }
        }
        return List.copyOf(result);
    }

    public void synchronizeChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
        List<Key> entries = powers.keySet().stream()
                .filter(key -> key.dimension().equals(level.dimension().location()))
                .filter(key -> new net.minecraft.world.level.ChunkPos(key.position()).equals(chunk.getPos()))
                .toList();
        for (Key key : entries) {
            var blockState = chunk.getBlockState(key.position());
            if (blockState.is(SignalBlockerBlock.INSTANCE)) {
                setPower(level, key.position(), blockState.getValue(SignalBlockerBlock.POWER));
            } else {
                remove(level, key.position());
            }
        }
    }

    public record ActiveBlock(BlockPos pos, int power) {
    }

    private record Key(ResourceLocation dimension, BlockPos position) {
    }
}
