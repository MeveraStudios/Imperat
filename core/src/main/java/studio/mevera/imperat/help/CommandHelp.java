package studio.mevera.imperat.help;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.ContextResolved;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

@ApiStatus.AvailableSince("1.0.0")
@SuppressWarnings("unchecked")
@ContextResolved
public final class CommandHelp {

    private final ImperatConfig<?> dispatcher;
    private final ExecutionContext<?> context;

    public CommandHelp(
        ImperatConfig<?> dispatcher,
        ExecutionContext<?> context
    ) {
        this.dispatcher = dispatcher;
        this.context = context;
    }

    public <S extends Source> void display(S source) {
        try {

            HelpProvider<S> provider = (HelpProvider<S>)
                    (context.command().hasHelpProvider() ?  context.command().getHelpProvider() : dispatcher.getHelpProvider());
            if (provider != null) {
                provider.provide((ExecutionContext<S>) context, source);
            }
        } catch (Throwable ex) {
            ((ImperatConfig<S>) dispatcher).handleExecutionThrowable(ex, (Context<S>) context, this.getClass(), "display(source)");
        }
    }

}
