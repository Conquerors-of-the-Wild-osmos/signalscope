package ru.itmo.ipkn.signalscope.block;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.menu.ReceiverMenu;
import ru.itmo.ipkn.signalscope.menu.ReceiverScreenData;
import ru.itmo.ipkn.signalscope.signal.SignalGeometry;

public final class ReceiverBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<ReceiverScreenData>, MenuProvider {

    public static final int DEFAULT_AZIMUTH = 0;
    public static final int DEFAULT_ELEVATION = 0;
    public static final int DEFAULT_CONE_ANGLE = 90;

    private int azimuth = DEFAULT_AZIMUTH;
    private int elevation = DEFAULT_ELEVATION;
    private int coneAngle = DEFAULT_CONE_ANGLE;
    private boolean detected;

    public ReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ReceiverBlocks.RECEIVER_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReceiverBlockEntity receiver) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean previous = receiver.detected;
        receiver.detected = receiver.findSource(serverLevel);
        if (previous != receiver.detected) {
            receiver.setChanged();
            serverLevel.updateNeighborsAt(pos, state.getBlock());
        }
    }

    public boolean detected() {
        return detected;
    }

    public int azimuth() {
        return azimuth;
    }

    public int elevation() {
        return elevation;
    }

    public int coneAngle() {
        return coneAngle;
    }

    public void setSettings(int azimuth, int elevation, int coneAngle) {
        this.azimuth = Math.floorMod(azimuth, 360);
        this.elevation = Math.clamp(elevation, -90, 90);
        this.coneAngle = Math.clamp(coneAngle, 1, 180);
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // ponytail: each receiver scans all sources every tick; index sources if installations become large.
    private boolean findSource(ServerLevel level) {
        Vec3 origin = Vec3.atCenterOf(worldPosition);
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && SignalscopeItems.hasActiveRadio(player)
                    && inCone(origin, player.getBoundingBox().getCenter())) {
                return true;
            }
        }
        for (BlockPos transmitter : TransmitterBlock.activePositions(level)) {
            if (inCone(origin, Vec3.atCenterOf(transmitter))) {
                return true;
            }
        }
        return false;
    }

    private boolean inCone(Vec3 origin, Vec3 source) {
        return SignalGeometry.inCone(azimuth, elevation, coneAngle,
                source.x - origin.x, source.y - origin.y, source.z - origin.z);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.signalscope.receiver");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new ReceiverMenu(containerId, worldPosition);
    }

    @Override
    public ReceiverScreenData getScreenOpeningData(ServerPlayer player) {
        return new ReceiverScreenData(worldPosition, azimuth, elevation, coneAngle);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("azimuth", azimuth);
        tag.putInt("elevation", elevation);
        tag.putInt("cone_angle", coneAngle);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        azimuth = Math.floorMod(tag.getInt("azimuth"), 360);
        elevation = Math.clamp(tag.getInt("elevation"), -90, 90);
        coneAngle = Math.clamp(tag.getInt("cone_angle"), 1, 180);
    }
}
