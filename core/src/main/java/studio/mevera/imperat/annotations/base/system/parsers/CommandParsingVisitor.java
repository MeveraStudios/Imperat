package studio.mevera.imperat.annotations.base.system.parsers;

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
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.PostProcessor;
import studio.mevera.imperat.annotations.PreProcessor;
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
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.annotations.base.system.AnnotationHandlerRegistry;
import studio.mevera.imperat.annotations.base.system.AnnotationProcessor;
import studio.mevera.imperat.annotations.base.system.InheritanceResolver;
import studio.mevera.imperat.annotations.base.system.ParameterInheritanceChain;
import studio.mevera.imperat.annotations.base.system.ParseContext;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
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
import studio.mevera.imperat.util.TypeWrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Rewritten command parsing visitor with extensible annotation processing
 * and automatic parameter inheritance resolution.
 */
@ApiStatus.Internal
@SuppressWarnings("unchecked")
public class CommandParsingVisitor<S extends Source> extends CommandClassVisitor<S, Set<Command<S>>> {

    private static final String VALUES_SEPARATION_CHAR = "\\|";

    private final ImperatConfig<S> config;
    private final InheritanceResolver<S> inheritanceResolver;
    private final AnnotationHandlerRegistry<S> processorRegistry;
    private final ParseContext<S> context;
    private final ElementSelector<MethodElement> methodSelector;
    private final CommandClassVisitor<S, Set<MethodThrowableResolver<?, S>>> errorHandlersVisitor;

    public CommandParsingVisitor(
            Imperat<S> imperat,
            AnnotationParser<S> parser,
            ElementSelector<MethodElement> methodSelector
    ) {
        super(imperat, parser, methodSelector);
        this.config = imperat.config();
        this.methodSelector = methodSelector;
        this.inheritanceResolver = new InheritanceResolver<>(config);
        this.processorRegistry = createProcessorRegistry();
        this.context = new ParseContext<>(imperat);
        this.errorHandlersVisitor = CommandClassVisitor.newThrowableParsingVisitor(imperat, parser);
    }

    /**
     * Creates and configures the annotation processor registry.
     * New annotations can be easily added here.
     */
    private AnnotationHandlerRegistry<S> createProcessorRegistry() {
        AnnotationHandlerRegistry<S> registry = new AnnotationHandlerRegistry<>();

        // Register core processors
        registry.register(new PermissionProcessor<>());
        registry.register(new DescriptionProcessor<>());
        registry.register(new CooldownProcessor<>());
        registry.register(new AsyncProcessor<>());
        registry.register(new PreProcessorProcessor<>());
        registry.register(new PostProcessorProcessor<>());

        return registry;
    }

    @Override
    public Set<Command<S>> visitCommandClass(@NotNull ClassElement clazz) throws Exception {
        Set<Command<S>> commands = new HashSet<>();

        // Validate root class constraints
        if (clazz.isRootClass()) {
            validateRootClass(clazz);
        }

        Annotation commandAnnotation = getCommandAnnotation(clazz);

        if (commandAnnotation != null) {
            // This is a command class - parse it
            Command<S> cmd = parseCommandClass(null, clazz, commandAnnotation);
            if (cmd != null) {
                commands.add(cmd);
            }
        } else {
            // No command annotation - look for nested command declarations
            parseNestedCommands(clazz, commands);
        }

        // Fail if any errors were collected
        context.failIfErrors();

        return commands;
    }

    private void validateRootClass(ClassElement clazz) {
        if (clazz.isAnnotationPresent(SubCommand.class)) {
            throw new IllegalStateException("Root command class cannot be annotated with @SubCommand");
        }
        if (AnnotationHelper.isAbnormalClass(clazz)) {
            throw new IllegalArgumentException("Abnormal root class: " + clazz.getName());
        }
    }

    private void parseNestedCommands(ClassElement clazz, Set<Command<S>> commands) throws Exception {
        for (ParseElement<?> element : clazz.getChildren()) {
            if (element instanceof ClassElement childClass) {
                RootCommand cmdAnn = childClass.getAnnotation(RootCommand.class);
                if (cmdAnn != null) {
                    Command<S> cmd = parseCommandClass(null, childClass, cmdAnn);
                    if (cmd != null) {
                        imperat.registerSimpleCommand(cmd);
                    }
                }
            } else if (element instanceof MethodElement method) {
                RootCommand cmdAnn = method.getAnnotation(RootCommand.class);
                if (cmdAnn != null) {
                    Command<S> cmd = parseCommandMethod(method, cmdAnn);
                    if (cmd != null) {
                        imperat.registerSimpleCommand(cmd);
                    }
                }
            }
        }
    }

    /**
     * Parses a class annotated with @RootCommand or @SubCommand
     */
    private @Nullable Command<S> parseCommandClass(
            @Nullable Command<S> parentCmd,
            ClassElement classElement,
            @NotNull Annotation annotation
    ) throws Exception {
        if (AnnotationHelper.isAbnormalClass(classElement)) {
            return null;
        }

        // Create command instance
        Command<S> cmd = createCommandInstance(annotation, classElement);
        if (cmd == null) {
            return null;
        }

        if (parentCmd != null) {
            cmd.setParent(parentCmd);
        }

        // Push to context stack
        context.pushCommand(cmd, classElement);

        try {
            // Process class-level annotations
            processClassAnnotations(classElement);

            // Parse error handlers
            parseErrorHandlers(classElement, cmd);

            // First pass: parse all @Execute methods (pathways)
            List<MethodElement> executeMethods = new ArrayList<>();
            List<MethodElement> subcommandMethods = new ArrayList<>();
            List<ClassElement> subcommandClasses = new ArrayList<>();

            categorizeChildren(classElement, executeMethods, subcommandMethods, subcommandClasses);

            // Process @Execute methods to create pathways
            for (MethodElement method : executeMethods) {
                if (methodSelector.canBeSelected(imperat, parser, method, true)) {
                    // Directly parse pathway here, no separate method
                    CommandPathway<S> pathway = parsePathway(cmd, method);
                    if (pathway != null) {
                        cmd.addPathway(pathway);
                    }
                }
            }

            // Process @SubCommand methods
            for (MethodElement method : subcommandMethods) {
                if (methodSelector.canBeSelected(imperat, parser, method, true)) {
                    parseSubCommandMethod(cmd, method);
                }
            }

            // Process @SubCommand classes
            for (ClassElement subClass : subcommandClasses) {
                parseSubCommandClass(cmd, subClass);
            }

            // Process embedded @RootCommand classes (independent root commands)
            for (ParseElement<?> child : classElement.getChildren()) {
                if (child instanceof ClassElement childClass &&
                            childClass.isAnnotationPresent(RootCommand.class) &&
                            !childClass.isAnnotationPresent(SubCommand.class)) {

                    Command<S> embedded = parseCommandClass(null, childClass,
                            Objects.requireNonNull(childClass.getAnnotation(RootCommand.class)));
                    if (embedded != null) {
                        imperat.registerSimpleCommand(embedded);
                    }
                }
            }

        } finally {
            context.popCommand();
        }

        return cmd;
    }

    private void categorizeChildren(
            ClassElement classElement,
            List<MethodElement> executeMethods,
            List<MethodElement> subcommandMethods,
            List<ClassElement> subcommandClasses
    ) {
        for (ParseElement<?> child : classElement.getChildren()) {
            if (child instanceof MethodElement method) {
                if (method.isAnnotationPresent(Execute.class)) {
                    executeMethods.add(method);
                } else if (method.isAnnotationPresent(SubCommand.class)) {
                    subcommandMethods.add(method);
                }
            } else if (child instanceof ClassElement childClass) {
                if (childClass.isAnnotationPresent(SubCommand.class)) {
                    subcommandClasses.add(childClass);
                }
            }
        }
    }

    /**
     * Parses a method annotated with @RootCommand (independent root command)
     */
    private @Nullable Command<S> parseCommandMethod(
            MethodElement method,
            @NotNull RootCommand annotation
    ) throws Exception {
        Command<S> cmd = createCommandFromMethod(annotation, method);
        if (cmd == null) {
            return null;
        }

        context.pushCommand(cmd, method.getParent());

        try {
            // Single pathway from this method
            if (methodSelector.canBeSelected(imperat, parser, method, true)) {
                CommandPathway<S> pathway = parsePathway(cmd, method);
                if (pathway != null) {
                    cmd.addPathway(pathway);
                }
            }
        } finally {
            context.popCommand();
        }

        return cmd;
    }

    /**
     * Parses a method annotated with @SubCommand
     */
    private void parseSubCommandMethod(Command<S> parentCmd, MethodElement method) throws Exception {
        SubCommand annotation = method.getAnnotation(SubCommand.class);
        if (annotation == null) {
            return;
        }

        String[] values = config.replacePlaceholders(annotation.value());
        Command<S> subcmd = Command.<S>create(imperat, parentCmd, parentCmd.getPosition() + 1, values[0])
                                    .aliases(Arrays.asList(values).subList(1, values.length))
                                    .build();

        subcmd.setParent(parentCmd);

        // FIX: Create a virtual class element for the subcommand method
        VirtualClassElement virtualElement = new VirtualClassElement(method.getParent(), method);

        // FIX: Push subcommand to context stack
        context.pushCommand(subcmd, virtualElement);

        try {
            // Now parsePathway will add to subcmd via context.buildAndAddPathway()
            if (methodSelector.canBeSelected(imperat, parser, method, true)) {
                CommandPathway<S> pathway = parsePathway(subcmd, method);
                // pathway is already added to subcmd by context.buildAndAddPathway()
            }
        } finally {
            context.popCommand();
        }

        parentCmd.addSubCommand(subcmd);

        context.queueSubcommand(new ParseContext.PendingSubcommand<>(
                subcmd,
                virtualElement,
                parentCmd
        ));
    }

    /**
     * Parses a class annotated with @SubCommand
     */
    private void parseSubCommandClass(Command<S> parentCmd, ClassElement classElement) throws Exception {
        SubCommand annotation = classElement.getAnnotation(SubCommand.class);
        if (annotation == null) {
            return;
        }

        Command<S> subcmd = parseCommandClass(parentCmd, classElement, annotation);
        if (subcmd != null) {
            parentCmd.addSubCommand(subcmd);
            context.queueSubcommand(new ParseContext.PendingSubcommand<>(
                    subcmd, classElement, parentCmd
            ));
        }
    }

    /**
     * Parses an @Execute method into a pathway
     */
    protected @Nullable CommandPathway<S> parsePathway(
            Command<S> cmd,
            MethodElement method
    ) throws Exception {
        // Resolve parameter inheritance
        InheritanceResolver.InheritanceResolution<S> resolution = resolveInheritance(method);
        context.setCurrentInheritanceChain(resolution.inheritanceChain());

        // Get inherited arguments from the chain
        List<Argument<S>> inheritedArgs = resolution.inheritanceChain().getChain()
                                                  .stream()
                                                  .map(ParameterInheritanceChain.InheritedParameter::getArgument)
                                                  .toList();

        // CRITICAL: Use the resolver's personal parameters, not all method parameters
        // The resolver already filtered out inherited params based on @InheritedArg annotation
        List<Argument<S>> personalParams = new ArrayList<>();

        for (InheritanceResolver.ParameterSignature sig : resolution.personalParameters()) {
            // Find matching ParameterElement
            ParameterElement param = findParameterBySignature(method, sig);
            if (param == null || isSenderParameter(param)) {
                continue;
            }

            Argument<S> argument = parseParameter(param);
            if (argument != null) {
                personalParams.add(argument);
            }
        }

        // Validate no conflicts
        resolution.inheritanceChain().validatePersonalParameters(personalParams);

        // Create pathway builder
        CommandPathway.Builder<S> builder = CommandPathway.<S>builder(method)
                                                    .inheritedParameters(inheritedArgs)
                                                    .parameters(personalParams);

        // Process method-level annotations via registry
        context.startPathway(builder);
        processMethodAnnotations(method);

        // Set execution with ALL parameters (inherited + personal)
        List<Argument<S>> allParams = new ArrayList<>(inheritedArgs);
        allParams.addAll(personalParams);
        MethodCommandExecutor<S> execution = MethodCommandExecutor.of(imperat, method, allParams);
        builder.execute(execution);

        // Handle @Execute specific attributes
        Execute executeAnn = method.getAnnotation(Execute.class);
        if (executeAnn != null) {
            builder.examples(Arrays.stream(executeAnn.examples())
                                     .map(config::replacePlaceholders)
                                     .toArray(String[]::new));
        }

        // Handle shortcuts
        Shortcut shortcutAnn = method.getAnnotation(Shortcut.class);
        if (shortcutAnn != null) {
            validateShortcut(shortcutAnn, method);
        }

        // Build and add pathway
        CommandPathway<S> pathway = context.buildAndAddPathway();

        // Store inheritance info
        cmd.registerInheritance(pathway, resolution.inheritanceChain());

        return pathway;
    }

    /**
     * Finds ParameterElement matching the signature from resolver
     */
    private @Nullable ParameterElement findParameterBySignature(
            MethodElement method,
            InheritanceResolver.ParameterSignature sig
    ) {
        for (ParameterElement param : method.getParameters()) {
            if (param.getName().equals(sig.getName()) &&
                        param.getType().equals(sig.getType())) {
                return param;
            }
        }
        return null;
    }

    /**
     * Resolves parameter inheritance for a method
     */
    private InheritanceResolver.InheritanceResolution<S> resolveInheritance(MethodElement method) throws Exception {
        Command<S> parent = context.getParentCommand();

        // Convert ParameterElements to signatures
        List<InheritanceResolver.ParameterSignature> signatures =
                method.getParameters().stream()
                        .filter(p -> !isSenderParameter(p))
                        .map(this::toSignature)
                        .toList();

        return inheritanceResolver.resolve(parent, signatures);
    }

    private InheritanceResolver.ParameterSignature toSignature(ParameterElement param) {
        return new InheritanceResolver.ParameterSignature() {
            @Override
            public String getName() {
                return param.getName();
            }

            @Override
            public Class<?> getType() {
                Type type = param.getType();
                return type instanceof Class<?> ? (Class<?>) type : Object.class;
            }

            @Override
            public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
                return param.getAnnotation(annotationClass);
            }
        };
    }

    /**
     * Parses a single parameter into an Argument
     */
    protected @Nullable Argument<S> parseParameter(ParameterElement param) {
        if (param.isContextResolved()) {
            return null;
        }

        // Check for flag/switch conflicts
        Flag flag = param.getAnnotation(Flag.class);
        Switch switchAnn = param.getAnnotation(Switch.class);

        if (flag != null && switchAnn != null) {
            throw new IllegalStateException("Parameter cannot have both @Flag and @Switch: " + param.getName());
        }

        // Resolve argument type
        ArgumentType<S, ?> type = resolveArgumentType(param);

        // Build base argument using Argument.of() factory
        String name = param.getName();
        boolean optional = param.isOptional();
        boolean greedy = param.getAnnotation(Greedy.class) != null;

        // Collect validators
        List<ArgValidator<S>> validators = parseValidators(param);

        // Create the argument
        Argument<S> argument = Argument.of(
                name,
                type,
                parsePermissions(param),
                parseDescription(param),
                optional,
                greedy,
                parseDefaultValue(param),
                parseSuggestions(param),
                validators
        );

        // Apply additional decorators
        applyParameterDecorators(argument, param);

        return argument;
    }

    @SuppressWarnings("unchecked")
    private <T> ArgumentType<S, T> resolveArgumentType(ParameterElement param) {
        ArgType argTypeAnn = param.getAnnotation(ArgType.class);

        if (argTypeAnn != null) {
            if (param.getAnnotation(Flag.class) != null || param.getAnnotation(Switch.class) != null) {
                throw new IllegalStateException("@ArgType cannot be used on flag/switch parameters");
            }
            return (ArgumentType<S, T>) config.getInstanceFactory()
                                                .createInstance(config, argTypeAnn.value());
        }

        TypeWrap<T> typeWrap = (TypeWrap<T>) TypeWrap.of(param.getElement().getParameterizedType());
        ArgumentType<S, T> type = (ArgumentType<S, T>) config.getArgumentType(typeWrap.getType());

        if (type == null) {
            throw new IllegalArgumentException("Unknown type: " + typeWrap.getType().getTypeName());
        }

        return type;
    }

    private void applyParameterDecorators(Argument<S> argument, ParameterElement param) {
        // Format
        Format formatAnn = param.getAnnotation(Format.class);
        if (formatAnn != null) {
            argument.setFormat(config.replacePlaceholders(formatAnn.value()));
        }
    }

    // ==================== Parameter Parsing Helpers ====================

    private Description parseDescription(ParameterElement param) {
        studio.mevera.imperat.annotations.Description descAnn =
                param.getAnnotation(studio.mevera.imperat.annotations.Description.class);
        if (descAnn != null) {
            return Description.of(config.replacePlaceholders(descAnn.value()));
        }
        return Description.EMPTY;
    }

    private PermissionsData parsePermissions(ParameterElement param) {
        Permission[] perms = param.getAnnotationsByType(Permission.class);
        PermissionsData data = PermissionsData.empty();
        for (Permission p : perms) {
            data.append(PermissionsData.fromText(config.replacePlaceholders(p.value())));
        }
        return data;
    }

    private @Nullable SuggestionProvider<S> parseSuggestions(ParameterElement param) {
        Suggest suggestAnn = param.getAnnotation(Suggest.class);
        if (suggestAnn != null) {
            return SuggestionProvider.staticSuggestions(
                    config.replacePlaceholders(suggestAnn.value())
            );
        }

        studio.mevera.imperat.annotations.SuggestionProvider providerAnn =
                param.getAnnotation(studio.mevera.imperat.annotations.SuggestionProvider.class);
        if (providerAnn != null) {
            try {
                return (SuggestionProvider<S>) config.getInstanceFactory()
                                                       .createInstance(config, providerAnn.value());
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to create suggestion provider for parameter: " + param.getName(), e);
            }
        }

        return null;
    }

    private DefaultValueProvider parseDefaultValue(ParameterElement param) {
        if (!param.isOptional()) {
            return DefaultValueProvider.empty();
        }

        Default defaultAnn = param.getAnnotation(Default.class);
        DefaultProvider providerAnn = param.getAnnotation(DefaultProvider.class);

        try {
            return AnnotationHelper.deduceOptionalValueSupplier(
                    param, defaultAnn, providerAnn, DefaultValueProvider.empty());
        } catch (CommandException e) {
            ImperatDebugger.error(AnnotationHelper.class, "deduceOptionalValueSupplier", e);
            return DefaultValueProvider.empty();
        }
    }

    private List<ArgValidator<S>> parseValidators(ParameterElement param) {
        List<ArgValidator<S>> validators = new ArrayList<>();

        // @Validators annotation
        Validators validatorsAnn = param.getAnnotation(Validators.class);
        if (validatorsAnn != null) {
            for (Class<? extends ArgValidator<?>> validatorClass : validatorsAnn.value()) {
                ArgValidator<S> validator = (ArgValidator<S>)
                                                    config.getInstanceFactory().createInstance(config, validatorClass);
                validators.add(validator);
            }
        }

        // @Values annotation (constrained values)
        Values valuesAnn = param.getAnnotation(Values.class);
        if (valuesAnn != null) {
            Set<String> values = Arrays.stream(valuesAnn.value())
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

            validators.add(new ConstrainedValueValidator<>(values, valuesAnn.caseSensitive()));
        }

        return validators;
    }

    // ==================== Annotation Processing ====================

    private <A extends Annotation> void processClassAnnotations(ClassElement classElement) {
        for (Annotation ann : classElement.getAnnotations()) {
            AnnotationProcessor<A, S> processor = processorRegistry.getProcessor((Class<A>) ann.annotationType());
            if (processor != null) {
                processor.process(context, (A) ann, classElement);
            }
        }
    }

    private <A extends Annotation> void processMethodAnnotations(MethodElement method) {
        for (Annotation annotation : method.getAnnotations()) {
            A ann = (A) annotation;
            AnnotationProcessor<A, S> processor = processorRegistry.getProcessor((Class<A>) ann.annotationType());
            if (processor != null) {
                processor.process(context, ann, method);
            }
        }
    }

    // ==================== Helper Methods ====================

    private Command<S> createCommandInstance(Annotation annotation, ParseElement<?> element) {
        if (annotation instanceof RootCommand cmdAnn) {
            String[] values = config.replacePlaceholders(cmdAnn.value());
            return Command.<S>create(imperat, null, 0, values[0])
                           .aliases(Arrays.asList(values).subList(1, values.length))
                           .build();
        } else if (annotation instanceof SubCommand subAnn) {
            String[] values = config.replacePlaceholders(subAnn.value());
            // Position will be set by parent
            return Command.<S>create(imperat, null, 0, values[0])
                           .aliases(Arrays.asList(values).subList(1, values.length))
                           .build();
        }
        return null;
    }

    private Command<S> createCommandFromMethod(RootCommand annotation, MethodElement method) {
        String[] values = config.replacePlaceholders(annotation.value());
        return Command.create(imperat, null, 0, values[0])
                       .aliases(Arrays.asList(values).subList(1, values.length))
                       .build();
    }

    private boolean isSenderParameter(ParameterElement param) {
        Type type = param.getElement().getParameterizedType();
        return imperat.canBeSender(type) || config.hasSourceResolver(type);
    }

    private <E extends Throwable> void parseErrorHandlers(ClassElement classElement, Command<S> cmd) throws Exception {
        var errorHandlers = errorHandlersVisitor.visitCommandClass(classElement);
        if (errorHandlers != null) {
            for (var handler : errorHandlers) {
                cmd.setThrowableResolver(
                        (Class<E>) handler.getExceptionType(),
                        (MethodThrowableResolver<E, S>) handler
                );
            }
        }
    }

    private void validateShortcut(Shortcut annotation, MethodElement method) {
        String value = annotation.value();
        if (value.isEmpty()) {
            throw new IllegalStateException("Shortcut value cannot be empty for: " + method.getName());
        }
        if (value.contains(" ")) {
            throw new IllegalStateException("Shortcut cannot contain spaces: " + method.getName());
        }
    }

    private Annotation getCommandAnnotation(ClassElement clazz) {
        RootCommand cmd = clazz.getAnnotation(RootCommand.class);
        if (cmd != null) {
            return cmd;
        }
        return clazz.getAnnotation(SubCommand.class);
    }

    // ==================== Annotation Processors ====================

    private static class PermissionProcessor<S extends Source>
            implements AnnotationProcessor<Permission, S> {

        @Override
        public void process(ParseContext<S> ctx, Permission ann, ParseElement<?> element) {
            String permLine = ctx.getConfig().replacePlaceholders(ann.value());
            PermissionsData data = PermissionsData.fromText(permLine);

            if (element instanceof ClassElement || element instanceof MethodElement) {
                ctx.currentCommand().setPermissionData(data);
            }
        }

        @Override
        public Class<Permission> getAnnotationType() {
            return Permission.class;
        }
    }

    private static class DescriptionProcessor<S extends Source>
            implements AnnotationProcessor<studio.mevera.imperat.annotations.Description, S> {

        @Override
        public void process(ParseContext<S> ctx,
                studio.mevera.imperat.annotations.Description ann,
                ParseElement<?> element) {
            String desc = ctx.getConfig().replacePlaceholders(ann.value());

            if (element instanceof ClassElement || element instanceof MethodElement) {
                ctx.currentCommand().describe(Description.of(desc));
            } else if (ctx.getCurrentPathwayBuilder() != null) {
                ctx.getCurrentPathwayBuilder().description(Description.of(desc));
            }
        }

        @Override
        public Class<studio.mevera.imperat.annotations.Description> getAnnotationType() {
            return studio.mevera.imperat.annotations.Description.class;
        }
    }

    private static class CooldownProcessor<S extends Source>
            implements AnnotationProcessor<Cooldown, S> {

        @Override
        public void process(ParseContext<S> ctx, Cooldown ann, ParseElement<?> element) {
            if (ctx.getCurrentPathwayBuilder() != null) {
                String perm = ann.permission().isEmpty() ? null : ann.permission();
                ctx.getCurrentPathwayBuilder().cooldown(ann.value(), ann.unit(), perm);
            }
        }

        @Override
        public Class<Cooldown> getAnnotationType() {
            return Cooldown.class;
        }
    }

    private static class AsyncProcessor<S extends Source>
            implements AnnotationProcessor<Async, S> {

        @Override
        public void process(ParseContext<S> ctx, Async ann, ParseElement<?> element) {
            if (ctx.getCurrentPathwayBuilder() != null) {
                ctx.getCurrentPathwayBuilder().coordinator(CommandCoordinator.async());
            }
        }

        @Override
        public Class<Async> getAnnotationType() {
            return Async.class;
        }
    }

    private static class PreProcessorProcessor<S extends Source>
            implements AnnotationProcessor<PreProcessor, S> {

        @Override
        public void process(ParseContext<S> ctx, PreProcessor ann, ParseElement<?> element) {
            for (Class<? extends CommandPreProcessor<?>> clazz : ann.value()) {
                CommandPreProcessor<S> processor = (CommandPreProcessor<S>)
                                                           ctx.getConfig().getInstanceFactory().createInstance(ctx.getConfig(), clazz);
                ctx.currentCommand().addPreProcessor(processor);
            }
        }

        @Override
        public Class<PreProcessor> getAnnotationType() {
            return PreProcessor.class;
        }
    }

    private static class PostProcessorProcessor<S extends Source>
            implements AnnotationProcessor<PostProcessor, S> {

        @Override
        public void process(ParseContext<S> ctx, PostProcessor ann, ParseElement<?> element) {
            for (Class<? extends CommandPostProcessor<?>> clazz : ann.value()) {
                CommandPostProcessor<S> processor = (CommandPostProcessor<S>)
                                                            ctx.getConfig().getInstanceFactory().createInstance(ctx.getConfig(), clazz);
                ctx.currentCommand().addPostProcessor(processor);
            }
        }

        @Override
        public Class<PostProcessor> getAnnotationType() {
            return PostProcessor.class;
        }
    }

    // ==================== Helper Classes ====================

    /**
     * Virtual ClassElement wrapper for method-based subcommands
     */
    private static class VirtualClassElement extends ClassElement {

        private final MethodElement method;

        public VirtualClassElement(ClassElement parent, MethodElement method) {
            super(parent.parser, parent, method.getElement().getDeclaringClass(),
                    parent.getObjectInstance());
            this.method = method;
        }

        @Override
        public Set<ParseElement<?>> getChildren() {
            Set<ParseElement<?>> children = new LinkedHashSet<>();
            children.add(method);
            return children;
        }
    }
}