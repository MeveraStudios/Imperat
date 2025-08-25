package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

public final class StringHelpComponent extends HelpComponent<String> {
    
    StringHelpComponent(@NotNull String componentValue) {
        super(componentValue);
    }
    
    
    @Override
    public <S extends Source> void send(S source) {
        source.reply(componentValue);
    }
    
    @Override
    public @NotNull HelpComponent<String> append(String other) {
        this.componentValue = componentValue + other;
        return this;
    }
    
    @Override
    public @NotNull HelpComponent<String> repeat(int times) {
        this.componentValue = componentValue.repeat(times);
        return this;
    }
}
