package ozokuz.incore.integration.ldlib.ui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.TransformTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class BottomBorderTexture extends TransformTexture {
    private final int fillColor;
    private final int bottomBorderColor;
    private final int borderThickness;

    public BottomBorderTexture(int fillColor, int bottomBorderColor, int borderThickness) {
        this.fillColor = fillColor;
        this.bottomBorderColor = bottomBorderColor;
        this.borderThickness = Math.max(1, borderThickness);
    }

    @Override
    public BottomBorderTexture copy() {
        BottomBorderTexture copied = new BottomBorderTexture(fillColor, bottomBorderColor, borderThickness);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        int left = Mth.floor(x);
        int top = Mth.floor(y);
        int right = Mth.ceil(x + width);
        int bottom = Mth.ceil(y + height);
        if (right <= left || bottom <= top) {
            return;
        }

        graphics.fill(left, top, right, bottom, fillColor);
        int borderTop = Math.max(top, bottom - borderThickness);
        graphics.fill(left, borderTop, right, bottom, bottomBorderColor);
    }
}
