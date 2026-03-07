package studio.mevera.imperat.tests.contextresolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.tests.TestCommandSource;

public class SomeDataCR implements ContextArgumentProvider<TestCommandSource, SomeData> {

    @Override
    public @Nullable SomeData provide(
            @NotNull ExecutionContext<TestCommandSource> context,
            @Nullable ParameterElement parameter
    ) throws CommandException {
        return new SomeData("test");
    }
}
