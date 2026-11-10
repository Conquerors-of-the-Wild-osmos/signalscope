package ru.itmo.ipkn.signalscope.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.OpenALSoundEvent;
import org.lwjgl.openal.AL10;
import ru.itmo.ipkn.signalscope.Signalscope;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RadioVoicechatPlugin implements VoicechatPlugin {
    private static final String CATEGORY = "signalscope";

    @Override
    public String getPluginId() {
        return Signalscope.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> RadioState.clearServer());
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> event.getVoicechat().registerVolumeCategory(
                event.getVoicechat().volumeCategoryBuilder().setId(CATEGORY).setName("Signalscope").build()));
        registration.registerEvent(OpenALSoundEvent.Post.class, event -> {
            if (CATEGORY.equals(event.getCategory())) {
                float gain = RadioState.clientGain(event.getChannelId());
                AL10.alSourcef(event.getSource(), AL10.AL_GAIN, AL10.alGetSourcef(event.getSource(), AL10.AL_GAIN) * gain);
            }
        });
    }

    void onClientSound(ClientSoundEvent event) {
        if (!RadioState.isClientTransmitting()) {
            event.cancel();
        }
    }

    void onMicrophonePacket(MicrophonePacketEvent event) {
        boolean previouslyCancelled = event.isCancelled();
        // Also suppress proximity/group chat for players not using a radio.
        event.cancel();
        if (previouslyCancelled) {
            return;
        }
        var sender = event.getSenderConnection();
        if (sender == null || !sender.isConnected()) {
            return;
        }
        UUID senderId = sender.getPlayer().getUuid();
        var players = RadioState.serverPlayers();
        var source = players.get(senderId);
        if (source == null) {
            return;
        }
        // An empty frame closes the stream after release; it contains no microphone audio.
        if (!source.transmitting() && event.getPacket().getOpusEncodedData().length != 0) {
            return;
        }
        var packet = event.getPacket().staticSoundPacketBuilder()
                .channelId(channelId(senderId))
                .category(CATEGORY)
                .build();
        var api = event.getVoicechat();
        for (var entry : players.entrySet()) {
            if (entry.getKey().equals(senderId) || !entry.getValue().dimension().equals(source.dimension())) {
                continue;
            }
            var receiver = api.getConnectionOf(entry.getKey());
            if (receiver != null && receiver.isConnected() && !receiver.isDisabled()) {
                api.sendStaticSoundPacketTo(receiver, packet);
            }
        }
    }

    public static UUID channelId(UUID sender) {
        return UUID.nameUUIDFromBytes(("signalscope:radio:" + sender).getBytes(StandardCharsets.UTF_8));
    }
}
