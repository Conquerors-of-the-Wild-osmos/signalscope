package ru.itmo.ipkn.signalscope.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.signal.SignalGeometry;
import ru.itmo.ipkn.signalscope.voice.RadioState;

import java.util.Comparator;

public final class SignalscopeHud {

    private static final float LABEL_SCALE = 0.9F;
    private static final int LABEL_SPACING = 12;
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
                Math.min(config.maxNicknames, Math.max(1, ((int) centerY - 38) / LABEL_SPACING)));
        boolean hasLockedTarget = false;
        for (int index = 0; index < ordered.size(); index++) {
            Target target = ordered.get(index);
            boolean locked = target.angle() <= config.stableAngle;
            var arc = ScopeHudGeometry.arc(target.angle(), hudWidth, config.stableAngle, config.receptionAngle);
            int color = locked ? 0xFF70FF70 : 0xFFFF6060;
            if (locked) {
                hasLockedTarget = true;
            } else {
                drawHalfRing(graphics, centerX - arc.separation(), centerY, true, arc, color);
                drawHalfRing(graphics, centerX + arc.separation(), centerY, false, arc, color);
            }
            if (index < visible) {
                drawLabel(graphics, client,
                        Component.translatable("hud.signalscope.player", target.signal().name(), Math.round(target.distance())),
                        centerX, centerY - 26 - index * LABEL_SPACING, color);
            }
        }
        // Draw locked signals last so overlapping red arcs cannot cover the green ring.
        if (hasLockedTarget) {
            var arc = ScopeHudGeometry.arc(0, hudWidth, config.stableAngle, config.receptionAngle);
            drawHalfRing(graphics, centerX, centerY, true, arc, 0xFF70FF70);
            drawHalfRing(graphics, centerX, centerY, false, arc, 0xFF70FF70);
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
                                     ScopeHudGeometry.Arc arc, int color) {
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        var previousOuter = arc.point(left, 0, true);
        var previousInner = arc.point(left, 0, false);
        for (int step = 1; step <= ScopeHudGeometry.ARC_SEGMENTS; step++) {
            var outer = arc.point(left, step, true);
            var inner = arc.point(left, step, false);
            // Match vanilla GUI quad winding on both mirrored halves.
            if (left) {
                vertex(vertices, pose, x, y, previousOuter, color);
                vertex(vertices, pose, x, y, outer, color);
                vertex(vertices, pose, x, y, inner, color);
                vertex(vertices, pose, x, y, previousInner, color);
            } else {
                vertex(vertices, pose, x, y, previousInner, color);
                vertex(vertices, pose, x, y, inner, color);
                vertex(vertices, pose, x, y, outer, color);
                vertex(vertices, pose, x, y, previousOuter, color);
            }
            previousOuter = outer;
            previousInner = inner;
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y,
                               ScopeHudGeometry.Point point, int color) {
        vertices.addVertex(pose, x + point.x(), y + point.y(), 0).setColor(color);
    }

    private record Target(PlayerSignalsPayload.Signal signal, double distance, double angle) {
    }
}
