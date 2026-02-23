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
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.command.tree.ArgumentNode;
import studio.mevera.imperat.command.tree.LiteralCommandNode;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

@SuppressWarnings("unchecked")
public abstract non-sealed class BaseBrigadierManager<S extends Source> implements BrigadierManager<S> {

    protected final Imperat<S> dispatcher;

    protected BaseBrigadierManager(Imperat<S> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public @NotNull <T> com.mojang.brigadier.tree.LiteralCommandNode<T> parseCommandIntoNode(@NotNull Command<S> command) {
        var tree = command.tree();
        var root = tree.uniqueVersionedTree();
        return this.<T>convertRoot(root).build();
    }

    @SuppressWarnings("unchecked")
    private <T> LiteralArgumentBuilder<T> convertRoot(LiteralCommandNode<S> root) {
        LiteralArgumentBuilder<T> builder = (LiteralArgumentBuilder<T>)
                  literal(root.getData().name())
                    .requires((obj) -> {
                        var source = wrapCommandSource(obj);
                        return root.getData().isIgnoringACPerms()
                                       || dispatcher.config().getPermissionChecker().hasPermission(source, root.getData());
                    });
        executor(builder);

        for (var child : root.getChildren()) {
            builder.then(convertNode(root, root, child));
        }
        return builder;
    }

    private <T> com.mojang.brigadier.tree.CommandNode<T> convertNode(
            LiteralCommandNode<S> root,
            studio.mevera.imperat.command.tree.CommandNode<?, ?> parent,
            studio.mevera.imperat.command.tree.CommandNode<S, ?> node
    ) {
        var argType = getArgumentType(node.getData());

        ArgumentBuilder<T, ?> childBuilder = node instanceof LiteralCommandNode<?> ?
                                                     LiteralArgumentBuilder.literal(node.getData().name())
                                                     : RequiredArgumentBuilder.argument(node.getData().name(), argType);

        childBuilder.requires((obj) -> {
            var permissionResolver = dispatcher.config().getPermissionChecker();
            var source = wrapCommandSource(obj);

            if (node instanceof LiteralCommandNode<?> literalCommandNode
                        && literalCommandNode.getData().isIgnoringACPerms()) {
                return true;
            }

            return (permissionResolver.hasPermission(source, node.getData()));
        });

        executor(childBuilder);
        if (!(node instanceof LiteralCommandNode<?>)) {
            ((RequiredArgumentBuilder<T, ?>) childBuilder).suggests(
                    createSuggestionProvider(root.getData(), node.getData())
            );
        }

        if (node.isTrueFlag()) {
            String name = node.getData().name() + "_value";
            ArgumentNode<S> flagValueNode =
                    studio.mevera.imperat.command.tree.CommandNode.createArgumentNode(
                            node,
                            Argument.required(name, dispatcher.config()
                                                                    .getArgumentType(node.getData().asFlagParameter().inputValueType()))
                                    .permission(node.getPermissionsData())
                                    .build(),
                            node.getDepth() + 1,
                            node.getExecutableUsage()
                    );

            for (var trueFlagChildren : node.getChildren()) {
                flagValueNode.addChild(trueFlagChildren);
            }

            childBuilder.then(convertNode(root, node, flagValueNode));
            return childBuilder.build();
        }

        for (var innerChild : node.getChildren()) {
            childBuilder.then(convertNode(root, node, innerChild));
        }

        return childBuilder.build();
    }


    private @NotNull <T> SuggestionProvider<T> createSuggestionProvider(
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
