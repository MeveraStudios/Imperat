package dev.velix.imperat;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.Description;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.FlagParameter;
import dev.velix.imperat.command.suggestions.CompletionArg;
import dev.velix.imperat.command.tree.CommandNode;
import dev.velix.imperat.command.tree.ParameterNode;
import dev.velix.imperat.context.ArgumentQueue;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.SuggestionContext;
import dev.velix.imperat.util.TypeUtility;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

@SuppressWarnings("unchecked")
public abstract non-sealed class BaseBrigadierManager<S extends Source> implements BrigadierManager<S> {

    protected final Imperat<S> dispatcher;
    protected final List<ArgumentTypeResolver> resolvers = new ArrayList<>();

    protected BaseBrigadierManager(Imperat<S> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public @NotNull <T> LiteralCommandNode<T> parseCommandIntoNode(@NotNull Command<S> command) {
        var tree = command.tree();
        var root = tree.getRoot();
        return this.<T>convertRoot(root).build();
    }

    @SuppressWarnings("unchecked")
    private <T> LiteralArgumentBuilder<T> convertRoot(CommandNode<S> root) {
        LiteralArgumentBuilder<T> builder = (LiteralArgumentBuilder<T>) literal(root.getData().name())
            .requires((obj) -> {
                var source = wrapCommandSource(obj);
                return root.getData().isIgnoringACPerms()
                    || dispatcher.getPermissionResolver().hasPermission(source, root.getData().permission());
            });
        executor(builder);

        for (var child : root.getChildren()) {
            builder.then(convertNode(root, root, child));
        }
        return builder;
    }

    private <T> com.mojang.brigadier.tree.CommandNode<T> convertNode(CommandNode<S> root, ParameterNode<?, ?> parent, ParameterNode<S, ?> node) {

        ArgumentBuilder<T, ?> childBuilder = node instanceof CommandNode<?> ?
            LiteralArgumentBuilder.literal(node.getData().name())
            : RequiredArgumentBuilder.argument(node.getData().name(), getArgumentType(node.getData()));

        childBuilder.requires((obj) -> {
            var permissionResolver = dispatcher.getPermissionResolver();
            var source = wrapCommandSource(obj);

            boolean isIgnoringAC = root.getData().isIgnoringACPerms();
            if (parent != root && parent instanceof CommandNode<?> parentCmdNode) {
                isIgnoringAC = isIgnoringAC && parentCmdNode.getData().isIgnoringACPerms();
            }
            if (node instanceof CommandNode<?> commandNode) {
                isIgnoringAC = isIgnoringAC && commandNode.getData().isIgnoringACPerms();
            }
            if (isIgnoringAC) {
                return true;
            }
            boolean hasParentPerm = permissionResolver.hasPermission(source, parent.getData().permission());
            boolean hasNodePerm = permissionResolver.hasPermission(source, node.getData().permission());

            return (hasParentPerm && hasNodePerm);
        });

        executor(childBuilder);
        if (!(node instanceof CommandNode<?>)) {
            ((RequiredArgumentBuilder<T, ?>) childBuilder).suggests(
                createSuggestionProvider(root.getData(), node.getData())
            );
        }

        for (var innerChild : node.getChildren()) {
            childBuilder.then(convertNode(root, node, innerChild));
        }

        return childBuilder.build();
    }


    private @NotNull <T> SuggestionProvider<T> createSuggestionProvider(
        Command<S> command,
        CommandParameter<S> parameter
    ) {

        //ImperatDebugger.debug("suggestion resolver is null=%s for param '%s'", parameterResolver == null, parameter.format());

        return (context, builder) -> {
            S source = this.wrapCommandSource(context.getSource());
            String paramFormat = parameter.format();
            String desc = parameter.description() != Description.EMPTY ? parameter.description().toString() : "";
            Message tooltip = new LiteralMessage(paramFormat + (desc.isEmpty() ? "" : " - " + desc));

            String input = context.getInput();
            boolean hadExtraSpace = Character.isWhitespace(input.charAt(input.length() - 1));

            //ImperatDebugger.debug("input= '%s', last-space='%s'", input, hadExtraSpace);

            String[] processed = processedInput(input);
            //ImperatDebugger.debug("processed array= '%s'", Arrays.toString(processed));

            ArgumentQueue args = ArgumentQueue.parseAutoCompletion(processed, hadExtraSpace);
            //ImperatDebugger.debug("Parsed queue= '%s'",args.join(":"));
            //ImperatDebugger.debug("Last-argument='%s'", args.getLast());

            CompletionArg arg = new CompletionArg(args.getLast(), args.size() - 1);
            SuggestionContext<S> ctx = dispatcher.getContextFactory().createSuggestionContext(source, command, args, arg);

            return dispatcher.getParameterSuggestionResolver(parameter).asyncAutoComplete(ctx, parameter)
                .thenCompose((results) -> {
                    results
                        .stream()
                        .filter(c -> arg.isEmpty() || c.toLowerCase().startsWith(arg.value().toLowerCase()))
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach((res) -> builder.suggest(res, tooltip));
                    return builder.buildFuture();
                });
        };
    }

    private void executor(ArgumentBuilder<?, ?> builder) {
        builder.executes((context) -> {
            String input = context.getInput();
            S sender = this.wrapCommandSource(context.getSource());
            dispatcher.dispatch(sender, input);
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        });
    }

    private String[] processedInput(final String input) {
        String result = input;
        if (result.charAt(0) == '/')
            result = result.substring(1);

        String[] split = result.split(" ");
        String[] argumentsOnly = new String[split.length - 1];
        System.arraycopy(split, 1, argumentsOnly, 0, split.length - 1);

        return argumentsOnly;
    }

    //resolvers methods

    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerArgumentResolver(
        Class<T> type,
        ArgumentTypeResolver argumentTypeResolver
    ) {
        resolvers.add((param) -> {
            if (param.isFlag()) {

                FlagParameter<S> flagParameter = (FlagParameter<S>) param.asFlagParameter();
                if (flagParameter.isSwitch()) {
                    return argumentTypeResolver.resolveArgType(flagParameter);
                }

                return param.valueType() == flagParameter.flagData().inputType()
                    ? argumentTypeResolver.resolveArgType(param) : null;
            }
            return TypeUtility.matches(param.valueType(), type) ? argumentTypeResolver.resolveArgType(param) : null;
        });
    }

    @Override
    public void registerArgumentResolver(ArgumentTypeResolver argumentTypeResolver) {
        resolvers.add(argumentTypeResolver);
    }

    @Override
    public @NotNull ArgumentType<?> getArgumentType(CommandParameter<S> parameter) {
        for (var resolver : resolvers) {
            var resolved = resolver.resolveArgType(parameter);
            if (resolved != null)
                return resolved;
        }
        return getStringArgType(parameter);
    }


    private StringArgumentType getStringArgType(CommandParameter<S> parameter) {
        if (parameter.isGreedy()) return StringArgumentType.greedyString();
        else return StringArgumentType.string();
    }


}
