package ru.itmo.ipkn.signalscope;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.server.SignalscopeServer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Signalscope implements ModInitializer {
    public static final String MOD_ID = "signalscope";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
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
