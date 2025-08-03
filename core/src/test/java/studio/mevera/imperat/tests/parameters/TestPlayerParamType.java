package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

public final class TestPlayerParamType extends BaseParameterType<TestSource, TestPlayer> {
    
    @Override
    public @NotNull TestPlayer resolve(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull CommandInputStream<TestSource> inputStream,
            @NotNull String input
    ) throws ImperatException {
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
}