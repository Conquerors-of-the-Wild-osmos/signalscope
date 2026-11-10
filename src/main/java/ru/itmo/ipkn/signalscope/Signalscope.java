package ru.itmo.ipkn.signalscope;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.server.SignalscopeServer;

public class Signalscope implements ModInitializer {

    public static final String MOD_ID = "signalscope";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SignalscopeItems.initialize();
        PayloadTypeRegistry.playS2C().register(PlayerSignalsPayload.TYPE, PlayerSignalsPayload.CODEC);
        SignalscopeServer.initialize();
        LOGGER.info("Signalscope PoC initialized");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
