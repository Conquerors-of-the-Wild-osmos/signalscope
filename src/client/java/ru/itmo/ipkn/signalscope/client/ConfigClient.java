package ru.itmo.ipkn.signalscope.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ConfigClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SignalscopeConfig.load();
        var key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.signalscope.config",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.signalscope"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(SignalscopeConfig.screen(null));
                }
            }
        });
    }
}
