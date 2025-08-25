package studio.mevera.imperat.command.tree.help.renderers.layouts;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.tree.help.HelpComponent;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

public interface UsageDecorator<S extends Source, C> {
    
    @NotNull HelpComponent<C> decorate(
            Command<S> lastOwningCommand,
            CommandUsage<S> pathway,
            ExecutionContext<S> context,
            HelpRenderOptions<S, C> renderOptions
    );
    
}
