package ru.itmo.ipkn.signalscope.client;

import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RadioToggleInputTest {

    @Test
    void onlyItemUseOnNewPressToggles() {
        var input = new RadioToggleInput();
        input.used(InteractionHand.MAIN_HAND);
        assertEquals(InteractionHand.MAIN_HAND, input.tick(true));
        input.used(InteractionHand.MAIN_HAND);
        assertNull(input.tick(true));
        assertNull(input.tick(false));
        // An interactive block consumes the click; no item-use event occurs.
        assertNull(input.tick(true));
        assertNull(input.tick(false));
        input.used(InteractionHand.OFF_HAND);
        assertEquals(InteractionHand.OFF_HAND, input.tick(true));
    }
}
