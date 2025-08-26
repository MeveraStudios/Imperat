package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

public abstract class HelpComponent<S extends Source, C> {
    
    protected @NotNull C componentValue;
    
    protected HelpComponent(@NotNull C componentValue) {
        this.componentValue = componentValue;
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
    
    //one for CommandParameter
    public @NotNull HelpComponent<S, C> appendParameterFormat(CommandParameter<S> parameter) {
        return this.appendText(parameter.format());
    }
    
    public abstract @NotNull HelpComponent<S, C> repeat(int times);
    
    public static <S extends Source> StringHelpComponent<S> plainText(@NotNull String text) {
        return new StringHelpComponent<>(text);
    }
    
}
