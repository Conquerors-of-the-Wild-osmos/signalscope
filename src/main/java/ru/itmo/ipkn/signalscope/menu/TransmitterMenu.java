package ru.itmo.ipkn.signalscope.menu;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import ru.itmo.ipkn.signalscope.Signalscope;
import ru.itmo.ipkn.signalscope.block.TransmitterBlock;

public final class TransmitterMenu extends AbstractContainerMenu {
    public static final ExtendedScreenHandlerType<TransmitterMenu, Data> TYPE = Registry.register(
            BuiltInRegistries.MENU, Signalscope.id("transmitter"),
            new ExtendedScreenHandlerType<>((id, inventory, data) -> new TransmitterMenu(id, data), Data.CODEC));
    private final Data data;

    public TransmitterMenu(int id, Data data) {
        super(TYPE, id);
        this.data = data;
    }

    public Data data() { return data; }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        BlockPos pos = data.pos();
        return player.level().getBlockEntity(pos) instanceof TransmitterBlock.Entity
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(Rename.TYPE, Rename.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Rename.TYPE, (payload, context) -> {
            var player = context.player();
            if (player.containerMenu instanceof TransmitterMenu menu && menu.stillValid(player)
                    && player.level().getBlockEntity(menu.data.pos()) instanceof TransmitterBlock.Entity entity) {
                entity.setName(payload.name());
            }
        });
    }

    public record Data(BlockPos pos, String name) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.ofMember(
                (data, buf) -> { buf.writeBlockPos(data.pos()); buf.writeUtf(data.name(), 16); },
                buf -> new Data(buf.readBlockPos(), buf.readUtf(16)));
    }

    public record Rename(String name) implements CustomPacketPayload {
        public static final Type<Rename> TYPE = new Type<>(Signalscope.id("transmitter_name"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rename> CODEC = StreamCodec.ofMember(
                (payload, buf) -> buf.writeUtf(payload.name(), 16), buf -> new Rename(buf.readUtf(16)));
        @Override public Type<Rename> type() { return TYPE; }
    }
}
