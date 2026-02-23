package studio.mevera.imperat.tests.contextresolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.tests.TestSource;

public class PlayerDataContextArgumentResolver implements ContextArgumentProvider<TestSource, PlayerData> {

    /**
     * Resolves a parameter's default value
     * if it has been not input by the user
     *
     * @param context   the context
     * @param parameter the parameter (null if used the classic way)
     * @return the resolved default-value
     */
    @Override
    public @Nullable PlayerData resolve(
            @NotNull ExecutionContext<TestSource> context,
            @Nullable ParameterElement parameter
    ) throws CommandException {
        TestSource source = context.source();
        return new PlayerData(source.name(), source.uuid());
    }

}