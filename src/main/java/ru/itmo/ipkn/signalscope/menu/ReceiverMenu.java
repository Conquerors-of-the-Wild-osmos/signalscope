package ru.itmo.ipkn.signalscope.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class ReceiverMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int azimuth;
    private final int elevation;
    private final int coneAngle;

    public ReceiverMenu(int containerId, BlockPos pos) {
        this(containerId, pos, 0, 0, 90);
    }

    public ReceiverMenu(int containerId, ReceiverScreenData data) {
        this(containerId, data.pos(), data.azimuth(), data.elevation(), data.coneAngle());
    }

    private ReceiverMenu(int containerId, BlockPos pos, int azimuth, int elevation, int coneAngle) {
        super(ReceiverMenus.RECEIVER, containerId);
        this.pos = pos;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.coneAngle = coneAngle;
    }

    public BlockPos pos() {
        return pos;
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

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof ru.itmo.ipkn.signalscope.block.ReceiverBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
