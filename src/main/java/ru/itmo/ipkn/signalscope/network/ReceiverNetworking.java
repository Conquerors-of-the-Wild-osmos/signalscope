package ru.itmo.ipkn.signalscope.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import ru.itmo.ipkn.signalscope.block.ReceiverBlockEntity;
import ru.itmo.ipkn.signalscope.menu.ReceiverMenu;

public final class ReceiverNetworking {

    private ReceiverNetworking() {
    }

    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(ReceiverSettingsPayload.TYPE, (payload, context) -> {
            if (!(context.player().containerMenu instanceof ReceiverMenu menu)
                    || !menu.pos().equals(payload.pos()) || !menu.stillValid(context.player())) {
                return;
            }
            if (context.player().level().getBlockEntity(payload.pos()) instanceof ReceiverBlockEntity receiver) {
                receiver.setSettings(payload.azimuth(), payload.elevation(), payload.coneAngle());
            }
        });
    }
}
