package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestSource;

import java.math.BigDecimal;

public class BigDecimalParamType extends BaseParameterType<TestSource, BigDecimal> {
    
    
    @Override
    public @Nullable BigDecimal resolve(@NotNull ExecutionContext<TestSource> context, @NotNull CommandInputStream<TestSource> inputStream, @NotNull String input) throws
            CommandException {
        try {
            double d = Double.parseDouble(input);
            return BigDecimal.valueOf(d);
        }catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    @Override
    public boolean matchesInput(int rawPosition, Context<TestSource> context, CommandParameter<TestSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }
        
        try {
            double d = Double.parseDouble(input);
            BigDecimal.valueOf(d);
            return true;
        }catch (Exception ex) {
            return false;
        }
    }
}
