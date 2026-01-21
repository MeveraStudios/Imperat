package studio.mevera.imperat.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.tree.help.theme.StringHelpComponent;

public class BukkitStringHelpComponent extends StringHelpComponent<BukkitSource> {

    protected BukkitStringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }

    public static BukkitStringHelpComponent of(@NotNull String componentValue) {
        return new BukkitStringHelpComponent(componentValue);
    }

}
