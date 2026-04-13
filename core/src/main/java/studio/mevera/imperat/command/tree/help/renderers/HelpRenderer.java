package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;

import java.util.List;

public interface HelpRenderer<S extends CommandSource, O> {

    @NotNull List<O> render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpResult<S> help
    );
}
