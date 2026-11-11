package ru.itmo.ipkn.signalscope.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.Signalscope;

import java.util.HashMap;
import java.util.Map;

public final class SignalBlockerRenderer {

    private static final int MAX_POWER = 15;
    private static SignalBlockerRenderer instance;
    private final Map<BlockPos, Integer> blockers = new HashMap<>();

    void initialize() {
        instance = this;
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> scan(chunk));
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
                blockers.keySet().removeIf(pos -> pos.getX() >> 4 == chunk.getPos().x
                        && pos.getZ() >> 4 == chunk.getPos().z));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> render(context.matrixStack(), context.consumers(),
                context.camera().getPosition()));
    }

    void clear() {
        blockers.clear();
    }

    private void scan(LevelChunk chunk) {
        int minY = chunk.getMinBuildHeight();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        var sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            if (!section.maybeHas(SignalBlockerRenderer::isSignalBlocker)) {
                continue;
            }
            int minSectionY = minY + sectionIndex * 16;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        BlockPos pos = new BlockPos(minX + x, minSectionY + y, minZ + z);
                        update(pos, section.getBlockState(x, y, z));
                    }
                }
            }
        }
    }

    public static void onBlockStateChanged(BlockPos pos, BlockState state) {
        if (instance != null) {
            instance.update(pos, state);
        }
    }

    private static boolean isSignalBlocker(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(Signalscope.id("signal_blocker"));
    }

    private void update(BlockPos pos, BlockState state) {
        if (!isSignalBlocker(state)) {
            blockers.remove(pos);
            return;
        }
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("power") && state.getValue(property) instanceof Integer power) {
                if (power > 0 && power <= MAX_POWER) {
                    blockers.put(pos.immutable(), power);
                } else {
                    blockers.remove(pos);
                }
                return;
            }
        }
        blockers.remove(pos);
    }

    private void render(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource consumers, Vec3 camera) {
        if (!Minecraft.getInstance().getDebugOverlay().showDebugScreen()) return;
        VertexConsumer vertices = consumers.getBuffer(RenderType.lines());
        blockers.forEach((pos, power) -> {
            int radius = power - 1;
            AABB box = new AABB(pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                    pos.getX() + power, pos.getY() + power, pos.getZ() + power)
                    .move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(matrices, vertices, box, 0.85F, 0.2F, 0.2F, 0.45F);
        });
    }
}
