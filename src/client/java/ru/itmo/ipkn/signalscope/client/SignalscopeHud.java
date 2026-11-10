package ru.itmo.ipkn.signalscope.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.signal.SignalGeometry;
import ru.itmo.ipkn.signalscope.voice.RadioState;

import java.util.Comparator;

public final class SignalscopeHud {

    private static final double LOCK_ANGLE = 5;
    private final ClientSignals signals;

    public SignalscopeHud(ClientSignals signals) {
        this.signals = signals;
    }

    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || !client.player.isAlive()) {
            return;
        }
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        if (RadioState.isClientTransmitting()) {
            graphics.drawCenteredString(client.font, Component.translatable("hud.signalscope.transmitting"),
                    centerX, centerY + 24, 0xFF70FF70);
        }
        if (!SignalscopeItems.holdsSignalscope(client.player)) {
            return;
        }

        float partialTick = delta.getGameTimeDeltaPartialTick(false);
        Vec3 eye = client.player.getEyePosition(partialTick);
        Vec3 look = client.player.getViewVector(partialTick);
        var ordered = signals.players().stream()
                .sorted(Comparator.comparingDouble(signal -> signal.position().distanceToSqr(eye)))
                .toList();
        if (ordered.isEmpty()) {
            graphics.drawCenteredString(client.font, Component.translatable("hud.signalscope.no_players"),
                    centerX, centerY - 32, 0xFFAAAAAA);
            return;
        }

        // Keep the nearest player's row closest to the crosshair. Fit the remaining rows above it.
        int visible = Math.min(ordered.size(), Math.max(1, (centerY - 24) / 30));
        for (int index = 0; index < visible; index++) {
            PlayerSignalsPayload.Signal signal = ordered.get(index);
            Vec3 offset = signal.position().subtract(eye);
            double angle = SignalGeometry.angleDegrees(look.x, look.y, look.z, offset.x, offset.y, offset.z);
            boolean locked = angle <= LOCK_ANGLE;
            int separation = (int) Math.round(Math.clamp((angle - LOCK_ANGLE) / 85, 0.0, 1.0)
                    * Math.min(90, graphics.guiWidth() / 4));
            int color = locked ? 0xFF70FF70 : angle > 90 ? 0x88FF6060 : 0xFFFF6060;
            int y = centerY - 30 - index * 30;
            drawHalfRing(graphics, centerX - separation, y, true, color);
            drawHalfRing(graphics, centerX + separation, y, false, color);
            graphics.drawCenteredString(client.font,
                    Component.translatable("hud.signalscope.player", signal.name(), Math.round(offset.length())),
                    centerX, y - 19, color);
        }
        if (visible < ordered.size()) {
            graphics.drawCenteredString(client.font,
                    Component.translatable("hud.signalscope.more", ordered.size() - visible),
                    centerX, Math.max(2, centerY - 30 - visible * 30), 0xFFAAAAAA);
        }
    }

    private static void drawHalfRing(GuiGraphics graphics, int x, int y, boolean left, int color) {
        for (int degrees = -90; degrees <= 90; degrees += 8) {
            double radians = Math.toRadians(degrees);
            int dx = (int) Math.round(Math.cos(radians) * 7) * (left ? -1 : 1);
            int dy = (int) Math.round(Math.sin(radians) * 7);
            graphics.fill(x + dx, y + dy, x + dx + 2, y + dy + 2, color);
        }
    }
}
