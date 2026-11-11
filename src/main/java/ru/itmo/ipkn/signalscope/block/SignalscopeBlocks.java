package ru.itmo.ipkn.signalscope.block;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import ru.itmo.ipkn.signalscope.Signalscope;

public final class SignalscopeBlocks {

    public static final Block SIGNAL_BLOCKER = Registry.register(
            BuiltInRegistries.BLOCK, Signalscope.id("signal_blocker"), SignalBlockerBlock.INSTANCE
    );
    public static final Item SIGNAL_BLOCKER_ITEM = Registry.register(
            BuiltInRegistries.ITEM, Signalscope.id("signal_blocker"), new BlockItem(SIGNAL_BLOCKER, new Item.Properties())
    );

    private SignalscopeBlocks() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> entries.accept(SIGNAL_BLOCKER_ITEM));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                SignalBlockerState.get(level.getServer()).synchronizeChunk(level, chunk)
        );
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                SignalBlockerState.get(level.getServer()).synchronizeChunk(level, chunk)
        );
    }
}
