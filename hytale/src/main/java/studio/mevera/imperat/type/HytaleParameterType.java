package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.ParseException;

public class HytaleParameterType<T> extends BaseParameterType<HytaleSource, T> {

    private final ArgumentType<T> hytaleArgType;
    private final ExceptionProvider exceptionProvider;

    public HytaleParameterType(Class<T> type, ArgumentType<T> hytaleArgType, ExceptionProvider provider) {
        super(type);
        this.hytaleArgType = hytaleArgType;
        this.exceptionProvider = provider;
    }
    public HytaleParameterType(Data<T> data) {
        super(data.type);
        this.hytaleArgType = data.argumentType;
        this.exceptionProvider = data.provider;
    }


    @Override
    public @Nullable T resolve(
            @NotNull ExecutionContext<HytaleSource> context,
            @NotNull CommandInputStream<HytaleSource> inputStream,
            @NotNull String input
    ) throws ImperatException {
        String[] rawInput = context.arguments().toArray(String[]::new);
        final ParseResult parseResult = new ParseResult();
        T parsedArg = hytaleArgType.parse(rawInput, parseResult);
        if (parseResult.failed()) {
            throw exceptionProvider.fetch(input, context);
        } else {
            //success, lets skip the same amount
            int numberOfArgs = hytaleArgType.getNumberOfParameters();
            for (int i = 0; i < numberOfArgs; i++) {
                inputStream.skipRaw();
            }
            return parsedArg;
        }
    }

    public ArgumentType<T> getHytaleArgType() {
        return hytaleArgType;
    }

    @FunctionalInterface
    public interface ExceptionProvider {

        ExceptionProvider DEFAULT = (in, ctx)-> new ParseException(in, ctx) {};

        ImperatException fetch(String input, Context<HytaleSource> context);
    }

    public record Data<T>(Class<T> type, ArgumentType<T> argumentType, ExceptionProvider provider) {

    }
}
