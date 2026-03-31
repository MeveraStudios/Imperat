package studio.mevera.imperat;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.command.tree.ArgumentNode;
import studio.mevera.imperat.command.tree.LiteralCommandNode;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;

@SuppressWarnings("unchecked")
public abstract non-sealed class BaseBrigadierManager<S extends CommandSource> implements BrigadierManager<S> {

    protected final Imperat<S> dispatcher;

    protected BaseBrigadierManager(Imperat<S> dispatcher) {
        this.dispatcher = dispatcher;
    }

    private static <BS> com.mojang.brigadier.tree.LiteralCommandNode<BS> cloneWithDiffName(
            com.mojang.brigadier.tree.LiteralCommandNode<BS> brigOriginalNode,
            String newName
    ) {
        var clone = new com.mojang.brigadier.tree.LiteralCommandNode<>(newName,
                brigOriginalNode.getCommand(), brigOriginalNode.getRequirement(), brigOriginalNode.getRedirect(),
                brigOriginalNode.getRedirectModifier(), brigOriginalNode.isFork());
        for (var child : brigOriginalNode.getChildren()) {
            clone.addChild(child);
        }
        return clone;
    }

    @Override
    public @NotNull <BS> com.mojang.brigadier.tree.LiteralCommandNode<BS> parseCommandIntoNode(@NotNull Command<S> command) {
        var tree = command.tree();
        var root = tree.rootNode();
        return this.<BS>convertRoot(root).build();
    }

    @SuppressWarnings("unchecked")
    private <BS> LiteralArgumentBuilder<BS> convertRoot(LiteralCommandNode<S> root) {
        LiteralArgumentBuilder<BS> builder = (LiteralArgumentBuilder<BS>)
                                                    literal(root.getData().getName())
                    .requires((obj) -> {
                        var source = wrapCommandSource(obj);
                        return root.getData().isIgnoringACPerms()
                                       || dispatcher.config().getPermissionChecker().hasPermission(source, root.getData());
                    });
        executor(builder);

        for (var child : root.getChildren()) {

            var innerChildBrigNode = this.<BS>convertImperatNodeToBrigadierNode(root, child);
            builder.then(innerChildBrigNode);

            if (child instanceof LiteralCommandNode) {
                //adding aliases for the literal child
                LiteralCommandNode<S> literalImperatCommandNode = (LiteralCommandNode<S>) child;
                //child is sub-command/literal, check if that literal (sub-cmd) has aliases,
                for (var alias : literalImperatCommandNode.getData().aliases()) {
                    com.mojang.brigadier.tree.LiteralCommandNode<BS>
                            aliasBrigNode = cloneWithDiffName((com.mojang.brigadier.tree.LiteralCommandNode<BS>) innerChildBrigNode, alias);
                    builder.then(aliasBrigNode);
                }

            }
        }
        return builder;
    }

    protected <BS> com.mojang.brigadier.tree.CommandNode<BS> convertImperatNodeToBrigadierNode(
            LiteralCommandNode<S> rootImperatNode,
            studio.mevera.imperat.command.tree.CommandNode<S, ?> currentImperatNode
    ) {
        var argType = getArgumentType(currentImperatNode.getData());

        ArgumentBuilder<BS, ?> childBuilder = currentImperatNode instanceof LiteralCommandNode<?> ?
                                                      LiteralArgumentBuilder.literal(currentImperatNode.getData().getName())
                                                      : RequiredArgumentBuilder.argument(currentImperatNode.getData().getName(), argType);

        childBuilder.requires((obj) -> {
            var permissionResolver = dispatcher.config().getPermissionChecker();
            var source = wrapCommandSource(obj);

            if (currentImperatNode instanceof LiteralCommandNode<?> literalCommandNode
                        && literalCommandNode.getData().isIgnoringACPerms()) {
                return true;
            }

            return (permissionResolver.hasPermission(source, currentImperatNode.getData()));
        });

        executor(childBuilder);
        if (!(currentImperatNode instanceof LiteralCommandNode<?>)) {
            ((RequiredArgumentBuilder<BS, ?>) childBuilder).suggests(
                    createSuggestionProvider(rootImperatNode.getData(), currentImperatNode.getData())
            );
        }

        if (currentImperatNode.isTrueFlag()) {
            String name = currentImperatNode.getData().getName() + "_value";
            ArgumentNode<S> flagValueNode =
                    studio.mevera.imperat.command.tree.CommandNode.createArgumentNode(
                            currentImperatNode,
                            Argument.required(name, dispatcher.config()
                                                            .getArgumentType(currentImperatNode.getData().asFlagParameter().inputValueType()))
                                    .permission(currentImperatNode.getPermissionsData())
                                    .build(),
                            currentImperatNode.getDepth() + 1,
                            currentImperatNode.getExecutableUsage()
                    );

            for (var trueFlagChildren : currentImperatNode.getChildren()) {
                flagValueNode.addChild(trueFlagChildren);
            }

            childBuilder.then(convertImperatNodeToBrigadierNode(rootImperatNode, flagValueNode));
            return childBuilder.build();
        }

        var builtCurrentNode = childBuilder.build();

        for (var innerChild : currentImperatNode.getChildren()) {
            var innerChildBrigNode = this.<BS>convertImperatNodeToBrigadierNode(rootImperatNode, innerChild);
            builtCurrentNode.addChild(innerChildBrigNode);

            if (innerChild instanceof LiteralCommandNode) {
                //adding aliases for the literal child
                LiteralCommandNode<S> literalImperatCommandNode = (LiteralCommandNode<S>) innerChild;
                //child is sub-command/literal, check if that literal (sub-cmd) has aliases,
                for (var alias : literalImperatCommandNode.getData().aliases()) {
                    com.mojang.brigadier.tree.LiteralCommandNode<BS>
                            aliasBrigNode = cloneWithDiffName((com.mojang.brigadier.tree.LiteralCommandNode<BS>) innerChildBrigNode, alias);
                    builtCurrentNode.addChild(aliasBrigNode);
                }

            }
        }


        return builtCurrentNode;
    }

    private @NotNull <BS> SuggestionProvider<BS> createSuggestionProvider(
            Command<S> command,
            Argument<S> parameter
    ) {
        return (context, builder) -> {
            S source = this.wrapCommandSource(context.getSource());
            String paramFormat = parameter.format();
            Description desc = parameter.getDescription();
            Message tooltip = new LiteralMessage(paramFormat + (desc.isEmpty() ? "" : " - " + desc.getValue()));

            String input = context.getInput();
            while (input.startsWith("/")) {
                input = input.substring(1);
            }

            int firstSpaceIndex = input.indexOf(' ');
            String label = input.substring(0, firstSpaceIndex);

            String argsInput = input.substring(firstSpaceIndex);
            ArgumentInput args = ArgumentInput.parseAutoCompletion(argsInput, false);

            SuggestionContext<S> ctx = dispatcher.config().getContextFactory().createSuggestionContext(dispatcher, source, command, label, args);
            CompletionArg arg = ctx.getArgToComplete();

            return dispatcher.config().getParameterSuggestionResolver(parameter).provideAsynchronously(ctx, parameter)
                           .thenCompose((results) -> {
                               results
                                       .stream()
                                       .filter((c) -> arg.isEmpty() || c.toLowerCase().startsWith(arg.value().toLowerCase()))
                                       .forEachOrdered((res) -> builder.suggest(res, tooltip));
                               return builder.buildFuture();
                           });
        };
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
