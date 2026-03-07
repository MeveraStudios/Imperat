package studio.mevera.imperat.tests;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;

public class ExampleDefaultProvider implements DefaultValueProvider {

    @Override
    public @Nullable <S extends CommandSource> String provide(
            ExecutionContext<S> context,
            Argument<S> parameter
    ) {
        //some logic to determine the default value based on the context and parameter
        //but in this example, we will just return a static value

        return "false";
    }
}
