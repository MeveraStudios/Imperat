package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ParseException;
import studio.mevera.imperat.resolvers.SuggestionResolver;

public class HytaleParameterType<T> extends BaseParameterType<HytaleSource, T> {

    private final ArgumentType<T> hytaleArgType;
    private final ExceptionProvider exceptionProvider;
    private final SuggestionResolver<HytaleSource> suggestionResolver;

    public HytaleParameterType(Class<T> type, ArgumentType<T> hytaleArgType, ExceptionProvider provider) {
        super(type);
        this.hytaleArgType = hytaleArgType;
        this.exceptionProvider = provider;
        this.suggestionResolver = (ctx, commandParameter) -> {
            SuggestionResult result = new SuggestionResult();
            hytaleArgType.suggest(ctx.source().origin(), ctx.getArgToComplete().value(), ctx.arguments().size(), result);
            return result.getSuggestions();
        };
    }

    public HytaleParameterType(Data<T> data) {
        this(data.type, data.argumentType, data.provider);
    }


    @Override
    public @Nullable T resolve(
            @NotNull ExecutionContext<HytaleSource> context,
            @NotNull CommandInputStream<HytaleSource> inputStream,
            @NotNull String input
    ) throws CommandException {
        String[] rawInput = context.arguments().toArray(String[]::new);
        final ParseResult parseResult = new ParseResult();
        T parsedArg = hytaleArgType.parse(rawInput, parseResult);
        if (parseResult.failed()) {
            throw exceptionProvider.fetch(input);
        } else {
            //success, lets skip the same amount
            int numberOfArgs = hytaleArgType.getNumberOfParameters();
            for (int i = 1; i < numberOfArgs; i++) {
                inputStream.skipRaw();
            }
            return parsedArg;
        }
    }

    @Override
    public int getNumberOfParametersToConsume() {
        return hytaleArgType.getNumberOfParameters();
    }

    @Override
    public SuggestionResolver<HytaleSource> getSuggestionResolver() {
        return suggestionResolver;
    }

    public ArgumentType<T> getHytaleArgType() {
        return hytaleArgType;
    }

    @Override
    public boolean isGreedy(CommandParameter<HytaleSource> parameter) {
        return hytaleArgType.isListArgument();
    }

    @FunctionalInterface
    public interface ExceptionProvider {

        ExceptionProvider DEFAULT = (in) -> new ParseException(in) {};

        CommandException fetch(String input);
    }

    public record Data<T>(Class<T> type, ArgumentType<T> argumentType, ExceptionProvider provider) {

    }
}
