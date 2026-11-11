package ru.itmo.ipkn.signalscope.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import ru.itmo.ipkn.signalscope.Signalscope;

public final class ReceiverBlocks {

    public static final Block RECEIVER = Registry.register(BuiltInRegistries.BLOCK,
            Signalscope.id("receiver"), new ReceiverBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.5F)));
    public static final Item RECEIVER_ITEM = Registry.register(BuiltInRegistries.ITEM,
            Signalscope.id("receiver"), new BlockItem(RECEIVER, new Item.Properties()));
    public static final BlockEntityType<ReceiverBlockEntity> RECEIVER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Signalscope.id("receiver"),
            BlockEntityType.Builder.of(ReceiverBlockEntity::new, RECEIVER).build(null)
    );

    private ReceiverBlocks() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> entries.accept(RECEIVER_ITEM));
    }
}
