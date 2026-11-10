package ru.itmo.ipkn.signalscope.client;

import net.fabricmc.api.ClientModInitializer;

public final class ConfigClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SignalscopeConfig.load();
    }
}
