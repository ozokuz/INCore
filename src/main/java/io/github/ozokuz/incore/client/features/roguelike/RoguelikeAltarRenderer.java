package io.github.ozokuz.incore.client.features.roguelike;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.content.RoguelikeAltarBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RoguelikeAltarRenderer implements BlockEntityRenderer<RoguelikeAltarBlockEntity> {
    private final ItemRenderer itemRenderer;
    private final Font font;
    private final EntityRenderDispatcher entityRenderDispatcher;

    public RoguelikeAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
        this.entityRenderDispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(RoguelikeAltarBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var entries = blockEntity.displayEntries();
        int fullBright = LightTexture.FULL_BRIGHT;
        float time = (blockEntity.getLevel() == null ? 0.0F : blockEntity.getLevel().getGameTime()) + partialTick;

        if (blockEntity.crystalPlaced()) {
            float crystalY = 1.35F + Mth.sin(time * 0.05F) * 0.06F;
            poseStack.pushPose();
            poseStack.translate(0.5F, crystalY, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees((time * 3.0F) % 360.0F));
            poseStack.scale(0.55F, 0.55F, 0.55F);
            ItemStack crystalStack = new ItemStack(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get());
            itemRenderer.renderStatic(crystalStack, ItemDisplayContext.GROUND, fullBright, packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), 0);
            poseStack.popPose();

            if (entries.isEmpty()) {
                return;
            }

            int count = entries.size();
            float radius = 0.78F + Math.max(0.0F, (count - 3) * 0.06F);
            float orbitY = 0.85F;

            for (int i = 0; i < count; i++) {
                var entry = entries.get(i);
                var item = BuiltInRegistries.ITEM.get(entry.itemId());
                if (item == Items.AIR) {
                    continue;
                }

                float baseAngle = (float) (Math.PI * 2.0D * i / count);
                float orbit = time * 0.055F;
                float angle = baseAngle + orbit;
                float x = 0.5F + Mth.cos(angle) * radius;
                float z = 0.5F + Mth.sin(angle) * radius;
                float y = orbitY + Mth.sin(time * 0.09F + i * 0.75F) * 0.08F;

                poseStack.pushPose();
                poseStack.translate(x, y, z);
                poseStack.mulPose(Axis.YP.rotationDegrees((time * 4.5F + i * 45.0F) % 360.0F));
                poseStack.scale(0.42F, 0.42F, 0.42F);
                itemRenderer.renderStatic(new ItemStack(item), ItemDisplayContext.GROUND, fullBright, packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), i);
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(x, y - 0.22F, z);
                poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
                poseStack.scale(0.0125F, -0.0125F, 0.0125F);

                String text = entry.submittedAmount() + "/" + entry.requiredAmount();
                int color = entry.isComplete() ? 0xFF55FF55 : 0xFFFFFFFF;
                float textX = -font.width(text) / 2.0F;
                font.drawInBatch(text, textX, 0.0F, color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, fullBright);
                poseStack.popPose();
            }
        }
    }
}
