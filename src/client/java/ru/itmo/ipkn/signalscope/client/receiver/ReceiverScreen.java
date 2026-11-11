package ru.itmo.ipkn.signalscope.client.receiver;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ru.itmo.ipkn.signalscope.menu.ReceiverMenu;
import ru.itmo.ipkn.signalscope.network.ReceiverSettingsPayload;

public final class ReceiverScreen extends AbstractContainerScreen<ReceiverMenu> {

    public ReceiverScreen(ReceiverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 310;
        imageHeight = 142;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new SettingSlider(leftPos + 16, topPos + 34, "gui.signalscope.receiver.azimuth",
                0, 359, menu.azimuth()));
        addRenderableWidget(new SettingSlider(leftPos + 16, topPos + 65, "gui.signalscope.receiver.elevation",
                -90, 90, menu.elevation()));
        addRenderableWidget(new SettingSlider(leftPos + 16, topPos + 96, "gui.signalscope.receiver.cone_angle",
                1, 180, menu.coneAngle()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2D2D2D);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF171717);
        int cx = leftPos + 253, cy = topPos + 72, radius = 34;
        int direction = azimuth(), opening = coneAngle();
        // ponytail: pixel rasterization limits detail to the GUI radius; use vector rendering for larger diagrams.
        for (int dy = -radius; dy <= radius; dy++) {
            int edge = (int) Math.sqrt(radius * radius - dy * dy);
            int runStart = -edge, previousColor = 0;
            for (int dx = -edge; dx <= edge + 1; dx++) {
                double bearing = Math.toDegrees(Math.atan2(dx, -dy));
                double delta = Math.abs(Math.IEEEremainder(bearing - direction, 360));
                int color = delta <= opening / 2.0 ? 0xFF52D9AF : 0xFF183F3D;
                if (dx > -edge && (dx > edge || color != previousColor)) {
                    graphics.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, previousColor);
                    runStart = dx;
                }
                previousColor = color;
            }
        }
        double radians = Math.toRadians(direction);
        for (int r = 0; r <= radius; r++) {
            int x = cx + (int) Math.round(Math.sin(radians) * r);
            int y = cy - (int) Math.round(Math.cos(radians) * r);
            graphics.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
        }
        graphics.drawCenteredString(font, "N", cx, cy - radius - 12, 0xFFFFFFFF);
        int scaleX = leftPos + 298;
        graphics.fill(scaleX, cy - radius, scaleX + 3, cy + radius + 1, 0xFF183F3D);
        int markerY = cy + radius - (elevation() + 90) * (2 * radius) / 180;
        graphics.fill(scaleX - 3, markerY - 1, scaleX + 6, markerY + 2, 0xFF52D9AF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
    }

    private void send() {
        if (ClientPlayNetworking.canSend(ReceiverSettingsPayload.TYPE)) {
            ClientPlayNetworking.send(new ReceiverSettingsPayload(menu.pos(),
                    azimuth(), elevation(), coneAngle()));
        }
    }

    private int azimuth() {
        return ((SettingSlider) children().get(0)).value();
    }

    private int elevation() {
        return ((SettingSlider) children().get(1)).value();
    }

    private int coneAngle() {
        return ((SettingSlider) children().get(2)).value();
    }

    private final class SettingSlider extends AbstractSliderButton {
        private final String key;
        private final int min;
        private final int max;

        private SettingSlider(int x, int y, String key, int min, int max, int initialValue) {
            super(x, y, 196, 20, Component.empty(), (double) (initialValue - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        private int value() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key, value()));
        }

        @Override
        protected void applyValue() {
            send();
        }
    }
}
