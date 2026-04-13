package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Context;
import studio.mevera.imperat.command.tree.help.renderers.HelpRenderer;
import studio.mevera.imperat.command.tree.help.renderers.TextHelpRenderer;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;

import java.util.List;

/**
 * Public help API exposed to command methods through {@code @Context}.
 *
 * <p>This type owns the complete help flow:
 * query command help data, render it through a {@link HelpRenderer},
 * then optionally deliver it through a {@link HelpSender}.</p>
 *
 * @param <S> the command source type
 */
@Context
public final class CommandHelp<S extends CommandSource> {

    private final @NotNull ExecutionContext<S> context;

    private CommandHelp(@NotNull ExecutionContext<S> context) {
        this.context = context;
    }

    public static <S extends CommandSource> CommandHelp<S> create(ExecutionContext<S> context) {
        return new CommandHelp<>(context);
    }

    public @NotNull HelpResult<S> query() {
        return query(HelpQuery.<S>builder().build());
    }

    public @NotNull HelpResult<S> query(@NotNull HelpQuery<S> helpQuery) {
        return context.command()
                       .tree()
                       .queryHelp(enrichQuery(helpQuery));
    }

    public void show() {
        show(HelpQuery.<S>builder().build());
    }

    public void show(@NotNull HelpQuery<S> helpQuery) {
        show(helpQuery, TextHelpRenderer.create(), HelpSender.forReplies());
    }

    public void show(
            @NotNull HelpQuery<S> helpQuery,
            @NotNull HelpRenderer<S, String> renderer
    ) {
        show(helpQuery, renderer, HelpSender.forReplies());
    }

    public <O> void show(
            @NotNull HelpQuery<S> helpQuery,
            @NotNull HelpRenderer<S, O> renderer,
            @NotNull HelpSender<S, O> sender
    ) {
        sender.sendAll(context.source(), render(helpQuery, renderer));
    }

    public <O> @NotNull List<O> render(
            @NotNull HelpQuery<S> helpQuery,
            @NotNull HelpRenderer<S, O> renderer
    ) {
        return renderer.render(context, query(helpQuery));
    }

    public @NotNull ExecutionContext<S> getContext() {
        return context;
    }

    private @NotNull HelpQuery<S> enrichQuery(@NotNull HelpQuery<S> helpQuery) {
        return helpQuery.withFilter(HelpFilters.hasPermission(context.source(), context));
    }
}
