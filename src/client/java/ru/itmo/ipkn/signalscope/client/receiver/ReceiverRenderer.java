package ru.itmo.ipkn.signalscope.client.receiver;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;
import ru.itmo.ipkn.signalscope.block.ReceiverBlockEntity;

/** The top is a compass, with north at the negative Z edge. */
public final class ReceiverRenderer implements BlockEntityRenderer<ReceiverBlockEntity> {
    private static final float PIXEL = 1f / 16;

    public ReceiverRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(ReceiverBlockEntity receiver, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = poses.last().pose();
        double yaw = Math.toRadians(receiver.azimuth());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double threshold = Math.pow(Math.cos(Math.toRadians(receiver.coneAngle() / 2.0)), 2);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int color = compassPixel(x, z, sin, cos, threshold);
                if (color != 0) topRect(out, matrix, x * PIXEL, z * PIXEL,
                        (x + 1) * PIXEL, (z + 1) * PIXEL, 1.002f, color);
            }
        }
        topRect(out, matrix, 7 * PIXEL, PIXEL, 9 * PIXEL, 2 * PIXEL, 1.005f, 0xFFFFFFFF);

        // Each face uses a distinct plane for the marker, avoiding z-fighting with the scale.
        float markerY = markerY(receiver.elevation());
        for (int side = 0; side < 4; side++) {
            poses.pushPose();
            poses.translate(.5, 0, .5);
            poses.mulPose(Axis.YP.rotationDegrees(side * 90));
            poses.translate(-.5, 0, -.5);
            matrix = poses.last().pose();
            sideRect(out, matrix, 1.002f, 2 * PIXEL, 2 * PIXEL, 3 * PIXEL, 14 * PIXEL, 0xFF183F3D);
            sideRect(out, matrix, 1.005f, PIXEL, markerY, 4 * PIXEL, markerY + PIXEL, 0xFF52D9AF);
            poses.popPose();
        }
    }

    static float markerY(int elevation) {
        return 2 * PIXEL + 11 * PIXEL * (elevation + 90) / 180f;
    }

    /** Antialias circle and sector edges inside each Minecraft-sized pixel. */
    static int compassPixel(int x, int z, double sin, double cos, double threshold) {
        int inside = 0, sector = 0;
        for (int dz = 0; dz < 4; dz++) {
            for (int dx = 0; dx < 4; dx++) {
                double px = x + (dx + .5) / 4 - 8;
                double pz = z + (dz + .5) / 4 - 8;
                double distance2 = px * px + pz * pz;
                if (distance2 > 6.4 * 6.4) continue;
                inside++;
                double dot = px * sin - pz * cos;
                if (dot >= 0 && dot * dot >= distance2 * threshold) sector++;
            }
        }
        if (inside == 0) return 0;
        int red = (24 * (inside - sector) + 82 * sector) / inside;
        int green = (63 * (inside - sector) + 217 * sector) / inside;
        int blue = (61 * (inside - sector) + 175 * sector) / inside;
        return (inside * 255 / 16 << 24) | (red << 16) | (green << 8) | blue;
    }

    private static void topRect(VertexConsumer out, Matrix4f m, float x1, float z1, float x2, float z2, float y, int color) {
        vertex(out, m, x1, y, z1, color); vertex(out, m, x1, y, z2, color);
        vertex(out, m, x2, y, z2, color); vertex(out, m, x2, y, z1, color);
    }

    private static void sideRect(VertexConsumer out, Matrix4f m, float x, float z1, float y1, float z2, float y2, int color) {
        vertex(out, m, x, y1, z1, color); vertex(out, m, x, y2, z1, color);
        vertex(out, m, x, y2, z2, color); vertex(out, m, x, y1, z2, color);
    }

    private static void vertex(VertexConsumer out, Matrix4f m, float x, float y, float z, int color) {
        out.addVertex(m, x, y, z).setColor(color);
    }
}
