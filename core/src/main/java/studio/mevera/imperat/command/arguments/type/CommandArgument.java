package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

public final class CommandArgument<S extends CommandSource> extends ArgumentType<S, Command<S>> {

    private final Command<S> command;

    CommandArgument(Command<S> command) {
        super();
        this.command = command;
        suggestions.add(command.getName());
        suggestions.addAll(command.aliases());
    }

    @Override
    public Command<S> parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws CommandException {
        if (command.hasName(input)) {
            return command;
        }
        throw new CommandException("Invalid literal argument '%s'", input);
    }

    public String getName() {
        return command.getName();
    }
}
