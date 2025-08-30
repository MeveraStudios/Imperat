package studio.mevera.imperat.command.tree.help.theme;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseHelpTheme<S extends Source, C> implements HelpTheme<S, C> {
    
    private final @NotNull PresentationStyle style;
    private final int indentMultiplier;
    private final Function<C, HelpComponent<S, C>> componentFactory;
    private final Map<Option<?>, Object> optionValues = new HashMap<>();
    {
        optionValues.put(Option.SHOW_HEADER, true);
        optionValues.put(Option.SHOW_FOOTER, true);
    }
    
    protected BaseHelpTheme(
            @NotNull PresentationStyle style,
            int indentMultiplier,
            Function<C, HelpComponent<S, C>> componentFactory
    ) {
        this.style = style;
        this.indentMultiplier = indentMultiplier;
        this.componentFactory = componentFactory;
    }
    
    protected BaseHelpTheme(
            PresentationStyle style,
            Function<C, HelpComponent<S, C>> componentFactory
    ) {
        this(style, 1, componentFactory);
    }
    
    
    @Override
    public @NotNull PresentationStyle getPreferredStyle() {
        return style;
    }
    
    @Override
    public int getIndentMultiplier() {
        return indentMultiplier;
    }
    
    // Abstract content methods for subclasses to implement
    public abstract @NotNull C createEmptyContent();
    
    public abstract @NotNull C getBranchContent();
    
    public abstract @NotNull C getLastBranchContent();
    
    public abstract @NotNull C getIndentContent();
    
    public abstract @NotNull C getEmptyIndentContent();
    
    public abstract @NotNull C getHeaderContent(ExecutionContext<S> context);
    
    public abstract @NotNull C getFooterContent(ExecutionContext<S> context);
    
    // Final component methods using componentFactory
    @Override
    public final HelpComponent<S, C> createEmptyComponent() {
        return componentFactory.apply(createEmptyContent());
    }
    
    @Override
    public final @NotNull HelpComponent<S, C> getBranch() {
        return componentFactory.apply(getBranchContent());
    }
    
    @Override
    public final @NotNull HelpComponent<S, C> getLastBranch() {
        return componentFactory.apply(getLastBranchContent());
    }
    
    @Override
    public final @NotNull HelpComponent<S, C> getIndent() {
        return componentFactory.apply(getIndentContent());
    }
    
    @Override
    public final @NotNull HelpComponent<S, C> getEmptyIndent() {
        return componentFactory.apply(getEmptyIndentContent());
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
