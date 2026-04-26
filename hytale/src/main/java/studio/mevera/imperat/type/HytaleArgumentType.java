package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.HytaleCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.ResponseKey;

public class HytaleArgumentType<T> extends ArgumentType<HytaleCommandSource, T> {

    private final com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> hytaleArgType;
    private final ExceptionProvider exceptionProvider;
    private final SuggestionProvider<HytaleCommandSource> suggestionProvider;

    public HytaleArgumentType(Class<T> type, com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> hytaleArgType, ExceptionProvider provider) {
        super(type);
        this.hytaleArgType = hytaleArgType;
        this.exceptionProvider = provider;
        this.suggestionProvider = (ctx, Argument) -> {
            SuggestionResult result = new SuggestionResult();
            hytaleArgType.suggest(ctx.source().origin(), ctx.getArgToComplete().value(), ctx.arguments().size(), result);
            return result.getSuggestions();
        };
    }

    public HytaleArgumentType(Data<T> data) {
        this(data.type, data.argumentType, data.provider);
    }


    @Override
    public @Nullable T parse(@NotNull CommandContext<HytaleCommandSource> context, @NotNull Argument<HytaleCommandSource> argument,
            @NotNull Cursor<HytaleCommandSource> cursor) throws CommandException {
        // Hytale's parser maintains its own multi-token position over the
        // full raw input array, so we hand it the entire context and only
        // drain the cursor through to the budget length.
        String input = cursor.collectRemaining();
        String[] rawInput = context.arguments().toArray(String[]::new);
        final ParseResult parseResult = new ParseResult();
        T parsedArg = hytaleArgType.parse(rawInput, parseResult);
        if (parseResult.failed()) {
            throw exceptionProvider.fetch(input);
        }
        return parsedArg;
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<HytaleCommandSource> argument) {
        return hytaleArgType.getNumberOfParameters();
    }

    @Override
    public SuggestionProvider<HytaleCommandSource> getSuggestionProvider() {
        return suggestionProvider;
    }

    public com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> getHytaleArgType() {
        return hytaleArgType;
    }

    @Override
    public boolean isGreedy(Argument<HytaleCommandSource> parameter) {
        return hytaleArgType.isListArgument();
    }

    @FunctionalInterface
    public interface ExceptionProvider {

        ExceptionProvider DEFAULT = (in) -> new ArgumentParseException(ResponseKey.INVALID_UUID, in);

        CommandException fetch(String input);
    }

    /**
     * Wrapper for ExceptionProvider that uses a ResponseKey.
     * This allows centralized error message management through the response system.
     */
    public static class ResponseKeyExceptionProvider implements ExceptionProvider {

        private final ResponseKey responseKey;

        public ResponseKeyExceptionProvider(ResponseKey responseKey) {
            this.responseKey = responseKey;
        }

        @Override
        public CommandException fetch(String input) {
            return new ArgumentParseException(responseKey, input);
        }
    }

    public record Data<T>(Class<T> type, com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType<T> argumentType, ExceptionProvider provider) {

    }
}
