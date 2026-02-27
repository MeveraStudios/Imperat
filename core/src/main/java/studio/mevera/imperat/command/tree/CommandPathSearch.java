package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public final class CommandPathSearch<S extends Source> {

    private final LiteralCommandNode<S> root;

    private @NotNull CommandNode<S, ?> lastNode;
    private @NotNull CommandNode<S, ?> lastCommandNode;
    private final @NotNull List<CommandNode<S, ?>> visitedNodes = new ArrayList<>(20);
    private CommandPathway<S> directUsage, closestUsage;

    private Result result;

    private CommandPathSearch(@NotNull LiteralCommandNode<S> root, Result result) {
        this.root = root;
        this.lastCommandNode = root;
        this.lastNode = lastCommandNode;
        this.result = result;
    }

    private CommandPathSearch(Result result, @NotNull LiteralCommandNode<S> root, @NotNull CommandNode<S, ?> lastNode,
            CommandPathway<S> directUsage) {
        this.root = root;
        this.result = result;
        this.lastNode = lastNode;
        this.directUsage = directUsage;
        this.lastCommandNode = root;
    }

    public static <S extends Source> CommandPathSearch<S> of(LiteralCommandNode<S> root, final Result result) {
        return new CommandPathSearch<>(root, result);
    }

    public static <S extends Source> CommandPathSearch<S> unknown(LiteralCommandNode<S> root) {
        return of(root, Result.UNKNOWN);
    }

    public static <S extends Source> CommandPathSearch<S> freshlyNew(Command<S> command) {
        CommandPathSearch<S> dispatch = of(command.tree().rootNode(), Result.UNKNOWN);
        dispatch.append(command.tree().rootNode());
        dispatch.setFoundPath(command.getDefaultPathway());
        return dispatch;
    }

    public static <S extends Source> CommandPathSearch<S> paused(Command<S> command) {
        CommandPathSearch<S> dispatch = of(command.tree().rootNode(), Result.PAUSE);
        Command<S> target = null;
        if (command.tree() != null) {
            target = command;
        } else {
            Command<S> root = command;
            while (root != null) {

                if (root.getParent() == null) {
                    break;
                }
                root = root.getParent();
            }
            if (root != null && root != command) {
                target = root;
            }
        }
        if (target != null) {
            dispatch.append(target.tree().rootNode());
            dispatch.setFoundPath(target.getDefaultPathway());
        }

        return dispatch;
    }

    public void append(CommandNode<S, ?> node) {
        if (node == null) {
            return;
        }
        if (node.isLiteral()) {
            this.lastCommandNode = node;
        }
        this.lastNode = node;

        this.visitedNodes.add(node);
    }

    public @NotNull CommandNode<S, ?> getLastNode() {
        return lastNode;
    }

    public @Nullable CommandPathway<S> getFoundPath() {
        if (directUsage == null) {
            directUsage = calculateFoundPath();
        }
        return directUsage;
    }

    public void setFoundPath(CommandPathway<S> directUsage) {
        this.directUsage = directUsage;
    }

    private CommandPathway<S> calculateFoundPath() {
        var lastNode = getLastNode();
        if (lastNode.isExecutable()) {
            //use properties from the last node's executable usage.
            var executableUsage = lastNode.getExecutableUsage();
            assert executableUsage != null;

            List<Argument<S>> args = new ArrayList<>(visitedNodes.stream()
                                                             .map(CommandNode::getData)
                                                             .toList());

            var executableArgs = executableUsage.getArguments();
            for (var arg : executableArgs) {
                if (args.contains(arg)) {
                    continue;
                }
                args.add(arg);
            }

            return CommandPathway.<S>builder(executableUsage.getMethodElement())
                           .parameters(args)
                           .execute(executableUsage.getExecution())
                           .cooldown(executableUsage.getCooldown())
                           .description(executableUsage.getDescription())
                           .permission(executableUsage.getPermissionsData())
                           .examples(executableUsage.getExamples())
                           .registerFlags(executableUsage.getFlagExtractor().getRegisteredFlags())
                           .coordinator(executableUsage.getCoordinator())
                           .build(root.data);

        }
        return null;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public CommandPathway<S> getClosestUsage() {
        if (closestUsage == null) {
            //lazy computation, only when demanded for better performance :D
            closestUsage = computeClosestUsage();
        }

        return closestUsage;
    }



    private CommandPathway<S> computeClosestUsage() {
        if (directUsage != null && lastNode.isLast()) {
            return directUsage;
        }

        return (closestUsage = closestUsageLookup());
    }

    private CommandPathway<S> closestUsageLookup() {
        CommandPathway<S> closestUsage = null;

        Queue<CommandNode<S, ?>> nodes = new LinkedList<>();
        nodes.add(lastNode);

        CommandNode<S, ?> curr;
        while (!nodes.isEmpty()) {
            curr = nodes.poll();
            if (!curr.isLiteral() && curr.isExecutable()) {
                closestUsage = curr.getExecutableUsage();
                nodes.clear();
                break;
            }

            for (CommandNode<S, ?> child : curr.getChildren()) {
                nodes.add(child);
            }
        }

        if (closestUsage == null) {
            //if its still null, then let's go back to the last cmd node
            if (lastCommandNode.isExecutable()) {
                closestUsage = lastCommandNode.getExecutableUsage();
            } else {
                closestUsage = ((LiteralCommandNode<S>) lastCommandNode).getData().getDefaultPathway();
            }
        }

        return closestUsage;
    }

    public CommandPathSearch<S> copy() {
        return new CommandPathSearch<>(result, root, lastNode, directUsage);
    }

    public @NotNull LiteralCommandNode<S> getLastCommandNode() {
        return (LiteralCommandNode<S>) lastCommandNode;
    }

    public void visitRemainingOptionalNodes() {
        CommandNode<S, ?> current = lastNode.getChild(CommandNode::isOptional);
        while (current != null) {
            visitedNodes.add(current);
            current = current.getChild(CommandNode::isOptional);
        }
    }

    /**
     * Defines a setResult from dispatching the command execution.
     */
    public enum Result {

        /**
         * The tree stopped midway for some reason,
         * most probably would be that the source doesn't have access
         * to a {@link CommandNode} that matches his corresponding input.
         */
        PAUSE,

        /**
         * Defines a complete dispatch of the command,
         * {@link CommandPathway} cannot be null unless the {@link StandardCommandTree} has issues
         */
        COMPLETE,

        /**
         * Defines an unknown execution/command, it's the default setResult
         */
        UNKNOWN,

        /**
         * Defines an execution that ended up with throwing an exception that had no handler
         */
        FAILURE;

        public boolean isStoppable() {
            return this == COMPLETE || this == PAUSE;
        }
    }
}
