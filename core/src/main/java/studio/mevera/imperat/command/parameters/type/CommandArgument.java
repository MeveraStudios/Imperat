package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;

import java.util.List;

public final class CommandArgument<S extends Source> extends ArgumentType<S, Command<S>> {

    private final String name;

    CommandArgument(String name, List<String> aliases) {
        super();
        this.name = name;
        suggestions.add(name);
        suggestions.addAll(aliases);
    }

    @Override
    public @Nullable Command<S> resolve(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput) throws
            CommandException {
        return cursor.currentParameter()
                       .map(Argument::asCommand).orElse(null);
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> ctx, Argument<S> parameter) {
        String input = ctx.arguments().get(rawPosition);
        return parameter.isCommand() &&
                       parameter.asCommand().hasName(input.toLowerCase());
    }

    public String getName() {
        return name;
    }

}
