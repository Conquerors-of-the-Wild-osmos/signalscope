package ru.itmo.ipkn.signalscope.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import ru.itmo.ipkn.signalscope.Signalscope;

public final class SignalscopeItems {
    public static final Item RADIO = Registry.register(BuiltInRegistries.ITEM,
            Signalscope.id("radio"), new RadioItem(new Item.Properties().stacksTo(1)));
    public static final Item SIGNALSCOPE = Registry.register(BuiltInRegistries.ITEM,
            Signalscope.id("signalscope"), new Item(new Item.Properties().stacksTo(1)));

    private SignalscopeItems() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(RADIO);
            entries.accept(SIGNALSCOPE);
        });
    }

    public static boolean isTransmitting(LivingEntity player) {
        return player.isAlive() && player.isUsingItem() && player.getUseItem().is(RADIO)
                && player.getItemInHand(player.getUsedItemHand()).is(RADIO);
    }

    public static boolean holdsSignalscope(LivingEntity player) {
        return player.getMainHandItem().is(SIGNALSCOPE) || player.getOffhandItem().is(SIGNALSCOPE);
    }
}
