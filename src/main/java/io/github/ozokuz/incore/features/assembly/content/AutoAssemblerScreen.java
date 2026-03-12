package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.features.assembly.client.AssemblyClientCache;
import io.github.ozokuz.incore.features.assembly.network.AssemblyNetworking;
import io.github.ozokuz.incore.features.assembly.recipe.AssemblyRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AutoAssemblerScreen extends AbstractContainerScreen<AutoAssemblerMenu> {
    private final List<Button> rowButtons = new ArrayList<>();
    private @Nullable EditBox searchBox;
    private @Nullable ResourceLocation selectedRecipeId;
    private int scroll;

    public AutoAssemblerScreen(AutoAssemblerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();
        AssemblyNetworking.requestSnapshot();
        searchBox = addRenderableWidget(new EditBox(font, leftPos + 136, topPos + 18, 84, 12, Component.translatable("screen.incore.auto_assembler.search")));
        searchBox.setBordered(false);
        searchBox.setResponder(value -> {
            scroll = 0;
            refreshRows();
        });
        addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.auto_assembler.apply"),
                button -> {
                    if (selectedRecipeId != null) {
                        AssemblyNetworking.selectRecipe(menu.assembler().getBlockPos().asLong(), selectedRecipeId.toString());
                    }
                }
        ).bounds(leftPos + 136, topPos + 90, 84, 20).build());
        for (int row = 0; row < 6; row++) {
            final int rowIndex = row;
            rowButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> selectRow(rowIndex)).bounds(leftPos + 136, topPos + 34 + row * 12, 84, 11).build()));
        }
        refreshRows();
    }

    public void updateFromCache() {
        refreshRows();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshRows();
    }

    private List<RecipeHolder<AssemblyRecipe>> visibleRecipes() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<RecipeHolder<AssemblyRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<AssemblyRecipe> holder : AssemblyRecipeUtil.allRecipes(minecraft.level.getRecipeManager())) {
            AssemblyRecipe recipe = holder.value();
            if (!AssemblyClientCache.isUnlocked(holder.id().toString()) || recipe.tier() > menu.tier()) {
                continue;
            }
            if (!query.isBlank() && !holder.id().toString().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            recipes.add(holder);
        }
        recipes.sort(Comparator.comparing(holder -> holder.id().toString()));
        return recipes;
    }

    private void refreshRows() {
        List<RecipeHolder<AssemblyRecipe>> recipes = visibleRecipes();
        int maxScroll = Math.max(0, recipes.size() - rowButtons.size());
        scroll = Math.clamp(scroll, 0, maxScroll);
        for (int row = 0; row < rowButtons.size(); row++) {
            int index = scroll + row;
            Button button = rowButtons.get(row);
            button.visible = index < recipes.size();
            button.active = index < recipes.size();
            if (index < recipes.size()) {
                button.setMessage(Component.literal(trim(recipes.get(index).id().toString(), 14)));
            }
        }
    }

    private void selectRow(int rowIndex) {
        List<RecipeHolder<AssemblyRecipe>> recipes = visibleRecipes();
        int index = scroll + rowIndex;
        if (index >= 0 && index < recipes.size()) {
            selectedRecipeId = recipes.get(index).id();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171717);
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF232323);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(guiGraphics, leftPos + 17 + col * 18, topPos + 29 + row * 18);
            }
        }
        for (int col = 0; col < 4; col++) {
            drawSlot(guiGraphics, leftPos + 103 + col * 18, topPos + 47);
        }
        guiGraphics.fill(leftPos + 135, topPos + 16, leftPos + 221, topPos + 110, 0xFF111111);
        int fill = Math.clamp((menu.progress() * 74) / menu.maxProgress(), 0, 74);
        guiGraphics.fill(leftPos + 136, topPos + 118, leftPos + 212, topPos + 124, 0xFF101010);
        guiGraphics.fill(leftPos + 137, topPos + 119, leftPos + 137 + fill, topPos + 123, 0xFF76B2F0);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0xFFF4F4F4, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.auto_assembler.tier", menu.tier()), 136, 6, 0xFFD0D0D0, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.auto_assembler.progress", menu.progress(), menu.maxProgress()), 136, 126, 0xFFD0D0D0, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, 96, 0xFFD0D0D0, false);
        if (selectedRecipeId != null) {
            guiGraphics.drawString(font, trim(selectedRecipeId.toString(), 18), 136, 136, 0xFFE8C37A, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF464646);
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF141414);
    }
}
