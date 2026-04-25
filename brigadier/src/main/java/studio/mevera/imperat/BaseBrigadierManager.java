package studio.mevera.imperat;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.util.Patterns;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract non-sealed class BaseBrigadierManager<S extends CommandSource> implements BrigadierManager<S> {

    protected final Imperat<S> dispatcher;

    protected BaseBrigadierManager(Imperat<S> dispatcher) {
        this.dispatcher = dispatcher;
    }

    private static <BS> LiteralCommandNode<BS> cloneWithDiffName(
            LiteralCommandNode<BS> brigOriginalNode,
            String newName
    ) {
        var clone = new LiteralCommandNode<>(newName,
                brigOriginalNode.getCommand(), brigOriginalNode.getRequirement(), brigOriginalNode.getRedirect(),
                brigOriginalNode.getRedirectModifier(), brigOriginalNode.isFork());
        for (var child : brigOriginalNode.getChildren()) {
            clone.addChild(child);
        }
        return clone;
    }

    private static <S extends CommandSource, BS> void injectCommandNodeAliasesIntoBrigadier(
            Command<S> imperatCommand,
            LiteralCommandNode<BS> brigCommandNode,
            ArgumentBuilder<BS, ?> parentBrigNodeBuilder
    ) {
        for (var alias : imperatCommand.aliases()) {
            parentBrigNodeBuilder.then(cloneWithDiffName(brigCommandNode, alias));
        }
    }

    @Override
    public @NotNull <BS> LiteralCommandNode<BS> parseCommandIntoNode(@NotNull Command<S> command) {
        return this.<BS>convertRoot(command.tree().rootNode()).build();
    }

    private <BS> LiteralArgumentBuilder<BS> convertRoot(Node<S> root) {
        Command<S> rootCommand = root.getMainArgument().asCommand();
        LiteralArgumentBuilder<BS> builder = (LiteralArgumentBuilder<BS>)
                                                     literal(rootCommand.getName())
                    .requires((obj) -> {
                        var source = wrapCommandSource(obj);
                        return rootCommand.isIgnoringACPerms()
                                       || dispatcher.config().getPermissionChecker().hasPermission(source, rootCommand);
                    });
        executor(builder);
        appendContinuations(rootCommand, root, builder, 0);
        return builder;
    }

    protected <BS> CommandNode<BS> convertImperatNodeToBrigadierNode(
            Command<S> rootCommand,
            Node<S> currentImperatNode
    ) {
        ArgumentBuilder<BS, ?> childBuilder = createBrigadierBuilder(rootCommand, currentImperatNode);
        executor(childBuilder);

        Argument<S> main = currentImperatNode.getMainArgument();
        if (!main.isCommand()) {
            ((RequiredArgumentBuilder<BS, ?>) childBuilder).suggests(
                    createSuggestionProvider(rootCommand, main)
            );
        }

        appendContinuations(rootCommand, currentImperatNode, childBuilder, 0);
        return childBuilder.build();
    }

    private <BS> ArgumentBuilder<BS, ?> createBrigadierBuilder(
            Command<S> rootCommand,
            Node<S> currentImperatNode
    ) {
        Argument<S> argument = currentImperatNode.getMainArgument();
        ArgumentBuilder<BS, ?> builder = argument.isCommand()
                                                 ? LiteralArgumentBuilder.literal(argument.asCommand().getName())
                                                 : RequiredArgumentBuilder.argument(argument.getName(), getArgumentType(argument));

        builder.requires((obj) -> isNodeVisible(rootCommand, currentImperatNode, wrapCommandSource(obj)));
        return builder;
    }

    private boolean isNodeVisible(Command<S> rootCommand, Node<S> node, S source) {
        Argument<S> argument = node.getMainArgument();
        if (argument.isCommand() && argument.asCommand().isSecret()) {
            return false;
        }

        if (rootCommand.isIgnoringACPerms() || (argument.isCommand() && argument.asCommand().isIgnoringACPerms())) {
            return true;
        }

        var checker = dispatcher.config().getPermissionChecker();
        return checker.hasPermission(source, node.getOriginalPathway())
                       && checker.hasPermission(source, argument);
    }

    private <BS> void appendContinuations(
            Command<S> rootCommand,
            Node<S> scopeNode,
            ArgumentBuilder<BS, ?> parentBuilder,
            int optionalIndex
    ) {
        appendFlagSuggestionTunnel(rootCommand, scopeNode, parentBuilder);
        appendOptionalContinuation(rootCommand, scopeNode, parentBuilder, optionalIndex);
        appendChildContinuations(rootCommand, scopeNode, parentBuilder);
    }

    private <BS> void appendOptionalContinuation(
            Command<S> rootCommand,
            Node<S> scopeNode,
            ArgumentBuilder<BS, ?> parentBuilder,
            int optionalIndex
    ) {
        List<Argument<S>> optionals = new ArrayList<>(scopeNode.getOptionalArguments());
        if (optionalIndex >= optionals.size()) {
            return;
        }

        Argument<S> optional = optionals.get(optionalIndex);
        RequiredArgumentBuilder<BS, ?> optionalBuilder =
                RequiredArgumentBuilder.argument(optional.getName(), getArgumentType(optional));
        optionalBuilder.requires((obj) -> isArgumentVisible(rootCommand, optional, scopeNode.getOriginalPathway(), wrapCommandSource(obj)));
        optionalBuilder.suggests(createSuggestionProvider(rootCommand, optional));
        executor(optionalBuilder);

        appendContinuations(rootCommand, scopeNode, optionalBuilder, optionalIndex + 1);
        parentBuilder.then(optionalBuilder);
    }

    private <BS> void appendChildContinuations(
            Command<S> rootCommand,
            Node<S> scopeNode,
            ArgumentBuilder<BS, ?> parentBuilder
    ) {
        for (Node<S> child : scopeNode.getChildren()) {
            var childBrigNode = this.<BS>convertImperatNodeToBrigadierNode(rootCommand, child);
            parentBuilder.then(childBrigNode);

            Argument<S> childArgument = child.getMainArgument();
            if (childArgument.isCommand()) {
                injectCommandNodeAliasesIntoBrigadier(
                        childArgument.asCommand(),
                        (LiteralCommandNode<BS>) childBrigNode,
                        parentBuilder
                );
            }
        }
    }

    private boolean isArgumentVisible(
            Command<S> rootCommand,
            Argument<S> argument,
            @Nullable CommandPathway<S> pathway,
            S source
    ) {
        if (argument.isCommand() && argument.asCommand().isSecret()) {
            return false;
        }

        if (rootCommand.isIgnoringACPerms() || (argument.isCommand() && argument.asCommand().isIgnoringACPerms())) {
            return true;
        }

        var checker = dispatcher.config().getPermissionChecker();
        return (pathway == null || checker.hasPermission(source, pathway))
                       && checker.hasPermission(source, argument);
    }

    private <BS> void appendFlagSuggestionTunnel(
            Command<S> command,
            Node<S> scopeNode,
            ArgumentBuilder<BS, ?> parentBuilder
    ) {
        List<CommandPathway<S>> flagScopes = resolveFlagScopePathways(command, scopeNode);
        if (flagScopes.stream().allMatch((pathway) -> pathway.getFlagExtractor().getRegisteredFlags().isEmpty())) {
            return;
        }

        RequiredArgumentBuilder<BS, String> tunnelBuilder =
                RequiredArgumentBuilder.argument(flagTunnelName(scopeNode), StringArgumentType.greedyString());
        tunnelBuilder.suggests(createFlagSuggestionProvider(command, flagScopes));
        executor(tunnelBuilder);
        parentBuilder.then(tunnelBuilder);
    }

    private @NotNull <BS> SuggestionProvider<BS> createSuggestionProvider(
            Command<S> command,
            Argument<S> parameter
    ) {
        return (context, builder) -> {
            SuggestionContext<S> ctx = createSuggestionContext(command, context.getSource(), context.getInput());
            CompletionArg arg = ctx.getArgToComplete();

            String paramFormat = parameter.format();
            Description desc = parameter.getDescription();
            Message tooltip = new LiteralMessage(paramFormat + (desc.isEmpty() ? "" : " - " + desc.getValue()));

            return dispatcher.config().getParameterSuggestionResolver(parameter).provideAsynchronously(ctx, parameter)
                           .thenCompose((results) -> {
                               results
                                       .stream()
                                       .filter((candidate) -> arg.isEmpty()
                                                                      || candidate.toLowerCase().startsWith(arg.value().toLowerCase()))
                                       .forEachOrdered((result) -> builder.suggest(result, tooltip));
                               return builder.buildFuture();
                           });
        };
    }

    private @NotNull <BS> SuggestionProvider<BS> createFlagSuggestionProvider(
            Command<S> command,
            List<CommandPathway<S>> flagScopes
    ) {
        return (context, builder) -> {
            SuggestionContext<S> suggestionContext = createSuggestionContext(command, context.getSource(), context.getInput());
            CompletionArg arg = suggestionContext.getArgToComplete();

            boolean valueCompletion = isValueFlagInputPosition(command, flagScopes, suggestionContext, arg);
            boolean completingAfterFlag = hasFlagInputBeforeOrAtCompletion(suggestionContext, arg);
            if (!valueCompletion && !completingAfterFlag && !shouldSuggestFlagNames(arg)) {
                return builder.buildFuture();
            }

            String normalizedInput = normalizeInput(context.getInput());
            return dispatcher.autoComplete(wrapCommandSource(context.getSource()), normalizedInput)
                           .thenCompose((results) -> {
                               var targetBuilder = builder.createOffset(resolveSuggestionStart(context.getInput(), arg));
                               results.stream()
                                       .filter((result) -> valueCompletion || completingAfterFlag || result.startsWith("-"))
                                       .forEachOrdered(targetBuilder::suggest);
                               return targetBuilder.buildFuture();
                           });
        };
    }

    private @NotNull SuggestionContext<S> createSuggestionContext(
            Command<S> command,
            Object rawSource,
            String rawInput
    ) {
        S source = wrapCommandSource(rawSource);
        String input = normalizeInput(rawInput);
        int firstSpaceIndex = input.indexOf(' ');
        String label = firstSpaceIndex == -1 ? input : input.substring(0, firstSpaceIndex);
        boolean endsWithSpace = !input.isEmpty() && Character.isWhitespace(input.charAt(input.length() - 1));
        int argumentsStart = firstSpaceIndex == -1 ? input.length() : firstSpaceIndex + 1;
        int argumentsEnd = endsWithSpace ? input.length() - 1 : input.length();
        String argumentsSection = argumentsStart >= argumentsEnd
                                          ? ""
                                          : input.substring(argumentsStart, argumentsEnd);
        ArgumentInput args = ArgumentInput.parseAutoCompletion(argumentsSection, endsWithSpace);
        return dispatcher.config().getContextFactory().createSuggestionContext(dispatcher, source, command, label, args);
    }

    private boolean isValueFlagInputPosition(
            Command<S> command,
            List<CommandPathway<S>> flagScopes,
            SuggestionContext<S> suggestionContext,
            CompletionArg arg
    ) {
        int previousIndex = arg.index() - 1;
        String previousInput = suggestionContext.arguments().getOr(previousIndex, null);
        if (previousInput == null || !Patterns.isInputFlag(previousInput)) {
            return false;
        }

        Boolean scopedMatch = isValueFlagInScopes(flagScopes, previousInput);
        if (scopedMatch != null) {
            return scopedMatch;
        }
        Boolean commandMatch = isValueFlagInScopes(allCommandPathways(command), previousInput);
        return commandMatch != null && commandMatch;
    }

    private @Nullable Boolean isValueFlagInScopes(List<CommandPathway<S>> flagScopes, String previousInput) {
        for (CommandPathway<S> flagScope : flagScopes) {
            var flagData = flagScope.getFlagDataFromInput(previousInput);
            if (flagData != null) {
                return !flagData.isSwitch();
            }
        }
        return null;
    }

    private boolean hasFlagInputBeforeOrAtCompletion(SuggestionContext<S> suggestionContext, CompletionArg arg) {
        int maxIndex = Math.min(arg.index(), suggestionContext.arguments().size() - 1);
        for (int index = 0; index <= maxIndex; index++) {
            String input = suggestionContext.arguments().getOr(index, null);
            if (input != null && Patterns.isInputFlag(input)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldSuggestFlagNames(CompletionArg arg) {
        return arg.isEmpty() || arg.value().startsWith("-");
    }

    private int resolveSuggestionStart(String rawInput, CompletionArg arg) {
        if (arg.isEmpty()) {
            return rawInput.length();
        }
        return Math.max(0, rawInput.length() - arg.value().length());
    }

    private List<CommandPathway<S>> resolveFlagScopePathways(Command<S> rootCommand, Node<S> scopeNode) {
        List<CommandPathway<S>> scopes = new ArrayList<>();
        addPathwayScope(scopes, scopeNode.getOriginalPathway());

        Argument<S> main = scopeNode.getMainArgument();
        if (main.isCommand()) {
            Command<S> commandScope = main.asCommand();
            for (CommandPathway<S> pathway : commandScope.getDedicatedPathways()) {
                addPathwayScope(scopes, pathway);
            }
            addPathwayScope(scopes, commandScope.getDefaultPathway());
        }

        for (CommandPathway<S> pathway : rootPathwaysForCommandScope(rootCommand, commandChainForNode(scopeNode))) {
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

    private List<CommandPathway<S>> rootPathwaysForCommandScope(Command<S> rootCommand, List<String> commandChain) {
        List<CommandPathway<S>> rootPathways = new ArrayList<>();
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

    private List<CommandPathway<S>> allCommandPathways(Command<S> command) {
        List<CommandPathway<S>> pathways = new ArrayList<>();
        List<Command<S>> visitedCommands = new ArrayList<>();
        collectCommandPathways(command, visitedCommands, pathways);
        return pathways;
    }

    private void collectCommandPathways(
            Command<S> command,
            List<Command<S>> visitedCommands,
            List<CommandPathway<S>> pathways
    ) {
        for (Command<S> visited : visitedCommands) {
            if (visited == command) {
                return;
            }
        }
        visitedCommands.add(command);

        for (CommandPathway<S> pathway : command.getDedicatedPathways()) {
            addPathwayScope(pathways, pathway);
        }
        addPathwayScope(pathways, command.getDefaultPathway());

        for (Command<S> child : command.getSubCommands()) {
            collectCommandPathways(child, visitedCommands, pathways);
        }
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

    private String normalizeInput(String input) {
        while (input.startsWith("/")) {
            input = input.substring(1);
        }
        return input;
    }

    private String flagTunnelName(Node<S> scopeNode) {
        return "_imperat_flags_" + nodeDepth(scopeNode);
    }

    private int nodeDepth(Node<S> node) {
        int depth = 0;
        Node<S> current = node;
        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private void executor(ArgumentBuilder<?, ?> builder) {
        builder.executes((context) -> {
            String input = context.getInput();
            S sender = this.wrapCommandSource(context.getSource());
            dispatcher.execute(sender, input);
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }


    protected StringArgumentType getStringArgType(Argument<S> parameter) {
        if (parameter.isGreedy()) {
            return StringArgumentType.greedyString();
        } else {
            return StringArgumentType.string();
        }
    }

}
