package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.internal.ContextFactory;
import studio.mevera.imperat.providers.SuggestionProvider;

public final class ExecutionConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    ExecutionConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public ExecutionConfig<S> commandPrefix(String commandPrefix) {
        config.setCommandPrefix(commandPrefix);
        return this;
    }

    public ExecutionConfig<S> globalCoordinator(CommandCoordinator<S> commandCoordinator) {
        config.setGlobalCommandCoordinator(commandCoordinator);
        return this;
    }

    public ExecutionConfig<S> contextFactory(ContextFactory<S> contextFactory) {
        config.setContextFactory(contextFactory);
        return this;
    }

    public ExecutionConfig<S> defaultSuggestionProvider(@NotNull SuggestionProvider<S> suggestionProvider) {
        config.setDefaultSuggestionProvider(suggestionProvider);
        return this;
    }

    public ExecutionConfig<S> globalDefaultPathwayBuilder(CommandPathway.Builder<S> usage) {
        config.setGlobalDefaultPathway(usage);
        return this;
    }

    public ExecutionConfig<S> overlapOptionalParameterSuggestions(boolean overlap) {
        config.setOptionalParameterSuggestionOverlap(overlap);
        return this;
    }

    public ExecutionConfig<S> handleMiddleOptionalArgSkipping(boolean toggle) {
        config.setHandleExecutionConsecutiveOptionalArgumentsSkip(toggle);
        return this;
    }

    public ExecutionConfig<S> parsingMode(CommandParsingMode mode) {
        config.setCommandParsingMode(mode);
        return this;
    }
}
