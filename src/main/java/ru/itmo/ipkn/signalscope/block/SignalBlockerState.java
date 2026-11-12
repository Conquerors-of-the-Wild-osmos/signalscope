package ru.itmo.ipkn.signalscope.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.signal.SignalScreenGeometry;

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
    private Map<ResourceLocation, List<ActiveBlock>> byDimension;
    private Map<ChunkKey, List<Key>> byChunk;

    public static SignalBlockerState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    static SignalBlockerState load(CompoundTag tag, HolderLookup.Provider registries) {
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
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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
        setPower(level.dimension().location(), position, power);
    }

    void setPower(ResourceLocation dimension, BlockPos position, int power) {
        Key key = new Key(dimension, position.immutable());
        if (power > 0) {
            Integer oldPower = powers.put(key, power);
            if (oldPower == null || oldPower != power) {
                invalidateIndex();
                setDirty();
            }
        } else if (powers.remove(key) != null) {
            invalidateIndex();
            setDirty();
        }
    }

    public void remove(ServerLevel level, BlockPos position) {
        setPower(level, position, 0);
    }

    public Collection<ActiveBlock> activeBlocks(ResourceKey<Level> dimension) {
        ensureIndex();
        return byDimension.getOrDefault(dimension.location(), List.of());
    }

    private void invalidateIndex() {
        byDimension = null;
        byChunk = null;
    }

    private void ensureIndex() {
        if (byDimension != null) return;
        Map<ResourceLocation, List<ActiveBlock>> dimensions = new HashMap<>();
        Map<ChunkKey, List<Key>> chunks = new HashMap<>();
        powers.forEach((key, power) -> {
            dimensions.computeIfAbsent(key.dimension(), ignored -> new ArrayList<>())
                    .add(new ActiveBlock(key.position(), power));
            chunks.computeIfAbsent(new ChunkKey(key.dimension(), new ChunkPos(key.position())),
                    ignored -> new ArrayList<>()).add(key);
        });
        dimensions.replaceAll((key, entries) -> List.copyOf(entries));
        chunks.replaceAll((key, entries) -> List.copyOf(entries));
        byDimension = dimensions;
        byChunk = chunks;
    }

    /**
     * Applies the same screen-boundary rule to player and block endpoints.
     */
    public static boolean blocks(Collection<ActiveBlock> screens, Vec3 from, Vec3 to) {
        for (ActiveBlock screen : screens) {
            BlockPos pos = screen.pos();
            if (SignalScreenGeometry.blocks(from.x, from.y, from.z, to.x, to.y, to.z,
                    pos.getX(), pos.getY(), pos.getZ(), screen.power())) return true;
        }
        return false;
    }

    public void synchronizeChunk(ServerLevel level, LevelChunk chunk) {
        ensureIndex();
        List<Key> entries = byChunk.getOrDefault(new ChunkKey(level.dimension().location(), chunk.getPos()), List.of());
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

    private record ChunkKey(ResourceLocation dimension, ChunkPos chunk) {
    }
}
