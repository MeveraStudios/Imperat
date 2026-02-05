package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;

public abstract class HelpComponent<S extends Source, C> {

    protected @NotNull C componentValue;

    protected HelpComponent(@NotNull C componentValue) {
        this.componentValue = componentValue;
    }

    public static <S extends Source> StringHelpComponent<S> plainText(@NotNull String text) {
        return new StringHelpComponent<>(text);
    }

    public abstract void send(S source);

    public abstract @NotNull HelpComponent<S, C> append(C other);

    public @NotNull HelpComponent<S, C> append(HelpComponent<S, C> other) {
        return this.append(other.componentValue);
    }

    public abstract HelpComponent<S, C> appendText(String text);

    public @NotNull HelpComponent<S, C> appendText(HelpComponent<S, String> other) {
        return this.appendText(other.componentValue);
    }

    //one for Argument
    public @NotNull HelpComponent<S, C> appendParameterFormat(Argument<S> parameter) {
        return this.appendText(parameter.format());
    }

    public abstract @NotNull HelpComponent<S, C> repeat(int times);

}
