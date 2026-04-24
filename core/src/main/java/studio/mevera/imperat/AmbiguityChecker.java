package studio.mevera.imperat;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.AmbiguousCommandException;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class AmbiguityChecker {

    private AmbiguityChecker() {
        throw new AssertionError();
    }

    static <S extends CommandSource> void checkAmbiguity(Command<S> command) {
        command.visualizeTree();
        var rootNode = command.tree().rootNode();
        checkAmbiguity(
                rootNode,
                rootNode
        );
    }

    static <S extends CommandSource> void checkAmbiguity(LiteralCommandNode<S> root, CommandNode<S, ?> node) {
        AmbiguityResult<S> result = checkIsNodeAmbiguous(root, node);
        if(result.isAmbiguous()) {
            throw new AmbiguousCommandException(root.getData(), node, result.argumentNodes());
        }

        for (CommandNode<S, ?> child : node.getChildren()) {
            checkAmbiguity(root, child);
        }
    }

    static <S extends CommandSource> AmbiguityResult<S> checkIsNodeAmbiguous(LiteralCommandNode<S> root, CommandNode<S, ?> node) {
        if (node.isGreedyParam() && !node.isLast()) {
            // Limited greedy (limit != -1) is allowed to have trailing args — the cap is enforced at parse time
            boolean isUnlimited = node.getData().greedyLimit() == -1;
            if (isUnlimited) {
                throw new AmbiguousCommandException("Greedy parameter '" + node.format() + "' in command '" + root.format() + "' must be the last"
                                                            + " parameter of the command !");
            }
        }
        if(node.isLast()) {
            return AmbiguityResult.failure();
        }

        var children = node.getChildren()
                               .stream().filter((n)-> !n.isFlag() && !n.isLiteral())
                               .toList();

        return detectAmbiguity(children);
    }

    private static <S extends CommandSource> AmbiguityResult<S> detectAmbiguity(Collection<CommandNode<S, ?>> nodes) {
        Set<Type> types = new HashSet<>();
        Set<Priority> priorities = new HashSet<>();
        int requiredCount = 0;
        int optionalCount = 0;

        for (CommandNode<S, ?> node : nodes) {
            if (node.isRequired()) {
                requiredCount++;
            } else if (node.isOptional()) {
                optionalCount++;
            }

            types.add(node.getData().valueType());
            priorities.add(node.getData().type().getPriority());
        }

        return new AmbiguityResult<>(
                nodes,
                (requiredCount > 0 && optionalCount == 0) || (requiredCount == 0 && optionalCount > 0),
                types.size() < nodes.size(),
                priorities.size() < nodes.size()
        );
    }

    record AmbiguityResult<S extends CommandSource>(
            Collection<CommandNode<S, ?>> argumentNodes,
            boolean allSameNature,
            boolean hasDuplicateTypes,
            boolean hasDuplicatePriorities
    ) {

        public static <S extends CommandSource> AmbiguityResult<S> failure() {
            return new AmbiguityResult<>(Collections.emptyList(), false, false, false);
        }

        public boolean isAmbiguous() {
            return argumentNodes.size() > 1 && allSameNature && (hasDuplicateTypes || hasDuplicatePriorities);
        }
    }
}
