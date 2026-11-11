package ru.itmo.ipkn.signalscope.client.receiver;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ru.itmo.ipkn.signalscope.menu.TransmitterMenu;

public final class TransmitterScreen extends AbstractContainerScreen<TransmitterMenu> {
    private EditBox input;
    public TransmitterScreen(TransmitterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 82;
        inventoryLabelY = 1000;
    }

    @Override protected void init() {
        super.init();
        input = new EditBox(font, leftPos + 12, topPos + 37, 196, 20,
                Component.translatable("gui.signalscope.transmitter.name"));
        input.setMaxLength(16);
        input.setValue(menu.data().name());
        input.setHint(Component.translatable("hud.signalscope.transmitter"));
        input.setResponder(value -> {
            if (ClientPlayNetworking.canSend(TransmitterMenu.Rename.TYPE)) {
                ClientPlayNetworking.send(new TransmitterMenu.Rename(value));
            }
        });
        addRenderableWidget(input);
        setInitialFocus(input);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input.isFocused() && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            input.keyPressed(keyCode, scanCode, modifiers);
            return true; // The subsequent charTyped event still inserts the character.
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!input.isMouseOver(mouseX, mouseY)) setFocused(null);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2D2D2D);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF171717);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFFFF, false);
    }
}
