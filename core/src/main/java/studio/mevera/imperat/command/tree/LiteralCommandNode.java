package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;

import java.util.LinkedList;
import java.util.Queue;

@ApiStatus.Internal
public final class LiteralCommandNode<S extends Source> extends CommandNode<S, Command<S>> {

    LiteralCommandNode(@Nullable CommandNode<S, ?> parent, @NotNull Command<S> data, int depth, @Nullable CommandPathway<S> usage) {
        super(parent, data, depth, usage);
    }

    @Override
    public String format() {
        return data.format();
    }

    @Override
    public Priority priority() {
        return Priority.MAXIMUM;
    }

    public LiteralCommandNode<S> copy() {
        return new LiteralCommandNode<>(this.getParent(), this.data, getDepth(), executableUsage);
    }

    private static <S extends Source> @Nullable CommandNode<S, ?> findNodeForPathway(Queue<Argument<S>> pathwayArgs, CommandNode<S, ?> currentNode) {

        if (pathwayArgs.isEmpty()) {
            return currentNode;
        }

        var arg = pathwayArgs.peek();
        if (arg.getName().equals(currentNode.data.getName()) && arg.valueType().equals(currentNode.data.valueType())) {
            pathwayArgs.remove();
            if (pathwayArgs.isEmpty()) {
                return currentNode;
            }
        }

        for (var child : currentNode.getChildren()) {
            var res = findNodeForPathway(pathwayArgs, child);
            if (res != null) {
                return res;
            }
        }

        return null;
    }

    public @Nullable CommandNode<S, ?> findNodeForPathway(CommandPathway<S> inheritedPathway) {
        return findNodeForPathway(new LinkedList<>(inheritedPathway.getArguments()), this);
    }


}
