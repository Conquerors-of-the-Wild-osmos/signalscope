package ru.itmo.ipkn.signalscope.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import ru.itmo.ipkn.signalscope.Signalscope;

import java.util.*;

public final class TransmitterBlock extends BaseEntityBlock {
    public static final TransmitterBlock BLOCK = new TransmitterBlock();
    public static BlockEntityType<Entity> ENTITY;
    private static final Map<ServerLevel, Set<BlockPos>> LOADED = new WeakHashMap<>();

    private TransmitterBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F)); }

    public static void initialize() {
        Registry.register(BuiltInRegistries.BLOCK, Signalscope.id("transmitter"), BLOCK);
        ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Signalscope.id("transmitter"),
                BlockEntityType.Builder.of(Entity::new, BLOCK).build(null));
        Registry.register(BuiltInRegistries.ITEM, Signalscope.id("transmitter"), new BlockItem(BLOCK, new Item.Properties()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> entries.accept(BLOCK));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> chunk.getBlockEntitiesPos().forEach(pos -> {
            if (chunk.getBlockEntity(pos) instanceof Entity) register(level, pos);
        }));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            var positions = LOADED.get(level);
            if (positions != null) positions.removeIf(pos -> (pos.getX() >> 4) == chunk.getPos().x && (pos.getZ() >> 4) == chunk.getPos().z);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> LOADED.clear());
    }

    private static void register(ServerLevel level, BlockPos pos) {
        LOADED.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.immutable());
    }

    public static Collection<BlockPos> activePositions(ServerLevel level) {
        var result = new ArrayList<BlockPos>();
        for (var pos : LOADED.getOrDefault(level, Set.of())) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BLOCK) && level.hasNeighborSignal(pos)) result.add(pos);
        }
        return result;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(BLOCK); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Entity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof Entity entity) serverPlayer.openMenu(entity);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel server && !oldState.is(state.getBlock())) register(server, pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            var positions = LOADED.get(server);
            if (positions != null) positions.remove(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static final class Entity extends BlockEntity implements
            net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<ru.itmo.ipkn.signalscope.menu.TransmitterMenu.Data> {
        private String name = "";
        public Entity(BlockPos pos, BlockState state) { super(ENTITY, pos, state); }
        public String name() { return name; }
        public void setName(String name) {
            this.name = net.minecraft.util.StringUtil.filterText(name).strip();
            setChanged();
        }
        @Override public net.minecraft.network.chat.Component getDisplayName() {
            return net.minecraft.network.chat.Component.translatable("block.signalscope.transmitter");
        }
        @Override public ru.itmo.ipkn.signalscope.menu.TransmitterMenu.Data getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
            return new ru.itmo.ipkn.signalscope.menu.TransmitterMenu.Data(worldPosition, name);
        }
        @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
            return new ru.itmo.ipkn.signalscope.menu.TransmitterMenu(id,
                    new ru.itmo.ipkn.signalscope.menu.TransmitterMenu.Data(worldPosition, name));
        }
        @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putString("name", name);
        }
        @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            String loaded = net.minecraft.util.StringUtil.filterText(tag.getString("name")).strip();
            name = loaded.substring(0, Math.min(16, loaded.length()));
        }
    }
}
