package ozokuz.incore.integration.ldlib.ui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.TransformTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class BeveledRectTexture extends TransformTexture {
    private final int fillColor;
    private final int outerBorderColor;
    private final int highlightColor;
    private final int shadowColor;
    private final int outerBorderThickness;
    private final int bevelThickness;

    public BeveledRectTexture(
            int fillColor,
            int outerBorderColor,
            int highlightColor,
            int shadowColor,
            int outerBorderThickness,
            int bevelThickness
    ) {
        this.fillColor = fillColor;
        this.outerBorderColor = outerBorderColor;
        this.highlightColor = highlightColor;
        this.shadowColor = shadowColor;
        this.outerBorderThickness = Math.max(0, outerBorderThickness);
        this.bevelThickness = Math.max(0, bevelThickness);
    }

    @Override
    public BeveledRectTexture copy() {
        var copied = new BeveledRectTexture(
                fillColor,
                outerBorderColor,
                highlightColor,
                shadowColor,
                outerBorderThickness,
                bevelThickness
        );
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

        int innerLeft = left;
        int innerTop = top;
        int innerRight = right;
        int innerBottom = bottom;
        if (outerBorderThickness > 0) {
            graphics.fill(left, top, right, top + outerBorderThickness, outerBorderColor);
            graphics.fill(left, bottom - outerBorderThickness, right, bottom, outerBorderColor);
            graphics.fill(left, top, left + outerBorderThickness, bottom, outerBorderColor);
            graphics.fill(right - outerBorderThickness, top, right, bottom, outerBorderColor);
            innerLeft += outerBorderThickness;
            innerTop += outerBorderThickness;
            innerRight -= outerBorderThickness;
            innerBottom -= outerBorderThickness;
        }

        if (bevelThickness <= 0 || innerRight <= innerLeft || innerBottom <= innerTop) {
            return;
        }

        int bevel = Math.min(
                bevelThickness,
                Math.min((innerRight - innerLeft) / 2, (innerBottom - innerTop) / 2)
        );
        if (bevel <= 0) {
            return;
        }

        graphics.fill(innerLeft, innerTop, innerRight, innerTop + bevel, highlightColor);
        graphics.fill(innerLeft, innerTop + bevel, innerLeft + bevel, innerBottom, highlightColor);
        graphics.fill(innerLeft + bevel, innerBottom - bevel, innerRight, innerBottom, shadowColor);
        graphics.fill(innerRight - bevel, innerTop, innerRight, innerBottom - bevel, shadowColor);
    }
}
