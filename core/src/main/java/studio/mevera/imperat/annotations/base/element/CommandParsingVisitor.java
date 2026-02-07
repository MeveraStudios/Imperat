package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.ArgType;
import studio.mevera.imperat.annotations.Async;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Cooldown;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Format;
import studio.mevera.imperat.annotations.GlobalAttachmentMode;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.PostProcessor;
import studio.mevera.imperat.annotations.PreProcessor;
import studio.mevera.imperat.annotations.Range;
import studio.mevera.imperat.annotations.Shortcut;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.annotations.SuggestionProvider;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Validators;
import studio.mevera.imperat.annotations.Values;
import studio.mevera.imperat.annotations.base.AnnotationHelper;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.MethodCommandExecutor;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.annotations.parameters.AnnotationArgumentDecorator;
import studio.mevera.imperat.annotations.parameters.NumericArgumentDecorator;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.command.parameters.StrictParameterList;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.command.parameters.validator.ConstrainedValueValidator;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApiStatus.Internal
class CommandParsingVisitor<S extends Source> extends CommandClassVisitor<S, Set<studio.mevera.imperat.command.Command<S>>> {

    private final static String VALUES_SEPARATION_CHAR = "\\|";
    private final ImperatConfig<S> config;
    private final CommandClassVisitor<S, Set<MethodThrowableResolver<?, S>>> errorHandlersVisitor;

    CommandParsingVisitor(Imperat<S> imperat, AnnotationParser<S> parser, ElementSelector<MethodElement> methodSelector) {
        super(imperat, parser, methodSelector);
        this.config = imperat.config();
        this.errorHandlersVisitor = CommandClassVisitor.newThrowableParsingVisitor(imperat, parser);
    }

    private static <S extends Source> boolean doesRequireParameterInheritance(
            @Nullable studio.mevera.imperat.command.Command<S> parentCmd,
            @NotNull MethodElement method
    ) {
        boolean requiresParameterInheritance = false;

        SubCommand annotation = method.getAnnotation(SubCommand.class);
        if (annotation != null) {
            var attachment = annotation.attachment();
            if (parentCmd == null) {
                requiresParameterInheritance = attachment.requiresParameterInheritance();
            } else {
                if (attachment == AttachmentMode.DEFAULT) {
                    requiresParameterInheritance = parentCmd.getDefaultUsage().size() > 0;
                } else {
                    requiresParameterInheritance = (attachment == AttachmentMode.MAIN || attachment == AttachmentMode.UNSET);
                }
            }
        } else if (method.isAnnotationPresent(Execute.class)) {
            var ann = method.getParent().getAnnotation(SubCommand.class);
            if (ann != null) {
                requiresParameterInheritance = ann.attachment().requiresParameterInheritance();
            } else if (parentCmd != null) {
                requiresParameterInheritance = parentCmd.getMainUsage().getParameters().isEmpty();
            }
        }
        return requiresParameterInheritance;
    }

    private static <S extends Source> @NotNull StringBuilder getMainUsageParametersCollected(StrictParameterList<S> mainUsageParameters) {
        StringBuilder builder = new StringBuilder();
        for (var p : mainUsageParameters) {
            builder.append(p.format()).append(" ");
        }
        return builder;
    }

    private static <S extends Source> @NotNull LinkedList<studio.mevera.imperat.command.Command<S>> getParenteralSequence(
            @Nullable studio.mevera.imperat.command.Command<S> parentCmd) {
        studio.mevera.imperat.command.Command<S> currentParent = parentCmd;
        LinkedList<studio.mevera.imperat.command.Command<S>> parenteralSequence = new LinkedList<>();
        while (currentParent != null) {
            parenteralSequence.addFirst(currentParent);
            currentParent = currentParent.parent();
        }
        return parenteralSequence;
    }

    @Override
    public Set<studio.mevera.imperat.command.Command<S>> visitCommandClass(
            @NotNull ClassElement clazz
    ) {

        Set<studio.mevera.imperat.command.Command<S>> commands = new HashSet<>();

        Annotation commandAnnotation = getCommandAnnotation(clazz);
        if (clazz.isRootClass() && commandAnnotation != null && clazz.isAnnotationPresent(SubCommand.class)) {
            throw new IllegalStateException("Root command class cannot be a @SubCommand");
        }

        if (commandAnnotation != null) {

            if (clazz.isRootClass() && AnnotationHelper.isAbnormalClass(clazz)) {
                throw new IllegalArgumentException("Abnormal root class '%s'".formatted(clazz.getName()));
            }

            studio.mevera.imperat.command.Command<S> cmd = loadCommand(null, clazz, commandAnnotation);
            if (cmd != null) {
                loadCommandMethods(clazz);
                commands.add(cmd);
            }

        } else {
            //no annotation
            for (ParseElement<?> element : clazz.getChildren()) {
                Command elementCommandAnnotation = element.getAnnotation(Command.class);
                if (elementCommandAnnotation != null) {
                    var cmd = loadCommand(null, element, elementCommandAnnotation);
                    if (cmd != null) {
                        imperat.registerSimpleCommand(cmd);
                    }
                }
            }

        }

        return commands;
    }

    private Annotation getCommandAnnotation(ClassElement clazz) {
        Command commandAnnotation = clazz.getAnnotation(Command.class);
        if (commandAnnotation != null) {
            return commandAnnotation;
        }

        return clazz.getAnnotation(SubCommand.class);
    }

    private void loadCommandMethods(ClassElement clazz) {
        for (ParseElement<?> element : clazz.getChildren()) {
            if (element instanceof MethodElement method && method.isAnnotationPresent(Command.class)) {
                var cmdAnn = method.getAnnotation(Command.class);
                assert cmdAnn != null;
                imperat.registerSimpleCommand(loadCommand(null, method, cmdAnn));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends Throwable> studio.mevera.imperat.command.Command<S> loadCmdInstance(Annotation cmdAnnotation, ParseElement<?> element) {
        PreProcessor preProcessor = element.getAnnotation(PreProcessor.class);
        PostProcessor postProcessor = element.getAnnotation(PostProcessor.class);

        Permission[] permissions = element.getAnnotationsByType(Permission.class);
        studio.mevera.imperat.annotations.Description description = element.getAnnotation(studio.mevera.imperat.annotations.Description.class);

        //help provider for this command
        //Help help = element.getAnnotation(Help.class);

        studio.mevera.imperat.command.Command.Builder<S> builder;
        if (cmdAnnotation instanceof Command cmdAnn) {
            final String[] values = config.replacePlaceholders(cmdAnn.value());
            builder = addCommonCommandData(element, preProcessor, postProcessor, permissions, description, values, cmdAnn.skipSuggestionsChecks());


        } else if (cmdAnnotation instanceof SubCommand subCommand) {
            final String[] values = config.replacePlaceholders(subCommand.value());
            assert values != null;

            builder = addCommonCommandData(element, preProcessor, postProcessor, permissions, description, values, subCommand.skipSuggestionsChecks());


        } else {
            return null;
        }

        var cmd = builder.build();

        if (element instanceof ClassElement classElement) {
            var errorHandlersCollected = errorHandlersVisitor.visitCommandClass(classElement);
            if (errorHandlersCollected != null) {
                for (var errorHandler : errorHandlersCollected) {
                    cmd.setThrowableResolver((Class<E>) errorHandler.getExceptionType(), (MethodThrowableResolver<E, S>) errorHandler);
                }
            }
        }

        return cmd;
    }

    @NotNull
    private studio.mevera.imperat.command.Command.Builder<S> addCommonCommandData(
        ParseElement<?> element, PreProcessor preProcessor, PostProcessor postProcessor, Permission[] permissions,
        studio.mevera.imperat.annotations.Description description, String[] values, boolean ignoreAC
    ) {
        final List<String> aliases = List.of(values).subList(1, values.length);
        studio.mevera.imperat.command.Command.Builder<S> builder = studio.mevera.imperat.command.Command.create(imperat, values[0], element)
                          .ignoreACPermissions(ignoreAC)
                          .aliases(aliases);

        PermissionsData permissionsData = PermissionsData.empty();
        for (Permission pe : permissions) {
            String permLine = config.replacePlaceholders(pe.value());

            permissionsData.append(
                    PermissionsData.fromText(permLine)
            );

        }
        builder.permission(permissionsData);

        if (description != null) {
            builder.description(
                    config.replacePlaceholders(description.value())
            );
        }

        if (preProcessor != null) {
            for (var processor : preProcessor.value()) {
                builder.preProcessor(loadPreProcessorInstance(processor));
            }
        }

        if (postProcessor != null) {
            for (var processor : postProcessor.value()) {
                builder.postProcessor(loadPostProcessorInstance(processor));
            }
        }
        return builder;
    }

    private @Nullable studio.mevera.imperat.command.Command<S> loadCommand(
            @Nullable studio.mevera.imperat.command.Command<S> parentCmd,
            ParseElement<?> parseElement,
            @NotNull Annotation annotation
    ) {
        if (AnnotationHelper.isAbnormalClass(parseElement)) {
            //sub abnormal class
            //ignore
            return null;
        }

        final studio.mevera.imperat.command.Command<S> cmd = loadCmdInstance(annotation, parseElement);
        if (parentCmd != null && cmd != null) {
            cmd.parent(parentCmd);
        }

        if (parseElement instanceof MethodElement method && cmd != null) {

            //Loading @Command/@SubCommand on methods
            if (!methodSelector.canBeSelected(imperat, parser, method, true)) {
                ImperatDebugger.debugForTesting("Method '%s' has failed verification", method.getName());
                return cmd;
            }

            var usage = loadUsage(parentCmd, cmd, method);

            if (usage != null) {
                cmd.addUsage(usage);
            }

            return cmd;

        } else if (parseElement instanceof ClassElement commandClass) {
            //Loading @Command/@SubCommand on classes

            //load command class
            // IMPORTANT: Process @Usage methods FIRST to establish mainUsage,
            // then process @SubCommand methods so they can attach to the correct usage
            for (ParseElement<?> element : commandClass.getChildren()) {

                if (element instanceof MethodElement method) {
                    if (cmd == null) {
                        throw new IllegalStateException(
                                "Method  '" + method.getElement().getName() + "' Cannot be treated as usage/subcommand, it doesn't have a parent ");
                    }

                    if (!methodSelector.canBeSelected(imperat, parser, method, true)) {
                        return cmd;
                    }

                    // Process @Usage methods first (skip @SubCommand for now)
                    if (method.isAnnotationPresent(Execute.class) && !method.isAnnotationPresent(SubCommand.class)) {
                        var usage = loadUsage(parentCmd, cmd, method);
                        if (usage != null) {
                            cmd.addUsage(usage);
                        }
                    }

                }
            }

            // Second pass: Process @SubCommand methods AFTER all usages are registered
            for (ParseElement<?> element : commandClass.getChildren()) {

                if (element instanceof MethodElement method) {
                    if (cmd == null) {
                        throw new IllegalStateException(
                                "Method  '" + method.getElement().getName() + "' Cannot be treated as usage/subcommand, it doesn't have a parent ");
                    }

                    if (!methodSelector.canBeSelected(imperat, parser, method, true)) {
                        return cmd;
                    }

                    if (method.isAnnotationPresent(SubCommand.class)) {
                        var subAnn = method.getAnnotation(SubCommand.class);
                        assert subAnn != null;
                        cmd.addSubCommand(loadCommand(cmd, method, subAnn), extractAttachmentMode(commandClass, subAnn));
                    }


                } else if (element instanceof ClassElement innerClass) {

                    if (innerClass.isAnnotationPresent(Command.class)) {
                        //separate embedded command
                        var innerCmdAnn = innerClass.getAnnotation(Command.class);
                        assert innerCmdAnn != null;
                        imperat.registerSimpleCommand(
                                loadCommand(null, innerClass, innerCmdAnn)
                        );
                        return null;
                    } else if (innerClass.isAnnotationPresent(SubCommand.class)) {
                        if (cmd == null) {
                            throw new IllegalStateException("Inner class '" + innerClass.getElement().getSimpleName()
                                                                    + "' Cannot be  treated as subcommand, it doesn't have a parent ");
                        }
                        SubCommand subCommandAnn = innerClass.getAnnotation(SubCommand.class);
                        assert subCommandAnn != null;

                        cmd.addSubCommand(
                                loadCommand(cmd, innerClass, subCommandAnn), extractAttachmentMode(commandClass, subCommandAnn)
                        );
                    }

                }


            }

        }

        return cmd;
    }

    private AttachmentMode extractAttachmentMode(ClassElement commandClass, SubCommand subCommandAnn) {
        AttachmentMode attachmentMode =
                config.getDefaultAttachmentMode() == AttachmentMode.UNSET ? subCommandAnn.attachment() : config.getDefaultAttachmentMode();
        GlobalAttachmentMode globalAttachmentMode = commandClass.getAnnotation(GlobalAttachmentMode.class);
        if (globalAttachmentMode != null && attachmentMode == AttachmentMode.UNSET) {
            attachmentMode = globalAttachmentMode.value();
        }
        return attachmentMode;
    }

    @SuppressWarnings("unchecked")
    private CommandPreProcessor<S> loadPreProcessorInstance(Class<? extends CommandPreProcessor<?>> clazz) {
        return (CommandPreProcessor<S>) config.getInstanceFactory().createInstance(config, clazz);
    }

    @SuppressWarnings("unchecked")
    private CommandPostProcessor<S> loadPostProcessorInstance(Class<? extends CommandPostProcessor<?>> clazz) {
        return (CommandPostProcessor<S>) config.getInstanceFactory().createInstance(config, clazz);
    }

    protected CommandUsage<S> loadUsage(
            @Nullable studio.mevera.imperat.command.Command<S> parentCmd,
            @NotNull studio.mevera.imperat.command.Command<S> loadedCmd,
            MethodElement method
    ) {

        MethodUsageData<S> usageData = loadParameters(method, parentCmd);
        var execution = MethodCommandExecutor.of(imperat, method, usageData.inheritedTotalParameters());

        studio.mevera.imperat.annotations.Description description = method.getAnnotation(studio.mevera.imperat.annotations.Description.class);
        Permission[] permissions = method.getAnnotationsByType(Permission.class);
        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        Async async = method.getAnnotation(Async.class);

        var builder = CommandUsage.<S>builder()
                              .parameters(usageData.personalParameters())
                              .execute(execution);

        Execute executeAnn = method.getAnnotation(Execute.class);

        if (executeAnn != null) {
            String[] examples = Arrays.stream(executeAnn.examples())
                                        .map(config::replacePlaceholders)
                                        .toArray(String[]::new);
            builder.examples(examples);
        }

        if (description != null) {
            builder.description(
                    Description.of(config.replacePlaceholders(description.value()))
            );
        }

        PermissionsData permissionsData = PermissionsData.empty();
        for (Permission pe : permissions) {
            String permLine = config.replacePlaceholders(pe.value());
            permissionsData.append(
                    PermissionsData.fromText(permLine)
            );
        }

        builder.permission(
                permissionsData
        );

        if (cooldown != null) {
            ImperatDebugger.debug("Method '%s' has cooldown", method.getName());
            String cooldownPerm = cooldown.permission();
            builder.cooldown(cooldown.value(), cooldown.unit(), cooldownPerm.isEmpty() ? null : cooldownPerm);
        }

        if (async != null) {
            builder.coordinator(CommandCoordinator.async());
        }
        boolean help = method.isHelp();
        var usage = builder
                       //.registerFlags(usageData.freeFlags)
                       .build(loadedCmd, help);

        Shortcut shortcutAnn = method.getAnnotation(Shortcut.class);
        if(shortcutAnn != null) {
            if(shortcutAnn.value().isEmpty() ) {
                throw new IllegalStateException("Shortcut value cannot be empty for method '" + method.getName() + "'");
            }

            if(shortcutAnn.value().contains(" ")) {
                throw new IllegalStateException("Shortcut value cannot contain spaces for method '" + method.getName() + "'");
            }

            var shortcut = loadUsageShortcut(method, usageData, loadedCmd, usage, shortcutAnn);
            loadedCmd.addShortcut(shortcut);
        }

        return usage;
    }

    protected MethodUsageData<S> loadParameters(
            @NotNull MethodElement method,
            @Nullable studio.mevera.imperat.command.Command<S> parentCmd
    ) {

        ImperatDebugger.debugForTesting("Loading for method '%s'", method.getName());
        LinkedList<Argument<S>> personalMethodInputParameters = new LinkedList<>();

        final StrictParameterList<S> mainUsageParameters = new StrictParameterList<>();

        boolean doesRequireParameterInheritance = doesRequireParameterInheritance(parentCmd, method);
        ImperatDebugger.debug("Method '%s' Requires inheritance= " + doesRequireParameterInheritance, method.getName());

        if (doesRequireParameterInheritance(parentCmd, method)) {
            LinkedList<studio.mevera.imperat.command.Command<S>> parenteralSequence = getParenteralSequence(parentCmd);
            for (studio.mevera.imperat.command.Command<S> parent : parenteralSequence) {
                parent.getMainUsage().getParameters()
                        .forEach((param) -> {
                            if (!param.isFlag()) {
                                mainUsageParameters.add(param);
                            }
                        });
            }

        }

        var inheritedParamsFormatted = getMainUsageParametersCollected(mainUsageParameters);
        ImperatDebugger.debugForTesting("Main usage params collected '%s'", inheritedParamsFormatted.toString());

        LinkedList<Argument<S>> totalMethodParameters = new LinkedList<>(mainUsageParameters);
        LinkedList<ParameterElement> originalMethodParameters = new LinkedList<>(method.getParameters());

        ParameterElement senderParam = null;

        if (doesRequireParameterInheritance && originalMethodParameters.size() - 1 == 0 && !mainUsageParameters.isEmpty() && parentCmd != null) {
            throw new IllegalStateException(
                    "You have inherited parameters ('%s') that are not declared in the method '%s' in class '%s'".formatted(inheritedParamsFormatted,
                            method.getName(), method.getParent().getName()));
        }

        while (!originalMethodParameters.isEmpty()) {

            ParameterElement parameterElement = originalMethodParameters.peek();
            if (parameterElement == null) {
                break;
            }
            //Type type = parameterElement.getElement().getParameterizedType();
            if (senderParam == null && isSenderParameter(parameterElement)) {
                senderParam = originalMethodParameters.remove();
                continue;
            }

            Argument<S> Argument = loadParameter(parameterElement);
            if (Argument == null) {
                originalMethodParameters.remove();
                continue;
            }

            Argument<S> mainParameter = mainUsageParameters.peek();

            if (mainParameter == null) {
                personalMethodInputParameters.add(Argument);
                totalMethodParameters.add(Argument);
                originalMethodParameters.remove();
                continue;
            }

            if (mainParameter.similarTo(Argument)) {
                var methodParam = originalMethodParameters.remove();
                ImperatDebugger.debugForTesting("Removing '%s' from method params", methodParam.getName());
                var mainUsageParam = mainUsageParameters.remove();
                ImperatDebugger.debugForTesting("Removing '%s' from main usage params", mainUsageParam.format());
                continue;
            }

            personalMethodInputParameters.add(Argument);
            totalMethodParameters.add(Argument);

            mainUsageParameters.remove();
            originalMethodParameters.remove();
        }
        return new MethodUsageData<>(personalMethodInputParameters, totalMethodParameters);
    }

    private boolean isSenderParameter(ParameterElement parameter) {
        Type type = parameter.getElement().getParameterizedType();
        return imperat.canBeSender(type) || config.hasSourceResolver(type);
    }

    protected studio.mevera.imperat.command.Command<S> loadUsageShortcut(
            @NotNull MethodElement method,
            @NotNull MethodUsageData<S> methodUsageData,
            @NotNull studio.mevera.imperat.command.Command<S> originalCommand,
            @NotNull CommandUsage<S> originalUsage,
            @NotNull Shortcut shortcutAnn
    ) {

        String shortcutValue = config.replacePlaceholders(shortcutAnn.value());

        studio.mevera.imperat.command.Command<S> shortcut = originalCommand.getShortcut(shortcutValue);
        if(shortcut == null) {
            shortcut = studio.mevera.imperat.command.Command.create(imperat, shortcutValue, method)
                                                                   .setMetaPropertiesFromOtherCommand(originalCommand)
                                                                   .build();
        }

        CommandUsage<S> fabricated = CommandUsage.<S>builder()
                                           .parameters(methodUsageData.inheritedTotalParameters())
                                           .execute(originalUsage.getExecution())
                                           .permission(originalUsage.getPermissionsData())
                                           .description(originalUsage.getDescription())
                                           .build(shortcut, originalUsage.isHelp());

        shortcut.addUsage(fabricated);
        return shortcut;
    }


    @SuppressWarnings("unchecked")
    protected  <T> @Nullable Argument<S> loadParameter(
            @NotNull ParameterElement parameter
    ) {

        //Parameter parameter = element.getElement();

        if (parameter.isContextResolved()) {
            return null;
        }

        Flag flag = parameter.getAnnotation(Flag.class);
        Switch switchAnnotation = parameter.getAnnotation(Switch.class);

        if (flag != null && switchAnnotation != null) {
            throw new IllegalStateException("both @Flag and @Switch at the same time !");
        }

        ArgumentType<S, T> type;
        ArgType argTypeAnn = parameter.getAnnotation(ArgType.class);
        if(argTypeAnn != null && (flag != null || switchAnnotation != null)) {
            throw new IllegalStateException("@ArgType cannot be used on flag/switch parameters");
        }
        else if(argTypeAnn != null) {
            type = (ArgumentType<S, T>) config.getInstanceFactory().createInstance(config, argTypeAnn.value());
        }else {
            TypeWrap<T> argumentTypeWrap = (TypeWrap<T>) TypeWrap.of(parameter.getElement().getParameterizedType());
            type = (ArgumentType<S, T>) config.getArgumentType(argumentTypeWrap.getType());
            if (type == null) {
                throw new IllegalArgumentException("Unknown type detected '" + argumentTypeWrap.getType().getTypeName() + "'");
            }
        }

        String name = parameter.getName();
        boolean optional = parameter.isOptional();

        //reading suggestion annotation
        //element.debug();

        Suggest suggestAnnotation = parameter.getAnnotation(Suggest.class);
        SuggestionProvider suggestionProvider = parameter.getAnnotation(SuggestionProvider.class);

        SuggestionResolver<S> suggestionResolver = null;

        if (suggestAnnotation != null) {
            suggestionResolver = SuggestionResolver.staticSuggestions(
                    config.replacePlaceholders(suggestAnnotation.value())
            );
        } else if (suggestionProvider != null) {
            String suggestionResolverName = config.replacePlaceholders(suggestionProvider.value().toLowerCase());
            var namedResolver = config.getNamedSuggestionResolver(
                    suggestionResolverName
            );
            if (namedResolver != null) {
                suggestionResolver = namedResolver;
            } else {
                throw new IllegalStateException("Unregistered named suggestion resolver : " + suggestionResolverName);
            }
        }

        boolean greedy = parameter.getAnnotation(Greedy.class) != null;
        boolean allowsGreedy =
                parameter.getType() == String.class || (TypeUtility.isAcceptableGreedyWrapper(parameter.getType()) && TypeUtility.hasGenericType(
                        parameter.getType(), String.class));
        if (greedy && !allowsGreedy) {
            throw new IllegalArgumentException(
                    "Argument '" + parameter.getName() + "' is greedy while having a non-greedy valueType '" + parameter.getType().getTypeName()
                            + "'");
        }

        Description desc = Description.EMPTY;
        if (parameter.isAnnotationPresent(studio.mevera.imperat.annotations.Description.class)) {
            var descAnn = parameter.getAnnotation(studio.mevera.imperat.annotations.Description.class);
            assert descAnn != null;
            desc = Description.of(
                    config.replacePlaceholders(descAnn.value())
            );
        }

        Permission[] perms = parameter.getAnnotationsByType(Permission.class);
        PermissionsData permissionsData = PermissionsData.empty();
        for (Permission peAnn : perms) {
            String txt = config.replacePlaceholders(peAnn.value());
            permissionsData.append(
                    PermissionsData.fromText(txt)
            );
        }

        OptionalValueSupplier optionalValueSupplier = OptionalValueSupplier.empty();
        if (optional) {
            Default defaultAnnotation = parameter.getAnnotation(Default.class);
            DefaultProvider provider = parameter.getAnnotation(DefaultProvider.class);
            try {
                optionalValueSupplier = AnnotationHelper.deduceOptionalValueSupplier(parameter, defaultAnnotation, provider, optionalValueSupplier);
            } catch (CommandException e) {
                ImperatDebugger.error(AnnotationHelper.class, "deduceOptionalValueSupplier", e);
            }
        }

        if (flag != null) {
            String[] flagAliases = flag.value();
            if (suggestAnnotation != null) {
                suggestionResolver = SuggestionResolver.staticSuggestions(config.replacePlaceholders(suggestAnnotation.value()));
            }

            return AnnotationArgumentDecorator.decorate(
                    Argument.flag(name, type)
                            .suggestForInputValue(suggestionResolver)
                            .aliases(getAllExceptFirst(flagAliases))
                            .flagDefaultInputValue(optionalValueSupplier)
                            .description(desc)
                            .permission(permissionsData)
                            .build(),
                    parameter
            );
        } else if (switchAnnotation != null) {
            String[] switchAliases = switchAnnotation.value();
            return AnnotationArgumentDecorator.decorate(
                    Argument.<S>flagSwitch(name)
                            .aliases(getAllExceptFirst(switchAliases))
                            .description(desc)
                            .permission(permissionsData)
                            .build(),
                    parameter
            );
        }

        List<ArgValidator<S>> validators = new ArrayList<>();

        if (parameter.isAnnotationPresent(Validators.class)) {
            Validators validatorsAnn = parameter.getAnnotation(Validators.class);
            assert validatorsAnn != null;
            for (Class<? extends ArgValidator<?>> validatorClass : validatorsAnn.value()) {
                ArgValidator<S> validatorInstance = (ArgValidator<S>) config.getInstanceFactory().createInstance(config, validatorClass);
                validators.add(validatorInstance);
            }
        }

        if (parameter.isAnnotationPresent(Values.class)) {
            Values valuesAnnotation = parameter.getAnnotation(Values.class);
            assert valuesAnnotation != null;

            Set<String> values = Arrays.stream(valuesAnnotation.value())
                                         .distinct()
                                         .map(config::replacePlaceholders)
                                         .flatMap(replaced -> {
                                             if (replaced.contains("|")) {
                                                 return Arrays.stream(replaced.split(VALUES_SEPARATION_CHAR));
                                             } else {
                                                 return Stream.of(replaced);
                                             }
                                         })
                                         .collect(Collectors.toCollection(LinkedHashSet::new));

            validators.add(new ConstrainedValueValidator<>(values, valuesAnnotation.caseSensitive()));
        }

        Argument<S> delegate = Argument.of(
                name, type, permissionsData, desc,
                optional, greedy, optionalValueSupplier, suggestionResolver,
                validators
        );

        if (parameter.isAnnotationPresent(Format.class)) {
            Format formatAnnotation = parameter.getAnnotation(Format.class);
            assert formatAnnotation != null;
            delegate.setFormat(config.replacePlaceholders(formatAnnotation.value()));
        }

        Argument<S> param = AnnotationArgumentDecorator.decorate(delegate, parameter);

        if (TypeUtility.isNumericType(TypeWrap.of(param.valueType()))
                    && parameter.isAnnotationPresent(Range.class)) {
            Range range = parameter.getAnnotation(Range.class);
            assert range != null;
            param = NumericArgumentDecorator.decorate(
                    param, NumericRange.of(range.min(), range.max())
            );
        }


        return param;
    }

    private List<String> getAllExceptFirst(String[] array) {
        List<String> flagAliases = new ArrayList<>(array.length - 1);
        flagAliases.addAll(List.of(array).subList(1, array.length));
        return flagAliases;
    }

    record MethodUsageData<S extends Source>(
            List<Argument<S>> personalParameters,
            List<Argument<S>> inheritedTotalParameters
    ) {

    }
}
