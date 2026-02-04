package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.parse.InvalidEnumException;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class ParameterEnum<S extends Source> extends BaseParameterType<S, Enum<?>> {

    public ParameterEnum(TypeWrap<Enum<?>> typeWrap) {
        super(typeWrap.getType());
        Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) typeWrap.getType();
        for (var constantEnum : type.getEnumConstants()) {
            suggestions.add(constantEnum.name());
        }
    }

    @Override
    public @NotNull Enum<?> resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, @NotNull String input)
            throws
            CommandException {

        Type enumType = commandInputStream.currentParameter()
                                .filter(param -> TypeUtility.matches(type, Enum.class))
                                .map(CommandParameter::valueType)
                                .orElse(type);

        try {
            return Enum.valueOf((Class<? extends Enum>) enumType, input);
        } catch (IllegalArgumentException | EnumConstantNotPresentException ex) {
            throw new InvalidEnumException(input, (Class<? extends Enum>) enumType);
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, CommandParameter<S> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            if (!TypeWrap.of(type).isSubtypeOf(Enum.class)) {
                return true;
            }
            Enum.valueOf((Class<? extends Enum>) type, input);
            return true;
        } catch (IllegalArgumentException | EnumConstantNotPresentException ex) {
            return false;
        }
    }

}
