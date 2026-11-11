package ru.itmo.ipkn.signalscope.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class SignalBlockerBlock extends Block {

    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    public static final SignalBlockerBlock INSTANCE = new SignalBlockerBlock(
            BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()
    );

    private SignalBlockerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        refreshPower(level, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        refreshPower(level, pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            SignalBlockerState.get(serverLevel.getServer()).remove(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void refreshPower(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int power = serverLevel.getBestNeighborSignal(pos);
        if (state.getValue(POWER) != power) {
            serverLevel.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_CLIENTS);
        }
        SignalBlockerState.get(serverLevel.getServer()).setPower(serverLevel, pos, power);
    }
}
