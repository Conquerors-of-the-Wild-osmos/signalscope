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

    private static final float LABEL_SCALE = 0.9F;
    private static final int LABEL_SPACING = 12;
    private static final int GREEN = 0xFF60E860;
    private static final int RED = 0xFFE85858;
    private final ClientSignals signals;

    public SignalscopeHud(ClientSignals signals) {
        this.signals = signals;
    }

    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || !client.player.isAlive()) {
            return;
        }
        if (!RadioState.isClientTransmitting() && !SignalscopeItems.holdsSignalscope(client.player)) {
            return;
        }
        var config = SignalscopeConfig.get();
        double guiScale = client.getWindow().getGuiScale();
        float scale = ScopeHudGeometry.hudScale(guiScale, config.hudScale);
        // Preserve vanilla's exact crosshair position while cancelling its size scaling.
        float centerX = ScopeHudGeometry.crosshairCenter(graphics.guiWidth()) / scale;
        float centerY = ScopeHudGeometry.crosshairCenter(graphics.guiHeight()) / scale;
        float hudWidth = (float) (client.getWindow().getWidth() / guiScale / scale);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1);
        try {
            renderHud(graphics, delta, client, config, centerX, centerY, hudWidth);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
        }
    }

    private void renderHud(GuiGraphics graphics, DeltaTracker delta, Minecraft client,
                           SignalscopeConfig config, float centerX, float centerY, float hudWidth) {
        if (RadioState.isClientTransmitting()) {
            graphics.drawCenteredString(client.font, Component.translatable("hud.signalscope.transmitting"),
                    (int) centerX, (int) centerY + 28, 0xFF70FF70);
        }
        if (!SignalscopeItems.holdsSignalscope(client.player)) {
            return;
        }

        float partialTick = delta.getGameTimeDeltaPartialTick(false);
        Vec3 eye = client.player.getEyePosition(partialTick);
        Vec3 look = client.player.getViewVector(partialTick);
        var ordered = signals.players().stream()
                .map(signal -> {
                    Vec3 offset = signal.position().subtract(eye);
                    double angle = SignalGeometry.angleDegrees(look.x, look.y, look.z, offset.x, offset.y, offset.z);
                    return new Target(signal, offset.length(), angle);
                })
                .filter(target -> ScopeHudGeometry.isVisible(target.angle(), config.receptionAngle))
                .sorted(Comparator.comparingDouble(Target::distance))
                .toList();
        if (ordered.isEmpty()) {
            // A hidden target behind the player is not an empty server snapshot.
            if (signals.shouldShowEmptyNotice()) {
                drawLabel(graphics, client, Component.translatable("hud.signalscope.no_players"),
                        centerX, centerY - 38, 0xFFAAAAAA);
            }
            return;
        }

        // Labels stack above the crosshair; all signal arcs share the crosshair's horizontal axis.
        int visible = Math.min(ordered.size(),
                Math.clamp(((int) centerY - 38) / LABEL_SPACING, 1, config.maxNicknames));
        boolean hasLockedTarget = false;
        for (int index = 0; index < ordered.size(); index++) {
            Target target = ordered.get(index);
            int state = ScopeHudGeometry.state(target.angle(), config.stableAngle, config.receptionAngle);
            boolean locked = state == 0;
            int color = locked ? GREEN : RED;
            if (locked) {
                hasLockedTarget = true;
            } else {
                float separation = ScopeHudGeometry.separation(target.angle(), hudWidth,
                        config.stableAngle, config.receptionAngle);
                int arcColor = state == 4 ? (RED & 0x00FFFFFF) | 0x80000000 : color;
                drawHalfRing(graphics, centerX - separation, centerY, true, state, arcColor);
                drawHalfRing(graphics, centerX + separation, centerY, false, state, arcColor);
            }
            if (index < visible) {
                drawLabel(graphics, client,
                        Component.translatable("hud.signalscope.player", target.signal().name().isEmpty()
                                ? Component.translatable("hud.signalscope.transmitter")
                                : Component.literal(target.signal().name()), Math.round(target.distance())),
                        centerX, centerY - 26 - index * LABEL_SPACING, color);
            }
        }
        // Draw locked signals last so overlapping red arcs cannot cover the green ring.
        if (hasLockedTarget) {
            drawHalfRing(graphics, centerX, centerY, true, 0, GREEN);
            drawHalfRing(graphics, centerX, centerY, false, 0, GREEN);
        }
        if (visible < ordered.size()) {
            drawLabel(graphics, client,
                    Component.translatable("hud.signalscope.more", ordered.size() - visible),
                    centerX, Math.max(2, centerY - 26 - visible * LABEL_SPACING), 0xFFAAAAAA);
        }
    }

    private static void drawLabel(GuiGraphics graphics, Minecraft client, Component text, float x, float y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1);
        graphics.drawCenteredString(client.font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    private static void drawHalfRing(GuiGraphics graphics, float x, float y, boolean left,
                                     int state, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        int edgeColor = ScopeHudGeometry.edgeColor(color);
        for (int row = 0; row < 17; row++) {
            int inner = ScopeHudGeometry.innerOffset(state, row);
            int outer = ScopeHudGeometry.outerOffset(state, row);
            int start = left ? -outer - 1 : inner;
            int end = left ? -inner : outer + 1;
            graphics.fill(start, row - 8, end, row - 7, color);
            int outside = left ? start - 1 : end;
            graphics.fill(outside, row - 8, outside + 1, row - 7, edgeColor);
            if (inner > 0) {
                int inside = left ? end : start - 1;
                graphics.fill(inside, row - 8, inside + 1, row - 7, edgeColor);
            }
            if (row == 0 || row == 16) {
                int edgeY = row == 0 ? -9 : 9;
                graphics.fill(start, edgeY, end, edgeY + 1, edgeColor);
            }
        }
        graphics.pose().popPose();
    }

    private record Target(PlayerSignalsPayload.Signal signal, double distance, double angle) {
    }
}
