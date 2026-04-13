package studio.mevera.imperat.adventure;

import net.kyori.adventure.text.Component;
import studio.mevera.imperat.command.tree.help.HelpSender;

public final class AdventureHelpSender {

    private AdventureHelpSender() {
    }

    public static <S extends AdventureCommandSource> HelpSender<S, Component> replying() {
        return (source, component) -> source.reply(component);
    }
}
