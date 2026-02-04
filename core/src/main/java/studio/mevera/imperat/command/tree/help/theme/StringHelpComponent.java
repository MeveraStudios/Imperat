package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

public class StringHelpComponent<S extends Source> extends HelpComponent<S, String> {

    protected StringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }


    @Override
    public void send(S source) {
        source.reply(componentValue);
    }

    @Override
    public @NotNull HelpComponent<S, String> append(String other) {
        this.componentValue = componentValue + other;
        return this;
    }

    @Override
    public HelpComponent<S, String> appendText(String text) {
        return this.append(text);
    }

    @Override
    public @NotNull HelpComponent<S, String> repeat(int times) {
        this.componentValue = componentValue.repeat(times);
        return this;
    }
}
