package studio.mevera.imperat.command.tree.projection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single-pass walker that turns a {@link Command}'s runtime tree into a
 * {@link CommandTreeProjection}. The walk mirrors the legacy pathway-scope
 * resolution that platform integrations (Brigadier in particular) used to
 * re-implement themselves — it is centralised here so all consumers see
 * identical scoping semantics.
 *
 * <p>Package-private; users invoke {@link CommandTreeProjection#of(Command)}.</p>
 */
final class ProjectionBuilder {

    private ProjectionBuilder() {
    }

    static <S extends CommandSource> ProjectedNode<S> build(@NotNull Command<S> rootCommand) {
        Node<S> root = rootCommand.tree().rootNode();
        return buildNode(rootCommand, root);
    }

    private static <S extends CommandSource> ProjectedNode<S> buildNode(Command<S> rootCommand, Node<S> node) {
        List<ProjectedNode<S>> children = new ArrayList<>();
        for (Node<S> child : node.getChildren()) {
            children.add(buildNode(rootCommand, child));
        }

        List<Argument<S>> optionals = new ArrayList<>(node.getOptionalArguments());
        List<ProjectedFlag<S>> flags = collectFlagsForScope(rootCommand, node);

        return new ProjectedNode<>(node, children, optionals, flags);
    }

    /**
     * Resolves the flag set reachable at {@code scopeNode}, mirroring the
     * legacy {@code resolveFlagScopePathways} → flag-extractor walk. De-dupes
     * by canonical flag name so overlapping pathway scopes don't emit the
     * same flag twice.
     */
    private static <S extends CommandSource> List<ProjectedFlag<S>> collectFlagsForScope(Command<S> rootCommand, Node<S> scopeNode) {
        List<CommandPathway<S>> scopes = resolvePathwayScopes(rootCommand, scopeNode);

        // Preserve insertion order while de-duping by canonical name.
        Map<String, ProjectedFlag<S>> byName = new LinkedHashMap<>();
        for (CommandPathway<S> pathway : scopes) {
            for (FlagArgument<S> flag : pathway.getFlagExtractor().getRegisteredFlags()) {
                String key = flag.getName().toLowerCase(Locale.ROOT);
                if (byName.containsKey(key)) {
                    continue;
                }
                List<String> aliases = new ArrayList<>();
                aliases.add(flag.getName());
                aliases.addAll(flag.flagData().aliases());
                byName.put(key, new ProjectedFlag<>(flag, aliases, flag.isSwitch(), pathway));
            }
        }
        return new ArrayList<>(byName.values());
    }

    private static <S extends CommandSource> List<CommandPathway<S>> resolvePathwayScopes(Command<S> rootCommand, Node<S> scopeNode) {
        List<CommandPathway<S>> scopes = new ArrayList<>();
        addScope(scopes, scopeNode.getOriginalPathway());

        Argument<S> main = scopeNode.getMainArgument();
        if (main.isCommand()) {
            Command<S> commandScope = main.asCommand();
            for (CommandPathway<S> pathway : commandScope.getDedicatedPathways()) {
                addScope(scopes, pathway);
            }
            addScope(scopes, commandScope.getDefaultPathway());
        }

        for (CommandPathway<S> pathway : rootScopedPathways(rootCommand, commandChainFor(scopeNode))) {
            addScope(scopes, pathway);
        }
        return scopes;
    }

    private static <S extends CommandSource> void addScope(List<CommandPathway<S>> scopes, @Nullable CommandPathway<S> pathway) {
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

    private static <S extends CommandSource> List<String> commandChainFor(Node<S> node) {
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

    private static <S extends CommandSource> List<CommandPathway<S>> rootScopedPathways(Command<S> rootCommand, List<String> commandChain) {
        List<CommandPathway<S>> rootPathways = new ArrayList<>();
        for (CommandPathway<S> pathway : rootCommand.getDedicatedPathways()) {
            addScope(rootPathways, pathway);
        }
        addScope(rootPathways, rootCommand.getDefaultPathway());

        List<CommandPathway<S>> scoped = new ArrayList<>();
        for (CommandPathway<S> pathway : rootPathways) {
            if (isExactCommandScope(pathway, commandChain)) {
                addScope(scoped, pathway);
            }
        }
        return scoped;
    }

    private static <S extends CommandSource> boolean isExactCommandScope(CommandPathway<S> pathway, List<String> commandChain) {
        int prefixLen = leadingCommandPrefixLength(pathway);
        if (prefixLen != commandChain.size()) {
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

    private static <S extends CommandSource> int leadingCommandPrefixLength(CommandPathway<S> pathway) {
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
