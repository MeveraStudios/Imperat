package studio.mevera.imperat.context.internal.sur;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.sur.handlers.EmptyInputHandler;
import studio.mevera.imperat.context.internal.sur.handlers.FlagInputHandler;
import studio.mevera.imperat.context.internal.sur.handlers.FreeFlagHandler;
import studio.mevera.imperat.context.internal.sur.handlers.NonFlagWhenExpectingFlagHandler;
import studio.mevera.imperat.context.internal.sur.handlers.OptionalParameterHandler;
import studio.mevera.imperat.context.internal.sur.handlers.RequiredParameterHandler;
import studio.mevera.imperat.context.internal.sur.handlers.SubCommandHandler;
import studio.mevera.imperat.exception.CommandException;

@SuppressWarnings("unchecked")
public final class ParameterValueAssigner<S extends Source> {
    
    private static final ParameterChain<?> DEFAULT_CHAIN = createDefaultChainWithFreeFlagHandler();
    
    private final ExecutionContext<S> context;
    private final CommandInputStream<S> stream;
    private final ParameterChain<S> chain;

    ParameterValueAssigner(ExecutionContext<S> context, CommandUsage<S> usage) {
        this(context, usage, (ParameterChain<S>) DEFAULT_CHAIN);
    }
    
    ParameterValueAssigner(ExecutionContext<S> context, CommandUsage<S> usage, ParameterChain<S> customChain) {
        this.context = context;
        this.chain = customChain;
        this.stream = CommandInputStream.of(context.arguments(), usage);
    }

    private static <S extends Source> ParameterChain<S> createDefaultChainWithFreeFlagHandler() {
        return ChainFactory.<S>builder()
            .withHandler(new EmptyInputHandler<>())
            .withHandler(new SubCommandHandler<>())
            .withHandler(new FlagInputHandler<>())
            .withHandler(new NonFlagWhenExpectingFlagHandler<>())
            .withHandler(new RequiredParameterHandler<>())
            .withHandler(new OptionalParameterHandler<>())
            .withHandler(new FreeFlagHandler<>())
            .build();
    }

    public static <S extends Source> ParameterValueAssigner<S> create(
        ExecutionContext<S> context,
        CommandUsage<S> usage
    ) {
        return new ParameterValueAssigner<>(context, usage);
    }
    
    public static <S extends Source> ParameterValueAssigner<S> createWithCustomChain(
        ExecutionContext<S> context,
        CommandUsage<S> usage,
        ParameterChain<S> customChain
    ) {
        return new ParameterValueAssigner<>(context, usage, customChain);
    }
    
    public void resolve() throws CommandException {
        // ADD: Time the chain execution
        chain.execute(context, stream);
    }

    public Command<S> getCommand() {
        return context.getLastUsedCommand();
    }
}