package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.parse.InvalidEnumException;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class EnumArgument<S extends Source> extends ArgumentType<S, Enum<?>> {

    public EnumArgument(TypeWrap<Enum<?>> typeWrap) {
        super(typeWrap.getType());
        Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) typeWrap.getType();
        for (var constantEnum : type.getEnumConstants()) {
            suggestions.add(constantEnum.name());
        }
    }

    @Override
    public @NotNull Enum<?> parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput)
            throws
            CommandException {

        Type enumType = cursor.currentParameter()
                                .filter(param -> TypeUtility.matches(type, Enum.class))
                                .map(Argument::valueType)
                                .orElse(type);

        try {
            return Enum.valueOf((Class<? extends Enum>) enumType, correspondingInput);
        } catch (IllegalArgumentException | EnumConstantNotPresentException ex) {
            throw new InvalidEnumException(correspondingInput, (Class<? extends Enum>) enumType);
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, Argument<S> parameter) {
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
