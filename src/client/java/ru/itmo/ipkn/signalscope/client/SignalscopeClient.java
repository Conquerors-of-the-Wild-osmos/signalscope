package ru.itmo.ipkn.signalscope.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.voice.RadioState;

public class SignalscopeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        var signals = new ClientSignals();
        ClientPlayNetworking.registerGlobalReceiver(PlayerSignalsPayload.TYPE,
                (payload, context) -> signals.accept(payload, context.client()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            signals.clear();
            RadioState.setClientTransmitting(false);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            signals.clear();
            RadioState.setClientTransmitting(false);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RadioState.setClientTransmitting(client.player != null && client.screen == null
                    && !client.isPaused() && client.options.keyUse.isDown()
                    && SignalscopeItems.isTransmitting(client.player));
            signals.tick(client);
        });
        HudRenderCallback.EVENT.register(new SignalscopeHud(signals)::render);
    }
}
