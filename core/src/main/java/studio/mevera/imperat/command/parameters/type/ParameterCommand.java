package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;

import java.util.List;

public final class ParameterCommand<S extends Source> extends BaseParameterType<S, Command<S>> {
    private final String name;
    ParameterCommand(String name, List<String> aliases) {
        super();
        this.name = name;
        suggestions.add(name);
        suggestions.addAll(aliases);
    }

    @Override
    public @Nullable Command<S> resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, @NotNull String input) throws ImperatException {
        return commandInputStream.currentParameter()
            .map(CommandParameter::asCommand).orElse(null);
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> ctx, CommandParameter<S> parameter) {
        String input = ctx.arguments().get(rawPosition);
        return parameter.isCommand() &&
            parameter.asCommand().hasName(input.toLowerCase());
    }

    public String getName() {
        return name;
    }

}
