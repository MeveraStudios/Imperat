package studio.mevera.imperat.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.command.tree.help.theme.StringHelpComponent;

public class HytaleStringHelpComponent extends StringHelpComponent<HytaleSource> {
    
    protected HytaleStringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }
    
    public static HytaleStringHelpComponent of(@NotNull String componentValue) {
        return new HytaleStringHelpComponent(componentValue);
    }
}
