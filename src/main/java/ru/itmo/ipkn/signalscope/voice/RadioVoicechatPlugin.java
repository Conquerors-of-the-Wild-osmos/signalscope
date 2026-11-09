package ru.itmo.ipkn.signalscope.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RadioVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "signalscope";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> RadioState.clearServer());
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

    static UUID channelId(UUID sender) {
        return UUID.nameUUIDFromBytes(("signalscope:radio:" + sender).getBytes(StandardCharsets.UTF_8));
    }
}
