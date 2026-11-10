package ru.itmo.ipkn.signalscope.client;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.itmo.ipkn.signalscope.Signalscope;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SignalscopeConfig {

    private static final ConfigClassHandler<SignalscopeConfig> HANDLER = ConfigClassHandler
            .createBuilder(SignalscopeConfig.class)
            .id(Signalscope.id("client"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("signalscope.json"))
                    .build())
            .build();

    @SerialEntry
    public int stableAngle = 5;
    @SerialEntry
    public int receptionAngle = 90;
    @SerialEntry
    public int voiceVolume = 100;
    @SerialEntry
    public int maxNicknames = 5;
    @SerialEntry
    public int hudScale = 100;
    @SerialEntry
    public boolean toggleRadio = false;

    public static SignalscopeConfig get() {
        return HANDLER.instance();
    }

    public static void load() {
        HANDLER.load();
        validate();
    }

    private static void validate() {
        var config = get();
        config.stableAngle = Math.clamp(config.stableAngle, 0, 179);
        config.receptionAngle = Math.clamp(config.receptionAngle, config.stableAngle + 1, 180);
        config.voiceVolume = Math.clamp(config.voiceVolume, 0, 100);
        config.maxNicknames = Math.clamp(config.maxNicknames, 1, 100);
        config.hudScale = Math.clamp(config.hudScale, 50, 200);
    }

    private static Option<Integer> slider(String key, int defaultValue, Supplier<Integer> getter,
                                          Consumer<Integer> setter, int min, int max) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable("config.signalscope." + key))
                .description(OptionDescription.of(Component.translatable("config.signalscope." + key + ".description")))
                .binding(defaultValue, getter, setter)
                .controller(option -> IntegerSliderControllerBuilder.create(option).range(min, max).step(1))
                .build();
    }

    public static Screen screen(Screen parent) {
        var config = get();
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.signalscope.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.signalscope.category"))
                        .option(slider("stable_angle", 5, () -> config.stableAngle, value -> config.stableAngle = value, 0, 179))
                        .option(slider("reception_angle", 90, () -> config.receptionAngle, value -> config.receptionAngle = value, 1, 180))
                        .option(slider("voice_volume", 100, () -> config.voiceVolume, value -> config.voiceVolume = value, 0, 100))
                        .option(slider("max_nicknames", 5, () -> config.maxNicknames, value -> config.maxNicknames = value, 1, 10))
                        .option(slider("hud_scale", 100, () -> config.hudScale, value -> config.hudScale = value, 50, 200))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.signalscope.radio_mode"))
                                .description(OptionDescription.of(Component.translatable("config.signalscope.radio_mode.description")))
                                .binding(false, () -> config.toggleRadio, value -> config.toggleRadio = value)
                                .controller(option -> BooleanControllerBuilder.create(option)
                                        .formatValue(value -> Component.translatable(value
                                                ? "config.signalscope.radio_mode.toggle" : "config.signalscope.radio_mode.hold")))
                                .build())
                        .build())
                .save(() -> {
                    validate();
                    HANDLER.save();
                })
                .build().generateScreen(parent);
    }
}
