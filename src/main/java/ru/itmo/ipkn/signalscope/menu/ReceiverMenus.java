package ru.itmo.ipkn.signalscope.menu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import ru.itmo.ipkn.signalscope.Signalscope;

public final class ReceiverMenus {

    public static final ExtendedScreenHandlerType<ReceiverMenu, ReceiverScreenData> RECEIVER = Registry.register(
            BuiltInRegistries.MENU,
            Signalscope.id("receiver"),
            new ExtendedScreenHandlerType<>((containerId, inventory, data) -> new ReceiverMenu(containerId, data),
                    ReceiverScreenData.STREAM_CODEC)
    );

    private ReceiverMenus() {
    }

    public static void initialize() {
    }
}
