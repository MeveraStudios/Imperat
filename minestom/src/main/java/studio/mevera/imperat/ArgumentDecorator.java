package studio.mevera.imperat;

import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;

final class ArgumentDecorator<T> extends Argument<T> {

    private final CommandParameter<MinestomSource> parameter;
    private final Argument<T> argument;

    ArgumentDecorator(CommandParameter<MinestomSource> parameter, Argument<T> argument) {
        super(argument.getId(), argument.allowSpace(), argument.useRemaining());
        this.parameter = parameter;
        this.argument = argument;
    }


    @Override
    public @NotNull T parse(@NotNull CommandSender sender, @NotNull String input) throws ArgumentSyntaxException {
        return argument.parse(sender, input);
    }

    @Override
    public ArgumentParserType parser() {
        return argument.parser();
    }


    @Override
    public boolean isOptional() {
        return parameter.isOptional() && super.isOptional();
    }

}
