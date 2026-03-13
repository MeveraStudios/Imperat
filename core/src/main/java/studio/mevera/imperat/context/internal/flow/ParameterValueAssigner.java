package studio.mevera.imperat.context.internal.flow;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.tree.TreeExecutionResult;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.flow.handlers.EmptyInputHandler;
import studio.mevera.imperat.context.internal.flow.handlers.OptionalParameterHandler;
import studio.mevera.imperat.context.internal.flow.handlers.RequiredParameterHandler;
import studio.mevera.imperat.context.internal.flow.handlers.SubCommandHandler;
import studio.mevera.imperat.exception.CommandException;

@SuppressWarnings("unchecked")
public final class ParameterValueAssigner<S extends CommandSource> {

    private static final ParameterChain<?> DEFAULT_CHAIN = createDefaultChain();

    private final TreeExecutionResult<S> treeExecutionResult;
    private final ExecutionContext<S> context;
    private final Cursor<S> stream;
    private final ParameterChain<S> chain;

    ParameterValueAssigner(TreeExecutionResult<S> treeExecutionResult, CommandPathway<S> usage) {
        this(treeExecutionResult, usage, (ParameterChain<S>) DEFAULT_CHAIN);
    }

    ParameterValueAssigner(TreeExecutionResult<S> treeExecutionResult, CommandPathway<S> usage, ParameterChain<S> customChain) {
        this.treeExecutionResult = treeExecutionResult;
        this.context = treeExecutionResult.getExecutionContext();
        this.chain = customChain;
        assert context != null;
        this.stream = Cursor.of(context.arguments(), usage);
    }

    private static <S extends CommandSource> ParameterChain<S> createDefaultChain() {
        return ChainFactory.<S>builder()
                       .withHandler(new EmptyInputHandler<>())
                       .withHandler(new SubCommandHandler<>())
                       .withHandler(new RequiredParameterHandler<>())
                       .withHandler(new OptionalParameterHandler<>())
                       //.withHandler(new FreeFlagHandler<>())
                       .build();
    }

    public static <S extends CommandSource> ParameterValueAssigner<S> create(
            TreeExecutionResult<S> treeExecutionResult,
            CommandPathway<S> usage
    ) {
        return new ParameterValueAssigner<>(treeExecutionResult, usage);
    }

    public static <S extends CommandSource> ParameterValueAssigner<S> createWithCustomChain(
            TreeExecutionResult<S> treeExecutionResult,
            CommandPathway<S> usage,
            ParameterChain<S> customChain
    ) {
        return new ParameterValueAssigner<>(treeExecutionResult, usage, customChain);
    }

    public void resolve() throws CommandException {
        // ADD: Time the chain execution
        chain.execute(treeExecutionResult, context, stream);
    }

    public Command<S> getCommand() {
        return context.getLastUsedCommand();
    }
}