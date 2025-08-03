package studio.mevera.imperat.tests.contextresolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.resolvers.ContextResolver;
import studio.mevera.imperat.tests.TestSource;

public class PlayerDataContextResolver implements ContextResolver<TestSource, PlayerData> {

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
    ) throws ImperatException {
        TestSource source = context.source();
        return new PlayerData(source.name(), source.uuid());
    }

}