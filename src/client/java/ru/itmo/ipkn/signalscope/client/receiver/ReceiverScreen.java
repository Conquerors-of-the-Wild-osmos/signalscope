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
        imageWidth = 248;
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
            super(x, y, 216, 20, Component.empty(), (double) (initialValue - min) / (max - min));
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
