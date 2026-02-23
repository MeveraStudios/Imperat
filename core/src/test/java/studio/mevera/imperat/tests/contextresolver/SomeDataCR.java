package studio.mevera.imperat.tests.contextresolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.tests.TestSource;

public class SomeDataCR implements ContextArgumentProvider<TestSource, SomeData> {

    @Override
    public @Nullable SomeData resolve(
            @NotNull ExecutionContext<TestSource> context,
            @Nullable ParameterElement parameter
    ) throws CommandException {
        return new SomeData("test");
    }
}
