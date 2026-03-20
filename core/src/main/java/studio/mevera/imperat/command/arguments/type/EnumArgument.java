package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class EnumArgument<S extends CommandSource> extends ArgumentType<S, Enum<?>> {
    public EnumArgument(TypeWrap<Enum<?>> typeWrap) {
        super(typeWrap.getType());
        Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) typeWrap.getType();
        for (var constantEnum : type.getEnumConstants()) {
            suggestions.add(constantEnum.name());
        }
    }

    @Override
    public Enum<?> parse(@NotNull CommandContext<S> context, @NotNull String input) throws ArgumentParseException, ResponseException {
        Type enumType = type;
        try {
            return Enum.valueOf((Class<? extends Enum>) enumType, input);
        } catch (IllegalArgumentException | EnumConstantNotPresentException ex) {
            throw new ArgumentParseException(ResponseKey.INVALID_ENUM, input)
                          .withPlaceholder("enum_type", ((Class<?>) enumType).getTypeName());
        }
    }

    @Override
    public Enum<?> parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws ArgumentParseException, ResponseException {
        String input = cursor.currentRawIfPresent();
        if (input == null) {
            throw new IllegalArgumentException("No input available at cursor position");
        }
        return parse(context, input);
    }

}
