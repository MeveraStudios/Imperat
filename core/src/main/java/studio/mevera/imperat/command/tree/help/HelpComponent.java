package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

public abstract class HelpComponent<C> {
    
    protected @NotNull C componentValue;
    
    protected HelpComponent(@NotNull C componentValue) {
        this.componentValue = componentValue;
    }

    public abstract <S extends Source> void send(S source);
    
    public abstract @NotNull HelpComponent<C> append(C other);
    
    public abstract @NotNull HelpComponent<C> repeat(int times);
    
    public static StringHelpComponent plainText(@NotNull String text) {
        return new StringHelpComponent(text);
    }
    
}
