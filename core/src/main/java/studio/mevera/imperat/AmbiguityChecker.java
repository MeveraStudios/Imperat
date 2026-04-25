package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.AmbiguousCommandException;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AmbiguityChecker {

    private AmbiguityChecker() {
        throw new AssertionError();
    }

    static <S extends CommandSource> void checkAmbiguity(Command<S> command) {
        var rootNode = command.tree().rootNode();
        checkAmbiguity(command, rootNode);
    }

    private static <S extends CommandSource> void checkAmbiguity(Command<S> rootCommand, Node<S> node) {
        AmbiguityResult<S> result = checkIsNodeAmbiguous(node);
        if (result.isAmbiguous()) {
            throw new AmbiguousCommandException(rootCommand, node, result.argumentFormats());
        }

        for (Node<S> child : node.getChildren()) {
            checkAmbiguity(rootCommand, child);
        }
    }

    private static <S extends CommandSource> AmbiguityResult<S> checkIsNodeAmbiguous(@NotNull Node<S> node) {
        if (node.isGreedy()) {
            boolean hasTrailingArguments = !node.isLeaf() || !node.getOptionalArguments().isEmpty();
            boolean isUnlimitedGreedy = node.getMainArgument().greedyLimit() == -1;
            if (isUnlimitedGreedy && hasTrailingArguments) {
                throw new AmbiguousCommandException(
                        "Greedy parameter '" + node.format() + "' must be the last parameter of the command pathway."
                );
            }
        }

        List<Argument<S>> siblingArguments = new ArrayList<>();
        for (Node<S> child : node.getChildren()) {
            siblingArguments.add(child.getMainArgument());
        }
        siblingArguments.addAll(node.getOptionalArguments());

        if (siblingArguments.isEmpty()) {
            return AmbiguityResult.failure();
        }
        return detectAmbiguity(siblingArguments);
    }

    private static <S extends CommandSource> AmbiguityResult<S> detectAmbiguity(Collection<? extends Argument<S>> arguments) {
        Set<Type> types = new HashSet<>();
        Set<Priority> priorities = new HashSet<>();
        int requiredCount = 0;
        int optionalCount = 0;
        List<String> formats = new ArrayList<>(arguments.size());
        int checkedArguments = 0;

        for (Argument<S> argument : arguments) {
            if (argument.isCommand() || isGreedyArgument(argument)) {
                continue;
            }

            if (argument.isRequired()) {
                requiredCount++;
            } else if (argument.isOptional()) {
                optionalCount++;
            }

            types.add(argument.valueType());
            priorities.add(argument.type().getPriority());
            formats.add(argument.format());
            checkedArguments++;
        }

        if (checkedArguments <= 1) {
            return AmbiguityResult.failure();
        }

        return new AmbiguityResult<>(
                formats,
                (requiredCount == checkedArguments) || (optionalCount == checkedArguments),
                types.size() < checkedArguments,
                priorities.size() < checkedArguments
        );
    }

    private static <S extends CommandSource> boolean isGreedyArgument(Argument<S> argument) {
        return argument.isGreedy() || argument.type().isGreedy(argument);
    }

    record AmbiguityResult<S extends CommandSource>(
            Collection<String> argumentFormats,
            boolean allSameNature,
            boolean hasDuplicateTypes,
            boolean hasDuplicatePriorities
    ) {

        static <S extends CommandSource> AmbiguityResult<S> failure() {
            return new AmbiguityResult<>(Collections.emptyList(), false, false, false);
        }

        boolean isAmbiguous() {
            return argumentFormats.size() > 1 && allSameNature && (hasDuplicateTypes || hasDuplicatePriorities);
        }
    }
}
