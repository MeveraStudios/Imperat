package studio.mevera.imperat.annotations.base.system;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.tree.ArgumentNode;
import studio.mevera.imperat.command.tree.CommandNode;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.command.tree.LiteralCommandNode;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Composes subcommand trees into parent command trees.
 * Handles attachment of subtrees at the correct nodes based on inheritance.
 */
public final class TreeComposer<S extends Source> {

    private final Map<Command<S>, CommandTree<S>> trees = new IdentityHashMap<>();

    /**
     * Builds a tree for a command and all its subcommands recursively.
     */
    public CommandTree<S> compose(Command<S> rootCommand) {
        // Build tree for root first
        CommandTree<S> rootTree = buildTree(rootCommand);
        trees.put(rootCommand, rootTree);

        // Recursively attach subcommand trees
        for (Command<S> subcommand : rootCommand.getSubCommands()) {
            attachSubcommandTree(rootCommand, subcommand);
        }

        return rootTree;
    }

    private CommandTree<S> buildTree(Command<S> command) {
        // Create tree with command's own pathways
        CommandTree<S> tree = CommandTree.create(command.imperat().config(), command);

        // Parse all pathways into the tree
        for (CommandPathway<S> pathway : command.getAllPossiblePathways()) {
            tree.parseUsage(pathway);
        }

        return tree;
    }

    private void attachSubcommandTree(Command<S> parent, Command<S> subcommand) {
        // Build subcommand's tree first
        CommandTree<S> subTree = buildTree(subcommand);
        trees.put(subcommand, subTree);

        // Determine attachment points based on inheritance
        List<TreeAttachmentPoint<S>> attachmentPoints = determineAttachmentPoints(parent, subcommand);

        if (attachmentPoints.isEmpty()) {
            // No inheritance - attach at parent root
            attachAtRoot(parent, subcommand, subTree);
        } else {
            // Attach at each determined point
            for (TreeAttachmentPoint<S> point : attachmentPoints) {
                attachAtPoint(point, subTree);
            }
        }

        // Recursively process subcommand's subcommands
        for (Command<S> nestedSub : subcommand.getSubCommands()) {
            attachSubcommandTree(subcommand, nestedSub);
        }
    }

    /**
     * Determines where in the parent's tree this subcommand should attach.
     * Based on inheritance chains of the subcommand's pathways.
     */
    private List<TreeAttachmentPoint<S>> determineAttachmentPoints(
            Command<S> parent,
            Command<S> subcommand
    ) {
        List<TreeAttachmentPoint<S>> points = new ArrayList<>();
        CommandTree<S> parentTree = trees.get(parent);

        for (CommandPathway<S> pathway : subcommand.getAllPossiblePathways()) {
            ParameterInheritanceChain<S> inheritance = getInheritanceChain(subcommand, pathway);

            if (inheritance.isEmpty()) {
                // No inheritance for this pathway - will attach at root
                continue;
            }

            // Find the node in parent's tree where inheritance ends
            CommandNode<S, ?> attachmentNode = findAttachmentNode(parentTree, inheritance);

            if (attachmentNode == null) {
                throw new IllegalStateException(
                        "Cannot attach subcommand '" + subcommand.getName() + "' pathway: " +
                                "inheritance chain doesn't match any node in parent tree. " +
                                "Inheritance: " + inheritance.getChain().stream()
                                                          .map(p -> p.getArgument().getName())
                                                          .toList()
                );
            }

            points.add(new TreeAttachmentPoint<>(
                    parent,
                    subcommand,
                    pathway,
                    attachmentNode,
                    inheritance
            ));
        }

        return points;
    }

    /**
     * Finds the node in parent's tree that matches the end of inheritance chain.
     */
    private @Nullable CommandNode<S, ?> findAttachmentNode(
            CommandTree<S> parentTree,
            ParameterInheritanceChain<S> inheritance
    ) {
        if (inheritance.isEmpty()) {
            return parentTree.rootNode();
        }

        // Walk the tree following the inheritance chain
        CommandNode<S, ?> current = parentTree.rootNode();
        List<ParameterInheritanceChain.InheritedParameter<S>> chain = inheritance.getChain();

        for (ParameterInheritanceChain.InheritedParameter<S> inherited : chain) {
            Argument<S> expectedArg = inherited.getArgument();

            // Find matching child
            CommandNode<S, ?> next = findMatchingChild(current, expectedArg);

            if (next == null) {
                // Chain doesn't match this branch
                return null;
            }

            current = next;
        }

        // Return the node AFTER the last inherited parameter
        // (where subcommand's own parameters start)
        return current;
    }

    private @Nullable CommandNode<S, ?> findMatchingChild(
            CommandNode<S, ?> parent,
            Argument<S> expectedArg
    ) {
        for (CommandNode<S, ?> child : parent.getChildren()) {
            if (matchesArgument(child, expectedArg)) {
                return child;
            }
        }
        return null;
    }

    private boolean matchesArgument(CommandNode<S, ?> node, Argument<S> expected) {
        if (!(node instanceof ArgumentNode)) {
            return false;
        }

        Argument<S> nodeArg = ((ArgumentNode<S>) node).getData();

        return nodeArg.getName().equalsIgnoreCase(expected.getName()) &&
                       nodeArg.valueType().equals(expected.valueType());
    }

    private void attachAtRoot(Command<S> parent, Command<S> subcommand, CommandTree<S> subTree) {
        // Add subcommand as a literal node under parent's root
        LiteralCommandNode<S> subcommandNode = CommandNode.createCommandNode(
                trees.get(parent).rootNode(),
                subcommand,
                1, // depth under parent root
                null // not executable itself
        );

        // Attach subcommand's tree under this node
        for (CommandNode<S, ?> child : subTree.rootNode().getChildren()) {
            subcommandNode.addChild(child);
        }

        trees.get(parent).rootNode().addChild(subcommandNode);
    }

    private void attachAtPoint(TreeAttachmentPoint<S> point, CommandTree<S> subTree) {
        // Create subcommand node at attachment point
        LiteralCommandNode<S> subcommandNode = CommandNode.createCommandNode(
                point.attachmentNode(),
                point.subcommand(),
                point.attachmentNode().getDepth() + 1,
                null
        );

        // Attach subcommand's children
        for (CommandNode<S, ?> child : subTree.rootNode().getChildren()) {
            subcommandNode.addChild(child);
        }

        point.attachmentNode().addChild(subcommandNode);
    }

    private ParameterInheritanceChain<S> getInheritanceChain(
            Command<S> command,
            CommandPathway<S> pathway
    ) {
        // Access the inheritance chain stored during parsing
        // This requires Command to store inheritance info
        return command.getInheritanceChain(pathway);
    }

    private record TreeAttachmentPoint<S extends Source>(
            Command<S> parent,
            Command<S> subcommand,
            CommandPathway<S> pathway,
            CommandNode<S, ?> attachmentNode,
            ParameterInheritanceChain<S> inheritanceChain
    ) {

    }
}