package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

import java.util.List;

public final class TestPlayerParamType extends BaseParameterType<TestSource, TestPlayer> {
    
    @Override
    public TestPlayer resolve(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull CommandInputStream<TestSource> inputStream,
            @NotNull String input
    ) throws NotPlayerException {
        // Note: We can't easily call matchesInput here without the raw position and context,
        // so we'll just validate the input directly
        if (input.length() > 16) {
            throw new NotPlayerException();
        }
        try {
            Double.parseDouble(input);
            throw new NotPlayerException(); // Numbers are not valid player names
        } catch (NumberFormatException e) {
            // Good, it's not a number
        }
        return new TestPlayer(input);
    }
    
    @Override
    public boolean matchesInput(int rawPosition, Context<TestSource> context, CommandParameter<TestSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }
        
        try{
            Double.parseDouble(input);
            return false;
        }catch (Exception exception) {
            System.out.println("Matching input = '" + input  +"'");
            System.out.println("Contains '-' ? " + input.contains("-"));
            return !input.contains("-");
        }
    }
    
    @Override
    public SuggestionResolver<TestSource> getSuggestionResolver() {
        return (ctx, p)-> {
            return List.of("MQZEN", "MOHAMED");
        };
    }
}