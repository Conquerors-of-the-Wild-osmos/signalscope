package ru.itmo.ipkn.signalscope.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.itmo.ipkn.signalscope.client.SignalBlockerRenderer;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    private void signalscope$updateSignalBlocker(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        SignalBlockerRenderer.onBlockStateChanged(pos, state);
    }
}
