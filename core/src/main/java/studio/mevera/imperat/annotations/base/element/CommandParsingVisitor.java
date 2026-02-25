package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.ArgType;
import studio.mevera.imperat.annotations.Async;
import studio.mevera.imperat.annotations.Cooldown;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Format;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.InheritedArg;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.PostProcessor;
import studio.mevera.imperat.annotations.PreProcessor;
import studio.mevera.imperat.annotations.Range;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.Shortcut;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Validators;
import studio.mevera.imperat.annotations.Values;
import studio.mevera.imperat.annotations.base.AnnotationHelper;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.MethodCommandExecutor;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.annotations.parameters.AnnotationArgumentDecorator;
import studio.mevera.imperat.annotations.parameters.NumericArgumentDecorator;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.command.parameters.validator.ConstrainedValueValidator;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private static <S extends Source> @NotNull LinkedList<studio.mevera.imperat.command.Command<S>> getParenteralSequence
            (@Nullable studio.mevera.imperat.command.Command<S> parentCommand) {

        studio.mevera.imperat.command.Command<S> currentParent = parentCommand;
        LinkedList<studio.mevera.imperat.command.Command<S>> parenteralSequence = new LinkedList<>();
        while (currentParent != null) {
            parenteralSequence.addFirst(currentParent);
            currentParent = currentParent.getParent();
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
                RootCommand elementRootCommandAnnotation = element.getAnnotation(RootCommand.class);
                if (elementRootCommandAnnotation != null) {
                    var cmd = loadCommand(null, element, elementRootCommandAnnotation);
                    if (cmd != null) {
                        imperat.registerSimpleCommand(cmd);
                    }
                }
            }

        }

        return commands;
    }

    private Annotation getCommandAnnotation(ClassElement clazz) {
        RootCommand rootCommandAnnotation = clazz.getAnnotation(RootCommand.class);
        if (rootCommandAnnotation != null) {
            return rootCommandAnnotation;
        }

        return clazz.getAnnotation(SubCommand.class);
    }

    private void loadCommandMethods(ClassElement clazz) {
        for (ParseElement<?> element : clazz.getChildren()) {
            if (element instanceof MethodElement method && method.isAnnotationPresent(RootCommand.class)) {
                var cmdAnn = method.getAnnotation(RootCommand.class);
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
        if (cmdAnnotation instanceof RootCommand cmdAnn) {
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
            cmd.setParent(parentCmd);
        }

        if (parseElement instanceof MethodElement method && cmd != null) {

            //Loading @RootCommand/@SubCommand on methods
            if (!methodSelector.canBeSelected(imperat, parser, method, true)) {
                ImperatDebugger.debugForTesting("Method '%s' has failed verification", method.getName());
                return cmd;
            }

            var pathway = loadPathway(parentCmd, cmd, method);

            if (pathway != null) {
                System.out.println("Loaded pathway for method '" + method.getName() + "' with parameters " + pathway.formatted());
                cmd.addPathway(pathway);
            }

            return cmd;

        } else if (parseElement instanceof ClassElement commandClass) {
            //Loading @RootCommand/@SubCommand on classes

            //load command class
            // IMPORTANT: Process @Execute methods FIRST to establish mainUsage,
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

                    // Process @Execute methods first (skip @SubCommand for now)
                    if (method.isAnnotationPresent(Execute.class) && !method.isAnnotationPresent(SubCommand.class)) {

                        var pathway = loadPathway(parentCmd, cmd, method);
                        if (pathway != null) {
                            cmd.addPathway(pathway);
                        }
                        System.out.println("XXXX- METHOD ELEMENT FOR PATHWAYS OF RootCommand '" + cmd.getName() + "' -XXXX");
                        for (var path : cmd.getDedicatedPathways()) {
                            System.out.println(" - " + path.formatted() + " :: " + (path.getMethodElement() == null ? "NO METHOD ELEMENT" :
                                                                                            path.getMethodElement().getName()));
                        }
                    }

                }
            }

            // Second pass: Process @SubCommand methods AFTER all usages are registered
            for (ParseElement<?> element : commandClass.getChildren()) {

                if (element instanceof MethodElement method) {
                    if (cmd == null) {
                        throw new IllegalStateException(
                                "Method '" + method.getElement().getName() + "' Cannot be treated as usage/subcommand, it doesn't have a parent ");
                    }

                    if (!methodSelector.canBeSelected(imperat, parser, method, true)) {
                        return cmd;
                    }

                    if (method.isAnnotationPresent(SubCommand.class)) {
                        var subAnn = method.getAnnotation(SubCommand.class);
                        assert subAnn != null;
                        cmd.addSubCommand(loadCommand(cmd, method, subAnn));
                    }


                } else if (element instanceof ClassElement innerClass) {

                    if (innerClass.isAnnotationPresent(RootCommand.class)) {
                        //separate embedded command
                        var innerCmdAnn = innerClass.getAnnotation(RootCommand.class);
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
                                loadCommand(cmd, innerClass, subCommandAnn)
                        );
                    }

                }


            }

        }

        return cmd;
    }

    @SuppressWarnings("unchecked")
    private CommandPreProcessor<S> loadPreProcessorInstance(Class<? extends CommandPreProcessor<?>> clazz) {
        return (CommandPreProcessor<S>) config.getInstanceFactory().createInstance(config, clazz);
    }

    @SuppressWarnings("unchecked")
    private CommandPostProcessor<S> loadPostProcessorInstance(Class<? extends CommandPostProcessor<?>> clazz) {
        return (CommandPostProcessor<S>) config.getInstanceFactory().createInstance(config, clazz);
    }

    protected CommandPathway<S> loadPathway(
            @Nullable studio.mevera.imperat.command.Command<S> parentCmd,
            @NotNull studio.mevera.imperat.command.Command<S> loadedCmd,
            MethodElement method
    ) {
        System.out.println("parent='" + (parentCmd == null ? "N/A" : parentCmd.getName()) + "', loaded-cmd='" + loadedCmd.getName() + "', method='"
                                   + method.getName() + "'");
        MethodUsageData<S> usageData = loadParameters(method, parentCmd);
        var execution = MethodCommandExecutor.of(imperat, method, usageData.inheritedTotalParameters());

        studio.mevera.imperat.annotations.Description description = method.getAnnotation(studio.mevera.imperat.annotations.Description.class);
        Permission[] permissions = method.getAnnotationsByType(Permission.class);
        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        Async async = method.getAnnotation(Async.class);

        var builder = CommandPathway.<S>builder(method)
                              .inheritancePathway(usageData.inheritedPathway())
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
        var usage = builder
                       //.registerFlags(usageData.freeFlags)
                            .build(loadedCmd);

        Shortcut shortcutAnn = method.getAnnotation(Shortcut.class);
        if(shortcutAnn != null) {
            if(shortcutAnn.value().isEmpty() ) {
                throw new IllegalStateException("Shortcut value cannot be empty for method '" + method.getName() + "'");
            }

            if(shortcutAnn.value().contains(" ")) {
                throw new IllegalStateException("Shortcut value cannot contain spaces for method '" + method.getName() + "'");
            }

            var shortcut = loadPathwayShortcut(method, usageData, loadedCmd, usage, shortcutAnn);
            loadedCmd.addShortcut(shortcut);
        }

        return usage;
    }

    protected MethodUsageData<S> loadParameters(
            @NotNull MethodElement method,
            @Nullable studio.mevera.imperat.command.Command<S> parentCmd
    ) {
        var parents = getParenteralSequence(parentCmd);
        Map<ParameterElement, InheritedArgData<S>> inheritedArguments = new LinkedHashMap<>();

        CommandPathway<S> detectedInheritedPathway = null;
        Command<S> parent = null;
        for (var commandParent : parents) {
            //map<pe, InheritedArgData>
            for (CommandPathway<S> pathway : commandParent.getDedicatedPathways()) {
                if (methodParamsMatchesPathwayInSequence(pathway, method)) {

                    detectedInheritedPathway = pathway;
                    parent = commandParent;
                    break;
                }
            }
            if (detectedInheritedPathway != null) {
                break;
            }
            /*if (detectedInheritedPathway != null) {
                List<ParameterElement> methodParams = method.getParameters().stream()
                                                              .filter(p -> !isSenderParameter(p))
                                                              .toList();

                MethodElement inheritedPathwayMethod = detectedInheritedPathway.getMethodElement();
                assert inheritedPathwayMethod != null;

                List<Argument<S>> total = methodParams.stream()
                                                  .filter(p -> !isSenderParameter(p))
                                                  .map(this::loadParameter)
                                                  .toList();


                List<Argument<S>> personalParameters = methodParams.stream()
                                                               .map(this::loadParameter)
                                                               .filter(arg -> !total.contains(arg))
                                                               .toList();

                System.out.println("INHERITED-TOTALL= '" + String.join(" ", total.stream().map(Argument::format).toList()) + "'");
                return new MethodUsageData<>(detectedInheritedPathway, personalParameters, total);
            }
*/
        }

        List<ParameterElement> personalParameters = new ArrayList<>();

        for (ParameterElement methodParameter : method.getParameters()) {
            if (isSenderParameter(methodParameter)) {
                continue;
            }

            if (methodParameter.isAnnotationPresent(InheritedArg.class)) {
                //inherited pe
                if (detectedInheritedPathway == null) {
                    throw new IllegalStateException("Method '" + method.getName() + "' has @InheritedArg on parameter '" + methodParameter.getName()
                                                            + "' but no matching pathway for this parameter was found in all parents");
                }
                inheritedArguments.put(
                        methodParameter,
                        new InheritedArgData<>(
                                parent,
                                detectedInheritedPathway,
                                methodParameter,
                                loadParameter(methodParameter)
                        )
                );
            } else {
                //personal pe
                personalParameters.add(methodParameter);
            }
        }

        List<Argument<S>> personalArgs = personalParameters.stream()
                                                 .map(this::loadParameter)
                                                 .toList();

        //total = inherited + personal (in order)
        List<Argument<S>> totalArgs = new ArrayList<>();
        for (ParameterElement inherited : inheritedArguments.keySet()) {
            totalArgs.add(loadParameter(inherited));
        }

        totalArgs.addAll(personalArgs);


        return new MethodUsageData<>(null, personalArgs, totalArgs);
    }

    private boolean methodParamsMatchesPathwayInSequence(CommandPathway<S> pathway, MethodElement methodElement) {

        int methodParamSize = methodElement.getParameters().size() - 1; //excluding sender
        if (pathway.size() > methodParamSize) {
            return false;
        }

        List<ParameterElement> methodArgs = methodElement.getParameters().stream()
                                                    .filter(p -> !isSenderParameter(p))
                                                    .toList();

        MethodElement pathwayMethod = pathway.getMethodElement();
        if (pathwayMethod == null) {
            throw new IllegalStateException("Pathway method element is null for parent's pathway: '" + pathway.formatted() + "'");
        }

        List<ParameterElement> pathwayArgs = pathwayMethod.getParameters().stream()
                                                     .filter(p -> !isSenderParameter(p))
                                                     .toList();

        for (int i = 0; i < pathwayArgs.size(); i++) {
            ParameterElement methodArg = methodArgs.get(i);
            ParameterElement pathwayArg = pathwayArgs.get(i);

            if (notEqual(methodArg, pathwayArg)) {
                return false;
            }
        }

        return true;
    }

    private boolean notEqual(ParameterElement element, ParameterElement other) {
        Type type = element.getElement().getParameterizedType();
        Type otherType = other.getElement().getParameterizedType();
        return !element.getName().equals(other.getName()) || !type.equals(otherType);
    }

    protected studio.mevera.imperat.command.Command<S> loadPathwayShortcut(
            @NotNull MethodElement method,
            @NotNull MethodUsageData<S> methodUsageData,
            @NotNull studio.mevera.imperat.command.Command<S> originalCommand,
            @NotNull CommandPathway<S> originalUsage,
            @NotNull Shortcut shortcutAnn
    ) {

        String shortcutValue = config.replacePlaceholders(shortcutAnn.value());

        studio.mevera.imperat.command.Command<S> shortcut = originalCommand.getShortcut(shortcutValue);
        if(shortcut == null) {
            shortcut = studio.mevera.imperat.command.Command.create(imperat, shortcutValue, method)
                                                                   .setMetaPropertiesFromOtherCommand(originalCommand)
                                                                   .build();
        }

        CommandPathway<S> fabricated = CommandPathway.<S>builder(originalUsage.getMethodElement())
                                           .parameters(methodUsageData.inheritedTotalParameters())
                                           .execute(originalUsage.getExecution())
                                           .permission(originalUsage.getPermissionsData())
                                           .description(originalUsage.getDescription())
                                               .build(shortcut);

        shortcut.addPathway(fabricated);
        return shortcut;
    }

    private boolean isSenderParameter(ParameterElement parameter) {
        Type type = parameter.getElement().getParameterizedType();
        return imperat.canBeSender(type) || config.hasSourceResolver(type);
    }

    record InheritedArgData<S extends Source>(
            studio.mevera.imperat.command.Command<S> inheritedFrom,
            CommandPathway<S> inheritedPathway,
            ParameterElement parameter,
            Argument<S> argument
    ) {

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
        studio.mevera.imperat.annotations.SuggestionProvider
                suggestionProvider = parameter.getAnnotation(studio.mevera.imperat.annotations.SuggestionProvider.class);

        SuggestionProvider<S> suggestionProviderFunction = null;

        if (suggestAnnotation != null) {
            suggestionProviderFunction = SuggestionProvider.staticSuggestions(
                    config.replacePlaceholders(suggestAnnotation.value())
            );
        } else if (suggestionProvider != null) {
            try {
                suggestionProviderFunction = (SuggestionProvider<S>) config.getInstanceFactory().createInstance(config, suggestionProvider.value());
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Failed to instantiate suggestion provider '" + suggestionProvider.value().getName() + "' for parameter '"
                                + parameter.getName() + "'", exception);
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

        DefaultValueProvider defaultValueProvider = DefaultValueProvider.empty();
        if (optional) {
            Default defaultAnnotation = parameter.getAnnotation(Default.class);
            DefaultProvider provider = parameter.getAnnotation(DefaultProvider.class);
            try {
                defaultValueProvider = AnnotationHelper.deduceOptionalValueSupplier(parameter, defaultAnnotation, provider, defaultValueProvider);
            } catch (CommandException e) {
                ImperatDebugger.error(AnnotationHelper.class, "deduceOptionalValueSupplier", e);
            }
        }

        if (flag != null) {
            String[] flagAliases = flag.value();
            if (suggestAnnotation != null) {
                suggestionProviderFunction = SuggestionProvider.staticSuggestions(config.replacePlaceholders(suggestAnnotation.value()));
            }

            return AnnotationArgumentDecorator.decorate(
                    Argument.flag(name, type)
                            .suggestForInputValue(suggestionProviderFunction)
                            .aliases(getAllExceptFirst(flagAliases))
                            .flagDefaultInputValue(defaultValueProvider)
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
                optional, greedy, defaultValueProvider, suggestionProviderFunction,
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
            @Nullable CommandPathway<S> inheritedPathway,
            List<Argument<S>> personalParameters,
            List<Argument<S>> inheritedTotalParameters
    ) {

    }
}
