package io.github.ozokuz.incore.client.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import io.github.ozokuz.incore.features.research.client.ResearchRecipeLockClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@EmiEntrypoint
public class INCoreEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeDecorator((recipe, widgets) -> {
            ResourceLocation id = recipe.getId();
            if (id == null || !ResearchRecipeLockClientCache.isLocked(id)) {
                return;
            }

            ResearchRecipeLockClientCache.LockDisplay lockDisplay = ResearchRecipeLockClientCache.lockDisplay(id);
            String researchTitle = lockDisplay.primaryResearchTitle();
            if (researchTitle == null || researchTitle.isBlank()) {
                researchTitle = Component.translatable("emi.incore.locked.unknown").getString();
            }

            if (lockDisplay.hasDuplicateCandidates() && ResearchRecipeLockClientCache.consumeDuplicateChatWarning()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable(
                                    "incore.research.lock_mapping_duplicate",
                                    id.toString(),
                                    lockDisplay.candidateCount(),
                                    researchTitle
                            ),
                            false
                    );
                }
            }

            int iconX = Math.max(4, widgets.getWidth() / 2 + 10);
            int iconY = Math.max(3, widgets.getHeight() / 2 - 8);
            // Layered lock draw gives the glyph a thicker, more noticeable appearance.
            widgets.addText(Component.literal("🔒").getVisualOrderText(), iconX + 1, iconY + 1, 0xFF4A0E0E, false);
            widgets.addText(Component.literal("🔒").getVisualOrderText(), iconX, iconY, 0xFFFF4F4F, true);
            widgets.addTooltipText(
                    List.of(
                            Component.translatable("emi.incore.locked.tooltip.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            Component.translatable("emi.incore.locked.tooltip.desc").withStyle(ChatFormatting.GRAY),
                            Component.translatable("emi.incore.locked.tooltip.research", researchTitle).withStyle(ChatFormatting.GOLD)
                    ),
                    iconX - 1,
                    iconY - 1,
                    14,
                    14
            );
        });
    }
}
