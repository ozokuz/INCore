package ozokuz.incore.integration.ldlib.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.integration.ldlib.ui.elements.ClippedTextureProgressBar;
import ozokuz.incore.integration.ldlib.ui.elements.INCoreInfoSurfaceElement;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

public final class INCoreLdLibUiScaffold {
    private static final IGuiTexture BUTTON_IDLE_TEXTURE = new BeveledRectTexture(
            0xFF293544,
            0xFF141B24,
            0xFF6B8096,
            0xFF111821,
            0,
            1
    );
    private static final IGuiTexture BUTTON_HOVER_TEXTURE = new BeveledRectTexture(
            0xFF35516B,
            0xFF18212B,
            0xFF9AD6FF,
            0xFF15202B,
            0,
            1
    );
    private static final IGuiTexture BUTTON_PRESSED_TEXTURE = new BeveledRectTexture(
            0xFF1E2A36,
            0xFF10161D,
            0xFF14202A,
            0xFF7EB8D7,
            0,
            1
    );
    private static final SpriteTexture ENTROPY_BAR_BACKGROUND_TEXTURE = SpriteTexture.of(
            ResourceLocation.fromNamespaceAndPath("incore", "textures/gui/sprites/hud/experience_bar_background_white.png")
    );
    private static final SpriteTexture ENTROPY_BAR_PROGRESS_TEXTURE = SpriteTexture.of(
            ResourceLocation.fromNamespaceAndPath("incore", "textures/gui/sprites/hud/experience_bar_progress_white.png")
    );

    private INCoreLdLibUiScaffold() {
    }

    public static WindowScaffold createWindowShell(int width, int height) {
        UIElement root = new UIElement();
        root.addClass("incore-overlay-root");
        root.getLayout().widthPercent(100);
        root.getLayout().heightPercent(100);
        root.getLayout().justifyContent(AlignContent.CENTER);
        root.getLayout().alignItems(AlignItems.CENTER);

        UIElement window = INCoreInfoSurfaceElement.window();
        window.addClass("incore-window");
        window.getLayout().width(width);
        window.getLayout().height(height);
        window.getLayout().gapAll(8);
        window.getLayout().paddingAll(10);

        UIElement header = row();
        header.addClass("incore-window-header");
        header.getLayout().alignItems(AlignItems.CENTER);
        header.getLayout().justifyContent(AlignContent.SPACE_BETWEEN);
        header.getLayout().gapAll(8);

        UIElement body = new UIElement();
        body.addClass("incore-body");
        body.getLayout().flex(1);
        body.getLayout().gapAll(8);

        window.addChildren(header, body);
        root.addChild(window);
        return new WindowScaffold(root, window, header, body);
    }

    public static WindowScaffold createWindow(Component title, int width, int height) {
        WindowScaffold scaffold = createWindowShell(width, height);
        Label titleLabel = titleLabel(title);
        titleLabel.getLayout().flex(1);
        scaffold.header().addChild(titleLabel);
        return scaffold;
    }

    public static SectionScaffold createSection(Component title) {
        UIElement root = INCoreInfoSurfaceElement.card();
        root.addClass("incore-section");
        root.getLayout().widthPercent(100);
        root.getLayout().gapAll(6);
        root.getLayout().paddingAll(8);

        Label titleLabel = sectionTitle(title);

        UIElement body = new UIElement();
        body.addClass("incore-section-body");
        body.getLayout().widthPercent(100);
        body.getLayout().gapAll(6);

        root.addChildren(titleLabel, body);
        return new SectionScaffold(root, body);
    }

    public static UIElement row() {
        UIElement row = new UIElement();
        row.getLayout().flexDirection(FlexDirection.ROW);
        row.getLayout().gapAll(4);
        return row;
    }

    public static UIElement column() {
        UIElement column = new UIElement();
        column.getLayout().flexDirection(FlexDirection.COLUMN);
        column.getLayout().gapAll(4);
        return column;
    }

    public static UIElement spacer() {
        UIElement spacer = new UIElement();
        spacer.getLayout().flex(1);
        return spacer;
    }

    public static Label titleLabel(Component text) {
        Label label = new Label();
        label.setText(text);
        label.addClass("incore-title");
        label.textStyle(style -> {
            style.adaptiveWidth(true);
            style.textWrap(TextWrap.HIDE);
        });
        return label;
    }

    public static Label sectionTitle(Component text) {
        Label label = new Label();
        label.setText(text);
        label.addClass("incore-section-title");
        label.getLayout().widthPercent(100);
        label.textStyle(style -> {
            style.adaptiveHeight(true);
            style.textWrap(TextWrap.HIDE);
        });
        return label;
    }

    public static Label wrappedLabel(Component text) {
        Label label = new Label();
        label.setText(text);
        label.addClass("incore-label");
        label.getLayout().widthPercent(100);
        label.textStyle(style -> {
            style.adaptiveHeight(true);
            style.textWrap(TextWrap.WRAP);
        });
        return label;
    }

    public static Button actionButton(Component text) {
        return actionButton(text, 20);
    }

    public static Button actionButton(Component text, float height) {
        return styleActionButton(new Button().setText(text), height);
    }

    public static <T extends Button> T styleActionButton(T button) {
        return styleActionButton(button, 20);
    }

    public static <T extends Button> T styleActionButton(T button, float height) {
        button.addClass("incore-button");
        button.getLayout().widthPercent(100);
        button.getLayout().height(height);
        button.getLayout().alignItems(AlignItems.CENTER);
        button.getLayout().justifyContent(AlignContent.CENTER);
        button.text.getLayout().flex(1);
        button.text.getLayout().heightPercent(100);
        button.textStyle(style -> {
            style.adaptiveWidth(false);
            style.textWrap(TextWrap.HIDE);
        });
        button.buttonStyle(style -> style
                .baseTexture(BUTTON_IDLE_TEXTURE)
                .hoverTexture(BUTTON_HOVER_TEXTURE)
                .pressedTexture(BUTTON_PRESSED_TEXTURE)
        );
        return button;
    }

    public static ProgressBar slimProgressBar() {
        ProgressBar progressBar = new ClippedTextureProgressBar(
                ENTROPY_BAR_BACKGROUND_TEXTURE,
                ENTROPY_BAR_PROGRESS_TEXTURE
        ).setRange(0.0F, 1.0F);
        progressBar.getLayout().widthPercent(100);
        progressBar.getLayout().height(5);
        return progressBar;
    }

    public static ModularUI build(Player player, UIElement root) {
        return new ModularUI(
                UI.of(
                        root,
                        StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC),
                        StylesheetManager.INSTANCE.getStylesheetSafe(INCoreLdLibStyles.BASE)
                ),
                player
        );
    }

    public record WindowScaffold(UIElement root, UIElement window, UIElement header, UIElement body) {
    }

    public record SectionScaffold(UIElement root, UIElement body) {
    }
}
