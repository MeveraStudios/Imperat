package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.InvalidUUIDException;

import java.util.UUID;

public final class ParameterUUID<S extends Source> extends BaseParameterType<S, UUID> {
    public ParameterUUID() {
        super();
    }

    @Override
    public @NotNull UUID resolve(
            @NotNull ExecutionContext<S> context,
            @NotNull CommandInputStream<S> commandInputStream,
            @NotNull String input) throws ImperatException {

        try {
            return UUID.fromString(input);
        } catch (Exception ex) {
            throw new InvalidUUIDException(input, context);
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, CommandParameter<S> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            UUID.fromString(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
