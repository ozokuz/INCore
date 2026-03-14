package io.github.ozokuz.incore.client.features.roguelike;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlock;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikePortalBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class RoguelikePortalRenderer implements BlockEntityRenderer<RoguelikePortalBlockEntity> {
    private static final ResourceLocation PORTAL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float PORTAL_MIN = 0.0F;
    private static final float PORTAL_MAX = 1.0F;
    private static final float PORTAL_FACE_MIN = 6.0F / 16.0F;
    private static final float PORTAL_FACE_MAX = 10.0F / 16.0F;
    private static final float PORTAL_EPSILON = 0.001F;
    private static final float HUE_SPEED = 1.0F / 800.0F;

    public RoguelikePortalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RoguelikePortalBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(RoguelikePortalBlock.AXIS)) {
            return;
        }

        float time = blockEntity.getLevel().getGameTime() + partialTick;
        float hue = (time * HUE_SPEED) % 1.0F;
        int rgb = Mth.hsvToRgb(hue, 0.95F, 1.0F);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int alpha = 170;

        poseStack.pushPose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(PORTAL_TEXTURE));
        var pose = poseStack.last();

        if (state.getValue(RoguelikePortalBlock.AXIS) == net.minecraft.core.Direction.Axis.X) {
            // Overlay both visible faces of the NS portal slab.
            float northFace = PORTAL_FACE_MIN - PORTAL_EPSILON;
            float southFace = PORTAL_FACE_MAX + PORTAL_EPSILON;
            addQuad(consumer, poseStack, pose, PORTAL_MIN, 0.0F, northFace, PORTAL_MAX, 1.0F, northFace, red, green, blue, alpha, 0.0F, 0.0F, -1.0F);
            addQuad(consumer, poseStack, pose, PORTAL_MAX, 0.0F, southFace, PORTAL_MIN, 1.0F, southFace, red, green, blue, alpha, 0.0F, 0.0F, 1.0F);
        } else {
            // Overlay both visible faces of the EW portal slab.
            float westFace = PORTAL_FACE_MIN - PORTAL_EPSILON;
            float eastFace = PORTAL_FACE_MAX + PORTAL_EPSILON;
            addQuad(consumer, poseStack, pose, westFace, 0.0F, PORTAL_MIN, westFace, 1.0F, PORTAL_MAX, red, green, blue, alpha, -1.0F, 0.0F, 0.0F);
            addQuad(consumer, poseStack, pose, eastFace, 0.0F, PORTAL_MAX, eastFace, 1.0F, PORTAL_MIN, red, green, blue, alpha, 1.0F, 0.0F, 0.0F);
        }

        poseStack.popPose();
    }

    private static void addQuad(VertexConsumer consumer, PoseStack poseStack, PoseStack.Pose pose,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                int red, int green, int blue, int alpha,
                                float nx, float ny, float nz) {
        addVertex(consumer, poseStack, pose, x1, y1, z1, red, green, blue, alpha, 0.0F, 1.0F, nx, ny, nz);
        addVertex(consumer, poseStack, pose, x1, y2, z1, red, green, blue, alpha, 0.0F, 0.0F, nx, ny, nz);
        addVertex(consumer, poseStack, pose, x2, y2, z2, red, green, blue, alpha, 1.0F, 0.0F, nx, ny, nz);
        addVertex(consumer, poseStack, pose, x2, y1, z2, red, green, blue, alpha, 1.0F, 1.0F, nx, ny, nz);
    }

    private static void addVertex(VertexConsumer consumer, PoseStack poseStack, PoseStack.Pose pose,
                                  float x, float y, float z,
                                  int red, int green, int blue, int alpha,
                                  float u, float v,
                                  float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), nx, ny, nz);
    }
}
