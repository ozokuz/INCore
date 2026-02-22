package io.github.ozokuz.incore.integration.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import io.github.ozokuz.incore.features.market.client.MarketAutoBuyerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
public class INCoreEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(MarketAutoBuyerScreen.class, new EmiDragDropHandler<>() {
            @Override
            public boolean dropStack(MarketAutoBuyerScreen screen, EmiIngredient stack, int x, int y) {
                Bounds bounds = ghostTargetBounds(screen);
                if (!bounds.contains(x, y)) {
                    return false;
                }

                for (EmiStack emiStack : stack.getEmiStacks()) {
                    ItemStack itemStack = emiStack.getItemStack();
                    if (itemStack.isEmpty()) {
                        continue;
                    }
                    return screen.applyGhostTargetFromItemStack(itemStack);
                }
                return false;
            }

            @Override
            public void render(MarketAutoBuyerScreen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
                Bounds bounds = ghostTargetBounds(screen);
                int color = bounds.contains(mouseX, mouseY) ? 0x8844DD44 : 0x5522AA22;
                draw.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), color);
            }
        });
    }

    private static Bounds ghostTargetBounds(MarketAutoBuyerScreen screen) {
        return new Bounds(screen.ghostSlotLeft(), screen.ghostSlotTop(), 18, 18);
    }
}
