package ru.itmo.ipkn.signalscope.client.mixin;

import de.maxhenkel.voicechat.voice.client.PTTMicrophoneProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.itmo.ipkn.signalscope.voice.RadioState;

@Mixin(value = PTTMicrophoneProcessor.class, remap = false)
public class PTTMicrophoneProcessorMixin {

    @Redirect(
            method = "processInternal([SZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lde/maxhenkel/voicechat/voice/client/PTTMicrophoneProcessor;isPttButtonDown()Z",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private boolean signalscope$includeRadioPtt(PTTMicrophoneProcessor processor) {
        return processor.isPttButtonDown() || RadioState.isClientTransmitting();
    }
}
