package studio.mevera.imperat;

import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

final class ArgumentDecorator<T> extends Argument<T> {

    private final studio.mevera.imperat.command.parameters.Argument<MinestomSource> imperatArg;
    private final Argument<T> argument;

    ArgumentDecorator(studio.mevera.imperat.command.parameters.Argument<MinestomSource> imperatArg, Argument<T> minestomArg) {
        super(minestomArg.getId(), minestomArg.allowSpace(), minestomArg.useRemaining());
        this.imperatArg = imperatArg;
        this.argument = minestomArg;
    }

    @Override
    public @NotNull T parse(@NotNull CommandSender sender, @NotNull String input) throws ArgumentSyntaxException {
        return argument.parse(sender, input);
    }

    @Override
    public @NonNull ArgumentParserType parser() {
        return argument.parser();
    }

    @Override
    public boolean isOptional() {
        return imperatArg.isOptional() && super.isOptional();
    }

}
