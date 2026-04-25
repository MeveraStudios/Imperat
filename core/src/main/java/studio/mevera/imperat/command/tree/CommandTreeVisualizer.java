package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

@ApiStatus.Internal
public final class CommandTreeVisualizer<S extends CommandSource> {

    private final @Nullable CommandTree<S> tree;
    private final boolean showNodeTypes;

    CommandTreeVisualizer(@Nullable CommandTree<S> tree) {
        this(tree, true);
    }

    CommandTreeVisualizer(@Nullable CommandTree<S> tree, boolean showNodeTypes) {
        this.tree = tree;
        this.showNodeTypes = showNodeTypes;
    }

    public static <S extends CommandSource> CommandTreeVisualizer<S> of(@Nullable CommandTree<S> tree) {
        return new CommandTreeVisualizer<>(tree);
    }

    public void visualize() {
        visualizeSimple();
    }

    public void visualizeSimple() {
        if (tree == null || !ImperatDebugger.isEnabled()) {
            return;
        }
        ImperatDebugger.debug(getVisualizationString());
    }

    public String getVisualizationString() {
        if (tree == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n==== SuperCommandTree ====\n");
        builder.append("nodes=").append(tree.size()).append('\n').append('\n');
        renderNode(tree.rootNode(), builder, "", true);
        return builder.toString();
    }

    private void renderNode(Node<S> node, StringBuilder out, String prefix, boolean tail) {
        if (node.isRoot()) {
            out.append(formatNode(node)).append('\n');
        } else {
            out.append(prefix)
                    .append(tail ? "└── " : "├── ")
                    .append(formatNode(node))
                    .append('\n');
        }

        String childPrefix = prefix + childPrefixSegment(node, tail);
        renderNodeMetadata(node, out, childPrefix);

        List<Node<S>> children = node.getChildren().toList();
        for (int i = 0; i < children.size(); i++) {
            renderNode(children.get(i), out, childPrefix, i == children.size() - 1);
        }
    }

    private String childPrefixSegment(Node<S> node, boolean tail) {
        if (node.isRoot()) {
            return "";
        }
        return tail ? "    " : "│   ";
    }

    private void renderNodeMetadata(Node<S> node, StringBuilder out, String prefix) {
        String detailPrefix = prefix + "  ";

        out.append(detailPrefix)
                .append("pathway: ")
                .append(formatPathway(node.getOriginalPathway(), false))
                .append('\n');

        if (!node.getOptionalArguments().isEmpty()) {
            out.append(detailPrefix)
                    .append("optionals: ")
                    .append(formatArguments(node.getOptionalArguments()))
                    .append('\n');
        }

        List<CommandPathway<S>> flagScopes = effectivePathways(node);
        for (CommandPathway<S> flagScope : flagScopes) {
            if (flagScope.getFlagExtractor().getRegisteredFlags().isEmpty()) {
                continue;
            }
            out.append(detailPrefix)
                    .append("flags @ ")
                    .append(formatPathway(flagScope, false))
                    .append(": ")
                    .append(formatFlags(flagScope.getFlagExtractor().getRegisteredFlags()))
                    .append('\n');
        }
    }

    private String formatNode(Node<S> node) {
        String base = node.format();
        if (!showNodeTypes) {
            return base;
        }
        if (node.isRoot()) {
            return "[ROOT] " + base;
        }
        if (node.getMainArgument().isCommand()) {
            return "[SUB] " + base;
        }
        return "[REQ] " + base;
    }

    private String formatArguments(Collection<? extends Argument<S>> arguments) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Argument<S> argument : arguments) {
            joiner.add(formatArgument(argument));
        }
        return joiner.toString();
    }

    private String formatArgument(Argument<S> argument) {
        if (!showNodeTypes) {
            return argument.format();
        }
        if (argument.isFlag()) {
            return "[FLAG] " + argument.format();
        }
        if (argument.isCommand()) {
            return "[SUB] " + argument.format();
        }
        if (argument.isOptional()) {
            return "[OPT] " + argument.format();
        }
        return "[REQ] " + argument.format();
    }

    private String formatFlags(Collection<? extends FlagArgument<S>> flags) {
        StringJoiner joiner = new StringJoiner(", ");
        for (FlagArgument<S> flag : flags) {
            joiner.add(formatFlag(flag));
        }
        return joiner.toString();
    }

    private String formatFlag(FlagArgument<S> flag) {
        StringBuilder builder = new StringBuilder();
        if (showNodeTypes) {
            builder.append(flag.isSwitch() ? "[SWITCH] " : "[VALUE_FLAG] ");
        }
        builder.append(flag.format());

        List<String> aliases = flag.flagData().aliases();
        if (!aliases.isEmpty()) {
            StringJoiner aliasesJoiner = new StringJoiner(", ", " aliases=[", "]");
            for (String alias : aliases) {
                aliasesJoiner.add("-" + alias);
            }
            builder.append(aliasesJoiner);
        }
        return builder.toString();
    }

    private String formatPathway(CommandPathway<S> pathway, boolean includeFlags) {
        List<Argument<S>> arguments = includeFlags ? pathway.getArgumentsWithFlags() : pathway.getArguments();
        if (arguments.isEmpty()) {
            return "<default>";
        }

        StringJoiner joiner = new StringJoiner(" ");
        for (Argument<S> argument : arguments) {
            joiner.add(argument.format());
        }
        return joiner.toString();
    }

    private List<CommandPathway<S>> effectivePathways(Node<S> node) {
        List<CommandPathway<S>> scopes = new ArrayList<>();
        addPathwayScope(scopes, node.getOriginalPathway());

        Argument<S> main = node.getMainArgument();
        if (main.isCommand()) {
            Command<S> commandScope = main.asCommand();
            for (CommandPathway<S> pathway : commandScope.getDedicatedPathways()) {
                addPathwayScope(scopes, pathway);
            }
            addPathwayScope(scopes, commandScope.getDefaultPathway());
        }

        for (CommandPathway<S> pathway : rootPathwaysForCommandScope(commandChainForNode(node))) {
            addPathwayScope(scopes, pathway);
        }
        return scopes;
    }

    private void addPathwayScope(List<CommandPathway<S>> scopes, @Nullable CommandPathway<S> pathway) {
        if (pathway == null) {
            return;
        }
        for (CommandPathway<S> existing : scopes) {
            if (existing == pathway) {
                return;
            }
        }
        scopes.add(pathway);
    }

    private List<String> commandChainForNode(Node<S> node) {
        List<String> chain = new ArrayList<>();
        Node<S> current = node;
        while (current != null && !current.isRoot()) {
            Argument<S> main = current.getMainArgument();
            if (main.isCommand()) {
                chain.add(0, main.asCommand().getName());
            }
            current = current.getParent();
        }
        return chain;
    }

    private List<CommandPathway<S>> rootPathwaysForCommandScope(List<String> commandChain) {
        List<CommandPathway<S>> rootPathways = new ArrayList<>();
        Command<S> rootCommand = tree.rootNode().getMainArgument().asCommand();
        for (CommandPathway<S> pathway : rootCommand.getDedicatedPathways()) {
            addPathwayScope(rootPathways, pathway);
        }
        addPathwayScope(rootPathways, rootCommand.getDefaultPathway());

        List<CommandPathway<S>> scoped = new ArrayList<>();
        for (CommandPathway<S> pathway : rootPathways) {
            if (isExactCommandScope(pathway, commandChain)) {
                addPathwayScope(scoped, pathway);
            }
        }
        return scoped;
    }

    private boolean isExactCommandScope(CommandPathway<S> pathway, List<String> commandChain) {
        int commandPrefixLength = leadingCommandPrefixLength(pathway);
        if (commandPrefixLength != commandChain.size()) {
            return false;
        }

        List<Argument<S>> arguments = pathway.getArguments();
        for (int i = 0; i < commandChain.size(); i++) {
            Argument<S> argument = arguments.get(i);
            if (!argument.isCommand() || !argument.asCommand().hasName(commandChain.get(i))) {
                return false;
            }
        }
        return true;
    }

    private int leadingCommandPrefixLength(CommandPathway<S> pathway) {
        int count = 0;
        for (Argument<S> argument : pathway.getArguments()) {
            if (!argument.isCommand()) {
                break;
            }
            count++;
        }
        return count;
    }
}
