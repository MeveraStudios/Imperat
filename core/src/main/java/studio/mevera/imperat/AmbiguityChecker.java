package studio.mevera.imperat;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.tree.CommandNode;
import studio.mevera.imperat.command.tree.LiteralCommandNode;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

final class AmbiguityChecker {

    private AmbiguityChecker() {
        throw new AssertionError();
    }

    static <S extends Source> void checkAmbiguity(Command<S> command) {
        var rootNode = command.tree().rootNode();
        checkAmbiguity(
                rootNode,
                rootNode
        );
    }

    static <S extends Source> void checkAmbiguity(LiteralCommandNode<S> root, CommandNode<S, ?> node) {
        if(isNodeAmbiguous(root, node)) {
            throw new IllegalStateException("Command '" + root.format() + "' has ambiguous syntax at parameter '" + node.format() + "' !");
        }
        for (CommandNode<S, ?> child : node.getChildren()) {
            checkAmbiguity(root, child);
        }
    }

    static <S extends Source> boolean isNodeAmbiguous(LiteralCommandNode<S> root, CommandNode<S, ?> node) {
        if(node.isGreedyParam() && !node.isLast()) {
            throw new IllegalStateException("Greedy parameter '" + node.format() + "' in command '" + root.format() + "' must be the last"
                                                    + " parameter of the command !");
        }
        if(node.isLast()) {
            return false;
        }

        var children = node.getChildren()
                               .stream().filter((n)-> !n.isFlag() && !n.isLiteral())
                               .toList();

        return detectAmbiguity(children).toBoolean();
    }

    private static <S extends Source> AmbiguityResult<S> detectAmbiguity(Collection<CommandNode<S, ?>> nodes) {
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
            priorities.add(node.getData().type().priority());
        }

        return new AmbiguityResult<>(
                nodes,
                (requiredCount > 0 && optionalCount == 0) || (requiredCount == 0 && optionalCount > 0),
                types.size() != nodes.size(),
                priorities.size() != nodes.size()
        );
    }

    record AmbiguityResult<S extends Source>(
            Collection<CommandNode<S, ?>> argumentNodes,
            boolean allSameNature,
            boolean hasDuplicateTypes,
            boolean hasDuplicatePriorities
    ) {
        public boolean toBoolean() {
            return argumentNodes.size() > 1 && allSameNature && (hasDuplicateTypes || hasDuplicatePriorities);
        }
    }
}
