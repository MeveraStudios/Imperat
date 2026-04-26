package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class EnumArgument<S extends CommandSource> extends SimpleArgumentType<S, Enum<?>> {
    public EnumArgument(TypeWrap<Enum<?>> typeWrap) {
        super(typeWrap.getType());
        Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) typeWrap.getType();
        for (var constantEnum : type.getEnumConstants()) {
            suggestions.add(constantEnum.name());
        }
    }

    @Override
    public Enum<?> parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws ResponseException {
        Type enumType = type;
        try {
            return Enum.valueOf((Class<? extends Enum>) enumType, input);
        } catch (IllegalArgumentException | EnumConstantNotPresentException ex) {
            throw new ArgumentParseException(ResponseKey.INVALID_ENUM, input)
                          .withPlaceholder("enum_type", ((Class<?>) enumType).getTypeName());
        }
    }

}
