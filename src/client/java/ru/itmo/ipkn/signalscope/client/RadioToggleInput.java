package ru.itmo.ipkn.signalscope.client;

import net.minecraft.world.InteractionHand;

/**
 * Distinguishes item use from clicks consumed by blocks or entities.
 */
final class RadioToggleInput {

    private InteractionHand pending;
    private boolean wasDown;

    void used(InteractionHand hand) {
        pending = hand;
    }

    InteractionHand tick(boolean down) {
        InteractionHand result = down && !wasDown ? pending : null;
        pending = null;
        wasDown = down;
        return result;
    }

    void reset() {
        pending = null;
        wasDown = false;
    }
}
