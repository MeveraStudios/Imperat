package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

import java.util.List;

public final class TestPlayerParamType extends BaseParameterType<TestSource, TestPlayer> {
    
    @Override
    public @Nullable TestPlayer resolve(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull CommandInputStream<TestSource> inputStream,
            @NotNull String input
    ) {
        if(!matchesInput(input, inputStream.currentParameterIfPresent())) {
            return null;
        }
        return new TestPlayer(input);
    }
    
    @Override
    public boolean matchesInput(String input, CommandParameter<TestSource> parameter) {
        try{
            Double.parseDouble(input);
            return false;
        }catch (Exception exception) {
            return input.length() <= 16;
        }
    }
    
    @Override
    public SuggestionResolver<TestSource> getSuggestionResolver() {
        return (ctx, p)-> {
            return List.of("MQZEN", "MOHAMED");
        };
    }
}