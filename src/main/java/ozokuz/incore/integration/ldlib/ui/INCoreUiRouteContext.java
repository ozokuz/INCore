package ozokuz.incore.integration.ldlib.ui;

public sealed interface INCoreUiRouteContext permits INCoreUiRouteContext.Empty {
    enum Empty implements INCoreUiRouteContext {
        INSTANCE
    }
}
