package studio.mevera.imperat.adventure;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.theme.HelpComponent;
import studio.mevera.imperat.context.Source;

import java.util.function.BiConsumer;

public class AdventureHelpComponent<S extends Source> extends HelpComponent<S, Component> {
    
    private final BiConsumer<S, Component> sendMessageToSourceFunc;
    public AdventureHelpComponent(@NotNull Component componentValue, BiConsumer<S, Component> sendMessageToSourceFunc) {
        super(componentValue);
        this.sendMessageToSourceFunc = sendMessageToSourceFunc;
    }
    
    @Override
    public void send(S source) {
        sendMessageToSourceFunc.accept(source, componentValue);
    }
    
    @Override
    public @NotNull HelpComponent<S, Component> append(Component other) {
        this.componentValue = this.componentValue.append(other);
        return this;
    }
    
    @Override
    public HelpComponent<S, Component> appendText(String text) {
        return append(Component.text(text));
    }
    
    @Override
    public @NotNull HelpComponent<S, Component> repeat(int times) {
        Component result = Component.empty();
        for (int i = 0; i < times; i++) {
            result = result.append(this.componentValue);
        }
        this.componentValue = result;
        return this;
    }
}
