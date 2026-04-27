package studio.mevera.imperat.command.tree.walk;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Strategy producing {@link HelpResult}s from a command tree, extracted from
 * the former {@code SuperCommandTree}. Walks the {@link Command} hierarchy
 * (not the parsing tree) collecting executable pathways subject to query
 * limits, depth, secrecy, and registered filters.
 */
public final class TreeHelpQuery<S extends CommandSource> {

    private final Node<S> root;

    public TreeHelpQuery(@NotNull Node<S> root) {
        this.root = root;
    }

    public HelpResult<S> query(@NotNull HelpQuery<S> query) {
        if (query.getLimit() <= 0 || query.getMaxDepth() < 0) {
            return HelpResult.empty();
        }

        Command<S> rootCommand = root.getMainArgument().asCommand();
        if (rootCommand.isSecret()) {
            return HelpResult.empty();
        }

        List<HelpEntry<S>> entries = new ArrayList<>();
        Set<String> seenUsages = new LinkedHashSet<>();
        List<Command<S>> visitedCommands = new ArrayList<>();
        collectHelpEntries(rootCommand, rootCommand, query, visitedCommands, seenUsages, entries);
        return HelpResult.copyOf(entries);
    }

    private void collectHelpEntries(
            Command<S> rootCommand,
            Command<S> command,
            HelpQuery<S> query,
            List<Command<S>> visitedCommands,
            Set<String> seenUsages,
            List<HelpEntry<S>> entries
    ) {
        if (entries.size() >= query.getLimit() || hasVisited(visitedCommands, command) || isSecretCommandPath(command, rootCommand)) {
            return;
        }

        visitedCommands.add(command);
        for (CommandPathway<S> pathway : command.getDedicatedPathways()) {
            if (entries.size() >= query.getLimit()) {
                return;
            }
            addHelpEntry(rootCommand, command, pathway, query, seenUsages, entries);
        }

        for (Command<S> child : command.getSubCommands()) {
            collectHelpEntries(rootCommand, child, query, visitedCommands, seenUsages, entries);
            if (entries.size() >= query.getLimit()) {
                return;
            }
        }
    }

    private void addHelpEntry(
            Command<S> rootCommand,
            Command<S> ownerCommand,
            CommandPathway<S> pathway,
            HelpQuery<S> query,
            Set<String> seenUsages,
            List<HelpEntry<S>> entries
    ) {
        if (containsSecretCommand(pathway)) {
            return;
        }

        CommandPathway<S> helpPathway = createHelpPathway(rootCommand, ownerCommand, pathway);
        if (helpPathway.size() == 0 && !query.getRootUsagePredicate().test(pathway)) {
            return;
        }
        if (helpPathway.size() > query.getMaxDepth()) {
            return;
        }
        if (!passesHelpFilters(helpPathway, query)) {
            return;
        }

        HelpEntry<S> entry = HelpEntry.of(helpPathway);
        if (seenUsages.add(entry.getUsage())) {
            entries.add(entry);
        }
    }

    private CommandPathway<S> createHelpPathway(
            Command<S> rootCommand,
            Command<S> ownerCommand,
            CommandPathway<S> pathway
    ) {
        List<Argument<S>> helpArguments = new ArrayList<>();
        addOwnerCommandPrefix(rootCommand, ownerCommand, helpArguments);
        for (Argument<S> argument : pathway.getArguments()) {
            helpArguments.add(copyHelpArgument(rootCommand, argument));
        }

        return CommandPathway.<S>builder(pathway.getMethodElement())
                       .arguments(helpArguments)
                       .withFlags(pathway.getFlagExtractor().getRegisteredFlags())
                       .examples(pathway.getExamples())
                       .execute(pathway.getExecution())
                       .permission(pathway.getPermissionsData())
                       .description(pathway.getDescription())
                       .coordinator(pathway.getCoordinator())
                       .cooldown(pathway.getCooldown())
                       .build(rootCommand);
    }

    private void addOwnerCommandPrefix(
            Command<S> rootCommand,
            Command<S> ownerCommand,
            List<Argument<S>> target
    ) {
        List<Command<S>> prefixes = new ArrayList<>();
        Command<S> current = ownerCommand;
        while (current != null && current != rootCommand) {
            prefixes.add(0, current);
            current = current.getParent();
        }

        for (Command<S> prefix : prefixes) {
            target.add(copyCommandLiteral(rootCommand, prefix));
        }
    }

    private Argument<S> copyCommandLiteral(Command<S> rootCommand, Command<S> command) {
        List<String> aliases = command.aliases();
        String[] names = new String[aliases.size() + 1];
        names[0] = command.getName();
        for (int i = 0; i < aliases.size(); i++) {
            names[i + 1] = aliases.get(i);
        }
        return Argument.literal(rootCommand.imperat(), names);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Argument<S> copyHelpArgument(Command<S> rootCommand, Argument<S> argument) {
        if (argument.isCommand()) {
            return copyCommandLiteral(rootCommand, argument.asCommand());
        }

        Argument<S> copy = Argument.of(
                argument.getName(),
                (ArgumentType) argument.type(),
                argument.getPermissionsData(),
                argument.getDescription(),
                argument.isOptional(),
                isGreedyArgument(argument),
                argument.getDefaultValueSupplier(),
                argument.getSuggestionResolver(),
                argument.getValidators().toList()
        );
        copy.setFormat(argument.format());
        return copy;
    }

    private boolean passesHelpFilters(CommandPathway<S> pathway, HelpQuery<S> query) {
        for (var filter : query.getFilters()) {
            if (!filter.filter(pathway)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsSecretCommand(CommandPathway<S> pathway) {
        for (Argument<S> argument : pathway.getArguments()) {
            if (argument.isCommand() && argument.asCommand().isSecret()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSecretCommandPath(Command<S> command, Command<S> rootCommand) {
        Command<S> current = command;
        while (current != null && current != rootCommand) {
            if (current.isSecret()) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean hasVisited(List<Command<S>> visitedCommands, Command<S> command) {
        for (Command<S> visited : visitedCommands) {
            if (visited == command) {
                return true;
            }
        }
        return false;
    }

    private boolean isGreedyArgument(Argument<S> argument) {
        return argument.isGreedy() || argument.type().isGreedy(argument);
    }
}
