package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseHelpTheme<S extends CommandSource, C> implements HelpTheme<S, C> {

    private final Function<C, HelpComponent<S, C>> componentFactory;
    private final Map<Option<?>, Object> optionValues = new HashMap<>();

    {
        optionValues.put(Option.SHOW_HEADER, true);
        optionValues.put(Option.SHOW_FOOTER, true);
    }

    protected BaseHelpTheme(Function<C, HelpComponent<S, C>> componentFactory) {
        this.componentFactory = componentFactory;
    }

    // Abstract content methods for subclasses to implement
    public abstract @NotNull C createEmptyContent();

    public abstract @NotNull C getHeaderContent(ExecutionContext<S> context);

    public abstract @NotNull C getFooterContent(ExecutionContext<S> context);

    // Final component methods using componentFactory
    @Override
    public final HelpComponent<S, C> createEmptyComponent() {
        return componentFactory.apply(createEmptyContent());
    }


    @Override
    public final @NotNull HelpComponent<S, C> getHeader(ExecutionContext<S> context) {
        return componentFactory.apply(getHeaderContent(context));
    }

    @Override
    public final @NotNull HelpComponent<S, C> getFooter(ExecutionContext<S> context) {
        return componentFactory.apply(getFooterContent(context));
    }

    @Override @SuppressWarnings("unchecked")
    public <T> T getOptionValue(@NotNull Option<T> option) {
        return (T) optionValues.getOrDefault(option, option.defaultValue());
    }

    @Override
    public <T> void setOptionValue(@NotNull Option<T> option, T value) {
        optionValues.put(option, value);
    }
}
